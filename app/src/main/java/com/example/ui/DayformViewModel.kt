package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DayformDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.InsightsSummary
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.SubtaskDraft
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskFilter
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSortOption
import com.example.data.model.TaskWithDetails
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.DayformRepository
import com.example.notifications.ReminderScheduleStatus
import com.example.util.DateTimeUtil
import com.example.util.FeedbackHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val route: String, val label: String) {
    HOME("home", "Home"),
    TASKS("tasks", "Tasks"),
    INSIGHTS("insights", "Insights"),
    PROFILE("profile", "Profile")
}

sealed class UndoAction {
    data class TaskCompletionChanged(
        val taskId: Long,
        val previousCompleted: Boolean,
        val spawnedRecurringTaskId: Long?,
        val message: String
    ) : UndoAction()

    data class TaskDeleted(
        val task: TaskEntity,
        val subtasks: List<SubtaskEntity>,
        val message: String
    ) : UndoAction()
}

@androidx.compose.runtime.Immutable
data class DayformUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val allTasks: List<TaskWithDetails> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val completedAllTimeCount: Int = 0,
    val plannedTodayCount: Int = 0,
    val completedPlannedTodayCount: Int = 0,
    val pendingRemindersCount: Int = 0,
    val priorityTasks: List<TaskWithDetails> = emptyList(),
    val filteredTasks: List<TaskWithDetails> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val selectedCategoryId: Long? = null,
    val selectedSort: TaskSortOption = TaskSortOption.DUE_DATE,
    val isBoardLayout: Boolean = false,
    val showCalendarSubView: Boolean = false,
    val selectedCalendarEpochDay: Long = DateTimeUtil.currentLocalEpochDay(),
    val insights: InsightsSummary = InsightsSummary()
)

