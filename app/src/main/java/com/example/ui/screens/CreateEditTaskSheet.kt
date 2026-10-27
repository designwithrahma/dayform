package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.SubtaskDraft
import com.example.data.model.TaskPriority
import com.example.data.model.TaskWithDetails
import com.example.ui.components.SHARED_CREATE_TASK_FAB_KEY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.StarterCategoryColors
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.pressScaleClickable
import com.example.util.DateTimeUtil

private val SubtaskDraftListSaver = listSaver<SnapshotStateList<SubtaskDraft>, Bundle>(
    save = { list ->
        list.map { draft ->
            Bundle().apply {
                putLong("id", draft.id)
                putString("title", draft.title)
                putBoolean("isCompleted", draft.isCompleted)
            }
        }
    },
    restore = { bundles ->
        mutableStateListOf<SubtaskDraft>().apply {
            bundles.forEach { bundle ->
                add(
                    SubtaskDraft(
                        id = bundle.getLong("id"),
                        title = bundle.getString("title") ?: "",
                        isCompleted = bundle.getBoolean("isCompleted")
                    )
                )
            }
        }
    }
)

data class TaskEditorSaveUiState(
    val isSaving: Boolean = false,
    val isRetryingReminder: Boolean = false,
    val saveErrorMessage: String? = null,
    val reminderWarningMessage: String? = null,
    val persistedTaskId: Long? = null,
    val completedSuccessfully: Boolean = false
)

class TaskEditorSaveViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskEditorSaveUiState())
    val state: StateFlow<TaskEditorSaveUiState> = _state.asStateFlow()

    private var activeSaveJob: Job? = null

    fun submitSave(
        existingTaskId: Long?,
        onSaveTask: suspend (Long?) -> Result<Long>,
        checkReminderWarning: (() -> String?)? = null
    ) {
        if (activeSaveJob?.isActive == true || _state.value.isSaving) {
            return
        }
        val effectiveTaskId = _state.value.persistedTaskId ?: existingTaskId
        _state.update {
            it.copy(
                isSaving = true,
                saveErrorMessage = null,
                reminderWarningMessage = null,
                completedSuccessfully = false
            )
        }
        activeSaveJob = viewModelScope.launch {
            try {
                val result = onSaveTask(effectiveTaskId)
                if (result.isSuccess) {
                    val savedId = result.getOrNull() ?: effectiveTaskId
                    val warning = checkReminderWarning?.invoke()
                    if (warning != null) {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                persistedTaskId = savedId,
                                reminderWarningMessage = warning,
                                completedSuccessfully = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                persistedTaskId = savedId,
                                reminderWarningMessage = null,
                                completedSuccessfully = true
                            )
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saveErrorMessage = result.exceptionOrNull()?.localizedMessage
                                ?: "Failed to save task. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = e.localizedMessage ?: "An error occurred while saving."
                    )
                }
            }
        }
    }

    fun retryReminder(onRetryReminder: suspend (Long) -> Result<Boolean>) {
        val taskId = _state.value.persistedTaskId ?: return
        if (_state.value.isRetryingReminder || _state.value.isSaving) return
        _state.update { it.copy(isRetryingReminder = true) }
        viewModelScope.launch {
            val res = onRetryReminder(taskId)
            if (res.isSuccess) {
                _state.update {
                    it.copy(
                        isRetryingReminder = false,
                        reminderWarningMessage = null,
                        completedSuccessfully = true
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isRetryingReminder = false,
                        reminderWarningMessage = res.exceptionOrNull()?.localizedMessage
                            ?: "Reminder scheduling failed again. You can retry or continue."
                    )
                }
            }
        }
    }

    fun acknowledgeReminderWarningAndFinish() {
        _state.update {
            it.copy(
                reminderWarningMessage = null,
                completedSuccessfully = true
            )
        }
    }

    fun reset() {
        if (activeSaveJob?.isActive != true) {
            _state.value = TaskEditorSaveUiState()
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun CreateEditTaskSheet(
    existingTask: TaskWithDetails?,
    initialEpochDay: Long,
    categories: List<CategoryEntity>,
    use24HourFormat: Boolean,
    onSaveTask: suspend (
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
    ) -> Result<Long>,
    onCreateCategory: (name: String, colorHex: String) -> Unit,
    onCancel: () -> Unit,
    onSaveSuccess: () -> Unit = onCancel,
    checkReminderWarning: (() -> String?)? = null,
    onRetryReminder: (suspend (Long) -> Result<Boolean>)? = null,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val context = LocalContext.current
    val saveCoordinator: TaskEditorSaveViewModel = viewModel(
        key = "task_editor_save_${existingTask?.task?.id ?: "new"}"
    )
    val saveUiState by saveCoordinator.state.collectAsStateWithLifecycle()
    val isSaving = saveUiState.isSaving || saveUiState.isRetryingReminder
    val saveErrorMessage = saveUiState.saveErrorMessage
    val reminderWarningMessage = saveUiState.reminderWarningMessage

    LaunchedEffect(saveUiState.completedSuccessfully) {
        if (saveUiState.completedSuccessfully) {
            saveCoordinator.reset()
            onSaveSuccess()
        }
    }

    val initialTitle = existingTask?.task?.title ?: ""
    val initialDescription = existingTask?.task?.description ?: ""
    val baselineDueDate = existingTask?.task?.dueDateEpochDay ?: initialEpochDay
    val initialDueTime = existingTask?.task?.dueTimeMinutes
    val initialPriority = existingTask?.task?.priorityEnum ?: TaskPriority.MEDIUM
    val initialReminderOffset = existingTask?.task?.reminderEnum ?: ReminderOffset.NONE
    val initialCustomReminderMinutes: Int = remember(existingTask?.task?.customReminderMillis) {
        val millis = existingTask?.task?.customReminderMillis
        if (millis != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } else {
            9 * 60
        }
    }
    val initialRecurrenceType = existingTask?.task?.recurrenceEnum ?: RecurrenceType.NEVER
    val initialWeekdaysCsv = existingTask?.task?.recurrenceWeekdays?.ifBlank { "1,3,5" } ?: "1,3,5"
    val initialSubtaskList = remember(existingTask?.task?.id, existingTask?.subtasks) {
        existingTask?.subtasks?.map {
            SubtaskDraft(id = it.id, title = it.title, isCompleted = it.isCompleted)
        } ?: emptyList()
    }

    var title by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialTitle)
    }
    var titleError by rememberSaveable { mutableStateOf(false) }

    var description by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialDescription)
    }
    var dueDateEpochDay by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(baselineDueDate)
    }
    var dueTimeMinutes by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialDueTime)
    }
    var priority by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialPriority)
    }
    var selectedCategoryId by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(existingTask?.task?.categoryId ?: categories.firstOrNull()?.id)
    }
    var baselineCategoryId by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(existingTask?.task?.categoryId ?: categories.firstOrNull()?.id)
    }
    // If starter categories finish loading asynchronously when creating a new task,
    // adopt the first category as both selected and baseline so it is not flagged as dirty.
    LaunchedEffect(categories, existingTask?.task?.id) {
        if (existingTask == null && selectedCategoryId == null && categories.isNotEmpty()) {
            val defaultCatId = categories.first().id
            selectedCategoryId = defaultCatId
            baselineCategoryId = defaultCatId
        }
    }

    var reminderOffset by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialReminderOffset)
    }
    var customReminderMinutes by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf<Int?>(initialCustomReminderMinutes)
    }
    var recurrenceType by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialRecurrenceType)
    }
    var customWeekdaysCsv by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(initialWeekdaysCsv)
    }

    val subtaskDrafts = rememberSaveable(
        existingTask?.task?.id,
        saver = SubtaskDraftListSaver
    ) {
        mutableStateListOf<SubtaskDraft>().apply {
            initialSubtaskList.forEach { add(it) }
        }
    }
    var newSubtaskInput by rememberSaveable(existingTask?.task?.id) { mutableStateOf("") }

    var showAdvancedOptions by rememberSaveable(existingTask?.task?.id) {
        mutableStateOf(
            existingTask != null && (
                existingTask.task.reminderEnum != ReminderOffset.NONE ||
                    existingTask.task.recurrenceEnum != RecurrenceType.NEVER
                )
        )
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showCustomReminderTimePicker by rememberSaveable { mutableStateOf(false) }
    var showNewCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val isFormDirty by remember(
        title,
        description,
        dueDateEpochDay,
        dueTimeMinutes,
        priority,
        selectedCategoryId,
        baselineCategoryId,
        reminderOffset,
        customReminderMinutes,
        recurrenceType,
        customWeekdaysCsv,
        newSubtaskInput,
        initialTitle,
        initialDescription,
        baselineDueDate,
        initialDueTime,
        initialPriority,
        initialReminderOffset,
        initialCustomReminderMinutes,
        initialRecurrenceType,
        initialWeekdaysCsv,
        initialSubtaskList
    ) {
        derivedStateOf {
            title.trim() != initialTitle.trim() ||
                description.trim() != initialDescription.trim() ||
                dueDateEpochDay != baselineDueDate ||
                dueTimeMinutes != initialDueTime ||
                priority != initialPriority ||
                selectedCategoryId != baselineCategoryId ||
                reminderOffset != initialReminderOffset ||
                (reminderOffset == ReminderOffset.CUSTOM && customReminderMinutes != initialCustomReminderMinutes) ||
                recurrenceType != initialRecurrenceType ||
                (recurrenceType == RecurrenceType.CUSTOM_WEEKDAYS && customWeekdaysCsv != initialWeekdaysCsv) ||
                newSubtaskInput.isNotBlank() ||
                subtaskDrafts.toList() != initialSubtaskList
        }
    }

    val hasOpenOverlayDialog = showDatePicker ||
        showTimePicker ||
        showCustomReminderTimePicker ||
        showNewCategoryDialog ||
        showDiscardDialog

    // Intercept Back during in-flight save OR when the form has unsaved modifications without open dialogs.
    // When saving, Back is blocked to prevent tearing or duplicate creation.
    // When clean and not saving, this BackHandler is disabled so native Predictive Back runs freely.
    BackHandler(enabled = isSaving || (isFormDirty && !hasOpenOverlayDialog)) {
        if (isSaving) {
            // In-flight save: intercept and absorb back gesture
            return@BackHandler
        }
        showDiscardDialog = true
    }

    val attemptCancel: () -> Unit = {
        if (!isSaving) {
            if (isFormDirty && saveUiState.persistedTaskId == null) {
                showDiscardDialog = true
            } else {
                saveCoordinator.reset()
                onCancel()
            }
        }
    }

    // Restrained staggered entrance animation for form fields
    var contentRevealed by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) {
        contentRevealed = true
    }
    val formAlpha by animateFloatAsState(
        targetValue = if (contentRevealed) 1f else 0f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = DayformMotion.SUPPORTING_CONTENT_MS, easing = FastOutSlowInEasing)
        },
        label = "form_alpha"
    )
    val formTranslateY by animateFloatAsState(
        targetValue = if (contentRevealed) 0f else 28f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = DayformMotion.SCREEN_NAV_MS, easing = FastOutSlowInEasing)
        },
        label = "form_translate_y"
    )

    // Connect Create Task sheet to the central Neon Lime '+' button via sharedBounds
    val sharedFabSheetModifier = if (
        existingTask == null &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = SHARED_CREATE_TASK_FAB_KEY),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = DayformMotion.createTaskBoundsTransform(reducedMotion),
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp))
            )
        }
    } else {
        Modifier
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled gracefully
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(sharedFabSheetModifier)
            .background(CharcoalBg)
            .navigationBarsPadding()
            .imePadding()
            .testTag("create_edit_task_screen")
    ) {
        // Top Header Bar with Cancel and Save actions + subtle neon lime accent bar
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(NeonLime.copy(alpha = 0.65f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = attemptCancel,
                enabled = !isSaving,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSaving) CharcoalElevated.copy(alpha = 0.5f) else CharcoalElevated)
                    .testTag("cancel_task_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = if (isSaving) SecondaryText else Color.White
                )
            }

            Text(
                text = if (existingTask == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )

            Button(
                onClick = {
                    if (isSaving) return@Button
                    if (title.trim().isEmpty()) {
                        titleError = true
                        return@Button
                    }
                    val finalSubtasks = subtaskDrafts.toMutableList()
                    if (newSubtaskInput.trim().isNotEmpty()) {
                        finalSubtasks.add(SubtaskDraft(title = newSubtaskInput.trim()))
                    }
                    val customMillis = if (reminderOffset == ReminderOffset.CUSTOM) {
                        if (existingTask?.task?.customReminderMillis != null &&
                            dueDateEpochDay == baselineDueDate &&
                            customReminderMinutes == initialCustomReminderMinutes
                        ) {
                            existingTask.task.customReminderMillis
                        } else {
                            DateTimeUtil.epochDayAndMinutesToMillis(
                                dueDateEpochDay,
                                customReminderMinutes ?: initialCustomReminderMinutes
                            )
                        }
                    } else {
                        null
                    }
                    saveCoordinator.submitSave(
                        existingTaskId = existingTask?.task?.id,
                        onSaveTask = { effectiveTaskId ->
                            onSaveTask(
                                effectiveTaskId,
                                title.trim(),
                                description.trim(),
                                dueDateEpochDay,
                                dueTimeMinutes,
                                priority,
                                selectedCategoryId,
                                reminderOffset,
                                customMillis,
                                recurrenceType,
                                if (recurrenceType == RecurrenceType.CUSTOM_WEEKDAYS) customWeekdaysCsv else "",
                                finalSubtasks
                            )
                        },
                        checkReminderWarning = checkReminderWarning
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonLime,
                    contentColor = PrimaryDarkText,
                    disabledContainerColor = NeonLime.copy(alpha = 0.5f),
                    disabledContentColor = PrimaryDarkText.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("save_task_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("save_loading_indicator"),
                        color = PrimaryDarkText,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = saveErrorMessage != null,
            enter = DayformMotion.inlineBannerEnter(reducedMotion),
            exit = DayformMotion.inlineBannerExit(reducedMotion)
        ) {
            val activeSaveError = saveErrorMessage ?: ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OverdueRed.copy(alpha = 0.15f))
                    .border(1.dp, OverdueRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("save_error_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = OverdueRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeSaveError,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = OverdueRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AnimatedVisibility(
            visible = reminderWarningMessage != null,
            enter = DayformMotion.inlineBannerEnter(reducedMotion),
            exit = DayformMotion.inlineBannerExit(reducedMotion)
        ) {
            val activeReminderWarning = reminderWarningMessage ?: ""
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonLime.copy(alpha = 0.12f))
                    .border(1.dp, NeonLime.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("reminder_warning_banner"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = NeonLime,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeReminderWarning,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRetryReminder != null && saveUiState.persistedTaskId != null) {
                        TextButton(
                            onClick = { saveCoordinator.retryReminder(onRetryReminder) },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("retry_reminder_button")
                        ) {
                            Text(
                                text = "Retry Reminder",
                                color = NeonLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(
                        onClick = { saveCoordinator.acknowledgeReminderWarningAndFinish() },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("continue_after_reminder_warning_button")
                    ) {
                        Text(
                            text = "Continue",
                            color = SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = formAlpha
                    translationY = formTranslateY
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. TASK TITLE (Immediate & prominent)
            Column {
                Text(
                    text = "Task Title *",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonLime
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    placeholder = {
                        Text(
                            text = "e.g., Research for Flix Media",
                            color = SecondaryText
                        )
                    },
                    isError = titleError,
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalSurface,
                        unfocusedContainerColor = CharcoalSurface,
                        focusedBorderColor = NeonLime,
                        unfocusedBorderColor = CharcoalBorder,
                        errorBorderColor = OverdueRed,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonLime
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )
                AnimatedVisibility(
                    visible = titleError,
                    enter = DayformMotion.inlineBannerEnter(reducedMotion),
                    exit = DayformMotion.inlineBannerExit(reducedMotion)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Task title is required",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdueRed,
                            modifier = Modifier.testTag("task_title_error_text")
                        )
                    }
                }
            }

            // 2. DUE DATE & DUE TIME (Native-friendly pickers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Due Date Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(22.dp))
                        .pressScaleClickable(
                            pressedScale = 0.97f,
                            onClick = { showDatePicker = true }
                        )
                        .padding(16.dp)
                        .testTag("due_date_selector")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = NeonLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Due Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = DateTimeUtil.formatDayWithOrdinalMonth(dueDateEpochDay),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("due_date_value_text")
                    )
                }

                // Due Time Card (Optional)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(22.dp))
                        .pressScaleClickable(
                            pressedScale = 0.97f,
                            onClick = { showTimePicker = true }
                        )
                        .padding(16.dp)
                        .testTag("due_time_selector")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = NeonLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Due Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = SecondaryText
                            )
                        }
                        if (dueTimeMinutes != null) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = OverdueRed,
                                modifier = Modifier.clickable { dueTimeMinutes = null }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dueTimeMinutes?.let { DateTimeUtil.formatTime(it, use24HourFormat) }
                            ?: "Optional",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (dueTimeMinutes != null) Color.White else SecondaryText
                    )
                }
            }

            // 3. PRIORITY SELECTOR (Low, Medium, High, Urgent)
            Column {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { item ->
                        val selected = priority == item
                        val chipBg by animateColorAsState(
                            targetValue = if (selected) NeonLime else CharcoalSurface,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "priority_bg_${item.name}"
                        )
                        val chipBorder by animateColorAsState(
                            targetValue = if (selected) NeonLime else CharcoalBorder,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "priority_border_${item.name}"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (selected) PrimaryDarkText else Color.White,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "priority_text_${item.name}"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(chipBg)
                                .border(
                                    width = 1.dp,
                                    color = chipBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .pressScaleClickable(
                                    pressedScale = 0.95f,
                                    onClick = { priority = item }
                                )
                                .testTag("priority_chip_${item.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = chipText
                            )
                        }
                    }
                }
            }

            // 4. CATEGORY / PROJECT SELECTOR
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category / Project",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = "+ New Category",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonLime,
                        modifier = Modifier
                            .pressScaleClickable(
                                pressedScale = 0.95f,
                                onClick = { showNewCategoryDialog = true }
                            )
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val selected = selectedCategoryId == category.id
                        val catColor = parseHexColor(category.colorHex)
                        val catBg by animateColorAsState(
                            targetValue = if (selected) NeonLime else CharcoalSurface,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "cat_bg_${category.id}"
                        )
                        val catBorder by animateColorAsState(
                            targetValue = if (selected) NeonLime else CharcoalBorder,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "cat_border_${category.id}"
                        )
                        val catText by animateColorAsState(
                            targetValue = if (selected) PrimaryDarkText else Color.White,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "cat_text_${category.id}"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (selected) PrimaryDarkText else catColor,
                            animationSpec = DayformMotion.stateTween(reducedMotion),
                            label = "cat_dot_${category.id}"
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(catBg)
                                .border(
                                    width = 1.dp,
                                    color = catBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .pressScaleClickable(
                                    pressedScale = 0.95f,
                                    onClick = {
                                        selectedCategoryId = if (selected) null else category.id
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("category_chip_${category.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = catText
                            )
                        }
                    }
                }
            }

            // 5. DESCRIPTION / NOTES
            Column {
                Text(
                    text = "Description / Notes",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            text = "Add context, links, or deliverables...",
                            color = SecondaryText
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalSurface,
                        unfocusedContainerColor = CharcoalSurface,
                        focusedBorderColor = NeonLime,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonLime
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input")
                )
            }

            // 6. SUBTASKS BUILDER
            Column(
                modifier = Modifier.animateContentSize(
                    animationSpec = DayformMotion.contentSizeSpec(reducedMotion)
                )
            ) {
                AnimatedContent(
                    targetState = subtaskDrafts.size,
                    transitionSpec = DayformMotion.counterRollTransition(reducedMotion),
                    label = "sheet_subtask_count_anim"
                ) { count ->
                    Text(
                        text = "Subtasks ($count)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                subtaskDrafts.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalSurface)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${draft.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { subtaskDrafts.removeAt(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove subtask",
                                tint = SecondaryText
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskInput,
                        onValueChange = { newSubtaskInput = it },
                        placeholder = { Text("Add a subtask step...", color = SecondaryText) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CharcoalSurface,
                            unfocusedContainerColor = CharcoalSurface,
                            focusedBorderColor = NeonLime,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newSubtaskInput.trim().isNotEmpty()) {
                                    subtaskDrafts.add(SubtaskDraft(title = newSubtaskInput.trim()))
                                    newSubtaskInput = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_subtask_input")
                    )

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NeonLime)
                            .pressScaleClickable(
                                pressedScale = 0.90f,
                                onClick = {
                                    if (newSubtaskInput.trim().isNotEmpty()) {
                                        subtaskDrafts.add(SubtaskDraft(title = newSubtaskInput.trim()))
                                        newSubtaskInput = ""
                                    }
                                }
                            )
                            .testTag("sheet_add_subtask_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add subtask",
                            tint = PrimaryDarkText
                        )
                    }
                }
            }

            // 7. COLLAPSIBLE ADVANCED OPTIONS (Reminders & Repeat Schedule)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (showAdvancedOptions) 180f else 0f,
                    animationSpec = DayformMotion.stateTween(reducedMotion),
                    label = "advanced_chevron_rotation"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScaleClickable(
                            pressedScale = 0.98f,
                            onClick = { showAdvancedOptions = !showAdvancedOptions }
                        )
                        .testTag("toggle_advanced_options"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reminders & Repeat Schedule",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${reminderOffset.label} • Repeats: ${recurrenceType.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonLime
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle advanced options",
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                    )
                }

                AnimatedVisibility(
                    visible = showAdvancedOptions,
                    enter = DayformMotion.inlineBannerEnter(reducedMotion),
                    exit = DayformMotion.inlineBannerExit(reducedMotion)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Reminder Offset Selector
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = NeonLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reminder",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReminderOffset.entries.forEach { offset ->
                                    val selected = reminderOffset == offset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) NeonLime else CharcoalElevated)
                                            .clickable {
                                                reminderOffset = offset
                                                if (offset == ReminderOffset.CUSTOM) {
                                                    showCustomReminderTimePicker = true
                                                }
                                                if (offset != ReminderOffset.NONE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    val hasPerm = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (!hasPerm) {
                                                        notificationPermissionLauncher.launch(
                                                            Manifest.permission.POST_NOTIFICATIONS
                                                        )
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        val label = if (offset == ReminderOffset.CUSTOM && selected && customReminderMinutes != null) {
                                            "Custom (${DateTimeUtil.formatTime(customReminderMinutes!!, use24HourFormat)})"
                                        } else {
                                            offset.label
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (selected) PrimaryDarkText else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Repeat Schedule Selector
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Repeat,
                                    contentDescription = null,
                                    tint = NeonLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Repeat Schedule",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecurrenceType.entries.forEach { rec ->
                                    val selected = recurrenceType == rec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) NeonLime else CharcoalElevated)
                                            .clickable { recurrenceType = rec }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("recurrence_chip_${rec.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = rec.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (selected) PrimaryDarkText else Color.White
                                        )
                                    }
                                }
                            }

                            if (recurrenceType == RecurrenceType.CUSTOM_WEEKDAYS) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val activeDays = customWeekdaysCsv.split(",")
                                    .mapNotNull { it.trim().toIntOrNull() }
                                    .toSet()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    (1..7).forEach { isoDay ->
                                        val isDaySelected = isoDay in activeDays
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isDaySelected) NeonLime else CharcoalElevated)
                                                .clickable {
                                                    val updated = activeDays.toMutableSet()
                                                    if (isDaySelected && updated.size > 1) {
                                                        updated.remove(isoDay)
                                                    } else {
                                                        updated.add(isoDay)
                                                    }
                                                    customWeekdaysCsv = updated.sorted().joinToString(",")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = DateTimeUtil.getDayOfWeekShortLabel(isoDay).take(1),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = if (isDaySelected) PrimaryDarkText else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // DatePicker Modal
    if (showDatePicker) {
        val initialMillis = DateTimeUtil.epochDayToUtcPickerMillis(dueDateEpochDay)
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            dueDateEpochDay = DateTimeUtil.utcPickerMillisToEpochDay(utcMillis)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    ),
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag("date_picker_cancel_button")
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Modal for Due Time
    if (showTimePicker) {
        val currentMins = dueTimeMinutes ?: (11 * 60 + 30)
        val timePickerState = rememberTimePickerState(
            initialHour = currentMins / 60,
            initialMinute = currentMins % 60,
            is24Hour = use24HourFormat
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = CharcoalSurface,
            title = { Text("Select Due Time", color = Color.White) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        dueTimeMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    )
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Custom Reminder Time Picker
    if (showCustomReminderTimePicker) {
        val currentMins = customReminderMinutes ?: dueTimeMinutes ?: (9 * 60)
        val timePickerState = rememberTimePickerState(
            initialHour = currentMins / 60,
            initialMinute = currentMins % 60,
            is24Hour = use24HourFormat
        )
        AlertDialog(
            onDismissRequest = { showCustomReminderTimePicker = false },
            containerColor = CharcoalSurface,
            title = { Text("Custom Reminder Time", color = Color.White) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        customReminderMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showCustomReminderTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    )
                ) {
                    Text("Save Reminder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderTimePicker = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Quick Create Category Dialog
    if (showNewCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatColor by remember { mutableStateOf(StarterCategoryColors.first()) }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Create Category", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text("Category name", color = SecondaryText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonLime,
                            unfocusedBorderColor = CharcoalBorder
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StarterCategoryColors.take(6).forEach { hex ->
                            val selected = newCatColor == hex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newCatColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onCreateCategory(newCatName.trim(), newCatColor)
                            showNewCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Discard Unsaved Changes Confirmation Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = CharcoalSurface,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.testTag("discard_changes_dialog"),
            title = {
                Text(
                    text = "Discard changes?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes to this task. Would you like to keep editing or discard them?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.testTag("keep_editing_button")
                ) {
                    Text(
                        text = "Keep editing",
                        color = NeonLime,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        saveCoordinator.reset()
                        onCancel()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OverdueRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("discard_changes_button")
                ) {
                    Text(
                        text = "Discard changes",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        )
    }
}
