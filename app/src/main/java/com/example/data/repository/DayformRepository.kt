package com.example.data.repository

import android.content.Context
import com.example.data.local.DayformDao
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryTaskStat
import com.example.data.model.DayActivityStat
import com.example.data.model.InsightsSummary
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.SubtaskDraft
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskWithDetails
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.notifications.ReminderScheduleReport
import com.example.notifications.ReminderScheduleStatus
import com.example.notifications.ReminderScheduler
import com.example.util.DateTimeUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

import androidx.room.withTransaction
import com.example.data.local.DayformDatabase

data class TaskSaveOutcome(
    val taskId: Long,
    val reminderScheduled: Boolean,
    val reminderWarningMessage: String? = null,
    val reminderStatus: ReminderScheduleStatus = ReminderScheduleStatus.NONE_REQUESTED,
    val reminderNoticeMessage: String? = null
)

class DayformRepository(
    private val context: Context,
    private val dao: DayformDao,
    val preferencesRepository: UserPreferencesRepository,
    private val database: DayformDatabase? = null
) {

    companion object {
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024 // 10 MB upper bound
        const val MAX_BACKUP_ENTITIES = 10_000
        private val HEX_COLOR_REGEX = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    }

    /**
     * Optional hook invoked inside the active Room transaction AFTER task and subtask writes
     * have executed, before transaction commit. Allows deterministic mid-transaction rollback testing.
     */
    var midTransactionHook: (suspend (Long) -> Unit)? = null

    /**
     * Optional hook invoked inside the active Room backup import transaction AFTER the given
     * number of tasks have been inserted, before transaction commit.
     */
    var midImportTransactionHook: (suspend (Int) -> Unit)? = null

    /**
     * Post-commit reminder scheduler handler. Defaults to [ReminderScheduler.scheduleTaskReminder].
     */
    var reminderScheduleHandler: (Context, TaskEntity, Boolean) -> Result<Boolean> =
        ReminderScheduler::scheduleTaskReminder

    private val completionMutex = Mutex()
    private val importMutex = Mutex()

    private val _isImportInProgress = MutableStateFlow(false)
    val isImportInProgress: StateFlow<Boolean> = _isImportInProgress.asStateFlow()

    @Volatile
    var lastSaveOutcome: TaskSaveOutcome? = null
        private set

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        val db = database
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeAllCategories()

    val userSettingsFlow: Flow<UserSettings> = preferencesRepository.userSettingsFlow

    val tasksWithDetailsFlow: Flow<List<TaskWithDetails>> = combine(
        dao.observeAllTasks(),
        dao.observeAllSubtasks(),
        dao.observeAllCategories()
    ) { tasks, subtasks, categories ->
        val subtasksByTask = subtasks.groupBy { it.taskId }
        val categoryById = categories.associateBy { it.id }
        tasks.map { task ->
            TaskWithDetails(
                task = task,
                subtasks = (subtasksByTask[task.id] ?: emptyList()).sortedWith(
                    compareBy<SubtaskEntity> { it.sortOrder }.thenBy { it.id }
                ),
                category = task.categoryId?.let { categoryById[it] }
            )
        }
    }

    suspend fun ensureStarterCategories(): List<CategoryEntity> {
        val existing = dao.getAllCategoriesOnce()
        if (existing.isNotEmpty()) return existing

        val starters = listOf(
            CategoryEntity(name = "Personal", colorHex = "#DFFF65", sortOrder = 0),
            CategoryEntity(name = "Work", colorHex = "#C279F8", sortOrder = 1),
            CategoryEntity(name = "Study", colorHex = "#5CD6FF", sortOrder = 2),
            CategoryEntity(name = "Creative", colorHex = "#FF9F31", sortOrder = 3)
        )
        dao.insertCategories(starters)
        return dao.getAllCategoriesOnce()
    }

    suspend fun initializeFirstLaunch(
        displayName: String
    ) {
        ensureStarterCategories()
        preferencesRepository.completeOnboarding(displayName)
    }

    suspend fun saveTaskWithSubtasksDetailed(
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
    ): Result<TaskSaveOutcome> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty"))
        }

        val now = System.currentTimeMillis()
        val settings = preferencesRepository.userSettingsFlow.first()

        val (savedTaskId, entityToSchedule) = try {
            runInTransaction {
                val resultPair = if (existingTaskId != null && existingTaskId > 0L) {
                    val current = dao.getTaskById(existingTaskId)
                        ?: throw NoSuchElementException("Task with ID $existingTaskId does not exist")

                    val resolvedSeriesId = when {
                        recurrenceType == RecurrenceType.NEVER -> null
                        current.recurrenceSeriesId != null -> current.recurrenceSeriesId
                        else -> "series_${existingTaskId}_$now"
                    }
                    val resolvedAnchorDay = when {
                        recurrenceType != RecurrenceType.MONTHLY -> null
                        current.dueDateEpochDay != dueDateEpochDay || current.recurrenceAnchorDay == null ->
                            DateTimeUtil.getDayOfMonth(dueDateEpochDay)
                        else -> current.recurrenceAnchorDay
                    }

                    val updated = current.copy(
                        title = cleanTitle,
                        description = description.trim(),
                        updatedAt = now,
                        dueDateEpochDay = dueDateEpochDay,
                        dueTimeMinutes = dueTimeMinutes,
                        priority = priority.name,
                        categoryId = categoryId,
                        reminderOffset = reminderOffset.name,
                        customReminderMillis = customReminderMillis,
                        recurrenceType = recurrenceType.name,
                        recurrenceWeekdays = recurrenceWeekdays,
                        isSampleData = false,
                        recurrenceSeriesId = resolvedSeriesId,
                        recurrenceAnchorDay = resolvedAnchorDay
                    )
                    dao.updateTask(updated)

                    val existingSubtasks = dao.getSubtasksForTaskOnce(existingTaskId)
                    val validDrafts = subtaskDrafts
                        .map { it.copy(title = it.title.trim()) }
                        .filter { it.title.isNotEmpty() }

                    val retainedDraftIds = validDrafts.mapNotNull { if (it.id > 0L) it.id else null }.toSet()

                    // Delete removed subtasks
                    val subtasksToDelete = existingSubtasks.filter { it.id !in retainedDraftIds }
                    subtasksToDelete.forEach { dao.deleteSubtaskById(it.id) }

                    // Diff: update changed existing, preserve unchanged without db churn, insert new
                    val existingMap = existingSubtasks.associateBy { it.id }
                    val toUpdate = mutableListOf<SubtaskEntity>()
                    val toInsert = mutableListOf<SubtaskEntity>()

                    validDrafts.forEachIndexed { index, draft ->
                        if (draft.id > 0L && existingMap.containsKey(draft.id)) {
                            val existing = existingMap.getValue(draft.id)
                            if (existing.title != draft.title ||
                                existing.isCompleted != draft.isCompleted ||
                                existing.sortOrder != index
                            ) {
                                toUpdate.add(
                                    existing.copy(
                                        title = draft.title,
                                        isCompleted = draft.isCompleted,
                                        sortOrder = index
                                    )
                                )
                            }
                        } else {
                            toInsert.add(
                                SubtaskEntity(
                                    taskId = existingTaskId,
                                    title = draft.title,
                                    isCompleted = draft.isCompleted,
                                    sortOrder = index
                                )
                            )
                        }
                    }

                    if (toUpdate.isNotEmpty()) {
                        dao.updateSubtasks(toUpdate)
                    }
                    if (toInsert.isNotEmpty()) {
                        dao.insertSubtasks(toInsert)
                    }

                    existingTaskId to updated
                } else {
                    val initialAnchorDay = if (recurrenceType == RecurrenceType.MONTHLY) {
                        DateTimeUtil.getDayOfMonth(dueDateEpochDay)
                    } else {
                        null
                    }
                    val newTask = TaskEntity(
                        title = cleanTitle,
                        description = description.trim(),
                        createdAt = now,
                        updatedAt = now,
                        dueDateEpochDay = dueDateEpochDay,
                        dueTimeMinutes = dueTimeMinutes,
                        isCompleted = false,
                        completedAt = null,
                        priority = priority.name,
                        categoryId = categoryId,
                        reminderOffset = reminderOffset.name,
                        customReminderMillis = customReminderMillis,
                        recurrenceType = recurrenceType.name,
                        recurrenceWeekdays = recurrenceWeekdays,
                        sortOrder = 0,
                        isSampleData = false,
                        recurrenceSeriesId = if (recurrenceType != RecurrenceType.NEVER) {
                            "series_new_${now}_${cleanTitle.hashCode()}"
                        } else {
                            null
                        },
                        recurrenceAnchorDay = initialAnchorDay
                    )
                    val newId = dao.insertTask(newTask)
                    val validDrafts = subtaskDrafts
                        .map { it.copy(title = it.title.trim()) }
                        .filter { it.title.isNotEmpty() }

                    val subtaskEntities = validDrafts.mapIndexed { index, draft ->
                        SubtaskEntity(
                            taskId = newId,
                            title = draft.title,
                            isCompleted = draft.isCompleted,
                            sortOrder = index
                        )
                    }
                    if (subtaskEntities.isNotEmpty()) {
                        dao.insertSubtasks(subtaskEntities)
                    }
                    val savedTask = newTask.copy(id = newId)
                    newId to savedTask
                }

                // Invoke optional mid-transaction verification hook AFTER SQLite writes
                midTransactionHook?.invoke(resultPair.first)
                resultPair
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // Post-transaction reminder scheduling: failure is reported separately while keeping the task committed
        val isDefaultScheduler = reminderScheduleHandler == ReminderScheduler::scheduleTaskReminder
        val detailedReport: ReminderScheduleReport? = if (isDefaultScheduler) {
            ReminderScheduler.scheduleTaskReminderDetailed(
                context = context,
                task = entityToSchedule,
                notificationsEnabled = settings.notificationsEnabled
            )
        } else {
            null
        }

        val reminderResult: Result<Boolean> = if (detailedReport != null) {
            when (detailedReport.status) {
                ReminderScheduleStatus.SCHEDULED -> Result.success(true)
                ReminderScheduleStatus.NONE_REQUESTED,
                ReminderScheduleStatus.SKIPPED_PAST_TRIGGER -> Result.success(false)
                ReminderScheduleStatus.BLOCKED_APP_PREFERENCE,
                ReminderScheduleStatus.BLOCKED_OS_PERMISSION -> Result.failure(
                    IllegalStateException(detailedReport.userMessage ?: "Reminder blocked by notification settings.")
                )
                ReminderScheduleStatus.FAILED_RETRYABLE -> Result.failure(
                    IllegalArgumentException(detailedReport.userMessage ?: "Reminder could not be scheduled.")
                )
            }
        } else {
            try {
                reminderScheduleHandler(context, entityToSchedule, settings.notificationsEnabled)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val resolvedStatus = detailedReport?.status ?: when {
            reminderResult.isFailure -> ReminderScheduleStatus.FAILED_RETRYABLE
            reminderResult.getOrDefault(false) -> ReminderScheduleStatus.SCHEDULED
            entityToSchedule.reminderEnum == ReminderOffset.NONE || entityToSchedule.isCompleted ->
                ReminderScheduleStatus.NONE_REQUESTED
            else -> ReminderScheduleStatus.SKIPPED_PAST_TRIGGER
        }

        val outcome = if (reminderResult.isSuccess) {
            val scheduled = reminderResult.getOrDefault(false)
            val notice = if (resolvedStatus == ReminderScheduleStatus.SKIPPED_PAST_TRIGGER) {
                detailedReport?.userMessage ?: "Reminder time has already passed, so no alarm was scheduled."
            } else {
                null
            }
            TaskSaveOutcome(
                taskId = savedTaskId,
                reminderScheduled = scheduled,
                reminderWarningMessage = null,
                reminderStatus = resolvedStatus,
                reminderNoticeMessage = notice
            )
        } else {
            val reason = reminderResult.exceptionOrNull()?.localizedMessage
                ?: "Reminder alarm could not be registered with the system."
            TaskSaveOutcome(
                taskId = savedTaskId,
                reminderScheduled = false,
                reminderWarningMessage = "Task saved, but reminder failed: $reason",
                reminderStatus = resolvedStatus,
                reminderNoticeMessage = reason
            )
        }
        lastSaveOutcome = outcome
        return Result.success(outcome)
    }

    suspend fun saveTaskWithSubtasks(
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
        return saveTaskWithSubtasksDetailed(
            existingTaskId = existingTaskId,
            title = title,
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
        ).map { it.taskId }
    }

    suspend fun retryTaskReminder(taskId: Long): Result<Boolean> {
        val task = dao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task with ID $taskId not found"))
        val settings = preferencesRepository.userSettingsFlow.first()
        val res = try {
            reminderScheduleHandler(context, task, settings.notificationsEnabled)
        } catch (e: Exception) {
            Result.failure(e)
        }
        if (res.isSuccess) {
            val scheduled = res.getOrDefault(false)
            val status = when {
                scheduled -> ReminderScheduleStatus.SCHEDULED
                task.reminderEnum == ReminderOffset.NONE || task.isCompleted ->
                    ReminderScheduleStatus.NONE_REQUESTED
                else -> ReminderScheduleStatus.SKIPPED_PAST_TRIGGER
            }
            lastSaveOutcome = TaskSaveOutcome(
                taskId = taskId,
                reminderScheduled = scheduled,
                reminderWarningMessage = null,
                reminderStatus = status,
                reminderNoticeMessage = if (status == ReminderScheduleStatus.SKIPPED_PAST_TRIGGER) {
                    "Reminder time has already passed, so no alarm was scheduled."
                } else {
                    null
                }
            )
        }
        return res
    }

    /**
     * Toggles task completion atomically and idempotently.
     * - Protected by [completionMutex] and executed inside a Room transaction.
     * - If a recurring task is completed, spawns at most ONE next occurrence (with reset subtasks)
     *   tracked via [TaskEntity.spawnedNextTaskId] and [TaskEntity.recurrenceSeriesId].
     * - Preserves monthly anchor day-of-month across short months (e.g., Jan 31 -> Feb 28/29 -> Mar 31).
     * - Schedules/cancels reminders post-commit.
     */
    suspend fun setTaskCompleted(
        taskId: Long,
        completed: Boolean,
        spawnRecurringIfApplicable: Boolean = true
    ): Long? = completionMutex.withLock {
        val settings = preferencesRepository.userSettingsFlow.first()
        val now = System.currentTimeMillis()

        data class CompletionTxResult(
            val updatedTask: TaskEntity,
            val spawnedTask: TaskEntity?,
            val returnedSpawnedId: Long?
        )

        val txResult: CompletionTxResult = runInTransaction<CompletionTxResult?> {
            val task = dao.getTaskById(taskId) ?: return@runInTransaction null

            // Idempotency guard: if already in the requested completion state, do not re-run or re-spawn
            if (completed && task.isCompleted) {
                return@runInTransaction CompletionTxResult(
                    updatedTask = task,
                    spawnedTask = null,
                    returnedSpawnedId = task.spawnedNextTaskId
                )
            }
            if (!completed && !task.isCompleted) {
                return@runInTransaction CompletionTxResult(
                    updatedTask = task,
                    spawnedTask = null,
                    returnedSpawnedId = null
                )
            }

            val seriesId = when {
                task.recurrenceEnum == RecurrenceType.NEVER -> null
                !task.recurrenceSeriesId.isNullOrBlank() -> task.recurrenceSeriesId
                else -> "series_${task.id}_${task.createdAt}"
            }
            val anchorDay = when {
                task.recurrenceEnum != RecurrenceType.MONTHLY -> null
                task.recurrenceAnchorDay != null -> task.recurrenceAnchorDay
                else -> DateTimeUtil.getDayOfMonth(task.dueDateEpochDay)
            }

            var newlySpawnedTask: TaskEntity? = null
            var effectiveSpawnedId: Long? = task.spawnedNextTaskId

            if (completed && spawnRecurringIfApplicable && task.recurrenceEnum != RecurrenceType.NEVER) {
                // Check if a previously spawned child task for this occurrence still exists
                val existingChild = effectiveSpawnedId?.let { dao.getTaskById(it) }
                if (existingChild != null) {
                    effectiveSpawnedId = existingChild.id
                } else {
                    val nextEpochDay = DateTimeUtil.computeNextOccurrenceEpochDay(
                        currentEpochDay = task.dueDateEpochDay,
                        recurrenceTypeName = task.recurrenceType,
                        customWeekdaysCsv = task.recurrenceWeekdays,
                        anchorDayOfMonth = anchorDay
                    )
                    if (nextEpochDay != null) {
                        val allTasks = dao.getAllTasksOnce()
                        val matchingExisting = allTasks.firstOrNull { candidate ->
                            candidate.id != task.id &&
                                !candidate.isCompleted &&
                                candidate.dueDateEpochDay == nextEpochDay &&
                                seriesId != null &&
                                candidate.recurrenceSeriesId == seriesId
                        }

                        if (matchingExisting != null) {
                            effectiveSpawnedId = matchingExisting.id
                        } else {
                            val nextTask = task.copy(
                                id = 0,
                                createdAt = now,
                                updatedAt = now,
                                dueDateEpochDay = nextEpochDay,
                                isCompleted = false,
                                completedAt = null,
                                isSampleData = false,
                                recurrenceSeriesId = seriesId,
                                spawnedNextTaskId = null,
                                recurrenceAnchorDay = anchorDay
                            )
                            val insertedId = dao.insertTask(nextTask)
                            val existingSubtasks = dao.getSubtasksForTaskOnce(task.id)
                            if (existingSubtasks.isNotEmpty()) {
                                dao.insertSubtasks(
                                    existingSubtasks.mapIndexed { idx, sub ->
                                        SubtaskEntity(
                                            taskId = insertedId,
                                            title = sub.title,
                                            isCompleted = false,
                                            sortOrder = idx
                                        )
                                    }
                                )
                            }
                            effectiveSpawnedId = insertedId
                            newlySpawnedTask = nextTask.copy(id = insertedId)
                        }
                    }
                }
            } else if (!completed) {
                // If reopening and the previously spawned child no longer exists, clear spawnedNextTaskId
                val existingChild = effectiveSpawnedId?.let { dao.getTaskById(it) }
                if (existingChild == null) {
                    effectiveSpawnedId = null
                }
            }

            val updated = task.copy(
                isCompleted = completed,
                completedAt = if (completed) now else null,
                updatedAt = now,
                recurrenceSeriesId = seriesId ?: task.recurrenceSeriesId,
                recurrenceAnchorDay = anchorDay ?: task.recurrenceAnchorDay,
                spawnedNextTaskId = effectiveSpawnedId
            )
            dao.updateTask(updated)

            CompletionTxResult(
                updatedTask = updated,
                spawnedTask = newlySpawnedTask,
                returnedSpawnedId = if (completed) effectiveSpawnedId else null
            )
        } ?: return@withLock null

        // Post-commit reminder updates
        if (completed) {
            ReminderScheduler.cancelTaskReminder(context, txResult.updatedTask.id)
        } else {
            ReminderScheduler.scheduleTaskReminder(
                context,
                txResult.updatedTask,
                settings.notificationsEnabled
            )
        }

        txResult.spawnedTask?.let { spawned ->
            ReminderScheduler.scheduleTaskReminder(
                context,
                spawned,
                settings.notificationsEnabled
            )
        }

        txResult.returnedSpawnedId
    }

    /**
     * Safely undoes a task completion transition.
     * If [spawnedRecurringTaskId] was auto-generated by completing [taskId], it is only deleted
     * if the user has not independently edited or completed that spawned occurrence or its subtasks.
     */
    suspend fun undoTaskCompletion(
        taskId: Long,
        spawnedRecurringTaskId: Long?
    ) = undoTaskCompletion(
        taskId = taskId,
        previousCompleted = false,
        spawnedRecurringTaskId = spawnedRecurringTaskId
    )

    suspend fun undoTaskCompletion(
        taskId: Long,
        previousCompleted: Boolean,
        spawnedRecurringTaskId: Long?
    ) = completionMutex.withLock {
        val settings = preferencesRepository.userSettingsFlow.first()
        val now = System.currentTimeMillis()
        var deletedSpawnedId: Long? = null
        var reopenedParent: TaskEntity? = null

        runInTransaction {
            var remainingSpawnedId = spawnedRecurringTaskId
            if (spawnedRecurringTaskId != null) {
                val spawned = dao.getTaskById(spawnedRecurringTaskId)
                if (spawned != null) {
                    val spawnedSubtasks = dao.getSubtasksForTaskOnce(spawnedRecurringTaskId)
                    val isUnmodifiedAutoSpawn = !spawned.isCompleted &&
                        spawned.updatedAt == spawned.createdAt &&
                        spawnedSubtasks.none { it.isCompleted }
                    if (isUnmodifiedAutoSpawn) {
                        dao.deleteTaskById(spawnedRecurringTaskId)
                        deletedSpawnedId = spawnedRecurringTaskId
                        remainingSpawnedId = null
                    }
                } else {
                    remainingSpawnedId = null
                }
            }

            val parent = dao.getTaskById(taskId)
            if (parent != null) {
                val updatedParent = parent.copy(
                    isCompleted = previousCompleted,
                    completedAt = if (previousCompleted) now else null,
                    updatedAt = now,
                    spawnedNextTaskId = remainingSpawnedId
                )
                dao.updateTask(updatedParent)
                reopenedParent = updatedParent
            }
        }

        deletedSpawnedId?.let { ReminderScheduler.cancelTaskReminder(context, it) }
        reopenedParent?.let { parent ->
            if (parent.isCompleted) {
                ReminderScheduler.cancelTaskReminder(context, parent.id)
            } else {
                ReminderScheduler.scheduleTaskReminder(context, parent, settings.notificationsEnabled)
            }
        }
    }

    suspend fun updateTaskDescription(taskId: Long, newDescription: String) {
        val task = dao.getTaskById(taskId) ?: return
        dao.updateTask(
            task.copy(
                description = newDescription.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTaskPriority(taskId: Long, priority: TaskPriority) {
        val task = dao.getTaskById(taskId) ?: return
        dao.updateTask(
            task.copy(
                priority = priority.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTaskCategory(taskId: Long, categoryId: Long?) {
        val task = dao.getTaskById(taskId) ?: return
        dao.updateTask(
            task.copy(
                categoryId = categoryId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTaskReminder(
        taskId: Long,
        reminderOffset: ReminderOffset,
        customReminderMillis: Long? = null
    ): ReminderScheduleReport {
        val task = dao.getTaskById(taskId)
            ?: return ReminderScheduleReport(
                status = ReminderScheduleStatus.FAILED_RETRYABLE,
                userMessage = "Task not found"
            )
        val settings = preferencesRepository.userSettingsFlow.first()
        val updated = task.copy(
            reminderOffset = reminderOffset.name,
            customReminderMillis = customReminderMillis,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateTask(updated)
        return ReminderScheduler.scheduleTaskReminderDetailed(
            context = context,
            task = updated,
            notificationsEnabled = settings.notificationsEnabled
        )
    }

    suspend fun reconcileAllReminders(): Int {
        val settings = preferencesRepository.userSettingsFlow.first()
        val tasks = dao.getAllTasksOnce()
        return ReminderScheduler.reconcileReminders(
            context = context,
            tasks = tasks,
            notificationsEnabled = settings.notificationsEnabled
        )
    }

    suspend fun duplicateTask(taskId: Long): Long? {
        val original = dao.getTaskById(taskId) ?: return null
        val subtasks = dao.getSubtasksForTaskOnce(taskId)
        val now = System.currentTimeMillis()
        val copyTask = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            createdAt = now,
            updatedAt = now,
            isCompleted = false,
            completedAt = null,
            isSampleData = false,
            recurrenceSeriesId = if (original.recurrenceEnum != RecurrenceType.NEVER) {
                "series_copy_${now}_${original.id}"
            } else {
                null
            },
            spawnedNextTaskId = null
        )
        val newId = dao.insertTask(copyTask)
        if (subtasks.isNotEmpty()) {
            dao.insertSubtasks(
                subtasks.mapIndexed { idx, sub ->
                    SubtaskEntity(
                        taskId = newId,
                        title = sub.title,
                        isCompleted = sub.isCompleted,
                        sortOrder = idx
                    )
                }
            )
        }
        return newId
    }

    suspend fun deleteTask(taskId: Long): Pair<TaskEntity, List<SubtaskEntity>>? {
        val task = dao.getTaskById(taskId) ?: return null
        val subtasks = dao.getSubtasksForTaskOnce(taskId)
        ReminderScheduler.cancelTaskReminder(context, taskId)
        dao.deleteTaskById(taskId)
        return task to subtasks
    }

    suspend fun restoreDeletedTask(task: TaskEntity, subtasks: List<SubtaskEntity>) {
        val settings = preferencesRepository.userSettingsFlow.first()
        val restoredId = dao.insertTask(task)
        if (subtasks.isNotEmpty()) {
            dao.insertSubtasks(subtasks.map { it.copy(taskId = restoredId) })
        }
        ReminderScheduler.scheduleTaskReminder(
            context,
            task.copy(id = restoredId),
            settings.notificationsEnabled
        )
    }

    // Subtask operations
    suspend fun addSubtask(taskId: Long, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val existing = dao.getSubtasksForTaskOnce(taskId)
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertSubtask(
            SubtaskEntity(
                taskId = taskId,
                title = clean,
                isCompleted = false,
                sortOrder = nextOrder
            )
        )
    }

    suspend fun toggleSubtask(subtask: SubtaskEntity) {
        dao.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
    }

    suspend fun updateSubtaskTitle(subtask: SubtaskEntity, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        dao.updateSubtask(subtask.copy(title = clean))
    }

    suspend fun deleteSubtask(subtaskId: Long) {
        dao.deleteSubtaskById(subtaskId)
    }

    suspend fun moveSubtask(taskId: Long, subtaskId: Long, moveUp: Boolean) {
        val list = dao.getSubtasksForTaskOnce(taskId).toMutableList()
        val index = list.indexOfFirst { it.id == subtaskId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in list.indices) return

        val item = list.removeAt(index)
        list.add(targetIndex, item)
        val reindexed = list.mapIndexed { idx, sub -> sub.copy(sortOrder = idx) }
        dao.updateSubtasks(reindexed)
    }

    // Category operations
    suspend fun createCategory(name: String, colorHex: String): Long {
        val clean = name.trim()
        if (clean.isEmpty()) return -1L
        val existing = dao.getAllCategoriesOnce()
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        return dao.insertCategory(
            CategoryEntity(
                name = clean,
                colorHex = colorHex,
                sortOrder = nextOrder
            )
        )
    }

    suspend fun updateCategory(category: CategoryEntity, newName: String, newColorHex: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        dao.updateCategory(category.copy(name = clean, colorHex = newColorHex))
    }

    suspend fun deleteCategory(categoryId: Long, moveTasksToCategoryId: Long? = null) {
        dao.safeDeleteCategory(categoryId, moveTasksToCategoryId)
    }

    // Insights Calculation
    fun computeInsights(
        tasksWithDetails: List<TaskWithDetails>,
        categories: List<CategoryEntity>
    ): InsightsSummary {
        val today = DateTimeUtil.currentLocalEpochDay()
        val allTasks = tasksWithDetails.map { it.task }

        // Map each completed task to the epoch day it was completed (or its due date if completedAt is null)
        val completedEpochDays: List<Long> = allTasks
            .filter { it.isCompleted }
            .map { task ->
                task.completedAt?.let { DateTimeUtil.millisToLocalEpochDay(it) }
                    ?: task.dueDateEpochDay
            }

        val completedDaySet = completedEpochDays.toSet()

        val completedToday = completedEpochDays.count { it == today }
        val weekStartEpoch = today - 6L
        val completedThisWeek = completedEpochDays.count { it in weekStartEpoch..today }
        val plannedThisWeek = allTasks.count { it.dueDateEpochDay in weekStartEpoch..today }

        // Daily Streak: consecutive calendar days with at least one completed task
        var streak = 0
        var checkDay = if (today in completedDaySet) today else today - 1L
        while (checkDay in completedDaySet) {
            streak++
            checkDay -= 1L
        }

        val totalCompletedAllTime = allTasks.count { it.isCompleted }
        val totalTasksAllTime = allTasks.size
        val completionRate = if (totalTasksAllTime > 0) {
            ((totalCompletedAllTime.toFloat() / totalTasksAllTime.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val weeklyProgress = if (plannedThisWeek > 0) {
            (completedThisWeek.toFloat() / plannedThisWeek.toFloat()).coerceIn(0f, 1f)
        } else if (completedThisWeek > 0) {
            1f
        } else {
            0f
        }

        val sevenDays = (6 downTo 0).map { offset ->
            val day = today - offset
            val isoDay = DateTimeUtil.getIsoDayOfWeek(day)
            val completedOnDay = completedEpochDays.count { it == day }
            val plannedOnDay = allTasks.count { it.dueDateEpochDay == day }
            DayActivityStat(
                epochDay = day,
                dayShortLabel = DateTimeUtil.getDayOfWeekShortLabel(isoDay),
                completedCount = completedOnDay,
                plannedCount = plannedOnDay,
                isToday = day == today
            )
        }

        val categoryMap = categories.associateBy { it.id }
        val groupedByCategory = allTasks.groupBy { it.categoryId }
        val categoryStats = mutableListOf<CategoryTaskStat>()

        categories.forEach { cat ->
            val list = groupedByCategory[cat.id] ?: emptyList()
            if (list.isNotEmpty()) {
                categoryStats.add(
                    CategoryTaskStat(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        colorHex = cat.colorHex,
                        totalCount = list.size,
                        completedCount = list.count { it.isCompleted }
                    )
                )
            }
        }
        val uncategorized = groupedByCategory[null] ?: emptyList()
        if (uncategorized.isNotEmpty()) {
            categoryStats.add(
                CategoryTaskStat(
                    categoryId = null,
                    categoryName = "Uncategorized",
                    colorHex = "#989898",
                    totalCount = uncategorized.size,
                    completedCount = uncategorized.count { it.isCompleted }
                )
            )
        }

        return InsightsSummary(
            completedToday = completedToday,
            completedThisWeek = completedThisWeek,
            plannedThisWeek = plannedThisWeek,
            currentDailyStreak = streak,
            totalCompletedAllTime = totalCompletedAllTime,
            totalTasksAllTime = totalTasksAllTime,
            overallCompletionRatePercent = completionRate,
            weeklyProgressFraction = weeklyProgress,
            sevenDayActivity = sevenDays,
            categoryBreakdown = categoryStats.sortedByDescending { it.totalCount }
        )
    }

    // Export / Import JSON Backup
    suspend fun exportBackupJson(): String {
        val categories = dao.getAllCategoriesOnce()
        val tasks = dao.getAllTasksOnce()
        val subtasks = dao.getAllSubtasksOnce()
        val settings = preferencesRepository.userSettingsFlow.first()

        val root = JSONObject()
        root.put("app", "DAYFORM")
        root.put("schemaVersion", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("displayName", settings.displayName)

        val categoriesArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("colorHex", cat.colorHex)
                put("sortOrder", cat.sortOrder)
            }
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        val tasksArray = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
                put("dueDateEpochDay", task.dueDateEpochDay)
                if (task.dueTimeMinutes != null) put("dueTimeMinutes", task.dueTimeMinutes)
                put("isCompleted", task.isCompleted)
                if (task.completedAt != null) put("completedAt", task.completedAt)
                put("priority", task.priority)
                if (task.categoryId != null) put("categoryId", task.categoryId)
                put("reminderOffset", task.reminderOffset)
                if (task.customReminderMillis != null) put("customReminderMillis", task.customReminderMillis)
                put("recurrenceType", task.recurrenceType)
                put("recurrenceWeekdays", task.recurrenceWeekdays)
                put("sortOrder", task.sortOrder)
                if (task.recurrenceSeriesId != null) put("recurrenceSeriesId", task.recurrenceSeriesId)
                if (task.recurrenceAnchorDay != null) put("recurrenceAnchorDay", task.recurrenceAnchorDay)
            }
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        val subtasksArray = JSONArray()
        subtasks.forEach { sub ->
            val obj = JSONObject().apply {
                put("id", sub.id)
                put("taskId", sub.taskId)
                put("title", sub.title)
                put("isCompleted", sub.isCompleted)
                put("sortOrder", sub.sortOrder)
            }
            subtasksArray.put(obj)
        }
        root.put("subtasks", subtasksArray)

        return root.toString(2)
    }

    data class BackupPreview(
        val taskCount: Int,
        val categoryCount: Int,
        val subtaskCount: Int,
        val rawJson: String
    )

    private data class ValidatedCategoryRecord(
        val oldId: Long?,
        val name: String,
        val colorHex: String,
        val sortOrder: Int
    )

    private data class ValidatedTaskRecord(
        val oldId: Long?,
        val title: String,
        val description: String,
        val createdAt: Long,
        val updatedAt: Long,
        val dueDateEpochDay: Long,
        val dueTimeMinutes: Int?,
        val isCompleted: Boolean,
        val completedAt: Long?,
        val priority: String,
        val oldCategoryId: Long?,
        val reminderOffset: String,
        val customReminderMillis: Long?,
        val recurrenceType: String,
        val recurrenceWeekdays: String,
        val sortOrder: Int,
        val recurrenceSeriesId: String?,
        val recurrenceAnchorDay: Int?
    )

    private data class ValidatedSubtaskRecord(
        val oldId: Long?,
        val oldTaskId: Long,
        val title: String,
        val isCompleted: Boolean,
        val sortOrder: Int
    )

    private data class ParsedBackupPayload(
        val categories: List<ValidatedCategoryRecord>,
        val tasks: List<ValidatedTaskRecord>,
        val subtasks: List<ValidatedSubtaskRecord>
    )

    private fun parseAndValidateBackupPayload(rawJson: String): Result<ParsedBackupPayload> {
        if (rawJson.isBlank()) {
            return Result.failure(IllegalArgumentException("Backup file is empty."))
        }
        val byteLength = rawJson.toByteArray(Charsets.UTF_8).size
        if (byteLength > MAX_BACKUP_BYTES) {
            return Result.failure(
                IllegalArgumentException("Backup file exceeds maximum supported size of 10 MB.")
            )
        }

        return try {
            val root = JSONObject(rawJson)
            val app = root.optString("app", "")
            if (app != "DAYFORM") {
                return Result.failure(
                    IllegalArgumentException("Invalid backup file: Not a DAYFORM JSON backup.")
                )
            }

            if (root.has("schemaVersion") && !root.isNull("schemaVersion")) {
                val versionObj = root.get("schemaVersion")
                val version = when (versionObj) {
                    is Number -> versionObj.toInt()
                    else -> return Result.failure(
                        IllegalArgumentException("Backup file has an invalid non-numeric schemaVersion.")
                    )
                }
                if (version !in 1..2) {
                    return Result.failure(
                        IllegalArgumentException("Unsupported backup schemaVersion: $version.")
                    )
                }
            }

            if (!root.has("tasks") || root.isNull("tasks")) {
                return Result.failure(IllegalArgumentException("Backup file is missing 'tasks' array."))
            }
            val tasksArray = root.optJSONArray("tasks")
                ?: return Result.failure(IllegalArgumentException("Backup field 'tasks' must be a JSON array."))

            val categoriesArray = if (!root.has("categories") || root.isNull("categories")) {
                JSONArray()
            } else {
                root.optJSONArray("categories")
                    ?: return Result.failure(IllegalArgumentException("Backup field 'categories' must be a JSON array."))
            }

            val subtasksArray = if (!root.has("subtasks") || root.isNull("subtasks")) {
                JSONArray()
            } else {
                root.optJSONArray("subtasks")
                    ?: return Result.failure(IllegalArgumentException("Backup field 'subtasks' must be a JSON array."))
            }

            if (categoriesArray.length() > MAX_BACKUP_ENTITIES ||
                tasksArray.length() > MAX_BACKUP_ENTITIES ||
                subtasksArray.length() > MAX_BACKUP_ENTITIES
            ) {
                return Result.failure(
                    IllegalArgumentException("Backup file exceeds the maximum limit of $MAX_BACKUP_ENTITIES items.")
                )
            }

            val seenCategoryIds = mutableSetOf<Long>()
            val validatedCategories = mutableListOf<ValidatedCategoryRecord>()
            for (i in 0 until categoriesArray.length()) {
                val c = categoriesArray.optJSONObject(i)
                    ?: return Result.failure(
                        IllegalArgumentException("Category at index $i is not a valid JSON object.")
                    )
                val oldId = if (c.has("id") && !c.isNull("id")) {
                    val idVal = c.optLong("id", -1L)
                    if (idVal <= 0L) {
                        return Result.failure(
                            IllegalArgumentException("Category at index $i has an invalid ID: $idVal.")
                        )
                    }
                    if (!seenCategoryIds.add(idVal)) {
                        return Result.failure(
                            IllegalArgumentException("Duplicate category ID $idVal in backup file.")
                        )
                    }
                    idVal
                } else {
                    null
                }

                val name = if (c.has("name") && !c.isNull("name")) c.optString("name", "").trim() else ""
                if (name.isEmpty()) {
                    return Result.failure(
                        IllegalArgumentException("Category at index $i has an empty name.")
                    )
                }
                if (name.length > 100) {
                    return Result.failure(
                        IllegalArgumentException("Category '$name' exceeds maximum name length (100 chars).")
                    )
                }

                val colorHex = if (c.has("colorHex") && !c.isNull("colorHex")) {
                    c.optString("colorHex", "").trim()
                } else {
                    "#DFFF65"
                }
                if (!HEX_COLOR_REGEX.matches(colorHex)) {
                    return Result.failure(
                        IllegalArgumentException("Category '$name' has an invalid colorHex '$colorHex'.")
                    )
                }

                val sortOrder = c.optInt("sortOrder", i)
                validatedCategories.add(
                    ValidatedCategoryRecord(
                        oldId = oldId,
                        name = name,
                        colorHex = colorHex,
                        sortOrder = sortOrder
                    )
                )
            }

            val now = System.currentTimeMillis()
            val seenTaskIds = mutableSetOf<Long>()
            val validatedTasks = mutableListOf<ValidatedTaskRecord>()
            for (i in 0 until tasksArray.length()) {
                val t = tasksArray.optJSONObject(i)
                    ?: return Result.failure(
                        IllegalArgumentException("Task at index $i is not a valid JSON object.")
                    )

                val oldTaskId = if (t.has("id") && !t.isNull("id")) {
                    val idVal = t.optLong("id", -1L)
                    if (idVal <= 0L) {
                        return Result.failure(
                            IllegalArgumentException("Task at index $i has an invalid ID: $idVal.")
                        )
                    }
                    if (!seenTaskIds.add(idVal)) {
                        return Result.failure(
                            IllegalArgumentException("Duplicate task ID $idVal in backup file.")
                        )
                    }
                    idVal
                } else {
                    null
                }

                val title = if (t.has("title") && !t.isNull("title")) t.optString("title", "").trim() else ""
                if (title.isEmpty()) {
                    return Result.failure(
                        IllegalArgumentException("Task at index $i has an empty title.")
                    )
                }
                if (title.length > 500) {
                    return Result.failure(
                        IllegalArgumentException("Task at index $i exceeds maximum title length (500 chars).")
                    )
                }

                val description = if (t.has("description") && !t.isNull("description")) {
                    t.optString("description", "")
                } else {
                    ""
                }
                if (description.length > 10_000) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' exceeds maximum description length (10000 chars).")
                    )
                }

                if (!t.has("dueDateEpochDay") || t.isNull("dueDateEpochDay")) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' is missing a due date.")
                    )
                }
                val dueDayObj = t.get("dueDateEpochDay")
                val dueDateEpochDay = when (dueDayObj) {
                    is Number -> dueDayObj.toLong()
                    else -> return Result.failure(
                        IllegalArgumentException("Task '$title' has a non-numeric dueDateEpochDay.")
                    )
                }
                if (dueDateEpochDay !in -365_250L..365_250L) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' has an out-of-bounds dueDateEpochDay: $dueDateEpochDay.")
                    )
                }

                val dueTimeMinutes = if (t.has("dueTimeMinutes") && !t.isNull("dueTimeMinutes")) {
                    val dtObj = t.get("dueTimeMinutes")
                    val mins = when (dtObj) {
                        is Number -> dtObj.toInt()
                        else -> return Result.failure(
                            IllegalArgumentException("Task '$title' has a non-numeric dueTimeMinutes.")
                        )
                    }
                    if (mins !in 0..1439) {
                        return Result.failure(
                            IllegalArgumentException("Task '$title' has out-of-range dueTimeMinutes: $mins (expected 0..1439).")
                        )
                    }
                    mins
                } else {
                    null
                }

                val priorityRaw = if (t.has("priority") && !t.isNull("priority")) {
                    t.optString("priority", "")
                } else {
                    TaskPriority.MEDIUM.name
                }
                if (!TaskPriority.isValidName(priorityRaw)) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' has an invalid priority '$priorityRaw'.")
                    )
                }
                val normalizedPriority = TaskPriority.fromName(priorityRaw).name

                val reminderRaw = if (t.has("reminderOffset") && !t.isNull("reminderOffset")) {
                    t.optString("reminderOffset", "")
                } else {
                    ReminderOffset.NONE.name
                }
                if (!ReminderOffset.isValidName(reminderRaw)) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' has an invalid reminderOffset '$reminderRaw'.")
                    )
                }
                val normalizedReminder = ReminderOffset.fromName(reminderRaw).name

                val isCompleted = t.optBoolean("isCompleted", false)
                val customReminderMillis = if (t.has("customReminderMillis") && !t.isNull("customReminderMillis")) {
                    val crObj = t.get("customReminderMillis")
                    when (crObj) {
                        is Number -> crObj.toLong()
                        else -> return Result.failure(
                            IllegalArgumentException("Task '$title' has a non-numeric customReminderMillis.")
                        )
                    }
                } else {
                    null
                }
                if (normalizedReminder == ReminderOffset.CUSTOM.name && !isCompleted && (customReminderMillis == null || customReminderMillis <= 0L)) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' uses CUSTOM reminderOffset but is missing a valid customReminderMillis.")
                    )
                }

                val recurrenceRaw = if (t.has("recurrenceType") && !t.isNull("recurrenceType")) {
                    t.optString("recurrenceType", "")
                } else {
                    RecurrenceType.NEVER.name
                }
                if (!RecurrenceType.isValidName(recurrenceRaw)) {
                    return Result.failure(
                        IllegalArgumentException("Task '$title' has an invalid recurrenceType '$recurrenceRaw'.")
                    )
                }
                val normalizedRecurrence = RecurrenceType.fromName(recurrenceRaw).name

                val recurrenceWeekdays = if (t.has("recurrenceWeekdays") && !t.isNull("recurrenceWeekdays")) {
                    t.optString("recurrenceWeekdays", "").trim()
                } else {
                    ""
                }
                if (normalizedRecurrence == RecurrenceType.CUSTOM_WEEKDAYS.name) {
                    val tokens = recurrenceWeekdays.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val validDays = tokens.mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }
                    if (tokens.isEmpty() || validDays.size != tokens.size) {
                        return Result.failure(
                            IllegalArgumentException("Task '$title' has invalid recurrenceWeekdays '$recurrenceWeekdays'.")
                        )
                    }
                }

                val oldCategoryId = if (t.has("categoryId") && !t.isNull("categoryId")) {
                    val catObj = t.get("categoryId")
                    val cid = when (catObj) {
                        is Number -> catObj.toLong()
                        else -> return Result.failure(
                            IllegalArgumentException("Task '$title' has a non-numeric categoryId.")
                        )
                    }
                    if (cid !in seenCategoryIds) {
                        return Result.failure(
                            IllegalArgumentException("Task '$title' references missing categoryId $cid.")
                        )
                    }
                    cid
                } else {
                    null
                }

                val recurrenceSeriesId = if (t.has("recurrenceSeriesId") && !t.isNull("recurrenceSeriesId")) {
                    t.optString("recurrenceSeriesId", "").trim().ifEmpty { null }
                } else {
                    null
                }

                val recurrenceAnchorDay = if (t.has("recurrenceAnchorDay") && !t.isNull("recurrenceAnchorDay")) {
                    val anchorObj = t.get("recurrenceAnchorDay")
                    val anchor = when (anchorObj) {
                        is Number -> anchorObj.toInt()
                        else -> return Result.failure(
                            IllegalArgumentException("Task '$title' has a non-numeric recurrenceAnchorDay.")
                        )
                    }
                    if (anchor !in 1..31) {
                        return Result.failure(
                            IllegalArgumentException("Task '$title' has out-of-range recurrenceAnchorDay: $anchor.")
                        )
                    }
                    anchor
                } else if (normalizedRecurrence == RecurrenceType.MONTHLY.name) {
                    DateTimeUtil.getDayOfMonth(dueDateEpochDay)
                } else {
                    null
                }

                val createdAt = t.optLong("createdAt", now)
                val updatedAt = t.optLong("updatedAt", now)
                val completedAt = if (t.has("completedAt") && !t.isNull("completedAt")) {
                    t.optLong("completedAt", now)
                } else if (isCompleted) {
                    now
                } else {
                    null
                }

                validatedTasks.add(
                    ValidatedTaskRecord(
                        oldId = oldTaskId,
                        title = title,
                        description = description,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        dueDateEpochDay = dueDateEpochDay,
                        dueTimeMinutes = dueTimeMinutes,
                        isCompleted = isCompleted,
                        completedAt = completedAt,
                        priority = normalizedPriority,
                        oldCategoryId = oldCategoryId,
                        reminderOffset = normalizedReminder,
                        customReminderMillis = customReminderMillis,
                        recurrenceType = normalizedRecurrence,
                        recurrenceWeekdays = recurrenceWeekdays,
                        sortOrder = t.optInt("sortOrder", i),
                        recurrenceSeriesId = recurrenceSeriesId,
                        recurrenceAnchorDay = recurrenceAnchorDay
                    )
                )
            }

            val seenSubtaskIds = mutableSetOf<Long>()
            val validatedSubtasks = mutableListOf<ValidatedSubtaskRecord>()
            for (i in 0 until subtasksArray.length()) {
                val s = subtasksArray.optJSONObject(i)
                    ?: return Result.failure(
                        IllegalArgumentException("Subtask at index $i is not a valid JSON object.")
                    )

                val oldSubId = if (s.has("id") && !s.isNull("id")) {
                    val idVal = s.optLong("id", -1L)
                    if (idVal <= 0L) {
                        return Result.failure(
                            IllegalArgumentException("Subtask at index $i has an invalid ID: $idVal.")
                        )
                    }
                    if (!seenSubtaskIds.add(idVal)) {
                        return Result.failure(
                            IllegalArgumentException("Duplicate subtask ID $idVal in backup file.")
                        )
                    }
                    idVal
                } else {
                    null
                }

                val subTitle = if (s.has("title") && !s.isNull("title")) s.optString("title", "").trim() else ""
                if (subTitle.isEmpty()) {
                    return Result.failure(
                        IllegalArgumentException("Subtask at index $i has an empty title.")
                    )
                }
                if (subTitle.length > 500) {
                    return Result.failure(
                        IllegalArgumentException("Subtask at index $i exceeds maximum title length (500 chars).")
                    )
                }

                if (!s.has("taskId") || s.isNull("taskId")) {
                    return Result.failure(
                        IllegalArgumentException("Subtask '$subTitle' is missing 'taskId'.")
                    )
                }
                val taskIdObj = s.get("taskId")
                val oldTaskId = when (taskIdObj) {
                    is Number -> taskIdObj.toLong()
                    else -> return Result.failure(
                        IllegalArgumentException("Subtask '$subTitle' has a non-numeric taskId.")
                    )
                }
                if (oldTaskId !in seenTaskIds) {
                    return Result.failure(
                        IllegalArgumentException("Subtask '$subTitle' references missing taskId $oldTaskId.")
                    )
                }

                validatedSubtasks.add(
                    ValidatedSubtaskRecord(
                        oldId = oldSubId,
                        oldTaskId = oldTaskId,
                        title = subTitle,
                        isCompleted = s.optBoolean("isCompleted", false),
                        sortOrder = s.optInt("sortOrder", i)
                    )
                )
            }

            Result.success(
                ParsedBackupPayload(
                    categories = validatedCategories,
                    tasks = validatedTasks,
                    subtasks = validatedSubtasks
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("Invalid JSON format: ${e.localizedMessage ?: "Malformed content"}")
            )
        }
    }

    fun validateBackupJson(rawJson: String): Result<BackupPreview> {
        return parseAndValidateBackupPayload(rawJson).map { parsed ->
            BackupPreview(
                taskCount = parsed.tasks.size,
                categoryCount = parsed.categories.size,
                subtaskCount = parsed.subtasks.size,
                rawJson = rawJson
            )
        }
    }

    suspend fun importValidatedBackup(rawJson: String, replaceExisting: Boolean): Result<Int> {
        if (!importMutex.tryLock()) {
            return Result.failure(IllegalStateException("A backup import is already in progress."))
        }
        _isImportInProgress.value = true
        return try {
            val parsedResult = parseAndValidateBackupPayload(rawJson)
            if (parsedResult.isFailure) {
                return Result.failure(
                    parsedResult.exceptionOrNull() ?: IllegalArgumentException("Invalid backup")
                )
            }
            val payload = parsedResult.getOrThrow()

            // Capture existing task IDs before the transaction so if replaceExisting succeeds,
            // we can cancel their reminders post-commit without mutating alarms pre-commit.
            val previousTaskIds = if (replaceExisting) {
                dao.getAllTasksOnce().map { it.id }
            } else {
                emptyList()
            }

            val importedCount = runInTransaction {
                if (replaceExisting) {
                    dao.deleteAllTasks()
                    dao.deleteAllCategories()
                }

                val existingCategories = dao.getAllCategoriesOnce().toMutableList()
                val oldCatIdToNewId = mutableMapOf<Long, Long>()

                for (c in payload.categories) {
                    val match = existingCategories.firstOrNull {
                        it.name.equals(c.name, ignoreCase = true)
                    }
                    val targetId = if (match != null) {
                        match.id
                    } else {
                        val insertedCat = CategoryEntity(
                            name = c.name,
                            colorHex = c.colorHex,
                            sortOrder = c.sortOrder
                        )
                        val newCatId = dao.insertCategory(insertedCat)
                        existingCategories.add(insertedCat.copy(id = newCatId))
                        newCatId
                    }
                    if (c.oldId != null) {
                        oldCatIdToNewId[c.oldId] = targetId
                    }
                }

                val oldTaskIdToNewId = mutableMapOf<Long, Long>()
                var importedTasksCount = 0

                for (t in payload.tasks) {
                    val mappedCategoryId = t.oldCategoryId?.let { oldCatIdToNewId[it] }
                    val entity = TaskEntity(
                        title = t.title,
                        description = t.description,
                        createdAt = t.createdAt,
                        updatedAt = t.updatedAt,
                        dueDateEpochDay = t.dueDateEpochDay,
                        dueTimeMinutes = t.dueTimeMinutes,
                        isCompleted = t.isCompleted,
                        completedAt = t.completedAt,
                        priority = t.priority,
                        categoryId = mappedCategoryId,
                        reminderOffset = t.reminderOffset,
                        customReminderMillis = t.customReminderMillis,
                        recurrenceType = t.recurrenceType,
                        recurrenceWeekdays = t.recurrenceWeekdays,
                        sortOrder = t.sortOrder,
                        isSampleData = false,
                        recurrenceSeriesId = t.recurrenceSeriesId,
                        recurrenceAnchorDay = t.recurrenceAnchorDay
                    )
                    val newTaskId = dao.insertTask(entity)
                    importedTasksCount++
                    if (t.oldId != null) {
                        oldTaskIdToNewId[t.oldId] = newTaskId
                    }
                    // Allow mid-import transaction rollback testing after SQLite writes have started
                    midImportTransactionHook?.invoke(importedTasksCount)
                }

                for (s in payload.subtasks) {
                    val newTaskId = oldTaskIdToNewId[s.oldTaskId]
                        ?: throw IllegalStateException("Subtask '${s.title}' references unmapped taskId ${s.oldTaskId}.")
                    dao.insertSubtask(
                        SubtaskEntity(
                            taskId = newTaskId,
                            title = s.title,
                            isCompleted = s.isCompleted,
                            sortOrder = s.sortOrder
                        )
                    )
                }

                ensureStarterCategories()
                importedTasksCount
            }

            // Post-commit reminder reconciliation: only executed after Room transaction commits!
            if (replaceExisting && previousTaskIds.isNotEmpty()) {
                previousTaskIds.forEach { oldId ->
                    ReminderScheduler.cancelTaskReminder(context, oldId)
                }
            }
            reconcileAllReminders()

            Result.success(importedCount)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isImportInProgress.value = false
            importMutex.unlock()
        }
    }
}