class DayformViewModel(
    application: Application,
    val repository: DayformRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = DayformRepository(
            context = application.applicationContext,
            dao = DayformDatabase.getInstance(application.applicationContext).dayformDao(),
            preferencesRepository = UserPreferencesRepository(application.applicationContext),
            database = DayformDatabase.getInstance(application.applicationContext)
        )
    )

    private val preferencesRepository = repository.preferencesRepository

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedSort = MutableStateFlow(TaskSortOption.DUE_DATE)
    private val _isBoardLayout = MutableStateFlow(false)
    private val _showCalendarSubView = MutableStateFlow(false)
    private val _selectedCalendarEpochDay = MutableStateFlow(DateTimeUtil.currentLocalEpochDay())

    private val _undoEvent = MutableSharedFlow<UndoAction>(extraBufferCapacity = 1)
    val undoEvent: SharedFlow<UndoAction> = _undoEvent.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _activeTab = MutableStateFlow(MainTab.HOME)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _selectedTaskIdForDetails = MutableStateFlow<Long?>(null)
    val selectedTaskIdForDetails: StateFlow<Long?> = _selectedTaskIdForDetails.asStateFlow()

    private val _isCreateEditSheetOpen = MutableStateFlow(false)
    val isCreateEditSheetOpen: StateFlow<Boolean> = _isCreateEditSheetOpen.asStateFlow()

    private val _editingTaskId = MutableStateFlow<Long?>(null)
    val editingTaskId: StateFlow<Long?> = _editingTaskId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureStarterCategories()
            repository.reconcileAllReminders()
        }
    }

    private data class FilterCriteria(
        val query: String,
        val filter: TaskFilter,
        val categoryId: Long?,
        val sort: TaskSortOption,
        val isBoard: Boolean,
        val showCalendar: Boolean,
        val calendarDay: Long
    )

    private data class TaskDataSnapshot(
        val tasks: List<TaskWithDetails>,
        val categories: List<CategoryEntity>,
        val completedAllTime: Int,
        val plannedTodayCount: Int,
        val completedPlannedTodayCount: Int,
        val pendingRemindersCount: Int,
        val priorityTasks: List<TaskWithDetails>,
        val insights: InsightsSummary,
        val todayEpochDay: Long,
        val currentMinutes: Int
    )

    private val filterCriteriaFlow = combine(
        _searchQuery,
        _selectedFilter,
        _selectedCategoryId,
        _selectedSort,
        combine(_isBoardLayout, _showCalendarSubView, _selectedCalendarEpochDay) { b, c, d ->
            Triple(b, c, d)
        }
    ) { query, filter, catId, sort, triple ->
        FilterCriteria(
            query = query,
            filter = filter,
            categoryId = catId,
            sort = sort,
            isBoard = triple.first,
            showCalendar = triple.second,
            calendarDay = triple.third
        )
    }

    private val taskDataSnapshotFlow = combine(
        repository.tasksWithDetailsFlow,
        repository.categoriesFlow
    ) { tasks, categories ->
        val nowMillis = System.currentTimeMillis()
        val today = DateTimeUtil.currentLocalEpochDay(nowMillis)
        val currentMinutes = DateTimeUtil.currentMinutesFromMidnight(nowMillis)

        var completedAllTime = 0
        var plannedTodayCount = 0
        var completedPlannedTodayCount = 0
        var pendingRemindersCount = 0
        val incompleteTasks = ArrayList<TaskWithDetails>(tasks.size)

        for (item in tasks) {
            val t = item.task
            if (t.isCompleted) {
                completedAllTime++
            } else {
                incompleteTasks.add(item)
                if (t.reminderEnum != ReminderOffset.NONE) {
                    pendingRemindersCount++
                }
            }
            if (t.dueDateEpochDay == today) {
                plannedTodayCount++
                if (t.isCompleted) {
                    completedPlannedTodayCount++
                }
            }
        }

        // Priority tasks: most urgent incomplete tasks first (overdue or urgent/high priority or due today/soon)
        val priorityTasks = incompleteTasks.sortedWith(
            compareByDescending<TaskWithDetails> {
                DateTimeUtil.isTaskOverdue(
                    dueDateEpochDay = it.task.dueDateEpochDay,
                    dueTimeMinutes = it.task.dueTimeMinutes,
                    isCompleted = false,
                    nowMillis = nowMillis,
                    todayEpochDay = today,
                    currentMinutes = currentMinutes
                )
            }
                .thenBy { it.task.dueDateEpochDay }
                .thenByDescending { it.task.priorityEnum.sortWeight }
                .thenBy { it.task.dueTimeMinutes ?: Int.MAX_VALUE }
        )

        val insights = repository.computeInsights(tasks, categories)

        TaskDataSnapshot(
            tasks = tasks,
            categories = categories,
            completedAllTime = completedAllTime,
            plannedTodayCount = plannedTodayCount,
            completedPlannedTodayCount = completedPlannedTodayCount,
            pendingRemindersCount = pendingRemindersCount,
            priorityTasks = priorityTasks,
            insights = insights,
            todayEpochDay = today,
            currentMinutes = currentMinutes
        )
    }

    val uiState: StateFlow<DayformUiState> = combine(
        taskDataSnapshotFlow,
        repository.userSettingsFlow,
        filterCriteriaFlow
    ) { snapshot, settings, criteria ->
        val today = snapshot.todayEpochDay
        val currentMinutes = snapshot.currentMinutes
        val tasks = snapshot.tasks

        // Apply filters for AllTasksScreen
        val queryTrimmed = criteria.query.trim().lowercase()
        val filtered = tasks.filter { item ->
            val matchesQuery = if (queryTrimmed.isEmpty()) {
                true
            } else {
                item.task.title.lowercase().contains(queryTrimmed) ||
                    item.task.description.lowercase().contains(queryTrimmed) ||
                    (item.category?.name?.lowercase()?.contains(queryTrimmed) == true) ||
                    item.subtasks.any { it.title.lowercase().contains(queryTrimmed) }
            }

            val matchesCategory = criteria.categoryId == null || item.task.categoryId == criteria.categoryId

            val matchesTabFilter = when (criteria.filter) {
                TaskFilter.ALL -> true
                TaskFilter.TODAY -> item.task.dueDateEpochDay == today
                TaskFilter.UPCOMING -> !item.task.isCompleted && item.task.dueDateEpochDay >= today
                TaskFilter.COMPLETED -> item.task.isCompleted
                TaskFilter.OVERDUE -> DateTimeUtil.isTaskOverdue(
                    dueDateEpochDay = item.task.dueDateEpochDay,
                    dueTimeMinutes = item.task.dueTimeMinutes,
                    isCompleted = item.task.isCompleted,
                    todayEpochDay = today,
                    currentMinutes = currentMinutes
                )
            }

            matchesQuery && matchesCategory && matchesTabFilter
        }.let { list ->
            when (criteria.sort) {
                TaskSortOption.DUE_DATE -> list.sortedWith(
                    compareBy<TaskWithDetails> { it.task.isCompleted }
                        .thenBy { it.task.dueDateEpochDay }
                        .thenBy { it.task.dueTimeMinutes ?: Int.MAX_VALUE }
                        .thenByDescending { it.task.priorityEnum.sortWeight }
                )
                TaskSortOption.PRIORITY -> list.sortedWith(
                    compareBy<TaskWithDetails> { it.task.isCompleted }
                        .thenByDescending { it.task.priorityEnum.sortWeight }
                        .thenBy { it.task.dueDateEpochDay }
                )
                TaskSortOption.NEWEST -> list.sortedWith(
                    compareBy<TaskWithDetails> { it.task.isCompleted }
                        .thenByDescending { it.task.createdAt }
                )
                TaskSortOption.ALPHABETICAL -> list.sortedWith(
                    compareBy<TaskWithDetails> { it.task.isCompleted }
                        .thenBy { it.task.title.lowercase() }
                )
            }
        }

        DayformUiState(
            isLoading = false,
            settings = settings,
            allTasks = tasks,
            categories = snapshot.categories,
            completedAllTimeCount = snapshot.completedAllTime,
            plannedTodayCount = snapshot.plannedTodayCount,
            completedPlannedTodayCount = snapshot.completedPlannedTodayCount,
            pendingRemindersCount = snapshot.pendingRemindersCount,
            priorityTasks = snapshot.priorityTasks,
            filteredTasks = filtered,
            searchQuery = criteria.query,
            selectedFilter = criteria.filter,
            selectedCategoryId = criteria.categoryId,
            selectedSort = criteria.sort,
            isBoardLayout = criteria.isBoard,
            showCalendarSubView = criteria.showCalendar,
            selectedCalendarEpochDay = criteria.calendarDay,
            insights = snapshot.insights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DayformUiState()
    )

    // Navigation & Sheet State Actions
    fun selectTab(tab: MainTab) {
        _selectedTaskIdForDetails.value = null
        _activeTab.value = tab
    }

    fun openTasksWithFilter(filter: TaskFilter, showCalendar: Boolean = false) {
        _selectedFilter.value = filter
        _showCalendarSubView.value = showCalendar
        _selectedTaskIdForDetails.value = null
        _activeTab.value = MainTab.TASKS
    }

    fun openTaskDetails(taskId: Long) {
        _selectedTaskIdForDetails.value = taskId
    }

    fun closeTaskDetails() {
        _selectedTaskIdForDetails.value = null
    }

    fun openCreateTaskSheet(initialEpochDay: Long? = null) {
        if (initialEpochDay != null) {
            _selectedCalendarEpochDay.value = initialEpochDay
        }
        _editingTaskId.value = null
        _isCreateEditSheetOpen.value = true
    }

    fun openEditTaskSheet(taskId: Long) {
        _editingTaskId.value = taskId
        _isCreateEditSheetOpen.value = true
    }

    fun closeCreateEditSheet() {
        _isCreateEditSheetOpen.value = false
        _editingTaskId.value = null
    }

    // Filter & Search Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTaskFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSortOption(sort: TaskSortOption) {
        _selectedSort.value = sort
    }

    fun toggleBoardLayout() {
        _isBoardLayout.value = !_isBoardLayout.value
    }

    fun setShowCalendarSubView(show: Boolean) {
        _showCalendarSubView.value = show
    }

    fun selectCalendarDate(epochDay: Long) {
        _selectedCalendarEpochDay.value = epochDay
    }

    // Onboarding
    fun completeOnboarding(displayName: String) {
        viewModelScope.launch {
            repository.initializeFirstLaunch(displayName)
        }
    }

    // Task CRUD
    suspend fun saveTask(
        existingTaskId: Long?,
        title: String,
        description: String,
        dueDateEpochDay: Long,
        dueTimeMinutes: Int?,
        priority: TaskPriority,
        categoryId: Long?,
        reminderOffset: ReminderOffset,
        customReminderMillis: Long?,
        recurrenceType: RecurrenceType,
        recurrenceWeekdays: String,
        subtaskDrafts: List<SubtaskDraft>
    ): Result<Long> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            _toastMessage.emit("Please enter a task title")
            return Result.failure(IllegalArgumentException("Please enter a task title"))
        }
        val detailedResult = repository.saveTaskWithSubtasksDetailed(
            existingTaskId = existingTaskId,
            title = cleanTitle,
            description = description,
            dueDateEpochDay = dueDateEpochDay,
            dueTimeMinutes = dueTimeMinutes,
            priority = priority,
            categoryId = categoryId,
            reminderOffset = reminderOffset,
            customReminderMillis = customReminderMillis,
            recurrenceType = recurrenceType,
            recurrenceWeekdays = recurrenceWeekdays,
            subtaskDrafts = subtaskDrafts
        )
        if (detailedResult.isSuccess) {
            val outcome = detailedResult.getOrThrow()
            val settings = uiState.value.settings
            if (settings.hapticFeedbackEnabled) {
                FeedbackHelper.triggerHaptic(getApplication(), isHeavy = false)
            }
            if (outcome.reminderWarningMessage == null) {
                _isCreateEditSheetOpen.value = false
                _editingTaskId.value = null
                val baseLabel = if (existingTaskId != null) "Task updated" else "Task saved"
                if (outcome.reminderStatus == ReminderScheduleStatus.SKIPPED_PAST_TRIGGER) {
                    _toastMessage.emit("$baseLabel • Reminder time has already passed")
                } else {
                    _toastMessage.emit(baseLabel)
                }
            } else {
                _toastMessage.emit(outcome.reminderWarningMessage)
            }
        }
        return detailedResult.map { it.taskId }
    }

    suspend fun retryTaskReminder(taskId: Long): Result<Boolean> {
        val res = repository.retryTaskReminder(taskId)
        if (res.isSuccess) {
            if (res.getOrDefault(false)) {
                _toastMessage.emit("Reminder scheduled")
            } else {
                _toastMessage.emit("Reminder time has already passed — no alarm scheduled")
            }
        }
        return res
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            val settings = uiState.value.settings
            if (newCompleted) {
                FeedbackHelper.triggerCompletionFeedback(
                    context = getApplication(),
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    soundEnabled = settings.soundFeedbackEnabled
                )
            } else if (settings.hapticFeedbackEnabled) {
                FeedbackHelper.triggerHaptic(getApplication(), isHeavy = false)
            }

            val spawnedId = repository.setTaskCompleted(
                taskId = task.id,
                completed = newCompleted,
                spawnRecurringIfApplicable = true
            )

            val msg = if (newCompleted) {
                if (spawnedId != null) "Completed '${task.title}' • Next occurrence scheduled"
                else "Completed '${task.title}'"
            } else {
                "Reopened '${task.title}'"
            }

            _undoEvent.emit(
                UndoAction.TaskCompletionChanged(
                    taskId = task.id,
                    previousCompleted = task.isCompleted,
                    spawnedRecurringTaskId = spawnedId,
                    message = msg
                )
            )
        }
    }

    fun performUndo(action: UndoAction) {
        viewModelScope.launch {
            when (action) {
                is UndoAction.TaskCompletionChanged -> {
                    repository.undoTaskCompletion(
                        taskId = action.taskId,
                        previousCompleted = action.previousCompleted,
                        spawnedRecurringTaskId = action.spawnedRecurringTaskId
                    )
                }
                is UndoAction.TaskDeleted -> {
                    repository.restoreDeletedTask(action.task, action.subtasks)
                }
            }
        }
    }

    fun updateTaskDescription(taskId: Long, description: String) {
        viewModelScope.launch {
            repository.updateTaskDescription(taskId, description)
            _toastMessage.emit("Description saved")
        }
    }

    fun updateTaskPriority(taskId: Long, priority: TaskPriority) {
        viewModelScope.launch {
            repository.updateTaskPriority(taskId, priority)
            _toastMessage.emit("Priority set to ${priority.label}")
        }
    }

    fun updateTaskCategory(taskId: Long, categoryId: Long?) {
        viewModelScope.launch {
            repository.updateTaskCategory(taskId, categoryId)
            _toastMessage.emit("Category updated")
        }
    }

    fun updateTaskReminder(
        taskId: Long,
        reminderOffset: ReminderOffset,
        customReminderMillis: Long? = null
    ) {
        viewModelScope.launch {
            val report = repository.updateTaskReminder(taskId, reminderOffset, customReminderMillis)
            val msg = when (report.status) {
                ReminderScheduleStatus.SCHEDULED -> "Reminder scheduled: ${reminderOffset.label}"
                ReminderScheduleStatus.NONE_REQUESTED -> "Reminder: ${reminderOffset.label}"
                ReminderScheduleStatus.SKIPPED_PAST_TRIGGER ->
                    "Reminder saved (${reminderOffset.label}), but trigger time has already passed"
                ReminderScheduleStatus.BLOCKED_APP_PREFERENCE ->
                    "Reminder saved, but notifications are disabled in Settings"
                ReminderScheduleStatus.BLOCKED_OS_PERMISSION ->
                    "Reminder saved, but Android notification permission is not granted"
                ReminderScheduleStatus.FAILED_RETRYABLE ->
                    report.userMessage ?: "Reminder saved, but alarm registration failed"
            }
            _toastMessage.emit(msg)
        }
    }

    fun duplicateTask(taskId: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateTask(taskId)
            if (newId != null) {
                _toastMessage.emit("Task duplicated")
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteTask(taskId)
            if (_selectedTaskIdForDetails.value == taskId) {
                _selectedTaskIdForDetails.value = null
            }
            if (deleted != null) {
                _undoEvent.emit(
                    UndoAction.TaskDeleted(
                        task = deleted.first,
                        subtasks = deleted.second,
                        message = "Deleted '${deleted.first.title}'"
                    )
                )
            }
        }
    }

    // Subtask actions
    fun addSubtask(taskId: Long, title: String) {
        viewModelScope.launch {
            repository.addSubtask(taskId, title)
            if (uiState.value.settings.hapticFeedbackEnabled) {
                FeedbackHelper.triggerHaptic(getApplication(), isHeavy = false)
            }
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            if (!subtask.isCompleted) {
                FeedbackHelper.triggerCompletionFeedback(
                    context = getApplication(),
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    soundEnabled = settings.soundFeedbackEnabled
                )
            } else if (settings.hapticFeedbackEnabled) {
                FeedbackHelper.triggerHaptic(getApplication(), isHeavy = false)
            }
            repository.toggleSubtask(subtask)
        }
    }

    fun updateSubtaskTitle(subtask: SubtaskEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateSubtaskTitle(subtask, newTitle)
        }
    }

    fun deleteSubtask(subtaskId: Long) {
        viewModelScope.launch {
            repository.deleteSubtask(subtaskId)
        }
    }

    fun moveSubtask(taskId: Long, subtaskId: Long, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveSubtask(taskId, subtaskId, moveUp)
        }
    }

    // Categories
    fun createCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val id = repository.createCategory(name, colorHex)
            if (id > 0L) {
                _toastMessage.emit("Category '$name' created")
            }
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String, newColorHex: String) {
        viewModelScope.launch {
            repository.updateCategory(category, newName, newColorHex)
            _toastMessage.emit("Category updated")
        }
    }

    fun deleteCategory(categoryId: Long, moveTasksToCategoryId: Long? = null) {
        viewModelScope.launch {
            if (_selectedCategoryId.value == categoryId) {
                _selectedCategoryId.value = null
            }
            repository.deleteCategory(categoryId, moveTasksToCategoryId)
            _toastMessage.emit("Category deleted")
        }
    }

    // Settings & Personalization
    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateDisplayName(name)
            _toastMessage.emit("Display name updated")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            repository.reconcileAllReminders()
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setSoundFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSoundFeedbackEnabled(enabled)
        }
    }

    fun setWeekStartsOnMonday(monday: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWeekStartsOnMonday(monday)
        }
    }

    fun setUse24HourFormat(use24Hour: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUse24HourFormat(use24Hour)
        }
    }

    fun setHideCompletedSubtasks(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHideCompletedSubtasks(hide)
        }
    }

    fun emitMessage(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }
}
