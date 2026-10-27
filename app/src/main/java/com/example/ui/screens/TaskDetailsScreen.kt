package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskWithDetails
import com.example.ui.navigation.DayformRoutes
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonLimeDarkText
import com.example.ui.theme.OffWhiteCardBorder
import com.example.ui.theme.OffWhiteSurface
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SubtaskFooterGrey
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.pressScaleClickable
import com.example.util.DateTimeUtil

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun TaskDetailsScreen(
    taskWithDetails: TaskWithDetails,
    categories: List<CategoryEntity>,
    use24HourFormat: Boolean,
    hideCompletedSubtasks: Boolean,
    onBack: () -> Unit,
    onEditTask: (Long) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onUpdateDescription: (Long, String) -> Unit,
    onUpdatePriority: (Long, TaskPriority) -> Unit,
    onUpdateCategory: (Long, Long?) -> Unit,
    onUpdateReminder: (Long, ReminderOffset) -> Unit,
    onDuplicateTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onAddSubtask: (Long, String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onEditSubtaskTitle: (SubtaskEntity, String) -> Unit,
    onDeleteSubtask: (Long) -> Unit,
    onMoveSubtask: (Long, Long, Boolean) -> Unit,
    onToggleHideCompletedSubtasks: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedOriginKey: String = DayformRoutes.HOME
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val context = LocalContext.current
    val task = taskWithDetails.task
    val subtasks = taskWithDetails.subtasks

    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var isEditingDescription by rememberSaveable { mutableStateOf(false) }
    var descriptionDraft by rememberSaveable(task.description) { mutableStateOf(task.description) }

    var newSubtaskTitle by rememberSaveable { mutableStateOf("") }
    var editingSubtask by remember { mutableStateOf<SubtaskEntity?>(null) }
    var editingSubtaskText by remember { mutableStateOf("") }

    val formattedDueDate = DateTimeUtil.formatDayWithOrdinalMonth(task.dueDateEpochDay)
    val formattedDueTime = task.dueTimeMinutes?.let { DateTimeUtil.formatTime(it, use24HourFormat) }

    val visibleSubtasks = if (hideCompletedSubtasks) {
        subtasks.filter { !it.isCompleted }
    } else {
        subtasks
    }

    // Shared-bounds modifiers connecting TaskItemCard <-> TaskDetailsScreen
    val sharedTitleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "task_title_bounds_${sharedOriginKey}_${task.id}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = DayformMotion.taskCardBoundsTransform(reducedMotion),
                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
            )
        }
    } else {
        Modifier
    }

    val sharedCardPanelModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "task_card_bounds_${sharedOriginKey}_${task.id}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = DayformMotion.taskCardBoundsTransform(reducedMotion),
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(
                    RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 34.dp, bottomEnd = 34.dp)
                )
            )
        }
    } else {
        Modifier
    }

    val supportingHeaderEnterExitModifier = if (animatedVisibilityScope != null && !reducedMotion) {
        with(animatedVisibilityScope) {
            Modifier.animateEnterExit(
                enter = fadeIn(tween(DayformMotion.SUPPORTING_CONTENT_MS)) +
                    slideInVertically(
                        animationSpec = tween(DayformMotion.SUPPORTING_CONTENT_MS, easing = FastOutSlowInEasing),
                        initialOffsetY = { -it / 5 }
                    ),
                exit = fadeOut(tween(DayformMotion.QUICK_FEEDBACK_MS)) +
                    slideOutVertically(
                        animationSpec = tween(DayformMotion.QUICK_FEEDBACK_MS),
                        targetOffsetY = { -it / 6 }
                    )
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .testTag("task_details_screen")
    ) {
        // 1. TOP BAR: Back button, centered "Task Details", Reminder Bell on right
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(supportingHeaderEnterExitModifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CharcoalElevated)
                        .pressScaleClickable(pressedScale = 0.92f, onClick = onBack)
                        .semantics { contentDescription = "Navigate back" }
                        .testTag("details_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Task Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )

                // Reminder button matching reference image (dark circle with bell + lime indicator dot)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CharcoalElevated)
                        .pressScaleClickable(
                            pressedScale = 0.92f,
                            onClick = { showReminderDialog = true }
                        )
                        .semantics { contentDescription = "Configure task reminder" }
                        .testTag("details_reminder_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    if (task.reminderEnum != ReminderOffset.NONE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 11.dp, end = 12.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonLime)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. LARGE EXPRESSIVE TASK TITLE (Shared bounds with TaskItemCard title)
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                modifier = sharedTitleModifier.testTag("details_task_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. DUE DATE & METADATA ROW matching reference image
            Row(
                modifier = supportingHeaderEnterExitModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CharcoalElevated)
                        .border(1.dp, CharcoalBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = NeonLime,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (formattedDueTime != null) {
                            "Due Date: $formattedDueDate • $formattedDueTime"
                        } else {
                            "Due Date: $formattedDueDate"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("Priority: ${task.priorityEnum.label}")
                            taskWithDetails.category?.let { append(" • ${it.name}") }
                            if (task.recurrenceEnum != RecurrenceType.NEVER) {
                                append(" • ${task.recurrenceEnum.label}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                // Complete / Reopen quick pill button
                val completeBg by animateColorAsState(
                    targetValue = if (task.isCompleted) NeonLime else CharcoalElevated,
                    animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
                    label = "details_complete_bg"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(completeBg)
                        .border(
                            width = 1.dp,
                            color = if (task.isCompleted) NeonLime else CharcoalBorder,
                            shape = RoundedCornerShape(50)
                        )
                        .pressScaleClickable(
                            pressedScale = 0.94f,
                            onClick = { onToggleTaskComplete(task) }
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("details_toggle_complete_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (task.isCompleted) PrimaryDarkText else NeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (task.isCompleted) "Completed" else "Mark Done",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (task.isCompleted) PrimaryDarkText else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. LAYERED LIME DESCRIPTION PANEL + CONNECTED OFF-WHITE SUBTASKS PANEL
        // Shared bounds with the TaskItemCard container so the card expands into this layered view!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(sharedCardPanelModifier)
        ) {
            // LIME DESCRIPTION CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(NeonLime)
                    .animateContentSize(animationSpec = DayformMotion.contentSizeSpec(reducedMotion))
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 46.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Centered dark drag handle indicator at top of lime card
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PrimaryDarkText.copy(alpha = 0.75f))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            ),
                            color = PrimaryDarkText
                        )

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("details_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Task actions menu",
                                    tint = PrimaryDarkText
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = CharcoalElevated
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit full task", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = NeonLime)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onEditTask(task.id)
                                    },
                                    modifier = Modifier.testTag("menu_edit_task")
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (task.isCompleted) "Reopen task" else "Complete task",
                                            color = Color.White
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonLime)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onToggleTaskComplete(task)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Change priority", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = Color.White)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showPriorityDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to category", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Color.White)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showCategoryDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Set reminder", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = Color.White)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showReminderDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate task", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color.White)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onDuplicateTask(task.id)
                                    },
                                    modifier = Modifier.testTag("menu_duplicate_task")
                                )
                                DropdownMenuItem(
                                    text = { Text("Share task", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, contentDescription = null, tint = Color.White)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        val shareBody = buildString {
                                            append("Task: ${task.title}\n")
                                            append("Due: $formattedDueDate")
                                            if (formattedDueTime != null) append(" at $formattedDueTime")
                                            append("\nPriority: ${task.priorityEnum.label}\n")
                                            if (task.description.isNotBlank()) {
                                                append("\n${task.description}\n")
                                            }
                                            if (subtasks.isNotEmpty()) {
                                                append("\nSubtasks:\n")
                                                subtasks.forEach { s ->
                                                    val box = if (s.isCompleted) "[x]" else "[ ]"
                                                    append("$box ${s.title}\n")
                                                }
                                            }
                                        }
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, task.title)
                                            putExtra(Intent.EXTRA_TEXT, shareBody)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Share task")
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete task", color = OverdueRed) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = OverdueRed)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteConfirm = true
                                    },
                                    modifier = Modifier.testTag("menu_delete_task")
                                )
                            }
                        }
                    }

                    if (isEditingDescription) {
                        OutlinedTextField(
                            value = descriptionDraft,
                            onValueChange = { descriptionDraft = it },
                            placeholder = {
                                Text("Add notes or description...", color = NeonLimeDarkText.copy(alpha = 0.6f))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.45f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.35f),
                                focusedBorderColor = PrimaryDarkText,
                                unfocusedBorderColor = PrimaryDarkText.copy(alpha = 0.3f),
                                focusedTextColor = PrimaryDarkText,
                                unfocusedTextColor = PrimaryDarkText
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("details_description_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                descriptionDraft = task.description
                                isEditingDescription = false
                            }) {
                                Text("Cancel", color = PrimaryDarkText)
                            }
                            Button(
                                onClick = {
                                    onUpdateDescription(task.id, descriptionDraft)
                                    isEditingDescription = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryDarkText,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("details_save_description_button")
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        Text(
                            text = task.description.ifBlank {
                                "Tap here to add a description, project notes, or requirements..."
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = NeonLimeDarkText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    descriptionDraft = task.description
                                    isEditingDescription = true
                                }
                                .padding(vertical = 6.dp)
                                .testTag("details_description_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive metadata chips inside the Lime panel
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PrimaryDarkText.copy(alpha = 0.1f))
                                .pressScaleClickable(
                                    pressedScale = 0.94f,
                                    onClick = { showPriorityDialog = true }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Priority: ${task.priorityEnum.label}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PrimaryDarkText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PrimaryDarkText.copy(alpha = 0.1f))
                                .pressScaleClickable(
                                    pressedScale = 0.94f,
                                    onClick = { showCategoryDialog = true }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Category: ${taskWithDetails.category?.name ?: "Uncategorized"}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PrimaryDarkText
                            )
                        }

                        if (task.recurrenceEnum != RecurrenceType.NEVER) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(PrimaryDarkText.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Repeat,
                                    contentDescription = null,
                                    tint = PrimaryDarkText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = task.recurrenceEnum.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PrimaryDarkText
                                )
                            }
                        }
                    }
                }
            }

            // CONNECTED OFF-WHITE SUB TASKS CARD (overlapping the Lime card by 28.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 34.dp, bottomEnd = 34.dp))
                    .background(OffWhiteSurface)
                    .animateContentSize(animationSpec = DayformMotion.contentSizeSpec(reducedMotion))
                    .padding(horizontal = 22.dp, vertical = 26.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sub Tasks",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            ),
                            color = PrimaryDarkText
                        )
                        if (subtasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(PrimaryDarkText)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                AnimatedContent(
                                    targetState = "${taskWithDetails.completedSubtasksCount}/${taskWithDetails.totalSubtasksCount}",
                                    transitionSpec = DayformMotion.counterRollTransition(reducedMotion),
                                    label = "subtask_progress_anim"
                                ) { progressText ->
                                    Text(
                                        text = progressText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = NeonLime,
                                        modifier = Modifier.testTag("details_subtask_progress_badge")
                                    )
                                }
                            }
                        }
                    }

                    if (subtasks.any { it.isCompleted }) {
                        IconButton(
                            onClick = { onToggleHideCompletedSubtasks(!hideCompletedSubtasks) },
                            modifier = Modifier.testTag("toggle_hide_completed_subtasks")
                        ) {
                            Icon(
                                imageVector = if (hideCompletedSubtasks) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (hideCompletedSubtasks) {
                                    "Show completed subtasks"
                                } else {
                                    "Hide completed subtasks"
                                },
                                tint = SecondaryDarkText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (visibleSubtasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, OffWhiteCardBorder, RoundedCornerShape(22.dp))
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hideCompletedSubtasks && subtasks.isNotEmpty()) {
                                "All ${subtasks.size} subtasks are completed (hidden)."
                            } else {
                                "Break this task into smaller actionable steps below."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryDarkText
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visibleSubtasks.forEachIndexed { index, subtask ->
                            key(subtask.id) {
                                SubtaskPillRow(
                                    subtask = subtask,
                                    reducedMotion = reducedMotion,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < visibleSubtasks.lastIndex,
                                    onToggle = { onToggleSubtask(subtask) },
                                    onEditClick = {
                                        editingSubtask = subtask
                                        editingSubtaskText = subtask.title
                                    },
                                    onMoveUp = { onMoveSubtask(task.id, subtask.id, true) },
                                    onMoveDown = { onMoveSubtask(task.id, subtask.id, false) },
                                    onDelete = { onDeleteSubtask(subtask.id) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Signature "Add task to list" grey pill row from reference image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(SubtaskFooterGrey)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .pressScaleClickable(
                                pressedScale = 0.90f,
                                onClick = {
                                    if (newSubtaskTitle.isNotBlank()) {
                                        onAddSubtask(task.id, newSubtaskTitle)
                                        newSubtaskTitle = ""
                                    }
                                }
                            )
                            .semantics { contentDescription = "Add subtask" }
                            .testTag("details_add_subtask_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PrimaryDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = {
                            Text(
                                text = "Add task to list",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PrimaryDarkText.copy(alpha = 0.65f)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = PrimaryDarkText,
                            unfocusedTextColor = PrimaryDarkText,
                            cursorColor = PrimaryDarkText
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newSubtaskTitle.isNotBlank()) {
                                    onAddSubtask(task.id, newSubtaskTitle)
                                    newSubtaskTitle = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("details_new_subtask_input")
                    )
                }
            }
        }
    }

    // Edit Subtask Dialog
    editingSubtask?.let { targetSubtask ->
        AlertDialog(
            onDismissRequest = { editingSubtask = null },
            containerColor = CharcoalSurface,
            title = { Text("Edit Subtask", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editingSubtaskText,
                    onValueChange = { editingSubtaskText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonLime,
                        unfocusedBorderColor = CharcoalBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditSubtaskTitle(targetSubtask, editingSubtaskText)
                        editingSubtask = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = PrimaryDarkText
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSubtask = null }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Delete Task Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CharcoalSurface,
            title = { Text("Delete Task?", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to delete '${task.title}' and its subtasks?",
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteTask(task.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OverdueRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_task_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Priority Picker Dialog
    if (showPriorityDialog) {
        AlertDialog(
            onDismissRequest = { showPriorityDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Select Priority", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { priority ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (task.priorityEnum == priority) NeonLime else CharcoalElevated
                                )
                                .clickable {
                                    onUpdatePriority(task.id, priority)
                                    showPriorityDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = priority.label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (task.priorityEnum == priority) PrimaryDarkText else Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPriorityDialog = false }) {
                    Text("Close", color = SecondaryText)
                }
            }
        )
    }

    // Category Picker Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Move to Category", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (task.categoryId == null) NeonLime else CharcoalElevated
                            )
                            .clickable {
                                onUpdateCategory(task.id, null)
                                showCategoryDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Uncategorized",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (task.categoryId == null) PrimaryDarkText else Color.White
                        )
                    }

                    categories.forEach { category ->
                        val selected = task.categoryId == category.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) NeonLime else CharcoalElevated)
                                .clickable {
                                    onUpdateCategory(task.id, category.id)
                                    showCategoryDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(category.colorHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selected) PrimaryDarkText else Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Close", color = SecondaryText)
                }
            }
        )
    }

    // Reminder Offset Dialog
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Task Reminder", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderOffset.entries.filter { it != ReminderOffset.CUSTOM }.forEach { offset ->
                        val selected = task.reminderEnum == offset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) NeonLime else CharcoalElevated)
                                .clickable {
                                    onUpdateReminder(task.id, offset)
                                    showReminderDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = offset.label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selected) PrimaryDarkText else Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Close", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
private fun SubtaskPillRow(
    subtask: SubtaskEntity,
    reducedMotion: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onEditClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (subtask.isCompleted) 1.06f else 1f,
        animationSpec = DayformMotion.bouncyCheckSpring(reducedMotion),
        label = "subtask_check_scale"
    )
    val circleBg by animateColorAsState(
        targetValue = if (subtask.isCompleted) PrimaryDarkText else Color.Transparent,
        animationSpec = DayformMotion.stateTween(reducedMotion),
        label = "subtask_circle_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (subtask.isCompleted) SecondaryDarkText else PrimaryDarkText,
        animationSpec = DayformMotion.stateTween(reducedMotion),
        label = "subtask_text_color"
    )
    val rowBg by animateColorAsState(
        targetValue = if (subtask.isCompleted) Color.White.copy(alpha = 0.86f) else Color.White,
        animationSpec = DayformMotion.stateTween(reducedMotion),
        label = "subtask_row_bg"
    )
    val rowBorder by animateColorAsState(
        targetValue = if (subtask.isCompleted) OffWhiteCardBorder.copy(alpha = 0.55f) else OffWhiteCardBorder,
        animationSpec = DayformMotion.stateTween(reducedMotion),
        label = "subtask_row_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("subtask_row_${subtask.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular completion control matching right phone in reference image
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .pressScaleClickable(pressedScale = 0.88f, onClick = onToggle)
                .semantics {
                    contentDescription = if (subtask.isCompleted) {
                        "Mark subtask ${subtask.title} incomplete"
                    } else {
                        "Complete subtask ${subtask.title}"
                    }
                }
                .testTag("subtask_toggle_${subtask.id}"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(circleBg)
                    .border(1.8.dp, PrimaryDarkText, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = subtask.isCompleted,
                    enter = if (reducedMotion) EnterTransition.None else scaleIn(tween(150)) + fadeIn(tween(150)),
                    exit = if (reducedMotion) ExitTransition.None else scaleOut(tween(120)) + fadeOut(tween(120))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = subtask.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (subtask.isCompleted) FontWeight.Medium else FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = textColor,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditClick)
        )

        // Reorder and delete actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canMoveUp) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move subtask up",
                        tint = SecondaryDarkText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (canMoveDown) {
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move subtask down",
                        tint = SecondaryDarkText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("subtask_delete_${subtask.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete subtask",
                    tint = SecondaryDarkText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
