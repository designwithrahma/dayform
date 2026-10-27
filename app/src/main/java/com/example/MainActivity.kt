package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.TaskFilter
import com.example.data.model.TaskWithDetails
import com.example.notifications.ReminderScheduler
import com.example.ui.DayformViewModel
import com.example.ui.MainTab
import com.example.ui.UndoAction
import com.example.ui.components.DayformBottomNavBar
import com.example.ui.navigation.DayformRoutes
import com.example.ui.screens.AllTasksScreen
import com.example.ui.screens.CreateEditTaskSheet
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.TaskDetailsScreen
import com.example.ui.screens.UpcomingCalendarView
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.DayformMotion
import com.example.ui.theme.DayformTheme
import com.example.util.DateTimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: DayformViewModel by viewModels()
    private val _pendingDeepLinkTaskId = MutableStateFlow<Long?>(null)
    private val pendingDeepLinkTaskId: StateFlow<Long?> = _pendingDeepLinkTaskId.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.ensureNotificationChannel(this)
        handleNotificationDeepLink(intent)

        setContent {
            DayformTheme {
                val deepLinkTaskId by pendingDeepLinkTaskId.collectAsStateWithLifecycle()
                DayformApp(
                    viewModel = viewModel,
                    deepLinkTaskId = deepLinkTaskId,
                    onDeepLinkConsumed = { _pendingDeepLinkTaskId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationDeepLink(intent)
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        val taskId = intent?.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L) ?: -1L
        if (taskId > 0L) {
            _pendingDeepLinkTaskId.value = taskId
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DayformApp(
    viewModel: DayformViewModel,
    deepLinkTaskId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = DayformMotion.rememberReducedMotion()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Preserved list scroll states across tab & detail transitions
    val homeListState = rememberLazyListState()
    val tasksListState = rememberLazyListState()
    val tasksGridState = rememberLazyGridState()
    val calendarListState = rememberLazyListState()
    var hasPlayedInitialHomeEntrance by rememberSaveable { mutableStateOf(false) }

    // Observe Undo actions (task completion & task deletion)
    LaunchedEffect(viewModel) {
        viewModel.undoEvent.collectLatest { action ->
            val message = when (action) {
                is UndoAction.TaskCompletionChanged -> action.message
                is UndoAction.TaskDeleted -> action.message
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.performUndo(action)
            }
        }
    }

    // Observe informational toast/snackbar messages
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBg)
        )
        return
    }

    val startDestination = remember {
        if (uiState.settings.hasCompletedOnboarding) {
            DayformRoutes.HOME
        } else {
            DayformRoutes.ONBOARDING
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // If onboarding completes externally (e.g. via repository call in tests), transition to HOME
    LaunchedEffect(uiState.settings.hasCompletedOnboarding, currentRoute) {
        if (uiState.settings.hasCompletedOnboarding && currentRoute == DayformRoutes.ONBOARDING) {
            navController.navigate(DayformRoutes.HOME) {
                popUpTo(DayformRoutes.ONBOARDING) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Handle notification deep links or external viewModel.selectedTaskIdForDetails requests
    val vmRequestedTaskId by viewModel.selectedTaskIdForDetails.collectAsStateWithLifecycle()
    LaunchedEffect(deepLinkTaskId, vmRequestedTaskId, uiState.settings.hasCompletedOnboarding) {
        if (!uiState.settings.hasCompletedOnboarding) return@LaunchedEffect
        val targetId = deepLinkTaskId ?: vmRequestedTaskId
        if (targetId != null && targetId > 0L) {
            // Ensure HOME is in the backstack so pressing Back from a notification returns to Home
            navController.navigate(DayformRoutes.taskDetails(targetId, DayformRoutes.HOME)) {
                launchSingleTop = true
            }
            onDeepLinkConsumed()
            viewModel.closeTaskDetails()
        }
    }

    val showBottomBar = currentRoute in setOf(
        DayformRoutes.HOME,
        DayformRoutes.TASKS,
        DayformRoutes.CALENDAR,
        DayformRoutes.INSIGHTS,
        DayformRoutes.PROFILE
    )
    val activeMainTab = DayformRoutes.routeToMainTab(currentRoute) ?: MainTab.HOME

    SharedTransitionLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBg)
    ) {
        val sharedTransitionScope = this

        Scaffold(
            containerColor = CharcoalBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = if (reducedMotion) {
                        fadeIn(tween(0))
                    } else {
                        fadeIn(tween(DayformMotion.SMALL_STATE_MS)) +
                            slideInVertically(tween(DayformMotion.SMALL_STATE_MS)) { it / 2 }
                    },
                    exit = if (reducedMotion) {
                        fadeOut(tween(0))
                    } else {
                        fadeOut(tween(DayformMotion.QUICK_FEEDBACK_MS)) +
                            slideOutVertically(tween(DayformMotion.SMALL_STATE_MS)) { it / 2 }
                    }
                ) {
                    DayformBottomNavBar(
                        activeTab = activeMainTab,
                        onSelectTab = { tab ->
                            navigateToMainTab(navController, tab)
                            viewModel.selectTab(tab)
                        },
                        onAddClick = {
                            if (navBackStackEntry.isResumed()) {
                                val epochDay = if (currentRoute == DayformRoutes.CALENDAR) {
                                    uiState.selectedCalendarEpochDay
                                } else {
                                    DateTimeUtil.currentLocalEpochDay()
                                }
                                navController.navigate(DayformRoutes.createTask(epochDay)) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .background(CharcoalBg)
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                // 1. ONBOARDING
                composable(
                    route = DayformRoutes.ONBOARDING,
                    enterTransition = { DayformMotion.detailsEnterTransition(reducedMotion) },
                    exitTransition = { DayformMotion.detailsExitTransition(reducedMotion) }
                ) {
                    OnboardingScreen(
                        onCompleteOnboarding = { displayName ->
                            viewModel.completeOnboarding(displayName)
                            navController.navigate(DayformRoutes.HOME) {
                                popUpTo(DayformRoutes.ONBOARDING) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // 2. HOME DASHBOARD
                composable(
                    route = DayformRoutes.HOME,
                    enterTransition = {
                        val fromRoute = initialState.destination.route
                        if (DayformRoutes.routeToMainTab(fromRoute) != null) {
                            val forward = DayformRoutes.tabOrderIndex(DayformRoutes.HOME) >=
                                DayformRoutes.tabOrderIndex(fromRoute)
                            DayformMotion.tabEnterTransition(forward, reducedMotion)
                        } else {
                            DayformMotion.detailsEnterTransition(reducedMotion)
                        }
                    },
                    exitTransition = {
                        val toRoute = targetState.destination.route
                        if (DayformRoutes.routeToMainTab(toRoute) != null) {
                            val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                                DayformRoutes.tabOrderIndex(DayformRoutes.HOME)
                            DayformMotion.tabExitTransition(forward, reducedMotion)
                        } else {
                            DayformMotion.detailsExitTransition(reducedMotion)
                        }
                    },
                    popEnterTransition = {
                        val fromRoute = initialState.destination.route
                        if (DayformRoutes.routeToMainTab(fromRoute) != null) {
                            DayformMotion.tabEnterTransition(forward = false, reducedMotion = reducedMotion)
                        } else {
                            DayformMotion.detailsEnterTransition(reducedMotion)
                        }
                    },
                    popExitTransition = {
                        DayformMotion.detailsExitTransition(reducedMotion)
                    }
                ) { entry ->
                    HomeDashboardScreen(
                        uiState = uiState,
                        listState = homeListState,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@composable,
                        onOpenTaskDetails = { taskId ->
                            if (entry.isResumed()) {
                                navController.navigate(
                                    DayformRoutes.taskDetails(taskId, DayformRoutes.HOME)
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onToggleTaskComplete = { task -> viewModel.toggleTaskCompleted(task) },
                        onCreateTaskClick = {
                            if (entry.isResumed()) {
                                navController.navigate(
                                    DayformRoutes.createTask(DateTimeUtil.currentLocalEpochDay())
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onCompletedStatClick = {
                            viewModel.setTaskFilter(TaskFilter.COMPLETED)
                            navigateToMainTab(navController, MainTab.TASKS)
                        },
                        onPlannedStatClick = {
                            viewModel.setTaskFilter(TaskFilter.TODAY)
                            navigateToMainTab(navController, MainTab.TASKS)
                        },
                        onViewAllTasksClick = {
                            viewModel.setTaskFilter(TaskFilter.ALL)
                            navigateToMainTab(navController, MainTab.TASKS)
                        },
                        hasPlayedInitialEntrance = hasPlayedInitialHomeEntrance,
                        onMarkInitialEntrancePlayed = { hasPlayedInitialHomeEntrance = true }
                    )
                }

                // 3. ALL TASKS / SEARCH
                composable(
                    route = DayformRoutes.TASKS,
                    enterTransition = {
                        val fromRoute = initialState.destination.route
                        if (DayformRoutes.routeToMainTab(fromRoute) != null) {
                            val forward = DayformRoutes.tabOrderIndex(DayformRoutes.TASKS) >=
                                DayformRoutes.tabOrderIndex(fromRoute)
                            DayformMotion.tabEnterTransition(forward, reducedMotion)
                        } else {
                            DayformMotion.detailsEnterTransition(reducedMotion)
                        }
                    },
                    exitTransition = {
                        val toRoute = targetState.destination.route
                        if (DayformRoutes.routeToMainTab(toRoute) != null) {
                            val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                                DayformRoutes.tabOrderIndex(DayformRoutes.TASKS)
                            DayformMotion.tabExitTransition(forward, reducedMotion)
                        } else {
                            DayformMotion.detailsExitTransition(reducedMotion)
                        }
                    },
                    popEnterTransition = {
                        val fromRoute = initialState.destination.route
                        if (DayformRoutes.routeToMainTab(fromRoute) != null) {
                            val forward = DayformRoutes.tabOrderIndex(DayformRoutes.TASKS) >=
                                DayformRoutes.tabOrderIndex(fromRoute)
                            DayformMotion.tabEnterTransition(forward, reducedMotion)
                        } else {
                            DayformMotion.detailsEnterTransition(reducedMotion)
                        }
                    },
                    popExitTransition = {
                        val toRoute = targetState.destination.route
                        if (DayformRoutes.routeToMainTab(toRoute) != null) {
                            val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                                DayformRoutes.tabOrderIndex(DayformRoutes.TASKS)
                            DayformMotion.tabExitTransition(forward, reducedMotion)
                        } else {
                            DayformMotion.detailsExitTransition(reducedMotion)
                        }
                    }
                ) { entry ->
                    AllTasksScreen(
                        uiState = uiState,
                        listState = tasksListState,
                        gridState = tasksGridState,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@composable,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        onSelectFilter = { f -> viewModel.setTaskFilter(f) },
                        onSelectCategory = { c -> viewModel.setCategoryFilter(c) },
                        onSelectSort = { s -> viewModel.setSortOption(s) },
                        onToggleBoardLayout = { viewModel.toggleBoardLayout() },
                        onOpenCalendarView = {
                            if (entry.isResumed()) {
                                navController.navigate(DayformRoutes.CALENDAR) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenTaskDetails = { taskId ->
                            if (entry.isResumed()) {
                                navController.navigate(
                                    DayformRoutes.taskDetails(taskId, DayformRoutes.TASKS)
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onToggleTaskComplete = { task -> viewModel.toggleTaskCompleted(task) },
                        onCreateTaskClick = { day ->
                            if (entry.isResumed()) {
                                val targetDay = day ?: DateTimeUtil.currentLocalEpochDay()
                                navController.navigate(DayformRoutes.createTask(targetDay)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                // 4. UPCOMING & CALENDAR DESTINATION
                composable(
                    route = DayformRoutes.CALENDAR,
                    enterTransition = {
                        DayformMotion.tabEnterTransition(forward = true, reducedMotion = reducedMotion)
                    },
                    exitTransition = {
                        DayformMotion.detailsExitTransition(reducedMotion)
                    },
                    popEnterTransition = {
                        DayformMotion.detailsEnterTransition(reducedMotion)
                    },
                    popExitTransition = {
                        DayformMotion.tabExitTransition(forward = false, reducedMotion = reducedMotion)
                    }
                ) { entry ->
                    UpcomingCalendarView(
                        uiState = uiState,
                        listState = calendarListState,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@composable,
                        onBackToList = {
                            navController.safePopBackStack(entry)
                        },
                        onSelectDate = { day -> viewModel.selectCalendarDate(day) },
                        onOpenTaskDetails = { taskId, originKey ->
                            if (entry.isResumed()) {
                                navController.navigate(
                                    DayformRoutes.taskDetails(taskId, originKey)
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onToggleTaskComplete = { task -> viewModel.toggleTaskCompleted(task) },
                        onCreateTaskForDate = { day ->
                            if (entry.isResumed()) {
                                navController.navigate(DayformRoutes.createTask(day)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                // 5. INSIGHTS
                composable(
                    route = DayformRoutes.INSIGHTS,
                    enterTransition = {
                        val fromRoute = initialState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(DayformRoutes.INSIGHTS) >=
                            DayformRoutes.tabOrderIndex(fromRoute)
                        DayformMotion.tabEnterTransition(forward, reducedMotion)
                    },
                    exitTransition = {
                        val toRoute = targetState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                            DayformRoutes.tabOrderIndex(DayformRoutes.INSIGHTS)
                        DayformMotion.tabExitTransition(forward, reducedMotion)
                    },
                    popEnterTransition = {
                        val fromRoute = initialState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(DayformRoutes.INSIGHTS) >=
                            DayformRoutes.tabOrderIndex(fromRoute)
                        DayformMotion.tabEnterTransition(forward, reducedMotion)
                    },
                    popExitTransition = {
                        val toRoute = targetState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                            DayformRoutes.tabOrderIndex(DayformRoutes.INSIGHTS)
                        DayformMotion.tabExitTransition(forward, reducedMotion)
                    }
                ) {
                    InsightsScreen(insights = uiState.insights)
                }

                // 6. PROFILE & SETTINGS
                composable(
                    route = DayformRoutes.PROFILE,
                    enterTransition = {
                        val fromRoute = initialState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(DayformRoutes.PROFILE) >=
                            DayformRoutes.tabOrderIndex(fromRoute)
                        DayformMotion.tabEnterTransition(forward, reducedMotion)
                    },
                    exitTransition = {
                        val toRoute = targetState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                            DayformRoutes.tabOrderIndex(DayformRoutes.PROFILE)
                        DayformMotion.tabExitTransition(forward, reducedMotion)
                    },
                    popEnterTransition = {
                        val fromRoute = initialState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(DayformRoutes.PROFILE) >=
                            DayformRoutes.tabOrderIndex(fromRoute)
                        DayformMotion.tabEnterTransition(forward, reducedMotion)
                    },
                    popExitTransition = {
                        val toRoute = targetState.destination.route
                        val forward = DayformRoutes.tabOrderIndex(toRoute) >=
                            DayformRoutes.tabOrderIndex(DayformRoutes.PROFILE)
                        DayformMotion.tabExitTransition(forward, reducedMotion)
                    }
                ) {
                    ProfileSettingsScreen(
                        uiState = uiState,
                        repository = viewModel.repository,
                        onUpdateDisplayName = { name -> viewModel.updateDisplayName(name) },
                        onCreateCategory = { name, color -> viewModel.createCategory(name, color) },
                        onUpdateCategory = { cat, name, color ->
                            viewModel.updateCategory(cat, name, color)
                        },
                        onDeleteCategory = { id, moveTarget ->
                            viewModel.deleteCategory(id, moveTarget)
                        },
                        onToggleNotifications = { en -> viewModel.setNotificationsEnabled(en) },
                        onToggleHaptics = { en -> viewModel.setHapticFeedbackEnabled(en) },
                        onToggleSound = { en -> viewModel.setSoundFeedbackEnabled(en) },
                        onToggleWeekStartsMonday = { m -> viewModel.setWeekStartsOnMonday(m) },
                        onToggleUse24Hour = { h24 -> viewModel.setUse24HourFormat(h24) },
                        onShowMessage = { msg -> viewModel.emitMessage(msg) }
                    )
                }

                // 7. TASK DETAILS WITH TASK ID & ORIGIN (Card Morph Target)
                composable(
                    route = DayformRoutes.TASK_DETAILS_PATTERN,
                    arguments = listOf(
                        navArgument(DayformRoutes.ARG_TASK_ID) { type = NavType.LongType },
                        navArgument(DayformRoutes.ARG_ORIGIN) {
                            type = NavType.StringType
                            defaultValue = DayformRoutes.HOME
                        }
                    ),
                    enterTransition = { DayformMotion.detailsEnterTransition(reducedMotion) },
                    exitTransition = { DayformMotion.detailsExitTransition(reducedMotion) },
                    popEnterTransition = { DayformMotion.detailsEnterTransition(reducedMotion) },
                    popExitTransition = { DayformMotion.detailsExitTransition(reducedMotion) }
                ) { entry ->
                    val taskId = entry.arguments?.getLong(DayformRoutes.ARG_TASK_ID) ?: -1L
                    val origin = entry.arguments?.getString(DayformRoutes.ARG_ORIGIN)
                        ?: DayformRoutes.HOME

                    val liveTask = uiState.allTasks.firstOrNull { it.task.id == taskId }
                    // Cache the last non-null task snapshot so exit animations (including after Delete)
                    // render smoothly without disappearing abruptly or flashing a blank frame.
                    var cachedTask by remember(taskId) { mutableStateOf(liveTask) }
                    if (liveTask != null) {
                        cachedTask = liveTask
                    }

                    val displayTask: TaskWithDetails? = liveTask ?: cachedTask
                    if (displayTask != null) {
                        TaskDetailsScreen(
                            taskWithDetails = displayTask,
                            categories = uiState.categories,
                            use24HourFormat = uiState.settings.use24HourFormat,
                            hideCompletedSubtasks = uiState.settings.hideCompletedSubtasks,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@composable,
                            sharedOriginKey = origin,
                            onBack = { navController.safePopBackStack(entry) },
                            onEditTask = { id ->
                                if (entry.isResumed()) {
                                    navController.navigate(DayformRoutes.editTask(id)) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onToggleTaskComplete = { task -> viewModel.toggleTaskCompleted(task) },
                            onUpdateDescription = { id, desc ->
                                viewModel.updateTaskDescription(id, desc)
                            },
                            onUpdatePriority = { id, prio ->
                                viewModel.updateTaskPriority(id, prio)
                            },
                            onUpdateCategory = { id, catId ->
                                viewModel.updateTaskCategory(id, catId)
                            },
                            onUpdateReminder = { id, offset ->
                                viewModel.updateTaskReminder(id, offset)
                            },
                            onDuplicateTask = { id -> viewModel.duplicateTask(id) },
                            onDeleteTask = { id ->
                                navController.safePopBackStack(entry)
                                viewModel.deleteTask(id)
                            },
                            onAddSubtask = { tId, title -> viewModel.addSubtask(tId, title) },
                            onToggleSubtask = { sub -> viewModel.toggleSubtask(sub) },
                            onEditSubtaskTitle = { sub, newTitle ->
                                viewModel.updateSubtaskTitle(sub, newTitle)
                            },
                            onDeleteSubtask = { subId -> viewModel.deleteSubtask(subId) },
                            onMoveSubtask = { tId, subId, up ->
                                viewModel.moveSubtask(tId, subId, up)
                            },
                            onToggleHideCompletedSubtasks = { hide ->
                                viewModel.setHideCompletedSubtasks(hide)
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.safePopBackStack(entry)
                        }
                    }
                }

                // 8. CREATE TASK (Connected to Central Lime '+' Button via sharedBounds)
                composable(
                    route = DayformRoutes.CREATE_TASK_PATTERN,
                    arguments = listOf(
                        navArgument(DayformRoutes.ARG_EPOCH_DAY) {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    ),
                    enterTransition = { DayformMotion.detailsEnterTransition(reducedMotion) },
                    exitTransition = { DayformMotion.detailsExitTransition(reducedMotion) },
                    popEnterTransition = { DayformMotion.detailsEnterTransition(reducedMotion) },
                    popExitTransition = { DayformMotion.detailsExitTransition(reducedMotion) }
                ) { entry ->
                    val argEpochDay = entry.arguments?.getLong(DayformRoutes.ARG_EPOCH_DAY) ?: -1L
                    val initialDay = if (argEpochDay > 0L) {
                        argEpochDay
                    } else {
                        DateTimeUtil.currentLocalEpochDay()
                    }

                    CreateEditTaskSheet(
                        existingTask = null,
                        initialEpochDay = initialDay,
                        categories = uiState.categories,
                        use24HourFormat = uiState.settings.use24HourFormat,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@composable,
                        onSaveTask = { id, title, desc, dueDay, dueTime, prio, catId, remOffset, customRem, recType, recDays, subtasks ->
                            viewModel.saveTask(
                                existingTaskId = id,
                                title = title,
                                description = desc,
                                dueDateEpochDay = dueDay,
                                dueTimeMinutes = dueTime,
                                priority = prio,
                                categoryId = catId,
                                reminderOffset = remOffset,
                                customReminderMillis = customRem,
                                recurrenceType = recType,
                                recurrenceWeekdays = recDays,
                                subtaskDrafts = subtasks
                            )
                        },
                        checkReminderWarning = {
                            viewModel.repository.lastSaveOutcome?.reminderWarningMessage
                        },
                        onRetryReminder = { savedTaskId ->
                            viewModel.retryTaskReminder(savedTaskId)
                        },
                        onSaveSuccess = {
                            navController.safePopBackStack(entry)
                        },
                        onCreateCategory = { name, colorHex ->
                            viewModel.createCategory(name, colorHex)
                        },
                        onCancel = {
                            navController.safePopBackStack(entry)
                        }
                    )
                }

                // 9. EDIT TASK WITH TASK ID (Returns to originating TaskDetailsScreen on Save/Cancel)
                composable(
                    route = DayformRoutes.EDIT_TASK_PATTERN,
                    arguments = listOf(
                        navArgument(DayformRoutes.ARG_TASK_ID) { type = NavType.LongType }
                    ),
                    enterTransition = { DayformMotion.sheetEnterTransition(reducedMotion) },
                    exitTransition = { DayformMotion.sheetExitTransition(reducedMotion) },
                    popEnterTransition = { DayformMotion.sheetEnterTransition(reducedMotion) },
                    popExitTransition = { DayformMotion.sheetExitTransition(reducedMotion) }
                ) { entry ->
                    val taskId = entry.arguments?.getLong(DayformRoutes.ARG_TASK_ID) ?: -1L
                    val existingTask = uiState.allTasks.firstOrNull { it.task.id == taskId }
                    var cachedEditTask by remember(taskId) { mutableStateOf(existingTask) }
                    if (existingTask != null) {
                        cachedEditTask = existingTask
                    }
                    val targetTask = existingTask ?: cachedEditTask

                    if (targetTask != null) {
                        CreateEditTaskSheet(
                            existingTask = targetTask,
                            initialEpochDay = targetTask.task.dueDateEpochDay,
                            categories = uiState.categories,
                            use24HourFormat = uiState.settings.use24HourFormat,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this@composable,
                            onSaveTask = { id, title, desc, dueDay, dueTime, prio, catId, remOffset, customRem, recType, recDays, subtasks ->
                                viewModel.saveTask(
                                    existingTaskId = id,
                                    title = title,
                                    description = desc,
                                    dueDateEpochDay = dueDay,
                                    dueTimeMinutes = dueTime,
                                    priority = prio,
                                    categoryId = catId,
                                    reminderOffset = remOffset,
                                    customReminderMillis = customRem,
                                    recurrenceType = recType,
                                    recurrenceWeekdays = recDays,
                                    subtaskDrafts = subtasks
                                )
                            },
                            checkReminderWarning = {
                                viewModel.repository.lastSaveOutcome?.reminderWarningMessage
                            },
                            onRetryReminder = { savedTaskId ->
                                viewModel.retryTaskReminder(savedTaskId)
                            },
                            onSaveSuccess = {
                                navController.safePopBackStack(entry)
                            },
                            onCreateCategory = { name, colorHex ->
                                viewModel.createCategory(name, colorHex)
                            },
                            onCancel = {
                                navController.safePopBackStack(entry)
                            }
                        )
                    } else if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CharcoalBg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = com.example.ui.theme.NeonLime)
                        }
                    } else {
                        // Explicit missing task error screen — never silently fall back to Create New Task
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CharcoalBg)
                                .padding(horizontal = 24.dp)
                                .testTag("task_not_found_error_screen"),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Task Not Found",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The requested task no longer exists or could not be loaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = com.example.ui.theme.SecondaryText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navController.safePopBackStack(entry) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.example.ui.theme.NeonLime,
                                    contentColor = com.example.ui.theme.PrimaryDarkText
                                ),
                                modifier = Modifier.testTag("task_not_found_back_button")
                            ) {
                                Text("Go Back", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun NavBackStackEntry?.isResumed(): Boolean {
    return this?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
}

private fun NavHostController.safePopBackStack(entry: NavBackStackEntry?) {
    if (entry == null || entry.isResumed()) {
        popBackStack()
    }
}

/**
 * Navigates between main bottom tabs while preserving actual tab navigation history
 * (e.g. Home -> Tasks -> Insights -> Back returns to Tasks -> Back returns to Home)
 * and preventing duplicate destination stacking on double-taps (`launchSingleTop = true`).
 */
private fun navigateToMainTab(
    navController: NavHostController,
    tab: MainTab
) {
    val targetRoute = DayformRoutes.mainTabToRoute(tab)
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    if (currentRoute == targetRoute) return

    if (tab == MainTab.HOME) {
        val popped = navController.popBackStack(DayformRoutes.HOME, inclusive = false)
        if (!popped) {
            navController.navigate(DayformRoutes.HOME) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    } else {
        navController.navigate(targetRoute) {
            launchSingleTop = true
        }
    }
}
