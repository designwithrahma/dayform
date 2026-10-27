package com.example.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.TaskPriority
import com.example.data.model.TaskWithDetails
import com.example.ui.navigation.DayformRoutes
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.DeadlineOrange
import com.example.ui.theme.OffWhiteSurface
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryDarkText
import com.example.ui.theme.parseHexColor
import com.example.util.DateTimeUtil
import kotlinx.coroutines.delay

private val CardCornerShape = RoundedCornerShape(28.dp)
private val PillCornerShape = RoundedCornerShape(50)
private val SubtlePillBg = PrimaryDarkText.copy(alpha = 0.07f)
private val UrgentPillBg = OverdueRed.copy(alpha = 0.14f)
private val HighPillBg = DeadlineOrange.copy(alpha = 0.18f)
private val DefaultDeadlineBorderColor = SecondaryDarkText.copy(alpha = 0.45f)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    item: TaskWithDetails,
    use24Hour: Boolean,
    onCardClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedOriginKey: String = DayformRoutes.HOME
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val task = item.task
    val category = item.category
    val todayEpochDay = remember { DateTimeUtil.currentLocalEpochDay() }
    val isOverdue = remember(task.dueDateEpochDay, task.dueTimeMinutes, task.isCompleted, todayEpochDay) {
        !task.isCompleted && DateTimeUtil.isTaskOverdue(
            dueDateEpochDay = task.dueDateEpochDay,
            dueTimeMinutes = task.dueTimeMinutes,
            isCompleted = task.isCompleted,
            todayEpochDay = todayEpochDay
        )
    }

    val deadlineText = remember(task.dueDateEpochDay, task.dueTimeMinutes, use24Hour, todayEpochDay) {
        DateTimeUtil.formatTaskDuePill(
            dueDateEpochDay = task.dueDateEpochDay,
            dueTimeMinutes = task.dueTimeMinutes,
            use24Hour = use24Hour,
            todayEpochDay = todayEpochDay
        )
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScaleState = animateFloatAsState(
        targetValue = if (isCardPressed && !reducedMotion) 0.98f else 1f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "taskCardPressScale"
    )

    // Checkbox bounce pulse when completion state is toggled (rests at 1.0f)
    var previousCompleted by remember(task.id) { mutableStateOf(task.isCompleted) }
    var bounceActive by remember(task.id) { mutableStateOf(false) }
    val hasToggledCompletion = task.isCompleted != previousCompleted || bounceActive
    LaunchedEffect(task.isCompleted) {
        if (task.isCompleted != previousCompleted) {
            previousCompleted = task.isCompleted
            if (!reducedMotion) {
                bounceActive = true
                delay(170L)
                bounceActive = false
            }
        }
    }

    val titleColor = if (hasToggledCompletion && !reducedMotion) {
        animateColorAsState(
            targetValue = if (task.isCompleted) SecondaryDarkText else PrimaryDarkText,
            animationSpec = DayformMotion.stateTween(false),
            label = "taskTitleColor"
        ).value
    } else {
        if (task.isCompleted) SecondaryDarkText else PrimaryDarkText
    }

    val checkboxScaleState = animateFloatAsState(
        targetValue = if (bounceActive && !reducedMotion) 1.18f else 1f,
        animationSpec = DayformMotion.bouncyCheckSpring(reducedMotion),
        label = "checkboxBounceScale"
    )

    val checkboxFillColor = if (hasToggledCompletion && !reducedMotion) {
        animateColorAsState(
            targetValue = if (task.isCompleted) PrimaryDarkText else Color.Transparent,
            animationSpec = DayformMotion.stateTween(false),
            label = "checkboxFillColor"
        ).value
    } else {
        if (task.isCompleted) PrimaryDarkText else Color.Transparent
    }

    val sharedCardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "task_card_bounds_${sharedOriginKey}_${task.id}"
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = DayformMotion.taskCardBoundsTransform(reducedMotion),
                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                clipInOverlayDuringTransition = OverlayClip(CardCornerShape)
            )
        }
    } else {
        Modifier
    }

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val s = cardScaleState.value
                scaleX = s
                scaleY = s
            }
            .then(sharedCardModifier)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                role = Role.Button,
                onClick = onCardClick
            )
            .testTag("task_item_card_${task.id}"),
        shape = CardCornerShape,
        color = OffWhiteSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Title on Left, Circular Checkbox on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 28.sp
                    ),
                    color = titleColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(sharedTitleModifier)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Circular Checkbox on Right (48dp touch target, 36dp visual circle)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            val s = checkboxScaleState.value
                            scaleX = s
                            scaleY = s
                        }
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Checkbox,
                            onClick = onToggleComplete
                        )
                        .semantics {
                            contentDescription = if (task.isCompleted) {
                                "Mark ${task.title} incomplete"
                            } else {
                                "Mark ${task.title} completed"
                            }
                        }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(checkboxFillColor, CircleShape)
                            .border(
                                width = 2.dp,
                                color = PrimaryDarkText,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = task.isCompleted,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(tween(150)),
                            exit = scaleOut(tween(120)) + fadeOut(tween(120))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OffWhiteSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Pills FlowRow (Deadline + Category + Priority + Subtasks + Reminder/Recurrence)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Deadline Pill
                val pillBgColor: Color
                val pillTextColor: Color
                val hasBorder: Boolean
                val pillLabel: String

                if (isOverdue) {
                    pillBgColor = OverdueRed
                    pillTextColor = Color.White
                    hasBorder = false
                    pillLabel = "Overdue • $deadlineText"
                } else if (task.dueDateEpochDay == todayEpochDay) {
                    pillBgColor = DeadlineOrange
                    pillTextColor = Color.White
                    hasBorder = false
                    pillLabel = deadlineText
                } else {
                    pillBgColor = Color.Transparent
                    pillTextColor = PrimaryDarkText
                    hasBorder = true
                    pillLabel = deadlineText
                }

                Box(
                    modifier = if (hasBorder) {
                        Modifier
                            .border(1.dp, DefaultDeadlineBorderColor, PillCornerShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    } else {
                        Modifier
                            .background(pillBgColor, PillCornerShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    }
                ) {
                    Text(
                        text = pillLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = pillTextColor
                    )
                }

                // 2. Category Pill
                if (category != null) {
                    val catColor = remember(category.colorHex) {
                        parseHexColor(category.colorHex, PrimaryDarkText)
                    }
                    Row(
                        modifier = Modifier
                            .background(SubtlePillBg, PillCornerShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(catColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryDarkText
                        )
                    }
                }

                // 3. Priority Pill (Urgent or High)
                val priorityEnum = task.priorityEnum
                if (priorityEnum == TaskPriority.URGENT || priorityEnum == TaskPriority.HIGH) {
                    val priorityBg = if (priorityEnum == TaskPriority.URGENT) {
                        UrgentPillBg
                    } else {
                        HighPillBg
                    }
                    Box(
                        modifier = Modifier
                            .background(priorityBg, PillCornerShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = priorityEnum.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryDarkText
                        )
                    }
                }

                // 4. Subtasks Progress Pill
                if (item.totalSubtasksCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(SubtlePillBg, PillCornerShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${item.completedSubtasksCount}/${item.totalSubtasksCount} subtasks",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryDarkText
                        )
                    }
                }

                // 5. Reminder & Recurrence Icon Pill
                val hasReminder = task.reminderEnum != ReminderOffset.NONE
                val isRecurring = task.recurrenceEnum != RecurrenceType.NEVER
                if (hasReminder || isRecurring) {
                    Row(
                        modifier = Modifier
                            .background(SubtlePillBg, PillCornerShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasReminder) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = "Reminder set",
                                tint = PrimaryDarkText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (isRecurring) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring task",
                                tint = PrimaryDarkText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Description Preview (when non-blank and not in compact mode)
            if (!compact && task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryDarkText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
