package com.example

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DayformDatabase
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.SubtaskDraft
import com.example.data.model.TaskFilter
import com.example.data.model.TaskPriority
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DayformRepository
import com.example.ui.DayformViewModel
import com.example.ui.screens.CreateEditTaskSheet
import com.example.ui.theme.DayformTheme
import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.viewModelScope
import com.example.util.DateTimeUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.compose.animation.ExperimentalSharedTransitionApi
import org.junit.runner.RunWith
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import com.example.ui.DayformUiState
import com.example.ui.screens.HomeDashboardScreen
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class DayformRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: DayformDatabase
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var repository: DayformRepository
    private val createdViewModels = mutableListOf<DayformViewModel>()

    private fun createTestViewModel(): DayformViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return DayformViewModel(app, repository).also { createdViewModels.add(it) }
    }

    private suspend fun seedTestWorkspaceWithFixtures(displayName: String = "Jayson") {
        repository.initializeFirstLaunch(displayName)
        val categories = repository.ensureStarterCategories()
        val workId = categories.firstOrNull { it.name.equals("Work", ignoreCase = true) }?.id
        val creativeId = categories.firstOrNull { it.name.equals("Creative", ignoreCase = true) }?.id
        val studyId = categories.firstOrNull { it.name.equals("Study", ignoreCase = true) }?.id
        val personalId = categories.firstOrNull { it.name.equals("Personal", ignoreCase = true) }?.id
        val today = DateTimeUtil.currentLocalEpochDay()
        val now = System.currentTimeMillis()
        val dayMillis = 86_400_000L
        val dao = database.dayformDao()

        val task1Id = dao.insertTask(
            com.example.data.model.TaskEntity(
                title = "Research for Flix Media",
                description = "Gathering data for user persona and analyzing competitor streaming flows for the Q4 product pitch.",
                createdAt = now - 3 * 3600_000L,
                updatedAt = now - 3600_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 11 * 60 + 30,
                isCompleted = false,
                priority = TaskPriority.URGENT.name,
                categoryId = workId,
                reminderOffset = ReminderOffset.THIRTY_MIN_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 0
            )
        )
        dao.insertSubtasks(
            listOf(
                com.example.data.model.SubtaskEntity(taskId = task1Id, title = "Compile audience survey insights", isCompleted = true, sortOrder = 0),
                com.example.data.model.SubtaskEntity(taskId = task1Id, title = "Gathering data for user persona", isCompleted = false, sortOrder = 1),
                com.example.data.model.SubtaskEntity(taskId = task1Id, title = "Draft executive summary slides", isCompleted = false, sortOrder = 2)
            )
        )

        val task2Id = dao.insertTask(
            com.example.data.model.TaskEntity(
                title = "Lemark Logo Design & Content Creation",
                description = "Create a sleek and modern logo for a new tech startup that conveys innovation and sophistication.",
                createdAt = now - 5 * 3600_000L,
                updatedAt = now - 2 * 3600_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 15 * 60,
                isCompleted = false,
                priority = TaskPriority.HIGH.name,
                categoryId = creativeId,
                reminderOffset = ReminderOffset.ONE_HOUR_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 1
            )
        )
        dao.insertSubtasks(
            listOf(
                com.example.data.model.SubtaskEntity(taskId = task2Id, title = "Research on modern logo style", isCompleted = true, sortOrder = 0),
                com.example.data.model.SubtaskEntity(taskId = task2Id, title = "Creating a Mood board", isCompleted = false, sortOrder = 1),
                com.example.data.model.SubtaskEntity(taskId = task2Id, title = "Sketching", isCompleted = false, sortOrder = 2)
            )
        )

        dao.insertTasks(
            listOf(
                com.example.data.model.TaskEntity(
                    title = "Create Instagram carousel",
                    description = "Design a 6-slide typography-driven showcase.",
                    createdAt = now - 6 * 3600_000L,
                    updatedAt = now - 3 * 3600_000L,
                    dueDateEpochDay = today,
                    dueTimeMinutes = 17 * 60 + 30,
                    isCompleted = false,
                    priority = TaskPriority.HIGH.name,
                    categoryId = creativeId,
                    reminderOffset = ReminderOffset.TEN_MIN_BEFORE.name,
                    recurrenceType = RecurrenceType.NEVER.name,
                    sortOrder = 2
                ),
                com.example.data.model.TaskEntity(
                    title = "Review website homepage",
                    description = "Audit responsive breakpoints and contrast ratios.",
                    createdAt = now - 8 * 3600_000L,
                    updatedAt = now - 4 * 3600_000L,
                    dueDateEpochDay = today,
                    dueTimeMinutes = 19 * 60,
                    isCompleted = false,
                    priority = TaskPriority.MEDIUM.name,
                    categoryId = workId,
                    sortOrder = 3
                ),
                com.example.data.model.TaskEntity(
                    title = "Plan tomorrow's schedule",
                    description = "Review open tasks and set top 3 priorities.",
                    createdAt = now - 9 * 3600_000L,
                    updatedAt = now - 4 * 3600_000L,
                    dueDateEpochDay = today,
                    dueTimeMinutes = 20 * 60 + 30,
                    isCompleted = false,
                    priority = TaskPriority.MEDIUM.name,
                    categoryId = personalId,
                    recurrenceType = RecurrenceType.DAILY.name,
                    sortOrder = 4
                ),
                com.example.data.model.TaskEntity(
                    title = "Submit design assignment",
                    description = "Package interactive prototype and case study.",
                    createdAt = now - 12 * 3600_000L,
                    updatedAt = now - 6 * 3600_000L,
                    dueDateEpochDay = today + 1L,
                    dueTimeMinutes = 14 * 60,
                    isCompleted = false,
                    priority = TaskPriority.HIGH.name,
                    categoryId = studyId,
                    sortOrder = 5
                ),
                com.example.data.model.TaskEntity(
                    title = "Morning creative sprint & inbox zero",
                    description = "Cleared client feedback threads.",
                    createdAt = now - 6 * 3600_000L,
                    updatedAt = now - 3 * 3600_000L,
                    dueDateEpochDay = today,
                    dueTimeMinutes = 9 * 60,
                    isCompleted = true,
                    completedAt = now - 3 * 3600_000L,
                    priority = TaskPriority.MEDIUM.name,
                    categoryId = workId,
                    sortOrder = 6
                ),
                com.example.data.model.TaskEntity(
                    title = "Finalize typography scale tokens",
                    description = "Established Manrope display, title, and body styles.",
                    createdAt = now - dayMillis - 5 * 3600_000L,
                    updatedAt = now - dayMillis,
                    dueDateEpochDay = today - 1L,
                    dueTimeMinutes = 16 * 60,
                    isCompleted = true,
                    completedAt = now - dayMillis,
                    priority = TaskPriority.HIGH.name,
                    categoryId = creativeId,
                    sortOrder = 7
                )
            )
        )
    }

    @Before
    fun setUp() {
        composeTestRule.mainClock.autoAdvance = true
        context = ApplicationProvider.getApplicationContext()
        org.robolectric.Shadows.shadowOf(context as Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        database = Room.inMemoryDatabaseBuilder(
            context,
            DayformDatabase::class.java
        ).allowMainThreadQueries().build()
        DayformDatabase.setTestInstance(database)
        preferencesRepository = UserPreferencesRepository(context)
        repository = DayformRepository(
            context = context,
            dao = database.dayformDao(),
            preferencesRepository = preferencesRepository,
            database = database
        )
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        database.close()
        DayformDatabase.clearTestInstance()
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun appNameResourceIsDayform() {
        val appName = context.getString(R.string.app_name)
        assertEquals("DAYFORM", appName)
    }

    @Test
    fun repositoryCrudLifecycle(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        assertEquals(4, categories.size)
        val today = DateTimeUtil.currentLocalEpochDay()

        // 1. Create a task with subtasks and daily recurrence
        val taskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Design System Audit",
            description = "Verify color contrast and corner radii",
            dueDateEpochDay = today,
            dueTimeMinutes = 14 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.TEN_MIN_BEFORE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(
                SubtaskDraft(title = "Check #191919 charcoal background"),
                SubtaskDraft(title = "Check #DFFF65 neon lime cards")
            )
        ).getOrThrow()
        assertTrue(taskId > 0L)

        var allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(1, allTasks.size)
        assertEquals("Design System Audit", allTasks.first().task.title)
        assertEquals(2, allTasks.first().subtasks.size)

        // 2. Edit the task
        repository.saveTaskWithSubtasks(
            existingTaskId = taskId,
            title = "Design System Audit Updated",
            description = "Updated description",
            dueDateEpochDay = today,
            dueTimeMinutes = 15 * 60,
            priority = TaskPriority.URGENT,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(
                SubtaskDraft(title = "Check #191919 charcoal background", isCompleted = true),
                SubtaskDraft(title = "Check #DFFF65 neon lime cards", isCompleted = false)
            )
        ).getOrThrow()

        allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals("Design System Audit Updated", allTasks.first().task.title)
        assertEquals(TaskPriority.URGENT, allTasks.first().task.priorityEnum)
        assertEquals(1, allTasks.first().completedSubtasksCount)

        // 3. Add and toggle subtasks
        repository.addSubtask(taskId, "Verify Manrope font weights")
        allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(3, allTasks.first().subtasks.size)

        val thirdSubtask = allTasks.first().subtasks.last()
        repository.toggleSubtask(thirdSubtask)
        allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(2, allTasks.first().completedSubtasksCount)

        // 4. Complete recurring task -> marks current completed AND spawns tomorrow's occurrence
        val spawnedId = repository.setTaskCompleted(taskId, completed = true, spawnRecurringIfApplicable = true)
        assertNotNull(spawnedId)

        allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(2, allTasks.size)
        val completedTask = allTasks.first { it.task.id == taskId }
        val nextOccurrence = allTasks.first { it.task.id == spawnedId }
        assertTrue(completedTask.task.isCompleted)
        assertFalse(nextOccurrence.task.isCompleted)
        assertEquals(today + 1L, nextOccurrence.task.dueDateEpochDay)
        // Subtasks on spawned recurring task should be reset to incomplete
        assertTrue(nextOccurrence.subtasks.all { !it.isCompleted })

        // 5. Reopen completed task
        repository.setTaskCompleted(taskId, completed = false, spawnRecurringIfApplicable = false)
        allTasks = repository.tasksWithDetailsFlow.first()
        assertFalse(allTasks.first { it.task.id == taskId }.task.isCompleted)

        // 6. Delete and restore task
        val deletedPair = repository.deleteTask(taskId)
        assertNotNull(deletedPair)
        allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(1, allTasks.size)

        repository.restoreDeletedTask(deletedPair!!.first, deletedPair.second)
        allTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(2, allTasks.size)
    }

    @Test
    fun jsonBackupExportAndImportValidation(): Unit = runBlocking {
        seedTestWorkspaceWithFixtures("Jayson")
        val exportedJson = repository.exportBackupJson()
        val validation = repository.validateBackupJson(exportedJson)
        assertTrue(validation.isSuccess)
        val preview = validation.getOrNull()!!
        assertTrue(preview.taskCount >= 7)

        // Import with replaceExisting = true
        val importResult = repository.importValidatedBackup(exportedJson, replaceExisting = true)
        assertTrue(importResult.isSuccess)
        assertEquals(preview.taskCount, importResult.getOrNull())
    }

    @Test
    fun composeUiJourneyAcrossAllScreens(): Unit = runBlocking {
        seedTestWorkspaceWithFixtures("Jayson")
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            DayformTheme {
                DayformApp(viewModel = viewModel)
            }
        }

        // 1. Wait for Home Dashboard to load with starter sample tasks
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.settings.hasCompletedOnboarding &&
                viewModel.uiState.value.priorityTasks.isNotEmpty()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_greeting_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_card_completed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_card_planned").assertIsDisplayed()

        // 2. Home -> Task Details -> Edit Task -> Cancel -> Task Details -> Back -> Home
        val firstTaskId = viewModel.uiState.value.priorityTasks.first().task.id
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_$firstTaskId"))
        composeTestRule.onNodeWithTag("task_item_card_$firstTaskId").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sub Tasks").assertIsDisplayed()

        // Open Edit Task from Details via More menu -> Edit full task
        composeTestRule.onNodeWithTag("details_more_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_edit_task").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_edit_task_screen").assertIsDisplayed()

        // Cancel Edit (no changes made) -> returns immediately to Task Details (NOT Home)
        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()

        // Navigate back from Task Details -> returns to Home with reverse card morph
        composeTestRule.onNodeWithTag("details_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_dashboard_list").assertIsDisplayed()
        assertNull(viewModel.selectedTaskIdForDetails.value)

        // 3. Home -> Add Task (+) -> Cancel -> Home
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_edit_task_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_dashboard_list").assertIsDisplayed()

        // 4. Tasks -> Filter -> Task Details -> Back -> Tasks (preserving filter state)
        composeTestRule.onNodeWithTag("nav_tasks").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_tasks_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("filter_chip_today").performClick()
        composeTestRule.waitForIdle()
        assertEquals(TaskFilter.TODAY, viewModel.uiState.value.selectedFilter)

        val todayTaskId = viewModel.uiState.value.filteredTasks.first().task.id
        composeTestRule.onNodeWithTag("task_item_card_$todayTaskId").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("details_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_tasks_screen").assertIsDisplayed()
        assertEquals(TaskFilter.TODAY, viewModel.uiState.value.selectedFilter)

        // 5. Tasks -> Upcoming & Calendar view -> Task Details -> Back -> Calendar -> Back -> Tasks
        composeTestRule.onNodeWithTag("open_calendar_view_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("upcoming_calendar_view").assertIsDisplayed()

        val calendarTaskId = viewModel.uiState.value.allTasks
            .first { it.task.dueDateEpochDay == DateTimeUtil.currentLocalEpochDay() }
            .task.id
        composeTestRule.onNodeWithTag("upcoming_calendar_view")
            .performScrollToNode(hasTestTag("task_item_card_$calendarTaskId"))
        composeTestRule.onNodeWithTag("task_item_card_$calendarTaskId").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("details_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("upcoming_calendar_view").assertIsDisplayed()

        composeTestRule.onNodeWithTag("calendar_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_tasks_screen").assertIsDisplayed()

        // 6. Navigate across Insights and Profile tabs
        composeTestRule.onNodeWithTag("nav_insights").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("insights_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_settings_screen").assertIsDisplayed()

        // 7. Deep-link / ViewModel openTaskDetails opens Task Details cleanly and Back returns to prior screen
        viewModel.openTaskDetails(firstTaskId)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("details_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_settings_screen").assertIsDisplayed()
    }

    @Test
    fun sharedBoundsMorphAndMidTransitionClockVerification(): Unit = runBlocking {
        seedTestWorkspaceWithFixtures("Jayson")
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            DayformTheme {
                DayformApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.settings.hasCompletedOnboarding &&
                viewModel.uiState.value.priorityTasks.isNotEmpty()
        }
        composeTestRule.waitForIdle()

        val firstTask = viewModel.uiState.value.priorityTasks.first().task
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_${firstTask.id}"))
        composeTestRule.waitForIdle()

        // Pause clock to inspect mid-transition sharedBounds morph frames
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("task_item_card_${firstTask.id}").performClick()

        // Advance 160ms into the 340-420ms shared-bounds transition
        composeTestRule.mainClock.advanceTimeBy(160L)
        // Destination TaskDetailsScreen is actively entering and title is present in the hierarchy
        composeTestRule.onNodeWithTag("details_task_title").assertIsDisplayed()

        // Complete transition
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()

        // Now verify reverse morph on Android system Back (OnBackPressedDispatcher)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.mainClock.advanceTimeBy(160L)
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Card is still in its scrolled position on Home Dashboard
        composeTestRule.onNodeWithTag("task_item_card_${firstTask.id}").assertIsDisplayed()

        // Verify Plus Button -> Create Task -> Cancel mid-transition and reverse morph
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(160L)
        composeTestRule.onNodeWithTag("create_edit_task_screen").assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_add_task_button").assertIsDisplayed()
    }

    @Test
    fun unsavedEditProtectionAndSystemBackVerification(): Unit = runBlocking {
        seedTestWorkspaceWithFixtures("Jayson")
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            DayformTheme {
                DayformApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.settings.hasCompletedOnboarding &&
                viewModel.uiState.value.priorityTasks.isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // 1. Clean Create Task -> System Back does NOT show Discard dialog
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_edit_task_screen").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithTag("discard_changes_dialog").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("home_dashboard_list").assertIsDisplayed()

        // 2. Dirty Create Task -> System Back shows "Discard changes?" dialog
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Unsaved Draft Idea")
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("discard_changes_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()

        // 3. "Keep editing" dismisses dialog and preserves user's typed title
        composeTestRule.onNodeWithTag("keep_editing_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithTag("discard_changes_dialog").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("task_title_input").assertTextContains("Unsaved Draft Idea")

        // 4. Cancel button also triggers "Discard changes?" -> "Discard changes" exits to Home
        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("discard_changes_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("discard_changes_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_dashboard_list").assertIsDisplayed()

        // 5. Dirty Edit Task from TaskDetailsScreen -> Discard returns to TaskDetailsScreen with original title
        val firstTask = viewModel.uiState.value.priorityTasks.first().task
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_${firstTask.id}"))
        composeTestRule.onNodeWithTag("task_item_card_${firstTask.id}").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("details_more_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_edit_task").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("task_title_input").performTextReplacement("Modified But Unsaved Title")
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("discard_changes_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("discard_changes_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("details_task_title").assertTextEquals(firstTask.title)
    }

    @Test
    fun defaultTaskDateAccuracyAcrossHomeTasksCalendarAndEdit(): Unit = runBlocking {
        seedTestWorkspaceWithFixtures("Jayson")
        val viewModel = createTestViewModel()

        val todayEpoch = DateTimeUtil.currentLocalEpochDay()
        val futureEpoch = todayEpoch + 3L
        val expectedTodayLabel = DateTimeUtil.formatDayWithOrdinalMonth(todayEpoch)
        val expectedFutureLabel = DateTimeUtil.formatDayWithOrdinalMonth(futureEpoch)

        composeTestRule.setContent {
            DayformTheme {
                DayformApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.settings.hasCompletedOnboarding &&
                viewModel.uiState.value.priorityTasks.isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // 1. Navigate to Tasks -> Calendar and select future date (today + 3)
        composeTestRule.onNodeWithTag("nav_tasks").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_calendar_view_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("calendar_strip_day_3").performClick()
        composeTestRule.waitForIdle()
        assertEquals(futureEpoch, viewModel.uiState.value.selectedCalendarEpochDay)

        // 2. Calendar + Bottom Bar Add (+) -> Uses explicit selected calendar date (today + 3)
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("due_date_value_text", useUnmergedTree = true).assertTextEquals(expectedFutureLabel)
        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()

        // 3. Calendar "+ Add on this day" -> Uses that exact day (today + 3)
        composeTestRule.onNodeWithTag("upcoming_calendar_view")
            .performScrollToNode(hasTestTag("calendar_add_on_day_button"))
        composeTestRule.onNodeWithTag("calendar_add_on_day_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("due_date_value_text", useUnmergedTree = true).assertTextEquals(expectedFutureLabel)
        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()

        // 4. Return to Tasks and tap Bottom Bar Add (+) -> Uses Today (NOT the calendar's selected date!)
        composeTestRule.onNodeWithTag("calendar_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("due_date_value_text", useUnmergedTree = true).assertTextEquals(expectedTodayLabel)
        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()

        // 5. Return to Home and tap Bottom Bar Add (+) -> Uses Today (NOT the calendar's selected date!)
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_add_task_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("due_date_value_text", useUnmergedTree = true).assertTextEquals(expectedTodayLabel)
        composeTestRule.onNodeWithTag("cancel_task_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun preserveUnsavedDraftsAcrossConfigurationChanges(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val todayEpoch = DateTimeUtil.currentLocalEpochDay()
        val stateRestorationTester = StateRestorationTester(composeTestRule)

        stateRestorationTester.setContent {
            DayformTheme {
                CreateEditTaskSheet(
                    existingTask = null,
                    initialEpochDay = todayEpoch,
                    categories = categories,
                    use24HourFormat = false,
                    onSaveTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> Result.success(1L) },
                    onCreateCategory = { _, _ -> },
                    onCancel = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Enter title, description, select Urgent priority, and add 2 subtasks
        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Rotated Draft Task")
        composeTestRule.onNodeWithTag("priority_chip_urgent").performClick()
        composeTestRule.onNodeWithTag("task_description_input").performTextInput("Must survive rotation")
        composeTestRule.onNodeWithTag("sheet_subtask_input").performTextInput("Subtask Alpha")
        composeTestRule.onNodeWithTag("sheet_add_subtask_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sheet_subtask_input").performTextInput("Subtask Beta")
        composeTestRule.onNodeWithTag("sheet_add_subtask_button").performClick()
        composeTestRule.waitForIdle()

        // Emulate Android configuration change (e.g. device rotation)
        stateRestorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // Verify all entered fields and subtask drafts survived configuration recreation
        composeTestRule.onNodeWithTag("task_title_input").assertTextContains("Rotated Draft Task")
        composeTestRule.onNodeWithTag("task_description_input").assertTextContains("Must survive rotation")
        composeTestRule.onNodeWithText("1. Subtask Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. Subtask Beta").assertIsDisplayed()
    }

    @Test
    fun customReminderPreservationAcrossEditsAndDueDateChanges(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        val reminderMinutes = 15 * 60 + 45 // 3:45 PM
        val customReminderMillis = DateTimeUtil.epochDayAndMinutesToMillis(today, reminderMinutes)

        // 1. Save task with custom reminder
        val taskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Sprint Retrospective",
            description = "Initial retro agenda",
            dueDateEpochDay = today,
            dueTimeMinutes = 16 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.CUSTOM,
            customReminderMillis = customReminderMillis,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()

        var taskWithDetails = repository.tasksWithDetailsFlow.first().first { it.task.id == taskId }
        assertEquals(ReminderOffset.CUSTOM, taskWithDetails.task.reminderEnum)
        assertEquals(customReminderMillis, taskWithDetails.task.customReminderMillis)

        // 2. Edit unrelated field (description) without touching reminder or date
        // Verify custom reminder millis is preserved exactly
        repository.saveTaskWithSubtasks(
            existingTaskId = taskId,
            title = "Sprint Retrospective",
            description = "Updated retro notes with engineering team",
            dueDateEpochDay = today,
            dueTimeMinutes = 16 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.CUSTOM,
            customReminderMillis = taskWithDetails.task.customReminderMillis,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()

        taskWithDetails = repository.tasksWithDetailsFlow.first().first { it.task.id == taskId }
        assertEquals("Updated retro notes with engineering team", taskWithDetails.task.description)
        assertEquals(customReminderMillis, taskWithDetails.task.customReminderMillis)

        // 3. Move due date to tomorrow, keeping custom reminder time (15:45)
        val tomorrow = today + 1L
        val expectedTomorrowReminderMillis = DateTimeUtil.epochDayAndMinutesToMillis(tomorrow, reminderMinutes)
        repository.saveTaskWithSubtasks(
            existingTaskId = taskId,
            title = "Sprint Retrospective",
            description = "Updated retro notes with engineering team",
            dueDateEpochDay = tomorrow,
            dueTimeMinutes = 16 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.CUSTOM,
            customReminderMillis = expectedTomorrowReminderMillis,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()

        taskWithDetails = repository.tasksWithDetailsFlow.first().first { it.task.id == taskId }
        assertEquals(tomorrow, taskWithDetails.task.dueDateEpochDay)
        assertEquals(expectedTomorrowReminderMillis, taskWithDetails.task.customReminderMillis)
    }

    @Test
    fun atomicTaskSavingWithSubtaskStableIdsAndRollback(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()

        // 1. Create a task with 3 subtasks
        val taskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Mobile Release Checklist",
            description = "Verify build and dependencies",
            dueDateEpochDay = today,
            dueTimeMinutes = null,
            priority = TaskPriority.URGENT,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(
                SubtaskDraft(title = "Subtask 1"),
                SubtaskDraft(title = "Subtask 2"),
                SubtaskDraft(title = "Subtask 3")
            )
        ).getOrThrow()

        var task = repository.tasksWithDetailsFlow.first().first { it.task.id == taskId }
        assertEquals(3, task.subtasks.size)
        val originalSubtask1Id = task.subtasks[0].id
        val originalSubtask2Id = task.subtasks[1].id
        val originalSubtask3Id = task.subtasks[2].id

        // 2. Update: keep subtask 1 unchanged, edit subtask 2 title, remove subtask 3, add subtask 4
        repository.saveTaskWithSubtasks(
            existingTaskId = taskId,
            title = "Mobile Release Checklist v2",
            description = "Verify build and dependencies",
            dueDateEpochDay = today,
            dueTimeMinutes = null,
            priority = TaskPriority.URGENT,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(
                SubtaskDraft(id = originalSubtask1Id, title = "Subtask 1", isCompleted = false),
                SubtaskDraft(id = originalSubtask2Id, title = "Subtask 2 Renamed", isCompleted = true),
                SubtaskDraft(id = 0L, title = "Subtask 4 Added", isCompleted = false)
            )
        ).getOrThrow()

        task = repository.tasksWithDetailsFlow.first().first { it.task.id == taskId }
        assertEquals(3, task.subtasks.size)
        // Subtask 1 retains its stable ID
        assertEquals(originalSubtask1Id, task.subtasks[0].id)
        assertEquals("Subtask 1", task.subtasks[0].title)
        assertFalse(task.subtasks[0].isCompleted)

        // Subtask 2 retains its stable ID and updated fields
        assertEquals(originalSubtask2Id, task.subtasks[1].id)
        assertEquals("Subtask 2 Renamed", task.subtasks[1].title)
        assertTrue(task.subtasks[1].isCompleted)

        // Subtask 3 was removed
        assertFalse(task.subtasks.any { it.id == originalSubtask3Id })

        // Subtask 4 was assigned a fresh unique ID
        assertEquals("Subtask 4 Added", task.subtasks[2].title)
        assertTrue(task.subtasks[2].id > 0L && task.subtasks[2].id != originalSubtask3Id)

        // 3. Missing task save returns explicit failure
        val missingSaveResult = repository.saveTaskWithSubtasks(
            existingTaskId = 999999L,
            title = "Ghost Task",
            description = "",
            dueDateEpochDay = today,
            dueTimeMinutes = null,
            priority = TaskPriority.LOW,
            categoryId = null,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        )
        assertTrue(missingSaveResult.isFailure)
        assertTrue(missingSaveResult.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun saveConfirmationLoadingStateAndErrorHandling(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        var saveInvoked = false
        var shouldSucceed = false

        composeTestRule.setContent {
            DayformTheme {
                CreateEditTaskSheet(
                    existingTask = null,
                    initialEpochDay = today,
                    categories = categories,
                    use24HourFormat = false,
                    onSaveTask = { _, _, _, _, _, _, _, _, _, _, _, _ ->
                        saveInvoked = true
                        if (shouldSucceed) {
                            Result.success(101L)
                        } else {
                            Result.failure(IllegalStateException("Simulated database constraint failure"))
                        }
                    },
                    onCreateCategory = { _, _ -> },
                    onCancel = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Mission Critical Item")
        composeTestRule.waitForIdle()

        // 1. When save fails: error banner is displayed, draft remains preserved
        shouldSucceed = false
        composeTestRule.onNodeWithTag("save_task_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(saveInvoked)
        composeTestRule.onNodeWithTag("save_error_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Simulated database constraint failure").assertIsDisplayed()
        // Form remains open and title is intact
        composeTestRule.onNodeWithTag("task_title_input").assertTextContains("Mission Critical Item")

        // 2. When save succeeds: no error banner
        shouldSucceed = true
        composeTestRule.onNodeWithTag("save_task_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithTag("save_error_banner").fetchSemanticsNodes().size)
    }

    @Test
    fun zeroStateFreshLaunchHasZeroTasksZeroStreaksZeroReminders(): Unit = runBlocking {
        // Fresh install initialization with zero sample data
        repository.initializeFirstLaunch(displayName = "")

        val tasks = repository.tasksWithDetailsFlow.first()
        val categories = repository.categoriesFlow.first()
        val settings = preferencesRepository.userSettingsFlow.first()
        val insights = repository.computeInsights(tasks, categories)

        // Verifications for clean zero-state
        assertEquals(0, tasks.size)
        assertEquals(4, categories.size) // Starter system categories only
        assertEquals(0, insights.completedToday)
        assertEquals(0, insights.completedThisWeek)
        assertEquals(0, insights.plannedThisWeek)
        assertEquals(0, insights.currentDailyStreak)
        assertEquals(0, insights.totalCompletedAllTime)
        assertEquals(0, insights.totalTasksAllTime)
        assertEquals(0, insights.overallCompletionRatePercent)
        assertEquals(0f, insights.weeklyProgressFraction)

        val uiState = DayformUiState(
            isLoading = false,
            settings = settings,
            allTasks = tasks,
            categories = categories,
            completedAllTimeCount = 0,
            plannedTodayCount = 0,
            completedPlannedTodayCount = 0,
            pendingRemindersCount = 0,
            priorityTasks = emptyList(),
            filteredTasks = emptyList(),
            insights = insights
        )

        composeTestRule.setContent {
            DayformTheme {
                HomeDashboardScreen(
                    uiState = uiState,
                    onOpenTaskDetails = {},
                    onToggleTaskComplete = {},
                    onCreateTaskClick = {},
                    onCompletedStatClick = {},
                    onPlannedStatClick = {},
                    onViewAllTasksClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify Home UI zero-state elements, compact header row alignment, absence of avatar, and absence of sample-task button
        composeTestRule.onNodeWithTag("home_avatar_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("empty_load_samples_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_header_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_greeting_title").assertTextEquals("Hello, there")
        composeTestRule.onNodeWithTag("home_notification_bell").assertIsDisplayed()

        val greetingBounds = composeTestRule.onNodeWithTag("home_greeting_title").fetchSemanticsNode().boundsInRoot
        val bellBounds = composeTestRule.onNodeWithTag("home_notification_bell").fetchSemanticsNode().boundsInRoot
        assertTrue("Greeting must be positioned to the left of the notification bell", greetingBounds.right <= bellBounds.left)
        assertTrue("Greeting and notification bell must share the top header row without a vertical gap", kotlin.math.abs(greetingBounds.top - bellBounds.top) < 60f)

        composeTestRule.onNodeWithTag("home_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_create_task_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("priority_tasks_count_badge").assertTextEquals("0")
        composeTestRule.onNodeWithTag("today_progress_text").assertTextEquals("0 of 0 tasks completed today")
    }

    @Test
    fun transactionalRollbackPreservesIntegrityWhenSaveFails(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        val initialTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(0, initialTasks.size)

        // 1. Inject a real mid-transaction failure AFTER task and subtask SQLite writes have executed
        var wroteTaskBeforeFailure = false
        var wroteSubtasksCountBeforeFailure = 0
        repository.midTransactionHook = { writtenTaskId ->
            val uncommittedTask = database.dayformDao().getTaskById(writtenTaskId)
            val uncommittedSubtasks = database.dayformDao().getSubtasksForTaskOnce(writtenTaskId)
            wroteTaskBeforeFailure = uncommittedTask != null
            wroteSubtasksCountBeforeFailure = uncommittedSubtasks.size
            throw IllegalStateException("Injected mid-transaction failure after SQLite writes")
        }

        val createFailResult = try {
            repository.saveTaskWithSubtasks(
                existingTaskId = null,
                title = "Mid-Transaction Rollback Task",
                description = "Should roll back completely after SQLite writes",
                dueDateEpochDay = today,
                dueTimeMinutes = null,
                priority = TaskPriority.HIGH,
                categoryId = categories.first().id,
                reminderOffset = ReminderOffset.NONE,
                customReminderMillis = null,
                recurrenceType = RecurrenceType.NEVER,
                recurrenceWeekdays = "",
                subtaskDrafts = listOf(
                    SubtaskDraft(title = "Uncommitted Subtask A"),
                    SubtaskDraft(title = "Uncommitted Subtask B")
                )
            )
        } finally {
            repository.midTransactionHook = null
        }

        // Verify writes occurred inside the transaction before the failure, then rolled back completely
        assertTrue(wroteTaskBeforeFailure)
        assertEquals(2, wroteSubtasksCountBeforeFailure)
        assertTrue(createFailResult.isFailure)
        assertEquals(0, repository.tasksWithDetailsFlow.first().size)
        assertEquals(0, database.dayformDao().getAllSubtasksOnce().size)

        // 2. Commit a valid baseline task with 1 subtask, then fail mid-transaction during an edit
        val baselineId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Committed Baseline Task",
            description = "Original description",
            dueDateEpochDay = today,
            dueTimeMinutes = null,
            priority = TaskPriority.MEDIUM,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(SubtaskDraft(title = "Original Subtask"))
        ).getOrThrow()

        var sawUpdatedWritesBeforeAbort = false
        repository.midTransactionHook = { writtenTaskId ->
            val updatedTask = database.dayformDao().getTaskById(writtenTaskId)
            val updatedSubtasks = database.dayformDao().getSubtasksForTaskOnce(writtenTaskId)
            sawUpdatedWritesBeforeAbort =
                updatedTask?.title == "Corrupted Title" && updatedSubtasks.size == 2
            throw IllegalStateException("Abort edit mid-transaction after writes")
        }

        val editFailResult = try {
            val existingSubtask = database.dayformDao().getSubtasksForTaskOnce(baselineId).first()
            repository.saveTaskWithSubtasks(
                existingTaskId = baselineId,
                title = "Corrupted Title",
                description = "Corrupted description",
                dueDateEpochDay = today,
                dueTimeMinutes = null,
                priority = TaskPriority.URGENT,
                categoryId = categories.first().id,
                reminderOffset = ReminderOffset.NONE,
                customReminderMillis = null,
                recurrenceType = RecurrenceType.NEVER,
                recurrenceWeekdays = "",
                subtaskDrafts = listOf(
                    SubtaskDraft(id = existingSubtask.id, title = "Mutated Subtask"),
                    SubtaskDraft(id = 0L, title = "Extra Uncommitted Subtask")
                )
            )
        } finally {
            repository.midTransactionHook = null
        }

        assertTrue(sawUpdatedWritesBeforeAbort)
        assertTrue(editFailResult.isFailure)

        // Verify the baseline task and its subtask rolled back to their exact pre-edit state
        val preserved = repository.tasksWithDetailsFlow.first().single()
        assertEquals("Committed Baseline Task", preserved.task.title)
        assertEquals("Original description", preserved.task.description)
        assertEquals(1, preserved.subtasks.size)
        assertEquals("Original Subtask", preserved.subtasks.single().title)
    }

    @Test
    fun backGestureBlockedWhileSavingInCreateEditTaskSheet(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        val saveGate = CompletableDeferred<Result<Long>>()
        var cancelClicked = false
        var saveCompleted = false

        composeTestRule.setContent {
            DayformTheme {
                CreateEditTaskSheet(
                    existingTask = null,
                    initialEpochDay = today,
                    categories = categories,
                    use24HourFormat = false,
                    onSaveTask = { _, _, _, _, _, _, _, _, _, _, _, _ ->
                        saveGate.await()
                    },
                    onCreateCategory = { _, _ -> },
                    onCancel = { cancelClicked = true },
                    onSaveSuccess = { saveCompleted = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Draft Under Protection")
        composeTestRule.waitForIdle()

        // Prior to save, Cancel button is enabled
        composeTestRule.onNodeWithTag("cancel_task_button").assertIsEnabled()

        // Trigger save — suspends on saveGate
        composeTestRule.onNodeWithTag("save_task_button").performClick()
        composeTestRule.waitForIdle()

        // While save is suspended, Cancel and Save buttons are disabled and loading indicator is shown
        composeTestRule.onNodeWithTag("cancel_task_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("save_task_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("save_loading_indicator").assertIsDisplayed()

        // Dispatch system Back while save is suspended
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // System Back is intercepted: editor stays open, discard dialog does NOT appear, cancel is NOT invoked
        assertFalse(cancelClicked)
        assertFalse(saveCompleted)
        composeTestRule.onNodeWithTag("create_edit_task_screen").assertIsDisplayed()
        assertEquals(
            0,
            composeTestRule.onAllNodesWithTag("discard_changes_dialog").fetchSemanticsNodes().size
        )

        // Complete the suspended save and verify clean completion
        saveGate.complete(Result.success(1L))
        composeTestRule.waitForIdle()
        assertTrue(saveCompleted)
        assertFalse(cancelClicked)
    }

    @Test
    fun activityRecreationDuringInFlightSavePreventsDuplicatesAndUnlocksEditor(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        var currentSaveGate = CompletableDeferred<Boolean>()
        var saveInvocationCount = 0
        var saveCompletedCount = 0

        fun mountSheet(activity: ComponentActivity) {
            activity.setContent {
                DayformTheme {
                    CreateEditTaskSheet(
                        existingTask = null,
                        initialEpochDay = today,
                        categories = categories,
                        use24HourFormat = false,
                        onSaveTask = { existingId, title, desc, dueDay, dueTime, prio, catId, remOffset, customRem, recType, recDays, subtasks ->
                            saveInvocationCount++
                            val shouldSucceed = currentSaveGate.await()
                            if (!shouldSucceed) {
                                Result.failure(IllegalStateException("Transient failure across recreation"))
                            } else {
                                repository.saveTaskWithSubtasks(
                                    existingTaskId = existingId,
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
                            }
                        },
                        onCreateCategory = { _, _ -> },
                        onCancel = {},
                        onSaveSuccess = { saveCompletedCount++ }
                    )
                }
            }
        }

        val app = context.applicationContext as Application
        val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ComponentActivity && savedInstanceState != null) {
                    mountSheet(activity)
                }
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }

        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        try {
            composeTestRule.runOnUiThread {
                mountSheet(composeTestRule.activity)
            }
            composeTestRule.waitForIdle()

            // Enter draft text and start in-flight save #1
            composeTestRule.onNodeWithTag("task_title_input").performTextInput("Survived Activity Recreation")
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("save_task_button").performClick()
            composeTestRule.waitForIdle()

            assertEquals(1, saveInvocationCount)
            composeTestRule.onNodeWithTag("save_loading_indicator").assertIsDisplayed()

            // Recreate the Activity while save #1 is still suspended in-flight
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            // Draft text is preserved across recreation and in-flight save is still active & protected
            composeTestRule.onNodeWithTag("task_title_input").assertTextContains("Survived Activity Recreation")
            composeTestRule.onNodeWithTag("save_loading_indicator").assertIsDisplayed()
            composeTestRule.onNodeWithTag("save_task_button").assertIsNotEnabled()

            // Attempting to click save again during recreation does not trigger a duplicate save
            composeTestRule.onNodeWithTag("save_task_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(1, saveInvocationCount)

            // 1. Complete in-flight save #1 with failure to verify editor unlocks after recreation
            currentSaveGate.complete(false)
            composeTestRule.waitForIdle()

            // Editor is NOT permanently locked: error banner is shown and Save button is re-enabled
            composeTestRule.onNodeWithTag("save_error_banner").assertIsDisplayed()
            composeTestRule.onNodeWithTag("save_task_button").assertIsEnabled()
            assertEquals(0, repository.tasksWithDetailsFlow.first().size)

            // 2. Retry save #2, recreate Activity again while in-flight, and complete with success
            currentSaveGate = CompletableDeferred()
            composeTestRule.onNodeWithTag("save_task_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(2, saveInvocationCount)
            composeTestRule.onNodeWithTag("save_loading_indicator").assertIsDisplayed()

            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            // Still in-flight after second recreation; duplicate clicks are ignored
            composeTestRule.onNodeWithTag("save_task_button").assertIsNotEnabled()
            composeTestRule.onNodeWithTag("save_task_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(2, saveInvocationCount)

            currentSaveGate.complete(true)
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                saveCompletedCount == 1
            }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            composeTestRule.waitForIdle()

            // Verify exactly 1 task was persisted and onSaveSuccess fired once
            val persistedTasks = repository.tasksWithDetailsFlow.first()
            assertEquals(1, persistedTasks.size)
            assertEquals("Survived Activity Recreation", persistedTasks.single().task.title)
            assertEquals(1, saveCompletedCount)
        } finally {
            app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }

    @Test
    fun postCommitReminderFailureKeepsTaskSavedAndSupportsRetry(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        val viewModel = createTestViewModel()

        var scheduleAttempts = 0
        repository.reminderScheduleHandler = { _, _, _ ->
            scheduleAttempts++
            if (scheduleAttempts == 1) {
                Result.failure(IllegalStateException("AlarmManager scheduling temporarily unavailable"))
            } else {
                Result.success(true)
            }
        }

        var sheetClosedOnSuccess = false
        try {
            composeTestRule.setContent {
                DayformTheme {
                    CreateEditTaskSheet(
                        existingTask = null,
                        initialEpochDay = today + 1L,
                        categories = categories,
                        use24HourFormat = false,
                        onSaveTask = { id, title, desc, dueDay, dueTime, prio, catId, remOffset, customRem, recType, recDays, subtasks ->
                            // Force a custom reminder in the future so post-commit scheduling executes
                            val futureMillis = System.currentTimeMillis() + 3_600_000L
                            viewModel.saveTask(
                                existingTaskId = id,
                                title = title,
                                description = desc,
                                dueDateEpochDay = dueDay,
                                dueTimeMinutes = dueTime ?: (10 * 60),
                                priority = prio,
                                categoryId = catId,
                                reminderOffset = ReminderOffset.CUSTOM,
                                customReminderMillis = customRem ?: futureMillis,
                                recurrenceType = recType,
                                recurrenceWeekdays = recDays,
                                subtaskDrafts = subtasks
                            )
                        },
                        checkReminderWarning = {
                            repository.lastSaveOutcome?.reminderWarningMessage
                        },
                        onRetryReminder = { savedTaskId ->
                            viewModel.retryTaskReminder(savedTaskId)
                        },
                        onCreateCategory = { _, _ -> },
                        onCancel = {},
                        onSaveSuccess = { sheetClosedOnSuccess = true }
                    )
                }
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("task_title_input").performTextInput("Post-Commit Reminder Task")
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("save_task_button").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                composeTestRule.onAllNodesWithTag("reminder_warning_banner").fetchSemanticsNodes().isNotEmpty()
            }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            composeTestRule.waitForIdle()

            // 1. Task IS committed in Room despite post-commit reminder failure
            val savedTasks = repository.tasksWithDetailsFlow.first()
            assertEquals(1, savedTasks.size)
            assertEquals("Post-Commit Reminder Task", savedTasks.single().task.title)
            assertEquals(1, scheduleAttempts)

            // 2. Editor stays open with recoverable reminder warning banner and retry button
            assertFalse(sheetClosedOnSuccess)
            composeTestRule.onNodeWithTag("reminder_warning_banner").assertIsDisplayed()
            composeTestRule.onNodeWithTag("retry_reminder_button").assertIsDisplayed()

            // 3. Clicking Retry Reminder schedules the reminder without duplicating the task and completes
            composeTestRule.onNodeWithTag("retry_reminder_button").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                sheetClosedOnSuccess
            }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            composeTestRule.waitForIdle()

            assertEquals(2, scheduleAttempts)
            assertTrue(sheetClosedOnSuccess)
            assertEquals(1, repository.tasksWithDetailsFlow.first().size)
        } finally {
            repository.reminderScheduleHandler = com.example.notifications.ReminderScheduler::scheduleTaskReminder
        }
    }

    @Test
    fun backupImportValidationAndMidTransactionRollbackPreservesDatabaseAndAlarms(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        val futureEpochDay = today + 2L

        // Create a baseline task with subtasks and a future scheduled reminder
        val baselineTaskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Baseline Task Before Import",
            description = "Must survive failed import",
            dueDateEpochDay = futureEpochDay,
            dueTimeMinutes = 14 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.TEN_MIN_BEFORE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(
                SubtaskDraft(title = "Baseline Subtask 1"),
                SubtaskDraft(title = "Baseline Subtask 2")
            )
        ).getOrThrow()

        val validBackupJson = repository.exportBackupJson()

        // 1. Preflight validation rejects malformed and invalid payloads before any DB mutation
        assertTrue(repository.validateBackupJson("{not-valid-json").isFailure)
        assertTrue(repository.validateBackupJson("""{"app":"OTHER","schemaVersion":1,"tasks":[]}""").isFailure)
        assertTrue(repository.validateBackupJson("""{"app":"DAYFORM","schemaVersion":99,"tasks":[]}""").isFailure)
        assertTrue(repository.validateBackupJson("""{"app":"DAYFORM","schemaVersion":2}""").isFailure)
        // Invalid enum value
        assertTrue(
            repository.validateBackupJson(
                """{"app":"DAYFORM","schemaVersion":2,"categories":[],"tasks":[{"id":10,"title":"Bad Priority","dueDateEpochDay":$today,"priority":"SUPER_URGENT","reminderOffset":"NONE","recurrenceType":"NEVER"}],"subtasks":[]}"""
            ).isFailure
        )
        // Broken category foreign reference
        assertTrue(
            repository.validateBackupJson(
                """{"app":"DAYFORM","schemaVersion":2,"categories":[{"id":1,"name":"Work","colorHex":"#CCFF00"}],"tasks":[{"id":10,"title":"Orphan Category","dueDateEpochDay":$today,"categoryId":999,"priority":"MEDIUM","reminderOffset":"NONE","recurrenceType":"NEVER"}],"subtasks":[]}"""
            ).isFailure
        )
        // Broken subtask -> taskId reference
        assertTrue(
            repository.validateBackupJson(
                """{"app":"DAYFORM","schemaVersion":2,"categories":[],"tasks":[{"id":10,"title":"Parent","dueDateEpochDay":$today,"priority":"MEDIUM","reminderOffset":"NONE","recurrenceType":"NEVER"}],"subtasks":[{"id":100,"taskId":999,"title":"Orphan Subtask"}]}"""
            ).isFailure
        )
        // Duplicate task IDs
        assertTrue(
            repository.validateBackupJson(
                """{"app":"DAYFORM","schemaVersion":2,"categories":[],"tasks":[{"id":10,"title":"First","dueDateEpochDay":$today},{"id":10,"title":"Duplicate","dueDateEpochDay":$today}],"subtasks":[]}"""
            ).isFailure
        )

        // 2. Build a second valid backup with 2 tasks to test mid-import failure & full rollback
        val incomingJson = """
            {
              "app": "DAYFORM",
              "schemaVersion": 2,
              "exportedAt": ${System.currentTimeMillis()},
              "categories": [
                {"id": 50, "name": "Imported Cat", "colorHex": "#CCFF00", "iconKey": "tag", "isDefault": false, "sortOrder": 0}
              ],
              "tasks": [
                {"id": 501, "title": "Incoming Task 1", "description": "First", "dueDateEpochDay": $futureEpochDay, "dueTimeMinutes": 600, "priority": "HIGH", "categoryId": 50, "reminderOffset": "NONE", "recurrenceType": "NEVER", "recurrenceWeekdays": "", "isCompleted": false},
                {"id": 502, "title": "Incoming Task 2", "description": "Second", "dueDateEpochDay": $futureEpochDay, "dueTimeMinutes": 660, "priority": "LOW", "categoryId": 50, "reminderOffset": "NONE", "recurrenceType": "NEVER", "recurrenceWeekdays": "", "isCompleted": false}
              ],
              "subtasks": [
                {"id": 601, "taskId": 501, "title": "Incoming Subtask 1", "isCompleted": false, "sortOrder": 0}
              ]
            }
        """.trimIndent()
        assertTrue(repository.validateBackupJson(incomingJson).isSuccess)

        var wroteFirstIncomingTaskBeforeAbort = false
        repository.midImportTransactionHook = { writtenTaskIndex ->
            if (writtenTaskIndex == 1) {
                val allTasksMidTx = database.dayformDao().getAllTasksOnce()
                wroteFirstIncomingTaskBeforeAbort = allTasksMidTx.any { it.title == "Incoming Task 1" }
                throw IllegalStateException("Simulated mid-import failure after task #1 write")
            }
        }

        val replaceFailResult = try {
            repository.importValidatedBackup(incomingJson, replaceExisting = true)
        } finally {
            repository.midImportTransactionHook = null
        }

        // Verify mid-transaction write happened and then rolled back completely (replaceExisting = true)
        assertTrue(wroteFirstIncomingTaskBeforeAbort)
        assertTrue(replaceFailResult.isFailure)

        val preservedTasksAfterReplaceAbort = repository.tasksWithDetailsFlow.first()
        assertEquals(1, preservedTasksAfterReplaceAbort.size)
        assertEquals(baselineTaskId, preservedTasksAfterReplaceAbort.single().task.id)
        assertEquals("Baseline Task Before Import", preservedTasksAfterReplaceAbort.single().task.title)
        assertEquals(2, preservedTasksAfterReplaceAbort.single().subtasks.size)
        assertEquals(4, repository.categoriesFlow.first().size)

        // 3. Also verify mid-import rollback in merge mode (replaceExisting = false)
        repository.midImportTransactionHook = { writtenTaskIndex ->
            if (writtenTaskIndex == 1) {
                throw IllegalStateException("Simulated mid-import merge failure")
            }
        }
        val mergeFailResult = try {
            repository.importValidatedBackup(incomingJson, replaceExisting = false)
        } finally {
            repository.midImportTransactionHook = null
        }
        assertTrue(mergeFailResult.isFailure)
        assertEquals(1, repository.tasksWithDetailsFlow.first().size)
        assertEquals(4, repository.categoriesFlow.first().size)

        // 4. Successful replace import commits atomically
        val successImport = repository.importValidatedBackup(incomingJson, replaceExisting = true)
        assertTrue(successImport.isSuccess)
        assertEquals(2, successImport.getOrThrow())
        val finalTasks = repository.tasksWithDetailsFlow.first()
        assertEquals(2, finalTasks.size)
        assertTrue(finalTasks.any { it.task.title == "Incoming Task 1" && it.subtasks.size == 1 })
        assertTrue(finalTasks.any { it.task.title == "Incoming Task 2" })

        // Restore original backup cleanly
        assertTrue(repository.importValidatedBackup(validBackupJson, replaceExisting = true).isSuccess)
        assertEquals(1, repository.tasksWithDetailsFlow.first().size)
    }

    @Test
    fun recurringTaskCompletionIsAtomicIdempotentSeriesAwareAndUndoSafe(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val jan31 = java.time.LocalDate.of(2026, 1, 31).toEpochDay()

        // 1. Two distinct recurring tasks with the SAME title in different series
        val seriesATaskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Daily Standup",
            description = "Team A",
            dueDateEpochDay = jan31,
            dueTimeMinutes = 9 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceWeekdays = "",
            subtaskDrafts = listOf(SubtaskDraft(title = "Prepare notes", isCompleted = true))
        ).getOrThrow()

        val seriesBTaskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Daily Standup",
            description = "Team B",
            dueDateEpochDay = jan31,
            dueTimeMinutes = 10 * 60,
            priority = TaskPriority.MEDIUM,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.NONE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()

        // Complete Series A twice rapidly — idempotency must spawn only ONE next occurrence
        val spawnedA1 = repository.setTaskCompleted(seriesATaskId, true)
        val spawnedA2 = repository.setTaskCompleted(seriesATaskId, true)
        assertNotNull(spawnedA1)
        assertEquals(spawnedA1, spawnedA2)

        // Complete Series B — must NOT be suppressed by Series A having the same title and due date
        val spawnedB1 = repository.setTaskCompleted(seriesBTaskId, true)
        assertNotNull(spawnedB1)
        assertTrue(spawnedA1 != spawnedB1)

        // Verify monthly anchor day 31 clamped to Feb 28 and preserved recurrenceAnchorDay = 31
        val spawnedATask = database.dayformDao().getTaskById(spawnedA1!!)!!
        assertEquals(java.time.LocalDate.of(2026, 2, 28).toEpochDay(), spawnedATask.dueDateEpochDay)
        assertEquals(31, spawnedATask.recurrenceAnchorDay)
        // Complete Feb 28 occurrence -> must advance to Mar 31 (not Mar 28)
        val spawnedAMarchId = repository.setTaskCompleted(spawnedA1, true)!!
        val spawnedAMarchTask = database.dayformDao().getTaskById(spawnedAMarchId)!!
        assertEquals(java.time.LocalDate.of(2026, 3, 31).toEpochDay(), spawnedAMarchTask.dueDateEpochDay)

        // 2. Undo completion removes untouched auto-spawned occurrence
        repository.undoTaskCompletion(spawnedA1, spawnedAMarchId)
        assertNull(database.dayformDao().getTaskById(spawnedAMarchId))
        assertFalse(database.dayformDao().getTaskById(spawnedA1)!!.isCompleted)

        // 3. If user edits the spawned occurrence before pressing Undo, Undo preserves the edited occurrence
        val spawnedBTaskBeforeEdit = database.dayformDao().getTaskById(spawnedB1!!)!!
        database.dayformDao().updateTask(
            spawnedBTaskBeforeEdit.copy(
                title = "Daily Standup (User Edited)",
                updatedAt = spawnedBTaskBeforeEdit.createdAt + 5000L
            )
        )
        repository.undoTaskCompletion(seriesBTaskId, spawnedB1)
        assertFalse(database.dayformDao().getTaskById(seriesBTaskId)!!.isCompleted)
        val preservedEditedSpawn = database.dayformDao().getTaskById(spawnedB1)
        assertNotNull(preservedEditedSpawn)
        assertEquals("Daily Standup (User Edited)", preservedEditedSpawn!!.title)
    }

    @Test
    fun roomDatabaseMigration1To2PreservesExistingUserData(): Unit = runBlocking {
        val dbName = "dayform_migration_test_${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        try {
            // 1. Create a real v1 SQLite database matching exported schema 1.json and insert v1 rows
            val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            try {
                sqliteDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL)"
                )
                sqliteDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `dueTimeMinutes` INTEGER, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `priority` TEXT NOT NULL, `categoryId` INTEGER, `reminderOffset` TEXT NOT NULL, `customReminderMillis` INTEGER, `recurrenceType` TEXT NOT NULL, `recurrenceWeekdays` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isSampleData` INTEGER NOT NULL)"
                )
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDateEpochDay` ON `tasks` (`dueDateEpochDay`)")
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
                sqliteDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subtasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)")
                sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                sqliteDb.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '07b1e485b23a66319d1fa14db0ab1775')")

                sqliteDb.execSQL(
                    "INSERT INTO `categories` (`id`, `name`, `colorHex`, `sortOrder`) VALUES (1, 'Legacy Work', '#CCFF00', 0)"
                )
                sqliteDb.execSQL(
                    "INSERT INTO `tasks` (`id`, `title`, `description`, `createdAt`, `updatedAt`, `dueDateEpochDay`, `dueTimeMinutes`, `isCompleted`, `completedAt`, `priority`, `categoryId`, `reminderOffset`, `customReminderMillis`, `recurrenceType`, `recurrenceWeekdays`, `sortOrder`, `isSampleData`) VALUES (42, 'V1 Monthly Task', 'Migrated from v1', 1700000000000, 1700000000000, 20484, 540, 0, NULL, 'HIGH', 1, 'NONE', NULL, 'MONTHLY', '', 0, 0)"
                )
                sqliteDb.execSQL(
                    "INSERT INTO `subtasks` (`id`, `taskId`, `title`, `isCompleted`, `sortOrder`) VALUES (101, 42, 'V1 Checklist Item', 0, 0)"
                )
                sqliteDb.version = 1
            } finally {
                sqliteDb.close()
            }

            // 2. Open with Room v2 and MIGRATION_1_2
            val migratedDb = Room.databaseBuilder(context, DayformDatabase::class.java, dbName)
                .addMigrations(*DayformDatabase.ALL_MIGRATIONS)
                .allowMainThreadQueries()
                .build()

            try {
                val migratedDao = migratedDb.dayformDao()
                val migratedTasks = migratedDao.getAllTasksOnce()
                val migratedCategories = migratedDao.getAllCategoriesOnce()
                val migratedSubtasks = migratedDao.getAllSubtasksOnce()

                assertEquals(1, migratedCategories.size)
                assertEquals("Legacy Work", migratedCategories.single().name)
                assertEquals(1, migratedTasks.size)
                val v1Task = migratedTasks.single()
                assertEquals(42L, v1Task.id)
                assertEquals("V1 Monthly Task", v1Task.title)
                assertNull(v1Task.recurrenceSeriesId)
                assertNull(v1Task.spawnedNextTaskId)
                assertNull(v1Task.recurrenceAnchorDay)
                assertEquals(1, migratedSubtasks.size)
                assertEquals("V1 Checklist Item", migratedSubtasks.single().title)
            } finally {
                migratedDb.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun notificationOptOutReconciliationAndReminderStatusContract(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val today = DateTimeUtil.currentLocalEpochDay()
        val tomorrow = today + 1L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAlarmManager = org.robolectric.Shadows.shadowOf(alarmManager)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)

        // 1. Save a task with a future reminder while notifications are enabled
        preferencesRepository.setNotificationsEnabled(true)
        val futureTaskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Future Reminder Task",
            description = "Verify alarm lifecycle",
            dueDateEpochDay = tomorrow,
            dueTimeMinutes = 15 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.AT_DUE_TIME,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()

        assertEquals(
            com.example.notifications.ReminderScheduleStatus.SCHEDULED,
            repository.lastSaveOutcome?.reminderStatus
        )
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        // 2. Past trigger time returns SKIPPED_PAST_TRIGGER explicitly instead of pretending to schedule
        val pastMillis = System.currentTimeMillis() - 3_600_000L
        repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Past Reminder Task",
            description = "Already in the past",
            dueDateEpochDay = today - 2L,
            dueTimeMinutes = 9 * 60,
            priority = TaskPriority.LOW,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.CUSTOM,
            customReminderMillis = pastMillis,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()
        assertEquals(
            com.example.notifications.ReminderScheduleStatus.SKIPPED_PAST_TRIGGER,
            repository.lastSaveOutcome?.reminderStatus
        )

        // 3. Disabling notifications in preferences cancels all scheduled alarms and blocks receiver from posting
        val viewModel = createTestViewModel()
        viewModel.setNotificationsEnabled(false)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            shadowAlarmManager.scheduledAlarms.isEmpty()
        }
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        val broadcastIntent = android.content.Intent(context, com.example.notifications.TaskReminderReceiver::class.java).apply {
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_ID, futureTaskId)
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_TITLE, "Future Reminder Task")
        }
        val postedWhenOptedOut = com.example.notifications.TaskReminderReceiver.handleReminderBroadcast(context, broadcastIntent)
        assertFalse(postedWhenOptedOut)
        assertEquals(0, shadowNotificationManager.allNotifications.size)

        // 4. Re-enabling notifications and running BootCompletedReceiver reconciliation restores future alarm
        preferencesRepository.setNotificationsEnabled(true)
        val restoredCount = com.example.notifications.BootCompletedReceiver.handleSystemReconciliation(
            context,
            android.content.Intent.ACTION_BOOT_COMPLETED
        )
        assertEquals(1, restoredCount)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        // 5. Denying POST_NOTIFICATIONS returns BLOCKED_OS_PERMISSION and blocks broadcast posting
        org.robolectric.Shadows.shadowOf(context as Application)
            .denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        try {
            val blockedReport = repository.retryTaskReminder(futureTaskId)
            assertTrue(blockedReport.isFailure)
            assertTrue(blockedReport.exceptionOrNull()?.message?.contains("notification permission", ignoreCase = true) == true)
            assertFalse(com.example.notifications.TaskReminderReceiver.handleReminderBroadcast(context, broadcastIntent))
        } finally {
            org.robolectric.Shadows.shadowOf(context as Application)
                .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun profileSettingsDisplaysDeveloperCreditAndLaunchesLinktreeIntent(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val settings = preferencesRepository.userSettingsFlow.first()
        val uiState = DayformUiState(
            isLoading = false,
            settings = settings,
            allTasks = emptyList(),
            categories = categories,
            completedAllTimeCount = 0,
            plannedTodayCount = 0,
            completedPlannedTodayCount = 0,
            pendingRemindersCount = 0,
            priorityTasks = emptyList(),
            filteredTasks = emptyList(),
            insights = repository.computeInsights(emptyList(), categories)
        )

        composeTestRule.setContent {
            DayformTheme {
                com.example.ui.screens.ProfileSettingsScreen(
                    uiState = uiState,
                    repository = repository,
                    onUpdateDisplayName = {},
                    onCreateCategory = { _, _ -> },
                    onUpdateCategory = { _, _, _ -> },
                    onDeleteCategory = { _, _ -> },
                    onToggleNotifications = {},
                    onToggleHaptics = {},
                    onToggleSound = {},
                    onToggleWeekStartsMonday = {},
                    onToggleUse24Hour = {},
                    onShowMessage = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify Remove Sample Tasks button is completely absent
        composeTestRule.onNodeWithTag("remove_sample_tasks_button").assertDoesNotExist()

        // Scroll to the About DAYFORM card at the bottom of Profile & Settings
        composeTestRule.onNodeWithTag("profile_settings_screen")
            .performScrollToNode(hasTestTag("about_dayform_card"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_dayform_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_credit_name")
            .assertTextEquals("Rahmathullah | Designwithrahma")
        composeTestRule.onNodeWithText("https://linktr.ee/designwithrahma").assertIsDisplayed()

        // Click the Linktree row and verify external ACTION_VIEW HTTPS intent is launched
        composeTestRule.onNodeWithTag("developer_credit_linktree_row").performClick()
        composeTestRule.waitForIdle()

        val startedIntent = org.robolectric.Shadows.shadowOf(composeTestRule.activity).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(android.content.Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(
            android.net.Uri.parse("https://linktr.ee/designwithrahma"),
            startedIntent.data
        )
    }

    @Test
    fun deletedOrMissingTaskNeverGeneratesNotificationFromStaleAlarmIntent(): Unit = runBlocking {
        val categories = repository.ensureStarterCategories()
        val tomorrow = DateTimeUtil.currentLocalEpochDay() + 1L
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)
        notificationManager.cancelAll()

        preferencesRepository.setNotificationsEnabled(true)

        // 1. Create a task with a scheduled reminder, then delete the task from Room
        val taskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Task That Will Be Deleted",
            description = "Should never notify after deletion",
            dueDateEpochDay = tomorrow,
            dueTimeMinutes = 14 * 60,
            priority = TaskPriority.HIGH,
            categoryId = categories.first().id,
            reminderOffset = ReminderOffset.AT_DUE_TIME,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.NEVER,
            recurrenceWeekdays = "",
            subtaskDrafts = emptyList()
        ).getOrThrow()

        repository.deleteTask(taskId)
        assertNull(database.dayformDao().getTaskById(taskId))

        // 2. Fire a stale alarm Intent that still carries title and description extras for the deleted task
        val staleDeletedTaskIntent = android.content.Intent(
            context,
            com.example.notifications.TaskReminderReceiver::class.java
        ).apply {
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_TITLE, "Task That Will Be Deleted")
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_DESCRIPTION, "Stale Intent Description")
        }

        val postedForDeletedTask =
            com.example.notifications.TaskReminderReceiver.handleReminderBroadcast(context, staleDeletedTaskIntent)
        assertFalse(postedForDeletedTask)
        assertEquals(0, shadowNotificationManager.allNotifications.size)

        // 3. Fire a stale alarm Intent for an unknown/non-existent task ID with non-blank extras
        val staleUnknownTaskIntent = android.content.Intent(
            context,
            com.example.notifications.TaskReminderReceiver::class.java
        ).apply {
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_ID, 999999L)
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_TITLE, "Ghost Alarm Title")
            putExtra(com.example.notifications.ReminderScheduler.EXTRA_TASK_DESCRIPTION, "Ghost Alarm Body")
        }

        val postedForUnknownTask =
            com.example.notifications.TaskReminderReceiver.handleReminderBroadcast(context, staleUnknownTaskIntent)
        assertFalse(postedForUnknownTask)
        assertEquals(0, shadowNotificationManager.allNotifications.size)
    }

    @Test
    fun legacyV1MigratedSameTitleRecurringTasksNeverCollideOrDeleteUnrelatedTaskOnUndo(): Unit = runBlocking {
        val dbName = "dayform_v1_recurrence_collision_${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        try {
            // 1. Create a real v1 SQLite database with two unrelated tasks sharing the exact same title:
            //    - Task #42: Daily recurring task due on day 20484 (next occurrence day = 20485)
            //    - Task #43: Unrelated recurring task with the SAME title ("Team Sync") already due on day 20485
            val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            try {
                sqliteDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL)"
                )
                sqliteDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `dueTimeMinutes` INTEGER, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `priority` TEXT NOT NULL, `categoryId` INTEGER, `reminderOffset` TEXT NOT NULL, `customReminderMillis` INTEGER, `recurrenceType` TEXT NOT NULL, `recurrenceWeekdays` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isSampleData` INTEGER NOT NULL)"
                )
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDateEpochDay` ON `tasks` (`dueDateEpochDay`)")
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
                sqliteDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subtasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)")
                sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                sqliteDb.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '07b1e485b23a66319d1fa14db0ab1775')")

                sqliteDb.execSQL(
                    "INSERT INTO `categories` (`id`, `name`, `colorHex`, `sortOrder`) VALUES (1, 'Work', '#CCFF00', 0)"
                )
                sqliteDb.execSQL(
                    "INSERT INTO `tasks` (`id`, `title`, `description`, `createdAt`, `updatedAt`, `dueDateEpochDay`, `dueTimeMinutes`, `isCompleted`, `completedAt`, `priority`, `categoryId`, `reminderOffset`, `customReminderMillis`, `recurrenceType`, `recurrenceWeekdays`, `sortOrder`, `isSampleData`) VALUES (42, 'Team Sync', 'Engineering stream', 1700000000000, 1700000000000, 20484, 540, 0, NULL, 'HIGH', 1, 'NONE', NULL, 'DAILY', '', 0, 0)"
                )
                sqliteDb.execSQL(
                    "INSERT INTO `tasks` (`id`, `title`, `description`, `createdAt`, `updatedAt`, `dueDateEpochDay`, `dueTimeMinutes`, `isCompleted`, `completedAt`, `priority`, `categoryId`, `reminderOffset`, `customReminderMillis`, `recurrenceType`, `recurrenceWeekdays`, `sortOrder`, `isSampleData`) VALUES (43, 'Team Sync', 'Design stream due tomorrow', 1700000001000, 1700000001000, 20485, 600, 0, NULL, 'MEDIUM', 1, 'NONE', NULL, 'DAILY', '', 1, 0)"
                )
                sqliteDb.version = 1
            } finally {
                sqliteDb.close()
            }

            // 2. Open with Room v2 and MIGRATION_1_2
            val migratedDb = Room.databaseBuilder(context, DayformDatabase::class.java, dbName)
                .addMigrations(*DayformDatabase.ALL_MIGRATIONS)
                .allowMainThreadQueries()
                .build()

            try {
                val migratedRepo = DayformRepository(
                    context = context,
                    dao = migratedDb.dayformDao(),
                    preferencesRepository = preferencesRepository,
                    database = migratedDb
                )

                // Both legacy tasks initially have recurrenceSeriesId == null after migration
                val legacy42Before = migratedDb.dayformDao().getTaskById(42L)!!
                val legacy43Before = migratedDb.dayformDao().getTaskById(43L)!!
                assertNull(legacy42Before.recurrenceSeriesId)
                assertNull(legacy43Before.recurrenceSeriesId)

                // 3. Complete legacy Task #42 (due 20484 -> next occurrence 20485).
                //    Must NOT hijack unrelated Task #43 (which is also titled "Team Sync" and due on 20485)!
                val spawnedFrom42Id = migratedRepo.setTaskCompleted(42L, true)
                assertNotNull(spawnedFrom42Id)
                assertTrue("Unrelated legacy task #43 must not be linked as spawned child of #42", spawnedFrom42Id != 43L)

                val spawnedFrom42 = migratedDb.dayformDao().getTaskById(spawnedFrom42Id!!)!!
                assertEquals(20485L, spawnedFrom42.dueDateEpochDay)
                assertEquals("Engineering stream", spawnedFrom42.description)
                assertEquals("series_42_1700000000000", spawnedFrom42.recurrenceSeriesId)

                // 4. Undo completion of Task #42: must delete only spawnedFrom42Id and leave Task #43 untouched
                migratedRepo.undoTaskCompletion(42L, spawnedFrom42Id)
                assertNull(migratedDb.dayformDao().getTaskById(spawnedFrom42Id))
                val preserved43 = migratedDb.dayformDao().getTaskById(43L)
                assertNotNull("Unrelated legacy task #43 must remain intact after Undo of #42", preserved43)
                assertEquals("Design stream due tomorrow", preserved43!!.description)

                // 5. Completing both #42 and #43 produces two distinct next occurrences in their own series
                val respawned42Id = migratedRepo.setTaskCompleted(42L, true)
                val spawned43Id = migratedRepo.setTaskCompleted(43L, true)
                assertNotNull(respawned42Id)
                assertNotNull(spawned43Id)
                assertTrue(respawned42Id != spawned43Id)
                assertEquals(
                    "series_42_1700000000000",
                    migratedDb.dayformDao().getTaskById(respawned42Id!!)!!.recurrenceSeriesId
                )
                assertEquals(
                    "series_43_1700000001000",
                    migratedDb.dayformDao().getTaskById(spawned43Id!!)!!.recurrenceSeriesId
                )
            } finally {
                migratedDb.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun motionArchitectureRespectsReducedMotionAndPreservesOneTimeDashboardEntranceAndRapidTapSafety(): Unit = runBlocking {
        // 1. Populate test workspace and verify reduced-motion detection via Settings.Global.ANIMATOR_DURATION_SCALE
        seedTestWorkspaceWithFixtures(displayName = "Rahma")
        val viewModel = createTestViewModel()

        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f
        )
        var detectedReducedMotion = false
        var entrancePlayedCounter = 0

        try {
            composeTestRule.setContent {
                detectedReducedMotion = com.example.ui.theme.DayformMotion.rememberReducedMotion()
                DayformTheme {
                    DayformApp(viewModel = viewModel)
                }
            }
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                !viewModel.uiState.value.isLoading &&
                    composeTestRule.onAllNodesWithTag("home_dashboard_list").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.waitForIdle()

            // Verify reduced-motion setting is respected by DayformMotion
            assertTrue("rememberReducedMotion must return true when ANIMATOR_DURATION_SCALE is 0f", detectedReducedMotion)
            assertTrue(
                com.example.ui.theme.DayformMotion.itemPlacementSpec(reducedMotion = true) is
                    androidx.compose.animation.core.SnapSpec
            )
            assertTrue(
                com.example.ui.theme.DayformMotion.itemPlacementSpec(reducedMotion = false) is
                    androidx.compose.animation.core.SpringSpec
            )

            // 2. Restore normal animator duration scale (1f) and verify navigation & non-replay behavior
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )

            // Rapidly switch across bottom navigation tabs and return to Home
            composeTestRule.onNodeWithTag("nav_tasks").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("nav_insights").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("nav_profile").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("nav_home").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("home_dashboard_list").assertIsDisplayed()

            // Scroll to first priority task, open TaskDetailsScreen, then navigate back to Home
            val firstTaskId = viewModel.uiState.value.priorityTasks.first().task.id
            composeTestRule.onNodeWithTag("home_dashboard_list")
                .performScrollToNode(hasTestTag("task_item_card_$firstTaskId"))
            composeTestRule.onNodeWithTag("task_item_card_$firstTaskId").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()

            composeTestRule.onNodeWithTag("details_back_button").performClick()
            composeTestRule.waitForIdle()

            // Verify scroll position is preserved and task card is immediately interactive on Home
            composeTestRule.onNodeWithTag("task_item_card_$firstTaskId").assertIsDisplayed()
            composeTestRule.onNodeWithTag("home_dashboard_list")
                .performScrollToNode(hasTestTag("stat_card_completed"))
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("stat_card_completed").assertIsDisplayed()
            composeTestRule.onNodeWithTag("stat_card_planned").assertIsDisplayed()
        } finally {
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }
    }

    @Test
    fun homeAvatarRemovedAndVerticalTaskScrollRemainsSmoothAcrossLargeLists() {
        val categories = runBlocking {
            repository.initializeFirstLaunch(displayName = "Rahma")
            val cats = repository.ensureStarterCategories()
            val today = DateTimeUtil.currentLocalEpochDay()
            val priorities = TaskPriority.entries
            val dao = database.dayformDao()

            // Partition 100 tasks so Category 0 ("Personal") = 10 tasks, Category 1 ("Work") = 30 tasks,
            // Category 2 ("Study") = 60 tasks, and Total = 100 tasks.
            val taskEntities = (1..100).map { index ->
                val category = when {
                    index <= 10 -> cats[0]
                    index <= 40 -> cats[1]
                    else -> cats[2]
                }
                val priority = priorities[index % priorities.size]
                val dueDay = when (index % 3) {
                    0 -> today - 1L
                    1 -> today
                    else -> today + (index % 5).toLong()
                }
                com.example.data.model.TaskEntity(
                    title = "Perf Task #$index",
                    description = "Benchmarking scroll stability for task $index",
                    dueDateEpochDay = dueDay,
                    dueTimeMinutes = 9 * 60 + (index % 8) * 30,
                    priority = priority.name,
                    categoryId = category.id,
                    reminderOffset = if (index % 2 == 0) ReminderOffset.TEN_MIN_BEFORE.name else ReminderOffset.NONE.name,
                    recurrenceType = RecurrenceType.NEVER.name,
                    sortOrder = index
                )
            }
            val insertedTaskIds = dao.insertTasks(taskEntities)
            val subtaskEntities = insertedTaskIds.flatMapIndexed { idx, taskId ->
                listOf(
                    com.example.data.model.SubtaskEntity(
                        taskId = taskId,
                        title = "Subtask A for ${idx + 1}",
                        isCompleted = idx % 2 == 0,
                        sortOrder = 0
                    ),
                    com.example.data.model.SubtaskEntity(
                        taskId = taskId,
                        title = "Subtask B for ${idx + 1}",
                        isCompleted = false,
                        sortOrder = 1
                    )
                )
            }
            dao.insertSubtasks(subtaskEntities)
            cats
        }

        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            DayformTheme {
                DayformApp(viewModel = viewModel)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.priorityTasks.size == 100 &&
                composeTestRule.onAllNodesWithTag("home_dashboard_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // 1. Verify Home profile avatar is completely removed, while notification bell & greeting remain intact
        composeTestRule.onNodeWithTag("home_avatar_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_notification_bell").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_greeting_title").assertTextEquals("Hello, Rahma")

        // 2. Verify 10-task, 30-task, and 100-task scroll checkpoints on HomeDashboardScreen
        val tenthHomeTaskId = viewModel.uiState.value.priorityTasks[9].task.id
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_$tenthHomeTaskId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$tenthHomeTaskId").assertIsDisplayed()

        val thirtiethHomeTaskId = viewModel.uiState.value.priorityTasks[29].task.id
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_$thirtiethHomeTaskId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$thirtiethHomeTaskId").assertIsDisplayed()

        val hundredthHomeTaskId = viewModel.uiState.value.priorityTasks[99].task.id
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_$hundredthHomeTaskId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$hundredthHomeTaskId").assertIsDisplayed()

        // Scroll up to the 50th task, open its TaskDetailsScreen, and navigate back to verify
        // mid-list scroll position is preserved without resetting to the top
        val fiftiethHomeTaskId = viewModel.uiState.value.priorityTasks[50].task.id
        composeTestRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("task_item_card_$fiftiethHomeTaskId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$fiftiethHomeTaskId").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("details_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$fiftiethHomeTaskId").assertIsDisplayed()

        // 3. Switch to All Tasks screen and verify 10-task, 30-task, and 100-task list & 2-column grid scrolling
        composeTestRule.onNodeWithTag("nav_tasks").performClick()
        composeTestRule.waitForIdle()

        // 10-task category filter ("Personal")
        viewModel.setCategoryFilter(categories[0].id)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.filteredTasks.size == 10
        }
        composeTestRule.waitForIdle()
        val lastTenCategoryTaskId = viewModel.uiState.value.filteredTasks.last().task.id
        composeTestRule.onNodeWithTag("filtered_tasks_list")
            .performScrollToNode(hasTestTag("task_item_card_$lastTenCategoryTaskId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$lastTenCategoryTaskId").assertIsDisplayed()

        // 30-task category filter ("Work")
        viewModel.setCategoryFilter(categories[1].id)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.filteredTasks.size == 30
        }
        composeTestRule.waitForIdle()
        val lastThirtyCategoryTaskId = viewModel.uiState.value.filteredTasks.last().task.id
        composeTestRule.onNodeWithTag("filtered_tasks_list")
            .performScrollToNode(hasTestTag("task_item_card_$lastThirtyCategoryTaskId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$lastThirtyCategoryTaskId").assertIsDisplayed()

        // All 100 tasks filter (null category) in List and 2-Column Grid modes
        viewModel.setCategoryFilter(null)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.uiState.value.filteredTasks.size == 100
        }
        composeTestRule.waitForIdle()
        val lastAllTasksId = viewModel.uiState.value.filteredTasks.last().task.id
        composeTestRule.onNodeWithTag("filtered_tasks_list")
            .performScrollToNode(hasTestTag("task_item_card_$lastAllTasksId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$lastAllTasksId").assertIsDisplayed()

        // Switch to 2-Column Grid mode and scroll across all 100 tasks
        composeTestRule.onNodeWithTag("toggle_board_layout_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("filtered_tasks_grid")
            .performScrollToNode(hasTestTag("task_item_card_$lastAllTasksId"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_item_card_$lastAllTasksId").assertIsDisplayed()

        // 4. Verify Profile screen remains accessible from bottom navigation bar
        composeTestRule.onNodeWithTag("nav_profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_settings_screen").assertIsDisplayed()
    }
}



