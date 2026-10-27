package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ReminderOffset
import com.example.data.model.TaskEntity
import com.example.ui.DayformUiState
import com.example.ui.components.AsymmetricStatCards
import com.example.ui.components.TaskItemCard
import com.example.ui.navigation.DayformRoutes
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.LightText
import com.example.ui.theme.NeonLime
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.util.DateTimeUtil

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    uiState: DayformUiState,
    onOpenTaskDetails: (Long) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onCreateTaskClick: () -> Unit,
    onCompletedStatClick: () -> Unit,
    onPlannedStatClick: () -> Unit,
    onViewAllTasksClick: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    hasPlayedInitialEntrance: Boolean = true,
    onMarkInitialEntrancePlayed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val displayName = uiState.settings.displayName.trim()
    val greetingTitle = if (displayName.isNotEmpty()) {
        stringResource(R.string.greeting_prefix, displayName)
    } else {
        "Hello, there"
    }

    val currentOnOpenTaskDetails by rememberUpdatedState(onOpenTaskDetails)
    val currentOnToggleTaskComplete by rememberUpdatedState(onToggleTaskComplete)

    // One-time subtle initial entrance fade/offset on cold launch (never re-runs on tab return)
    val shouldAnimateEntrance = !hasPlayedInitialEntrance && !reducedMotion
    val entranceProgress = remember { Animatable(if (shouldAnimateEntrance) 0f else 1f) }
    LaunchedEffect(hasPlayedInitialEntrance, reducedMotion) {
        if (!hasPlayedInitialEntrance) {
            if (!reducedMotion) {
                entranceProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = DayformMotion.TAB_NAV_MS,
                        easing = DayformMotion.EmphasizedDecelerate
                    )
                )
            } else {
                entranceProgress.snapTo(1f)
            }
            onMarkInitialEntrancePlayed()
        }
    }

    var showRemindersSheet by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }

    val reminderTasks = remember(uiState.allTasks) {
        uiState.allTasks.filter {
            !it.task.isCompleted && it.task.reminderEnum != ReminderOffset.NONE
        }
    }

    val bellInteraction = remember { MutableInteractionSource() }
    val isBellPressed by bellInteraction.collectIsPressedAsState()
    val bellScaleState = animateFloatAsState(
        targetValue = if (isBellPressed && !reducedMotion) 0.94f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "bellPressScale"
    )

    BackHandler(enabled = showRemindersSheet) {
        showRemindersSheet = false
    }

    val entranceLayerModifier = if (entranceProgress.isRunning || entranceProgress.value < 1f) {
        Modifier.graphicsLayer {
            alpha = entranceProgress.value
            translationY = (1f - entranceProgress.value) * 18f
        }
    } else {
        Modifier
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .then(entranceLayerModifier)
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 128.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Compact Header Row: Greeting & Subtitle on Left, Notification Bell on Top-Right
        item(key = "header", contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("home_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = greetingTitle,
                        style = MaterialTheme.typography.displayMedium,
                        color = LightText,
                        modifier = Modifier.testTag("home_greeting_title")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.greeting_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryText
                    )
                }

                // Notification Bell Button with Active Indicator Dot
                Surface(
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            val s = bellScaleState.value
                            scaleX = s
                            scaleY = s
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = bellInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = { showRemindersSheet = true }
                        )
                        .semantics {
                            contentDescription = "View scheduled reminders (${uiState.pendingRemindersCount} active)"
                        }
                        .testTag("home_notification_bell"),
                    shape = CircleShape,
                    color = CharcoalSurface,
                    border = BorderStroke(1.dp, CharcoalBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = LightText,
                            modifier = Modifier.size(24.dp)
                        )
                        if (uiState.pendingRemindersCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-12).dp, y = 12.dp)
                                    .size(10.dp)
                                    .background(NeonLime, CircleShape)
                                    .testTag("home_notification_badge")
                            )
                        }
                    }
                }
            }
        }

        // 2. Asymmetric Stat Cards + Today's Progress Card
        item(key = "stat_cards", contentType = "stat_cards") {
            AsymmetricStatCards(
                completedCount = uiState.completedAllTimeCount,
                plannedCount = uiState.plannedTodayCount,
                completedPlannedTodayCount = uiState.completedPlannedTodayCount,
                onCompletedCardClick = onCompletedStatClick,
                onPlannedCardClick = onPlannedStatClick
            )
        }

        // 3. Priority Tasks Section Header (Circular Lime Count Badge on Left + Title + More Menu on Right)
        item(key = "priority_header", contentType = "priority_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CharcoalSurface, CircleShape)
                            .border(1.5.dp, NeonLime, CircleShape)
                            .semantics(mergeDescendants = true) {}
                            .testTag("priority_tasks_count_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.priorityTasks.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonLime,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.priority_tasks_heading),
                        style = MaterialTheme.typography.headlineMedium,
                        color = LightText
                    )
                }

                Box {
                    IconButton(
                        onClick = { showPriorityMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("priority_tasks_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More priority task options",
                            tint = SecondaryText
                        )
                    }

                    DropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false },
                        containerColor = CharcoalElevated
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "View all tasks",
                                    color = LightText
                                )
                            },
                            onClick = {
                                showPriorityMenu = false
                                onViewAllTasksClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Create new task",
                                    color = NeonLime
                                )
                            },
                            onClick = {
                                showPriorityMenu = false
                                onCreateTaskClick()
                            }
                        )
                    }
                }
            }
        }

        // 4. Priority Task Cards or Empty State
        if (uiState.priorityTasks.isEmpty()) {
            item(key = "empty_state", contentType = "empty_state") {
                HomeEmptyStateCard(
                    onCreateTaskClick = onCreateTaskClick
                )
            }
        } else {
            items(
                items = uiState.priorityTasks,
                key = { it.task.id },
                contentType = { "task_card" }
            ) { taskWithDetails ->
                val taskId = taskWithDetails.task.id
                val onCardClickRemembered = remember(taskId) {
                    { currentOnOpenTaskDetails(taskId) }
                }
                val onToggleRemembered = remember(taskWithDetails.task) {
                    { currentOnToggleTaskComplete(taskWithDetails.task) }
                }
                TaskItemCard(
                    item = taskWithDetails,
                    use24Hour = uiState.settings.use24HourFormat,
                    onCardClick = onCardClickRemembered,
                    onToggleComplete = onToggleRemembered,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedOriginKey = DayformRoutes.HOME,
                    modifier = if (reducedMotion || listState.isScrollInProgress) {
                        Modifier
                    } else {
                        Modifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = DayformMotion.itemPlacementSpec(false)
                        )
                    }
                )
            }
        }
    }

    // Scheduled Reminders Bottom Sheet
    if (showRemindersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRemindersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CharcoalSurface,
            contentColor = LightText
        ) {
            BackHandler {
                showRemindersSheet = false
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Scheduled Reminders",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LightText
                )
                Text(
                    text = if (reminderTasks.isEmpty()) {
                        "No active task reminders scheduled."
                    } else {
                        "You have ${reminderTasks.size} active task reminder(s)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                reminderTasks.forEach { item ->
                    Surface(
                        onClick = {
                            showRemindersSheet = false
                            onOpenTaskDetails(item.task.id)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = CharcoalElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = LightText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${DateTimeUtil.formatDayWithOrdinalMonth(item.task.dueDateEpochDay)} • ${item.task.reminderEnum.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonLime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyStateCard(
    onCreateTaskClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_empty_state"),
        shape = RoundedCornerShape(28.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(NeonLime.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonLime,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your day is a blank canvas",
                style = MaterialTheme.typography.headlineSmall,
                color = LightText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your first task to start organizing your priorities, deadlines, and subtasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateTaskClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonLime,
                    contentColor = PrimaryDarkText
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("empty_create_task_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.create_first_task),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
