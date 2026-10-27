package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ViewAgenda
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.TaskFilter
import com.example.data.model.TaskSortOption
import com.example.ui.DayformUiState
import com.example.ui.components.TaskItemCard
import com.example.ui.navigation.DayformRoutes
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.NeonLime
import com.example.ui.theme.PrimaryDarkText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.pressScaleClickable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AllTasksScreen(
    uiState: DayformUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (TaskFilter) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onSelectSort: (TaskSortOption) -> Unit,
    onToggleBoardLayout: () -> Unit,
    onOpenCalendarView: () -> Unit,
    onOpenTaskDetails: (Long) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onCreateTaskClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val reducedMotion = DayformMotion.rememberReducedMotion()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 20.dp)
            .testTag("all_tasks_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Header: "Your Tasks" + Calendar view button + Board/List toggle + Sort menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Tasks",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar / Upcoming view button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CharcoalElevated)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(50))
                        .pressScaleClickable(
                            pressedScale = 0.94f,
                            onClick = onOpenCalendarView
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("open_calendar_view_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar view",
                            tint = NeonLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Calendar",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }

                // List vs Compact Board layout toggle
                IconButton(
                    onClick = onToggleBoardLayout,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CharcoalElevated)
                        .testTag("toggle_board_layout_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isBoardLayout) {
                            Icons.Outlined.ViewAgenda
                        } else {
                            Icons.Outlined.GridView
                        },
                        contentDescription = if (uiState.isBoardLayout) "Switch to list layout" else "Switch to board layout",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Sort menu
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CharcoalElevated)
                            .testTag("tasks_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = "Sort tasks",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = CharcoalElevated
                    ) {
                        TaskSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Sort by: ${option.label}",
                                        color = if (uiState.selectedSort == option) NeonLime else Color.White,
                                        fontWeight = if (uiState.selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectSort(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Functional Search Field
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search titles, descriptions, categories...",
                    color = SecondaryText
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = SecondaryText
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = uiState.searchQuery.isNotEmpty(),
                    enter = DayformMotion.badgePopEnter(reducedMotion),
                    exit = DayformMotion.badgePopExit(reducedMotion)
                ) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = SecondaryText
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
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
                .testTag("tasks_search_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Filter Chips: All, Today, Upcoming, Completed, Overdue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilter.entries.forEach { filter ->
                val selected = uiState.selectedFilter == filter
                val chipBg by animateColorAsState(
                    targetValue = if (selected) NeonLime else CharcoalSurface,
                    animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
                    label = "filter_chip_bg_${filter.name}"
                )
                val chipTextColor by animateColorAsState(
                    targetValue = if (selected) PrimaryDarkText else Color.White,
                    animationSpec = if (reducedMotion) snap() else tween(DayformMotion.SMALL_STATE_MS),
                    label = "filter_chip_text_${filter.name}"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(chipBg)
                        .border(
                            width = 1.dp,
                            color = if (selected) NeonLime else CharcoalBorder,
                            shape = RoundedCornerShape(50)
                        )
                        .pressScaleClickable(
                            pressedScale = 0.95f,
                            onClick = { onSelectFilter(filter) }
                        )
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .testTag("filter_chip_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = chipTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val currentOnOpenTaskDetails by androidx.compose.runtime.rememberUpdatedState(onOpenTaskDetails)
        val currentOnToggleTaskComplete by androidx.compose.runtime.rememberUpdatedState(onToggleTaskComplete)

        // Category Filter Row
        if (uiState.categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allCatsSelected = uiState.selectedCategoryId == null
                val allCatsBg by animateColorAsState(
                    targetValue = if (allCatsSelected) CharcoalElevated else Color.Transparent,
                    animationSpec = DayformMotion.stateTween(reducedMotion),
                    label = "all_cats_bg"
                )
                val allCatsText by animateColorAsState(
                    targetValue = if (allCatsSelected) NeonLime else SecondaryText,
                    animationSpec = DayformMotion.stateTween(reducedMotion),
                    label = "all_cats_text"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(allCatsBg)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(50))
                        .pressScaleClickable(
                            pressedScale = 0.95f,
                            onClick = { onSelectCategory(null) }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All Categories",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = allCatsText
                    )
                }

                uiState.categories.forEach { category ->
                    val selected = uiState.selectedCategoryId == category.id
                    val catColor = remember(category.colorHex) { parseHexColor(category.colorHex) }
                    val catChipBg by animateColorAsState(
                        targetValue = if (selected) CharcoalElevated else Color.Transparent,
                        animationSpec = DayformMotion.stateTween(reducedMotion),
                        label = "cat_filter_bg_${category.id}"
                    )
                    val catChipBorder by animateColorAsState(
                        targetValue = if (selected) NeonLime else CharcoalBorder,
                        animationSpec = DayformMotion.stateTween(reducedMotion),
                        label = "cat_filter_border_${category.id}"
                    )
                    val catChipText by animateColorAsState(
                        targetValue = if (selected) Color.White else SecondaryText,
                        animationSpec = DayformMotion.stateTween(reducedMotion),
                        label = "cat_filter_text_${category.id}"
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(catChipBg)
                            .border(
                                width = 1.dp,
                                color = catChipBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .pressScaleClickable(
                                pressedScale = 0.95f,
                                onClick = {
                                    onSelectCategory(if (selected) null else category.id)
                                }
                            )
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
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = catChipText
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Filtered Task List or Compact Board Grid
        AnimatedContent(
            targetState = uiState.isBoardLayout to uiState.filteredTasks.isEmpty(),
            transitionSpec = {
                fadeIn(DayformMotion.stateTween(reducedMotion)) togetherWith
                    fadeOut(DayformMotion.microTween(reducedMotion))
            },
            label = "tasks_layout_mode_anim",
            modifier = Modifier.fillMaxSize()
        ) { (isBoard, isEmpty) ->
            if (isEmpty) {
                FilteredEmptyState(
                    filter = uiState.selectedFilter,
                    hasSearchQuery = uiState.searchQuery.isNotBlank(),
                    onCreateTask = { onCreateTaskClick(null) }
                )
            } else if (isBoard) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("filtered_tasks_grid")
                ) {
                    items(
                        items = uiState.filteredTasks,
                        key = { it.task.id },
                        contentType = { "task_card" }
                    ) { item ->
                        val taskId = item.task.id
                        val onCardClickRemembered = remember(taskId) {
                            { currentOnOpenTaskDetails(taskId) }
                        }
                        val onToggleRemembered = remember(item.task) {
                            { currentOnToggleTaskComplete(item.task) }
                        }
                        TaskItemCard(
                            item = item,
                            use24Hour = uiState.settings.use24HourFormat,
                            onCardClick = onCardClickRemembered,
                            onToggleComplete = onToggleRemembered,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedOriginKey = DayformRoutes.TASKS,
                            modifier = if (reducedMotion || gridState.isScrollInProgress) {
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
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("filtered_tasks_list")
                ) {
                    items(
                        items = uiState.filteredTasks,
                        key = { it.task.id },
                        contentType = { "task_card" }
                    ) { item ->
                        val taskId = item.task.id
                        val onCardClickRemembered = remember(taskId) {
                            { currentOnOpenTaskDetails(taskId) }
                        }
                        val onToggleRemembered = remember(item.task) {
                            { currentOnToggleTaskComplete(item.task) }
                        }
                        TaskItemCard(
                            item = item,
                            use24Hour = uiState.settings.use24HourFormat,
                            onCardClick = onCardClickRemembered,
                            onToggleComplete = onToggleRemembered,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedOriginKey = DayformRoutes.TASKS,
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
    }
}

@Composable
private fun FilteredEmptyState(
    filter: TaskFilter,
    hasSearchQuery: Boolean,
    onCreateTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CharcoalSurface)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(28.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                hasSearchQuery -> "No matching tasks found"
                filter == TaskFilter.COMPLETED -> "No completed tasks yet"
                filter == TaskFilter.OVERDUE -> "Zero overdue tasks!"
                filter == TaskFilter.TODAY -> "Nothing scheduled for today"
                filter == TaskFilter.UPCOMING -> "No upcoming tasks scheduled"
                else -> "Your task list is empty"
            },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                hasSearchQuery -> "Try adjusting your search keywords or category filters."
                filter == TaskFilter.OVERDUE -> "You're right on schedule with all your deadlines."
                else -> "Add a new task to organize your upcoming work."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
        if (!hasSearchQuery && filter != TaskFilter.OVERDUE) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onCreateTask,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonLime,
                    contentColor = PrimaryDarkText
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Task",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}
