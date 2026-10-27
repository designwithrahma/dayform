package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskEntity
import com.example.ui.DayformUiState
import com.example.ui.components.TaskItemCard
import com.example.ui.navigation.DayformRoutes
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.DeadlineOrange
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.pressScaleClickable
import com.example.util.DateTimeUtil
import java.util.Calendar

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UpcomingCalendarView(
    uiState: DayformUiState,
    onBackToList: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onOpenTaskDetails: (taskId: Long, origin: String) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onCreateTaskForDate: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val todayEpochDay = DateTimeUtil.currentLocalEpochDay()
    val selectedDay = uiState.selectedCalendarEpochDay

    var showFullMonthCalendar by rememberSaveable { mutableStateOf(true) }

    val initialCal = remember(selectedDay) { DateTimeUtil.epochDayToCalendar(selectedDay) }
    var displayedYear by rememberSaveable { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayedMonth by rememberSaveable { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

    val tasksByEpochDay = remember(uiState.allTasks) {
        uiState.allTasks.groupBy { it.task.dueDateEpochDay }
    }

    val overdueTasks = remember(uiState.allTasks, todayEpochDay) {
        uiState.allTasks.filter {
            DateTimeUtil.isTaskOverdue(
                it.task.dueDateEpochDay,
                it.task.dueTimeMinutes,
                it.task.isCompleted
            )
        }
    }

    val selectedDayTasks = tasksByEpochDay[selectedDay] ?: emptyList()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 20.dp)
            .testTag("upcoming_calendar_view"),
        contentPadding = PaddingValues(top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header bar with Back to List, "Upcoming & Calendar", Jump to Today, and Month/Strip toggle
        item(key = "cal_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToList,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CharcoalElevated)
                            .testTag("calendar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to task list",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Upcoming & Calendar",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Jump to Today pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CharcoalElevated)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(50))
                            .pressScaleClickable(
                                pressedScale = 0.94f,
                                onClick = {
                                    val todayCal = DateTimeUtil.epochDayToCalendar(todayEpochDay)
                                    displayedYear = todayCal.get(Calendar.YEAR)
                                    displayedMonth = todayCal.get(Calendar.MONTH)
                                    onSelectDate(todayEpochDay)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("calendar_jump_today_button")
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonLime
                        )
                    }

                    // Toggle between horizontal strip and full monthly grid
                    IconButton(
                        onClick = { showFullMonthCalendar = !showFullMonthCalendar },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CharcoalElevated)
                    ) {
                        Icon(
                            imageVector = if (showFullMonthCalendar) {
                                Icons.Outlined.ViewWeek
                            } else {
                                Icons.Outlined.CalendarMonth
                            },
                            contentDescription = "Toggle monthly calendar grid",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Compact Horizontal Date Selector Strip (14 days around today)
        item(key = "cal_date_strip") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                (-2..11).forEach { offset ->
                    val dayEpoch = todayEpochDay + offset
                    val cal = DateTimeUtil.epochDayToCalendar(dayEpoch)
                    val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
                    val shortWeekday = DateTimeUtil.getDayOfWeekShortLabel(
                        DateTimeUtil.getIsoDayOfWeek(dayEpoch)
                    )
                    val isSelected = dayEpoch == selectedDay
                    val hasTasks = !tasksByEpochDay[dayEpoch].isNullOrEmpty()

                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) NeonLime else CharcoalSurface,
                        animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
                        label = "strip_day_bg_$offset"
                    )

                    Column(
                        modifier = Modifier
                            .width(58.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillBg)
                            .border(
                                width = 1.dp,
                                color = if (dayEpoch == todayEpochDay && !isSelected) NeonLime else CharcoalBorder,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .pressScaleClickable(
                                pressedScale = 0.95f,
                                onClick = { onSelectDate(dayEpoch) }
                            )
                            .padding(vertical = 12.dp)
                            .testTag("calendar_strip_day_$offset"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shortWeekday,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PrimaryDarkText.copy(alpha = 0.75f) else SecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNumber.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (isSelected) PrimaryDarkText else Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !hasTasks -> Color.Transparent
                                        isSelected -> PrimaryDarkText
                                        else -> NeonLime
                                    }
                                )
                        )
                    }
                }
            }
        }

        // 3. Full Monthly Calendar Grid (Expandable with animated month transitions)
        item(key = "cal_month_grid") {
            AnimatedVisibility(
                visible = showFullMonthCalendar,
                enter = DayformMotion.inlineBannerEnter(reducedMotion),
                exit = DayformMotion.inlineBannerExit(reducedMotion)
            ) {
                val weekdayHeaders = if (uiState.settings.weekStartsOnMonday) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                } else {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
                        .padding(18.dp)
                ) {
                    // Month navigation header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month",
                                tint = Color.White
                            )
                        }

                        AnimatedContent(
                            targetState = displayedYear to displayedMonth,
                            transitionSpec = {
                                if (reducedMotion) {
                                    fadeIn(snap()) togetherWith fadeOut(snap())
                                } else {
                                    val isForward = targetState.first > initialState.first ||
                                        (targetState.first == initialState.first && targetState.second > initialState.second)
                                    val dir = if (isForward) 1 else -1
                                    (fadeIn(tween(DayformMotion.SMALL_STATE_MS)) +
                                        slideInHorizontally(tween(DayformMotion.SMALL_STATE_MS)) { dir * (it / 4) }) togetherWith
                                        (fadeOut(tween(DayformMotion.QUICK_FEEDBACK_MS)) +
                                            slideOutHorizontally(tween(DayformMotion.QUICK_FEEDBACK_MS)) { -dir * (it / 4) })
                                }
                            },
                            label = "month_title_anim"
                        ) { (yr, mo) ->
                            Text(
                                text = DateTimeUtil.getMonthYearTitle(yr, mo),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Weekday header row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdayHeaders.forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated month cells grid
                    AnimatedContent(
                        targetState = displayedYear to displayedMonth,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(snap()) togetherWith fadeOut(snap())
                            } else {
                                val isForward = targetState.first > initialState.first ||
                                    (targetState.first == initialState.first && targetState.second > initialState.second)
                                val dir = if (isForward) 1 else -1
                                (fadeIn(tween(DayformMotion.SMALL_STATE_MS)) +
                                    slideInHorizontally(
                                        tween(DayformMotion.SMALL_STATE_MS, easing = FastOutSlowInEasing)
                                    ) { dir * (it / 8) }) togetherWith
                                    (fadeOut(tween(DayformMotion.QUICK_FEEDBACK_MS)) +
                                        slideOutHorizontally(
                                            tween(DayformMotion.QUICK_FEEDBACK_MS)
                                        ) { -dir * (it / 8) })
                            }
                        },
                        label = "month_grid_anim"
                    ) { (yr, mo) ->
                        val monthCells = remember(yr, mo, uiState.settings.weekStartsOnMonday) {
                            DateTimeUtil.buildMonthCalendarCells(
                                year = yr,
                                monthZeroBased = mo,
                                weekStartsOnMonday = uiState.settings.weekStartsOnMonday
                            )
                        }

                        Column {
                            monthCells.chunked(7).forEach { weekRow ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    weekRow.forEach { epochDay ->
                                        if (epochDay == null) {
                                            Spacer(modifier = Modifier.weight(1f).height(44.dp))
                                        } else {
                                            val cal = DateTimeUtil.epochDayToCalendar(epochDay)
                                            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                                            val isSelected = epochDay == selectedDay
                                            val isToday = epochDay == todayEpochDay
                                            val dayTasks = tasksByEpochDay[epochDay] ?: emptyList()
                                            val hasIncomplete = dayTasks.any { !it.task.isCompleted }
                                            val hasCompleted = dayTasks.any { it.task.isCompleted }

                                            val cellBg by animateColorAsState(
                                                targetValue = when {
                                                    isSelected -> NeonLime
                                                    isToday -> CharcoalElevated
                                                    else -> Color.Transparent
                                                },
                                                animationSpec = if (reducedMotion) snap() else tween(DayformMotion.QUICK_FEEDBACK_MS),
                                                label = "cell_bg_$epochDay"
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(cellBg)
                                                    .clickable { onSelectDate(epochDay) },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium
                                                    ),
                                                    color = when {
                                                        isSelected -> PrimaryDarkText
                                                        isToday -> NeonLime
                                                        else -> Color.White
                                                    }
                                                )
                                                if (dayTasks.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when {
                                                                    isSelected -> PrimaryDarkText
                                                                    hasIncomplete -> DeadlineOrange
                                                                    hasCompleted -> NeonLime
                                                                    else -> SecondaryText
                                                                }
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Selected Date Tasks Section
        item(key = "cal_selected_date_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = DateTimeUtil.formatFullDateHeader(selectedDay),
                    transitionSpec = {
                        fadeIn(DayformMotion.stateTween(reducedMotion)) togetherWith
                            fadeOut(DayformMotion.microTween(reducedMotion))
                    },
                    label = "cal_selected_date_header_anim"
                ) { headerText ->
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                }

                Text(
                    text = "+ Add on this day",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = NeonLime,
                    modifier = Modifier
                        .pressScaleClickable(
                            pressedScale = 0.94f,
                            onClick = { onCreateTaskForDate(selectedDay) }
                        )
                        .padding(4.dp)
                        .testTag("calendar_add_on_day_button")
                )
            }
        }

        if (selectedDayTasks.isEmpty()) {
            item(key = "cal_selected_empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CharcoalSurface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No tasks scheduled for ${DateTimeUtil.formatShortMonthDay(selectedDay)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onCreateTaskForDate(selectedDay) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonLime,
                            contentColor = PrimaryDarkText
                        ),
                        modifier = Modifier.testTag("calendar_schedule_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedule Task", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        } else {
            items(
                items = selectedDayTasks,
                key = { "sel_${it.task.id}" },
                contentType = { "task_card" }
            ) { item ->
                val taskId = item.task.id
                val onCardClickRemembered = remember(taskId) {
                    { onOpenTaskDetails(taskId, DayformRoutes.CALENDAR) }
                }
                val onToggleRemembered = remember(item.task) {
                    { onToggleTaskComplete(item.task) }
                }
                TaskItemCard(
                    item = item,
                    use24Hour = uiState.settings.use24HourFormat,
                    onCardClick = onCardClickRemembered,
                    onToggleComplete = onToggleRemembered,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedOriginKey = DayformRoutes.CALENDAR,
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

        // 5. Overdue Tasks Section (if any exist)
        if (overdueTasks.isNotEmpty()) {
            item(key = "cal_overdue_header", contentType = "overdue_header") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Overdue Tasks (${overdueTasks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = OverdueRed
                )
            }
            items(
                items = overdueTasks,
                key = { "overdue_${it.task.id}" },
                contentType = { "task_card" }
            ) { item ->
                val taskId = item.task.id
                val onCardClickRemembered = remember(taskId) {
                    { onOpenTaskDetails(taskId, "calendar_overdue") }
                }
                val onToggleRemembered = remember(item.task) {
                    { onToggleTaskComplete(item.task) }
                }
                TaskItemCard(
                    item = item,
                    use24Hour = uiState.settings.use24HourFormat,
                    onCardClick = onCardClickRemembered,
                    onToggleComplete = onToggleRemembered,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedOriginKey = "calendar_overdue",
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
}
