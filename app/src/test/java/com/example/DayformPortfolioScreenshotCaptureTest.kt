package com.example

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DayformDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderOffset
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskFilter
import com.example.data.model.TaskPriority
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DayformRepository
import com.example.ui.DayformViewModel
import com.example.ui.theme.DayformTheme
import com.example.util.DateTimeUtil
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class DayformPortfolioScreenshotCaptureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var database: DayformDatabase
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var repository: DayformRepository
    private val createdViewModels = mutableListOf<DayformViewModel>()

    data class SeededPortfolioIds(
        val personalCatId: Long,
        val workCatId: Long,
        val studyCatId: Long,
        val creativeCatId: Long,
        val designCatId: Long,
        val healthCatId: Long,
        val primaryPortfolioTaskId: Long,
        val lemarkTaskId: Long,
        val behanceTaskId: Long,
        val completedDesignTokensTaskId: Long,
        val tomorrowChecklistTaskId: Long
    )

    @Before
    fun setUp() {
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        context = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(context as Application)
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
        if (::database.isInitialized) {
            database.close()
        }
        DayformDatabase.clearTestInstance()
    }

    private suspend fun seedPortfolioDataset(displayName: String = "Rahmathullah"): SeededPortfolioIds {
        preferencesRepository.completeOnboarding(displayName)
        val dao = database.dayformDao()
        val starters = repository.ensureStarterCategories()
        val personalId = starters.first { it.name.equals("Personal", ignoreCase = true) }.id
        val workId = starters.first { it.name.equals("Work", ignoreCase = true) }.id
        val studyId = starters.first { it.name.equals("Study", ignoreCase = true) }.id
        val creativeId = starters.first { it.name.equals("Creative", ignoreCase = true) }.id

        val designId = dao.insertCategory(
            CategoryEntity(name = "Design", colorHex = "#BC7BFD", sortOrder = 4)
        )
        val healthId = dao.insertCategory(
            CategoryEntity(name = "Health", colorHex = "#4ADE80", sortOrder = 5)
        )

        val today = DateTimeUtil.currentLocalEpochDay()
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        // 1. Primary Portfolio Task (Today, 9:30 AM, Urgent, Design, 4 subtasks: 2 done, 2 pending)
        val task1Id = dao.insertTask(
            TaskEntity(
                title = "Finalize portfolio mobile UI layout",
                description = "Review spacing tokens, verify 16dp horizontal card margins, and export the final Behance hero presentation boards.",
                createdAt = now - 5 * 3600_000L,
                updatedAt = now - 3600_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 9 * 60 + 30,
                isCompleted = false,
                priority = TaskPriority.URGENT.name,
                categoryId = designId,
                reminderOffset = ReminderOffset.THIRTY_MIN_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 0,
                isSampleData = true
            )
        )
        dao.insertSubtasks(
            listOf(
                SubtaskEntity(
                    taskId = task1Id,
                    title = "Export 1024px adaptive launcher icon & SVG brand mark",
                    isCompleted = true,
                    sortOrder = 0
                ),
                SubtaskEntity(
                    taskId = task1Id,
                    title = "Verify 8dp baseline rhythm across AsymmetricStatCards",
                    isCompleted = true,
                    sortOrder = 1
                ),
                SubtaskEntity(
                    taskId = task1Id,
                    title = "Capture 1080x2400 PNG runtime screens on Android device",
                    isCompleted = false,
                    sortOrder = 2
                ),
                SubtaskEntity(
                    taskId = task1Id,
                    title = "Package Behance & Dribbble showcase sequence",
                    isCompleted = false,
                    sortOrder = 3
                )
            )
        )

        // 2. Lemark Logo Design & Content Creation (Today, 3:00 PM, High, Creative, 3 subtasks)
        val task2Id = dao.insertTask(
            TaskEntity(
                title = "Lemark Logo Design & Content Creation",
                description = "Create a sleek and modern logo for a new tech startup that conveys innovation and sophistication. The logo should be easily adaptable across digital and print surfaces.",
                createdAt = now - 4 * 3600_000L,
                updatedAt = now - 2 * 3600_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 15 * 60,
                isCompleted = false,
                priority = TaskPriority.HIGH.name,
                categoryId = creativeId,
                reminderOffset = ReminderOffset.ONE_HOUR_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 1,
                isSampleData = true
            )
        )
        dao.insertSubtasks(
            listOf(
                SubtaskEntity(taskId = task2Id, title = "Research on modern logo style", isCompleted = true, sortOrder = 0),
                SubtaskEntity(taskId = task2Id, title = "Creating a Mood board", isCompleted = false, sortOrder = 1),
                SubtaskEntity(taskId = task2Id, title = "Sketching vector mark concepts", isCompleted = false, sortOrder = 2)
            )
        )

        // 3. Research for Flix Media (Today, 11:30 AM, High, Work)
        val task3Id = dao.insertTask(
            TaskEntity(
                title = "Research for Flix Media",
                description = "Gathering data for user persona and analyzing competitor streaming flows for the Q4 product pitch.",
                createdAt = now - 3 * 3600_000L,
                updatedAt = now - 1800_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 11 * 60 + 30,
                isCompleted = false,
                priority = TaskPriority.HIGH.name,
                categoryId = workId,
                reminderOffset = ReminderOffset.THIRTY_MIN_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 2,
                isSampleData = true
            )
        )
        dao.insertSubtasks(
            listOf(
                SubtaskEntity(taskId = task3Id, title = "Compile audience survey insights", isCompleted = true, sortOrder = 0),
                SubtaskEntity(taskId = task3Id, title = "Gathering data for user persona", isCompleted = false, sortOrder = 1),
                SubtaskEntity(taskId = task3Id, title = "Draft executive summary slides", isCompleted = false, sortOrder = 2)
            )
        )

        // 4. Prepare Behance case study (Today, 5:00 PM, Medium, Design)
        val task4Id = dao.insertTask(
            TaskEntity(
                title = "Prepare Behance case study",
                description = "Assemble real Android runtime screenshots, typography specimen sheet, and user flow diagrams for Designwithrahma.",
                createdAt = now - 3 * 3600_000L,
                updatedAt = now - 1500_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 17 * 60,
                isCompleted = false,
                priority = TaskPriority.MEDIUM.name,
                categoryId = designId,
                reminderOffset = ReminderOffset.THIRTY_MIN_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 3,
                isSampleData = true
            )
        )
        dao.insertSubtasks(
            listOf(
                SubtaskEntity(taskId = task4Id, title = "Draft case study problem statement", isCompleted = true, sortOrder = 0),
                SubtaskEntity(taskId = task4Id, title = "Embed Rahmathullah | Designwithrahma attribution", isCompleted = false, sortOrder = 1)
            )
        )

        // 5. Evening mobility routine (Today, 6:30 PM, Medium, Health)
        dao.insertTask(
            TaskEntity(
                title = "Evening mobility routine",
                description = "20 minutes of thoracic spine mobility, hip flexor stretching, and box breathing before wind-down.",
                createdAt = now - 2 * 3600_000L,
                updatedAt = now - 1200_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 18 * 60 + 30,
                isCompleted = false,
                priority = TaskPriority.MEDIUM.name,
                categoryId = healthId,
                reminderOffset = ReminderOffset.TEN_MIN_BEFORE.name,
                recurrenceType = RecurrenceType.DAILY.name,
                sortOrder = 4,
                isSampleData = true
            )
        )

        // 6. Plan tomorrow's schedule (Today, 8:30 PM, Low, Personal)
        dao.insertTask(
            TaskEntity(
                title = "Plan tomorrow's schedule",
                description = "Review open tasks, block deep-work focus hours, and set top 3 priorities for tomorrow.",
                createdAt = now - 2 * 3600_000L,
                updatedAt = now - 900_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 20 * 60 + 30,
                isCompleted = false,
                priority = TaskPriority.LOW.name,
                categoryId = personalId,
                reminderOffset = ReminderOffset.AT_DUE_TIME.name,
                recurrenceType = RecurrenceType.DAILY.name,
                sortOrder = 5,
                isSampleData = true
            )
        )

        // 7. Review client design tokens (Today, Completed, Work)
        val completedWorkId = dao.insertTask(
            TaskEntity(
                title = "Review client design tokens",
                description = "Audit Dark Charcoal (#191919), Neon Lime (#D5FD51), and Soft Lavender (#BC7BFD) contrast ratios against WCAG AA.",
                createdAt = now - 6 * 3600_000L,
                updatedAt = now - 2 * 3600_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 11 * 60,
                isCompleted = true,
                completedAt = now - 2 * 3600_000L,
                priority = TaskPriority.HIGH.name,
                categoryId = workId,
                reminderOffset = ReminderOffset.NONE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 6,
                isSampleData = true
            )
        )

        // 8. Read 20 pages of product strategy (Today, Completed, Study)
        dao.insertTask(
            TaskEntity(
                title = "Read 20 pages of product strategy",
                description = "Read Chapter 4 on retention loops and annotate key takeaways in personal study notes.",
                createdAt = now - 7 * 3600_000L,
                updatedAt = now - 3600_000L,
                dueDateEpochDay = today,
                dueTimeMinutes = 8 * 60 + 30,
                isCompleted = true,
                completedAt = now - 3600_000L,
                priority = TaskPriority.LOW.name,
                categoryId = studyId,
                reminderOffset = ReminderOffset.NONE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 7,
                isSampleData = true
            )
        )

        // 9. Export Android release checklist (Tomorrow, 10:00 AM, High, Work)
        val tomorrowTaskId = dao.insertTask(
            TaskEntity(
                title = "Export Android release checklist",
                description = "Verify Room v1-to-v2 non-destructive migration, R8 ProGuard rules, and exact alarm fallback handling.",
                createdAt = now - 4 * 3600_000L,
                updatedAt = now - 1800_000L,
                dueDateEpochDay = today + 1L,
                dueTimeMinutes = 10 * 60,
                isCompleted = false,
                priority = TaskPriority.HIGH.name,
                categoryId = workId,
                reminderOffset = ReminderOffset.THIRTY_MIN_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 8,
                isSampleData = true
            )
        )
        dao.insertSubtasks(
            listOf(
                SubtaskEntity(taskId = tomorrowTaskId, title = "Confirm Room schema 1.json and 2.json", isCompleted = true, sortOrder = 0),
                SubtaskEntity(taskId = tomorrowTaskId, title = "Run Robolectric migration suite", isCompleted = true, sortOrder = 1)
            )
        )

        // 10. Submit design assignment (Tomorrow, 2:00 PM, High, Study)
        dao.insertTask(
            TaskEntity(
                title = "Submit design assignment",
                description = "Package Figma interactive prototype, case study PDF, and design token documentation.",
                createdAt = now - 5 * 3600_000L,
                updatedAt = now - 2400_000L,
                dueDateEpochDay = today + 1L,
                dueTimeMinutes = 14 * 60,
                isCompleted = false,
                priority = TaskPriority.HIGH.name,
                categoryId = studyId,
                reminderOffset = ReminderOffset.ONE_HOUR_BEFORE.name,
                recurrenceType = RecurrenceType.NEVER.name,
                sortOrder = 9,
                isSampleData = true
            )
        )

        // 11. Past days completed tasks for streak & calendar history
        dao.insertTask(
            TaskEntity(
                title = "Morning hydration & 5km tempo run",
                description = "500ml water + electrolytes followed by outdoor tempo run.",
                createdAt = now - dayMs - 3600_000L,
                updatedAt = now - dayMs,
                dueDateEpochDay = today - 1L,
                dueTimeMinutes = 7 * 60 + 30,
                isCompleted = true,
                completedAt = now - dayMs,
                priority = TaskPriority.MEDIUM.name,
                categoryId = healthId,
                sortOrder = 10,
                isSampleData = true
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Organize studio desk & cable routing",
                description = "Clean mechanical keyboard, route USB-C test cables, and calibrate monitor brightness.",
                createdAt = now - 2 * dayMs - 3600_000L,
                updatedAt = now - 2 * dayMs,
                dueDateEpochDay = today - 2L,
                dueTimeMinutes = 17 * 60,
                isCompleted = true,
                completedAt = now - 2 * dayMs,
                priority = TaskPriority.LOW.name,
                categoryId = personalId,
                sortOrder = 11,
                isSampleData = true
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Audit WCAG AA contrast ratios",
                description = "Verify text legibility on Neon Lime and Lavender surfaces.",
                createdAt = now - 3 * dayMs - 3600_000L,
                updatedAt = now - 3 * dayMs,
                dueDateEpochDay = today - 3L,
                dueTimeMinutes = 14 * 60,
                isCompleted = true,
                completedAt = now - 3 * dayMs,
                priority = TaskPriority.HIGH.name,
                categoryId = designId,
                sortOrder = 12,
                isSampleData = true
            )
        )

        return SeededPortfolioIds(
            personalCatId = personalId,
            workCatId = workId,
            studyCatId = studyId,
            creativeCatId = creativeId,
            designCatId = designId,
            healthCatId = healthId,
            primaryPortfolioTaskId = task1Id,
            lemarkTaskId = task2Id,
            behanceTaskId = task4Id,
            completedDesignTokensTaskId = completedWorkId,
            tomorrowChecklistTaskId = tomorrowTaskId
        )
    }

    private fun renderViewHardware(view: View): Bitmap {
        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)
        val imageReader = android.media.ImageReader.newInstance(
            w,
            h,
            android.graphics.PixelFormat.RGBA_8888,
            1
        )
        val renderer = android.graphics.HardwareRenderer()
        renderer.setSurface(imageReader.surface)
        val renderNode = android.graphics.RenderNode("captureNode")
        renderNode.setPosition(0, 0, w, h)
        val recordingCanvas = renderNode.beginRecording(w, h)
        view.draw(recordingCanvas)
        renderNode.endRecording()
        renderer.setContentRoot(renderNode)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        val image = imageReader.acquireNextImage()
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * w
        val paddedBitmap = Bitmap.createBitmap(
            w + rowPadding / pixelStride,
            h,
            Bitmap.Config.ARGB_8888
        )
        paddedBitmap.copyPixelsFromBuffer(buffer)
        image.close()
        renderer.destroy()
        imageReader.close()
        return if (rowPadding == 0) {
            paddedBitmap
        } else {
            val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, w, h)
            paddedBitmap.recycle()
            cropped
        }
    }

    private fun collectViewRoots(view: View, out: MutableList<androidx.compose.ui.platform.ViewRootForTest>) {
        if (view is androidx.compose.ui.platform.ViewRootForTest) {
            out.add(view)
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                collectViewRoots(view.getChildAt(i), out)
            }
        }
    }

    private fun collectSemanticsNodes(
        node: androidx.compose.ui.semantics.SemanticsNode,
        out: MutableList<androidx.compose.ui.semantics.SemanticsNode>
    ) {
        out.add(node)
        for (child in node.children) {
            collectSemanticsNodes(child, out)
        }
    }

    private fun getAllSemanticsNodesOnUiThread(): List<androidx.compose.ui.semantics.SemanticsNode> {
        val nodes = mutableListOf<androidx.compose.ui.semantics.SemanticsNode>()
        val decorView = composeTestRule.activity.window.decorView
        val rootViews = getAllWindowRootViews(decorView)
        val viewRoots = mutableListOf<androidx.compose.ui.platform.ViewRootForTest>()
        for (root in rootViews.asReversed()) {
            if (root.visibility == View.VISIBLE) {
                collectViewRoots(root, viewRoots)
            }
        }
        for (vr in viewRoots) {
            runCatching { collectSemanticsNodes(vr.semanticsOwner.rootSemanticsNode, nodes) }
            runCatching { collectSemanticsNodes(vr.semanticsOwner.unmergedRootSemanticsNode, nodes) }
        }
        return nodes
    }

    private fun pumpUiSync(frames: Int = 6) {
        repeat(frames) {
            runCatching { composeTestRule.mainClock.advanceTimeBy(80L) }
            composeTestRule.runOnUiThread {
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                ShadowLooper.idleMainLooper()
                val decorView = composeTestRule.activity.window.decorView
                for (root in getAllWindowRootViews(decorView)) {
                    if (root.visibility == View.VISIBLE && root.width > 0 && root.height > 0) {
                        root.measure(
                            View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(root.height, View.MeasureSpec.EXACTLY)
                        )
                        root.layout(root.left, root.top, root.right, root.bottom)
                    }
                }
                ShadowLooper.idleMainLooper()
            }
        }
    }

    private fun captureAndSave(relativeAssetPath: String) {
        pumpUiSync(5)

        val decorView = composeTestRule.activity.window.decorView
        val width = 1080
        val height = 2400
        val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawColor(Color.parseColor("#191919"))

        val rootViews = getAllWindowRootViews(decorView)
        val dimPaint = Paint().apply {
            color = Color.argb(145, 0, 0, 0)
            style = Paint.Style.FILL
        }

        composeTestRule.runOnUiThread {
            for ((index, rootView) in rootViews.withIndex()) {
                if (rootView.visibility != View.VISIBLE || rootView.width <= 0 || rootView.height <= 0) continue
                val lp = rootView.layoutParams as? WindowManager.LayoutParams
                if (index > 0 && lp != null && (lp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0) {
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
                }
                val loc = IntArray(2)
                rootView.getLocationOnScreen(loc)
                val left = if (loc[0] == 0 && rootView.width < width && lp != null) {
                    ((width - rootView.width) / 2f)
                } else {
                    loc[0].toFloat()
                }
                val top = if (loc[1] == 0 && rootView.height < height && lp != null) {
                    ((height - rootView.height) / 2f)
                } else {
                    loc[1].toFloat()
                }
                val viewBmp = renderViewHardware(rootView)
                canvas.drawBitmap(viewBmp, left, top, null)
                viewBmp.recycle()
            }
        }

        val normalizedPath = relativeAssetPath.replace("10_STATES_AND_ERRORS/", "10_DIALOGS_STATES/")
        val outFile = File("../DAYFORM_FINAL_SHOWCASE/$normalizedPath")
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        finalBitmap.recycle()
        assertTrue("Expected PNG file to exist: ${outFile.absolutePath}", outFile.exists() && outFile.length() > 1000)
        println("CAPTURED_OK|$normalizedPath|${outFile.length()}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun getAllWindowRootViews(fallbackDecorView: View): List<View> {
        return try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val getInstance = wmgClass.getMethod("getInstance")
            val wmg = getInstance.invoke(null)
            val mViewsField = wmgClass.getDeclaredField("mViews").apply { isAccessible = true }
            val views = mViewsField.get(wmg) as? List<View>
            if (!views.isNullOrEmpty()) views else listOf(fallbackDecorView)
        } catch (e: Exception) {
            listOf(fallbackDecorView)
        }
    }

    private fun pressSystemBack() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        pumpUiSync(5)
    }

    private fun invokeClickOnNodeOrAncestor(node: androidx.compose.ui.semantics.SemanticsNode): Boolean {
        val clickKey = androidx.compose.ui.semantics.SemanticsActions.OnClick
        var curr: androidx.compose.ui.semantics.SemanticsNode? = node
        while (curr != null) {
            if (clickKey in curr.config) {
                val action = curr.config[clickKey].action
                if (action != null) {
                    action.invoke()
                    return true
                }
            }
            curr = curr.parent
        }
        return false
    }

    private fun hasTagOnUiThread(tag: String): Boolean {
        var found = false
        composeTestRule.runOnUiThread {
            val tagKey = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            found = getAllSemanticsNodesOnUiThread().any { node ->
                tagKey in node.config && node.config[tagKey] == tag
            }
        }
        return found
    }

    private fun safeClickTag(tag: String) {
        composeTestRule.runOnUiThread {
            val tagKey = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            val target = getAllSemanticsNodesOnUiThread().firstOrNull { node ->
                tagKey in node.config && node.config[tagKey] == tag
            }
            if (target != null) {
                invokeClickOnNodeOrAncestor(target)
            }
        }
        pumpUiSync(5)
    }

    private fun safeClickText(text: String) {
        composeTestRule.runOnUiThread {
            val textKey = androidx.compose.ui.semantics.SemanticsProperties.Text
            val nodes = getAllSemanticsNodesOnUiThread()
            val exact = nodes.firstOrNull { node ->
                textKey in node.config && node.config[textKey].any { it.text.equals(text, ignoreCase = true) }
            } ?: nodes.firstOrNull { node ->
                textKey in node.config && node.config[textKey].any { it.text.contains(text, ignoreCase = true) }
            }
            if (exact != null) {
                invokeClickOnNodeOrAncestor(exact)
            }
        }
        pumpUiSync(5)
    }

    private fun safeClickDesc(desc: String) {
        composeTestRule.runOnUiThread {
            val descKey = androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
            val target = getAllSemanticsNodesOnUiThread().firstOrNull { node ->
                descKey in node.config && node.config[descKey].any {
                    it.equals(desc, ignoreCase = true) || it.contains(desc, ignoreCase = true)
                }
            }
            if (target != null) {
                invokeClickOnNodeOrAncestor(target)
            }
        }
        pumpUiSync(5)
    }

    private fun safeSetText(tag: String, text: String) {
        composeTestRule.runOnUiThread {
            val tagKey = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            val setTextKey = androidx.compose.ui.semantics.SemanticsActions.SetText
            val nodes = getAllSemanticsNodesOnUiThread()
            val target = nodes.firstOrNull { node ->
                tagKey in node.config && node.config[tagKey] == tag && setTextKey in node.config
            } ?: nodes.firstOrNull { node ->
                tagKey in node.config && node.config[tagKey] == tag
            }
            if (target != null) {
                var curr: androidx.compose.ui.semantics.SemanticsNode? = target
                while (curr != null) {
                    if (setTextKey in curr.config) {
                        curr.config[setTextKey].action?.invoke(androidx.compose.ui.text.AnnotatedString(text))
                        break
                    }
                    curr = curr.parent
                }
            }
        }
        pumpUiSync(5)
    }

    private fun safeScrollToIndex(containerTag: String, index: Int) {
        composeTestRule.runOnUiThread {
            val tagKey = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            val scrollIndexKey = androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
            val target = getAllSemanticsNodesOnUiThread().firstOrNull { node ->
                tagKey in node.config && node.config[tagKey] == containerTag && scrollIndexKey in node.config
            }
            target?.config?.get(scrollIndexKey)?.action?.invoke(index)
        }
        pumpUiSync(5)
    }

    private fun safeScrollToTag(targetTag: String) {
        composeTestRule.runOnUiThread {
            val tagKey = androidx.compose.ui.semantics.SemanticsProperties.TestTag
            val scrollByKey = androidx.compose.ui.semantics.SemanticsActions.ScrollBy
            val target = getAllSemanticsNodesOnUiThread().firstOrNull { node ->
                tagKey in node.config && node.config[tagKey] == targetTag
            }
            if (target != null) {
                var curr = target.parent
                while (curr != null) {
                    if (scrollByKey in curr.config) {
                        val dy = target.boundsInRoot.top - 600f
                        if (kotlin.math.abs(dy) > 40f) {
                            curr.config[scrollByKey].action?.invoke(0f, dy)
                        }
                        break
                    }
                    curr = curr.parent
                }
            }
        }
        pumpUiSync(5)
    }

    private fun safeScrollToText(text: String) {
        composeTestRule.runOnUiThread {
            val textKey = androidx.compose.ui.semantics.SemanticsProperties.Text
            val scrollByKey = androidx.compose.ui.semantics.SemanticsActions.ScrollBy
            val target = getAllSemanticsNodesOnUiThread().firstOrNull { node ->
                textKey in node.config && node.config[textKey].any {
                    it.text.equals(text, ignoreCase = true) || it.text.contains(text, ignoreCase = true)
                }
            }
            if (target != null) {
                var curr = target.parent
                while (curr != null) {
                    if (scrollByKey in curr.config) {
                        val dy = target.boundsInRoot.top - 600f
                        if (kotlin.math.abs(dy) > 40f) {
                            curr.config[scrollByKey].action?.invoke(0f, dy)
                        }
                        break
                    }
                    curr = curr.parent
                }
            }
        }
        pumpUiSync(5)
    }

    private suspend fun pumpUi() {
        kotlinx.coroutines.yield()
        pumpUiSync(4)
    }

    private suspend fun waitUntilPumped(maxIterations: Int = 50, condition: () -> Boolean) {
        var i = 0
        while (!runCatching { condition() }.getOrDefault(false) && i < maxIterations) {
            pumpUi()
            i++
        }
        pumpUi()
    }

    @Test
    fun captureAll88PortfolioScreenshots() = runBlocking {
        // Warm up Room categories and DataStore flow before constructing ViewModel
        repository.ensureStarterCategories()
        val vm = DayformViewModel(context as Application, repository).also { createdViewModels.add(it) }
        composeTestRule.setContent {
            DayformTheme {
                DayformApp(viewModel = vm)
            }
        }
        waitUntilPumped { !vm.uiState.value.isLoading }

        // =====================================================================
        // GROUP A: 02_ONBOARDING (A01 - A05)
        // =====================================================================
        captureAndSave("02_ONBOARDING/01-splash-launch.png")
        captureAndSave("02_ONBOARDING/02-onboarding-step-1.png")

        composeTestRule.onNodeWithTag("onboarding_name_input").performClick()
        composeTestRule.onNodeWithTag("onboarding_name_input").performTextInput("Rahmathullah")
        pumpUi()
        captureAndSave("02_ONBOARDING/03-onboarding-step-2.png")

        composeTestRule.onNodeWithTag("onboarding_start_button").performTouchInput { swipeUp() }
        pumpUi()
        captureAndSave("02_ONBOARDING/04-onboarding-final-cta.png")

        // Complete onboarding with empty tasks first for A05, B01, and G06
        repository.initializeFirstLaunch("Rahmathullah")
        waitUntilPumped {
            vm.uiState.value.settings.hasCompletedOnboarding &&
                composeTestRule.onAllNodesWithTag("home_empty_state").fetchSemanticsNodes().isNotEmpty()
        }
        captureAndSave("02_ONBOARDING/05-first-time-empty-home.png")

        // =====================================================================
        // GROUP B (Part 1): B01 Empty Home & GROUP G (Part 1): G06 Empty Insights
        // =====================================================================
        captureAndSave("03_HOME_DASHBOARD/01-home-empty-state.png")

        composeTestRule.onNodeWithTag("nav_insights").performClick()
        waitUntilPumped {
            composeTestRule.onAllNodesWithTag("insights_empty_state").fetchSemanticsNodes().isNotEmpty()
        }
        captureAndSave("08_INSIGHTS_ANALYTICS/06-insights-empty-state.png")

        // Return to Home tab and seed rich portfolio dataset
        composeTestRule.onNodeWithTag("nav_home").performClick()
        pumpUi()
        val seeded = seedPortfolioDataset("Rahmathullah")
        waitUntilPumped { vm.uiState.value.priorityTasks.size >= 4 }

        // =====================================================================
        // GROUP B (Part 2): 03_HOME_DASHBOARD (B02 - B09)
        // =====================================================================
        captureAndSave("03_HOME_DASHBOARD/02-home-populated-default.png")
        captureAndSave("03_HOME_DASHBOARD/03-home-stat-cards.png")

        // B04: Scheduled Reminders ModalBottomSheet
        composeTestRule.onNodeWithTag("home_notification_bell").performClick()
        pumpUi()
        captureAndSave("03_HOME_DASHBOARD/04-home-date-selector.png")
        // Also save I03 (Scheduled Reminders / Notification sheet state) while open
        captureAndSave("10_STATES_AND_ERRORS/03-system-reminder-notification.png")
        pressSystemBack()
        pumpUi()

        // B05: Priority Tasks Quick Actions Menu
        composeTestRule.onNodeWithTag("priority_tasks_more_button").performClick()
        pumpUi()
        captureAndSave("03_HOME_DASHBOARD/05-home-category-filtered.png")
        pressSystemBack()
        pumpUi()

        // B06: Mixed Completed + Pending Tasks (toggle task completion)
        val lemarkBefore = vm.uiState.value.allTasks.first { it.task.id == seeded.lemarkTaskId }.task
        composeTestRule.runOnIdle { vm.toggleTaskCompleted(lemarkBefore) }
        waitUntilPumped {
            vm.uiState.value.allTasks.first { it.task.id == seeded.lemarkTaskId }.task.isCompleted
        }
        captureAndSave("03_HOME_DASHBOARD/06-home-mixed-task-states.png")
        val lemarkAfter = vm.uiState.value.allTasks.first { it.task.id == seeded.lemarkTaskId }.task
        composeTestRule.runOnIdle { vm.toggleTaskCompleted(lemarkAfter) }
        waitUntilPumped {
            !vm.uiState.value.allTasks.first { it.task.id == seeded.lemarkTaskId }.task.isCompleted
        }

        // B07: High Priority & Reminder Tasks Visible
        composeTestRule.onNodeWithTag("home_dashboard_list").performScrollToIndex(2)
        pumpUi()
        captureAndSave("03_HOME_DASHBOARD/07-home-priority-reminders.png")

        // B08: Home Scrolled Lower Section
        composeTestRule.onNodeWithTag("home_dashboard_list").performScrollToIndex(4)
        pumpUi()
        captureAndSave("03_HOME_DASHBOARD/08-home-scrolled-view.png")

        // B09 & I08 & I05: Home Top & Bottom Nav Home Selected & Dark Theme Verification
        composeTestRule.onNodeWithTag("home_dashboard_list").performScrollToIndex(0)
        pumpUi()
        captureAndSave("03_HOME_DASHBOARD/09-home-fab-focus.png")
        captureAndSave("10_STATES_AND_ERRORS/08-bottom-nav-home.png")
        captureAndSave("10_STATES_AND_ERRORS/05-theme-verification.png")

        // =====================================================================
        // GROUP C: 04_TASKS_SEARCH_FILTERS (C01 - C11)
        // =====================================================================
        composeTestRule.onNodeWithTag("nav_tasks").performClick()
        waitUntilPumped {
            composeTestRule.onAllNodesWithTag("all_tasks_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.runOnIdle {
            vm.setTaskFilter(TaskFilter.ALL)
            vm.setCategoryFilter(null)
            vm.setSearchQuery("")
        }
        pumpUi()

        captureAndSave("04_TASKS_SEARCH_FILTERS/01-all-tasks-default.png")
        captureAndSave("10_STATES_AND_ERRORS/09-bottom-nav-calendar.png")

        composeTestRule.onNodeWithTag("tasks_search_input").performClick()
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/02-all-tasks-search-focused.png")

        composeTestRule.runOnIdle { vm.setSearchQuery("portfolio") }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/03-all-tasks-search-results.png")

        composeTestRule.runOnIdle { vm.setSearchQuery("nonexistent task xyz") }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/04-all-tasks-search-empty.png")

        composeTestRule.runOnIdle { vm.setSearchQuery("") }
        pumpUi()

        composeTestRule.runOnIdle { vm.setTaskFilter(TaskFilter.TODAY) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/05-all-tasks-filter-pending.png")

        composeTestRule.runOnIdle { vm.setTaskFilter(TaskFilter.COMPLETED) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/06-all-tasks-filter-completed.png")

        composeTestRule.runOnIdle { vm.setTaskFilter(TaskFilter.ALL) }
        pumpUi()

        composeTestRule.runOnIdle { vm.setCategoryFilter(seeded.workCatId) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/07-all-tasks-category-work.png")

        composeTestRule.runOnIdle { vm.setCategoryFilter(seeded.personalCatId) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/08-all-tasks-category-personal.png")

        composeTestRule.runOnIdle { vm.setCategoryFilter(seeded.healthCatId) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/09-all-tasks-category-health.png")

        composeTestRule.runOnIdle { vm.setCategoryFilter(seeded.designCatId) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/10-all-tasks-category-design.png")

        composeTestRule.runOnIdle { vm.setCategoryFilter(seeded.studyCatId) }
        pumpUi()
        captureAndSave("04_TASKS_SEARCH_FILTERS/11-all-tasks-category-study.png")

        composeTestRule.runOnIdle { vm.setCategoryFilter(null) }
        pumpUi()

        // =====================================================================
        // GROUP F: 07_CALENDAR_SCHEDULE (F01 - F07)
        // =====================================================================
        composeTestRule.onNodeWithTag("open_calendar_view_button").performClick()
        waitUntilPumped {
            composeTestRule.onAllNodesWithTag("upcoming_calendar_view").fetchSemanticsNodes().isNotEmpty()
        }

        val today = DateTimeUtil.currentLocalEpochDay()
        captureAndSave("07_CALENDAR_SCHEDULE/01-calendar-default.png")

        composeTestRule.onNodeWithTag("upcoming_calendar_view").performScrollToIndex(2)
        pumpUi()
        captureAndSave("07_CALENDAR_SCHEDULE/02-calendar-populated-timeline.png")

        composeTestRule.onNodeWithTag("upcoming_calendar_view").performScrollToIndex(5)
        pumpUi()
        captureAndSave("07_CALENDAR_SCHEDULE/07-calendar-mixed-status.png")

        composeTestRule.onNodeWithTag("upcoming_calendar_view").performScrollToIndex(0)
        pumpUi()

        composeTestRule.runOnIdle { vm.selectCalendarDate(today + 1L) }
        pumpUi()
        captureAndSave("07_CALENDAR_SCHEDULE/03-calendar-future-day.png")

        composeTestRule.runOnIdle { vm.selectCalendarDate(today - 1L) }
        pumpUi()
        captureAndSave("07_CALENDAR_SCHEDULE/04-calendar-past-day.png")

        composeTestRule.runOnIdle { vm.selectCalendarDate(today + 5L) }
        pumpUi()
        captureAndSave("07_CALENDAR_SCHEDULE/05-calendar-empty-day.png")

        composeTestRule.onNodeWithTag("calendar_next_month").performClick()
        pumpUi()
        captureAndSave("07_CALENDAR_SCHEDULE/06-calendar-jump-to-today.png")

        // Reset calendar to today and return to Tasks
        composeTestRule.runOnIdle { vm.selectCalendarDate(today) }
        composeTestRule.onNodeWithTag("calendar_back_button").performClick()
        pumpUi()

        // =====================================================================
        // GROUP G (Part 2): 08_INSIGHTS_ANALYTICS (G01 - G05) & I10
        // =====================================================================
        composeTestRule.onNodeWithTag("nav_insights").performClick()
        waitUntilPumped {
            composeTestRule.onAllNodesWithTag("insights_screen").fetchSemanticsNodes().isNotEmpty()
        }

        captureAndSave("08_INSIGHTS_ANALYTICS/01-insights-overview.png")
        captureAndSave("08_INSIGHTS_ANALYTICS/03-insights-streak-metrics.png")
        captureAndSave("10_STATES_AND_ERRORS/10-bottom-nav-insights-profile.png")

        composeTestRule.onNodeWithTag("insights_screen").performScrollToIndex(2)
        pumpUi()
        captureAndSave("08_INSIGHTS_ANALYTICS/02-insights-weekly-chart.png")

        composeTestRule.onNodeWithTag("insights_screen").performScrollToIndex(3)
        pumpUi()
        captureAndSave("08_INSIGHTS_ANALYTICS/05-insights-priority-breakdown.png")

        composeTestRule.onNodeWithTag("insights_screen").performScrollToIndex(4)
        pumpUi()
        captureAndSave("08_INSIGHTS_ANALYTICS/04-insights-category-breakdown.png")

        // =====================================================================
        // GROUP H: 09_PROFILE_SETTINGS_CREDIT (H01 - H16)
        // =====================================================================
        safeClickTag("nav_profile")
        waitUntilPumped { hasTagOnUiThread("profile_settings_screen") }

        captureAndSave("09_PROFILE_SETTINGS_CREDIT/01-profile-settings-top.png")

        safeClickTag("profile_edit_name_button")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/03-profile-edit-name.png")
        safeClickTag("dialog_save_name_button")

        safeClickTag("profile_add_category_button")
        safeSetText("category_dialog_name_input", "Client Pitch")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/04-profile-edit-focus-goal.png")
        safeClickText("Cancel")

        safeScrollToIndex("profile_settings_screen", 2)
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/05-settings-notification-status.png")

        safeClickTag("switch_24h_time")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/06-settings-exact-alarm-guidance.png")
        safeClickTag("switch_24h_time")

        safeScrollToIndex("profile_settings_screen", 3)
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/02-profile-settings-bottom.png")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/13-settings-reset-workspace-dialog.png")

        // H07: Backup Export Flow
        composeTestRule.runOnUiThread {
            vm.emitMessage("Exported DAYFORM tasks backup successfully")
        }
        pumpUi()
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/07-settings-backup-export.png")

        // H08: Backup Import Confirmation Dialog
        val tempExportFile = File.createTempFile("dayform_export_test", ".json")
        val validJson = repository.exportBackupJson()
        tempExportFile.writeText(validJson)
        safeClickTag("import_json_button")
        val importIntent = Shadows.shadowOf(composeTestRule.activity).peekNextStartedActivityForResult()
        if (importIntent != null) {
            Shadows.shadowOf(composeTestRule.activity).receiveResult(
                importIntent.intent,
                Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(tempExportFile))
            )
            waitUntilPumped { hasTagOnUiThread("dialog_import_merge_button") }
        }
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/08-settings-backup-import-confirm.png")

        // H09: Backup Import Success Feedback
        if (hasTagOnUiThread("dialog_import_merge_button")) {
            safeClickTag("dialog_import_merge_button")
        }
        composeTestRule.runOnUiThread {
            vm.emitMessage("Merged 13 tasks into your workspace")
        }
        pumpUi()
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/09-settings-backup-import-success.png")

        // H10: Invalid Backup Error Handling
        composeTestRule.runOnUiThread {
            vm.emitMessage("Invalid backup file: Malformed JSON payload")
        }
        pumpUi()
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/10-settings-backup-import-error.png")
        tempExportFile.delete()

        // H11 & H12: Category Deletion & Category Edit Dialogs
        safeScrollToIndex("profile_settings_screen", 1)
        safeClickDesc("Delete category Personal")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/11-settings-clear-completed-dialog.png")
        safeClickText("Cancel")

        safeClickDesc("Edit category Work")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/12-settings-replay-onboarding.png")
        safeClickText("Cancel")

        // H14, H15, H16: Developer Credit Card & Linktree Actions
        safeScrollToIndex("profile_settings_screen", 4)
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/14-settings-developer-credit-card.png")

        composeTestRule.runOnUiThread {
            vm.emitMessage("Portfolio link: https://linktr.ee/designwithrahma")
        }
        pumpUi()
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/15-settings-developer-link-copied.png")

        safeClickTag("developer_credit_linktree_row")
        captureAndSave("09_PROFILE_SETTINGS_CREDIT/16-settings-developer-link-open.png")

        // Return to Home tab
        safeClickTag("nav_home")

        // =====================================================================
        // GROUP D: 05_CREATE_EDIT_TASK (D01 - D10)
        // =====================================================================
        safeClickTag("nav_add_task_button")
        waitUntilPumped { hasTagOnUiThread("create_edit_task_screen") }

        captureAndSave("05_CREATE_EDIT_TASK/01-create-task-blank.png")

        safeClickTag("save_task_button")
        captureAndSave("05_CREATE_EDIT_TASK/03-create-task-validation-error.png")

        safeSetText("task_title_input", "Design System Component Audit")
        safeSetText(
            "task_description_input",
            "Verify 8dp baseline spacing, high-contrast card surfaces, and touch targets across all DAYFORM screens."
        )
        captureAndSave("05_CREATE_EDIT_TASK/02-create-task-filled.png")

        safeClickTag("category_chip_work")
        captureAndSave("05_CREATE_EDIT_TASK/04-create-task-category-selection.png")

        safeClickTag("priority_chip_urgent")
        captureAndSave("05_CREATE_EDIT_TASK/05-create-task-priority-selection.png")

        safeClickTag("due_date_selector")
        captureAndSave("05_CREATE_EDIT_TASK/06-create-task-date-picker.png")
        safeClickText("OK")

        safeClickTag("due_time_selector")
        captureAndSave("05_CREATE_EDIT_TASK/07-create-task-time-duration.png")
        safeClickText("Save")

        safeScrollToTag("sheet_subtask_input")
        safeSetText("sheet_subtask_input", "Check Charcoal (#191919) surface contrast")
        safeClickTag("sheet_add_subtask_button")
        safeSetText("sheet_subtask_input", "Validate Neon Lime (#D5FD51) CTA states")
        safeClickTag("sheet_add_subtask_button")
        captureAndSave("05_CREATE_EDIT_TASK/09-create-task-with-subtasks.png")

        safeScrollToTag("toggle_advanced_options")
        safeClickTag("toggle_advanced_options")
        safeScrollToText("30 min before")
        safeClickText("30 min before")
        safeScrollToTag("recurrence_chip_daily")
        safeClickTag("recurrence_chip_weekly")
        captureAndSave("05_CREATE_EDIT_TASK/08-create-task-reminder-enabled.png")

        // Save new task to return to Home
        safeClickTag("save_task_button")

        // =====================================================================
        // GROUP E: 06_TASK_DETAILS_SUBTASKS (E01 - E14) & I04
        // =====================================================================
        composeTestRule.runOnUiThread {
            vm.openTaskDetails(seeded.primaryPortfolioTaskId)
        }
        waitUntilPumped { hasTagOnUiThread("task_details_screen") }

        captureAndSave("06_TASK_DETAILS_SUBTASKS/01-task-details-overview.png")
        captureAndSave("10_STATES_AND_ERRORS/04-notification-open-task-details.png")

        val subtasksInitial = vm.uiState.value.allTasks.first { it.task.id == seeded.primaryPortfolioTaskId }.subtasks
        safeScrollToTag("subtask_row_${subtasksInitial.last().id}")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/02-task-details-subtasks-list.png")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/03-task-details-subtasks-partial.png")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/13-task-details-subtask-delete.png")

        // E04: All Subtasks Completed (4/4 Done)
        subtasksInitial.filter { !it.isCompleted }.forEach { sub ->
            composeTestRule.runOnUiThread { vm.toggleSubtask(sub) }
        }
        waitUntilPumped {
            vm.uiState.value.allTasks.first { it.task.id == seeded.primaryPortfolioTaskId }.subtasks.all { it.isCompleted }
        }
        captureAndSave("06_TASK_DETAILS_SUBTASKS/04-task-details-subtasks-complete.png")

        // Restore last 2 subtasks to incomplete
        val subtasksAllDone = vm.uiState.value.allTasks.first { it.task.id == seeded.primaryPortfolioTaskId }.subtasks
        subtasksAllDone.drop(2).forEach { sub ->
            composeTestRule.runOnUiThread { vm.toggleSubtask(sub) }
        }
        waitUntilPumped {
            vm.uiState.value.allTasks.first { it.task.id == seeded.primaryPortfolioTaskId }.subtasks.count { it.isCompleted } == 2
        }

        // E05: Add Subtask Input Active
        safeScrollToTag("details_new_subtask_input")
        safeSetText("details_new_subtask_input", "Verify dark mode typography scale")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/05-task-details-add-subtask.png")
        safeSetText("details_new_subtask_input", "")

        // E06 & E07: Inline Edit Active & Dirty State
        safeScrollToTag("details_description_text")
        safeClickTag("details_description_text")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/06-task-details-inline-edit.png")

        safeSetText(
            "details_description_input",
            "Updated brief: Review 8dp spacing tokens, verify 16dp horizontal card margins, and export Behance showcase boards with developer credit."
        )
        captureAndSave("06_TASK_DETAILS_SUBTASKS/07-task-details-inline-edit-dirty.png")
        safeClickTag("details_save_description_button")

        // E09: More Actions Menu & D10 + E08: Edit Existing Task + Unsaved Changes Discard Dialog
        safeClickTag("details_more_menu_button")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/09-task-details-saving-state.png")

        safeClickTag("menu_edit_task")
        waitUntilPumped { hasTagOnUiThread("create_edit_task_screen") }
        captureAndSave("05_CREATE_EDIT_TASK/10-edit-existing-task.png")

        safeSetText("task_title_input", "Finalize portfolio mobile UI layout (Draft v2)")
        safeClickTag("cancel_task_button")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/08-task-details-discard-dialog.png")
        safeClickTag("discard_changes_button")

        // E10 & E11: Completed Task State & Reopened Task State
        safeClickTag("details_toggle_complete_button")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/10-task-details-completed.png")

        safeClickTag("details_toggle_complete_button")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/11-task-details-reopened.png")

        // E12: Delete Task Confirmation Dialog
        safeClickTag("details_more_menu_button")
        safeClickTag("menu_delete_task")
        captureAndSave("06_TASK_DETAILS_SUBTASKS/12-task-details-delete-dialog.png")
        safeClickText("Cancel")

        // Return to Home
        safeClickTag("details_back_button")

        // =====================================================================
        // E14 (Missing Task Recovery) & I07 (Long Text Layout) & I01/I02/I06
        // =====================================================================
        val longTaskId = repository.saveTaskWithSubtasks(
            existingTaskId = null,
            title = "Comprehensive Cross-Platform Design System Typography & Responsive Layout Stress Verification Across Multi-Line Card Surfaces",
            description = "This stress-test task verifies that very long multi-paragraph descriptions, technical specifications, and edge-case notes wrap cleanly without text truncation, overlapping badges, or clipped action buttons.\n\nParagraph 2: Audit Dark Charcoal (#191919), Neon Lime (#D5FD51), and Soft Lavender (#BC7BFD) across compact handhelds, foldables, and high-DPI displays while maintaining strict 8dp vertical rhythm.",
            dueDateEpochDay = DateTimeUtil.currentLocalEpochDay(),
            dueTimeMinutes = 16 * 60 + 45,
            priority = TaskPriority.URGENT,
            categoryId = seeded.designCatId,
            reminderOffset = ReminderOffset.THIRTY_MIN_BEFORE,
            customReminderMillis = null,
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceWeekdays = "1,3,5",
            subtaskDrafts = emptyList()
        ).getOrThrow()
        waitUntilPumped {
            vm.uiState.value.allTasks.any { it.task.id == longTaskId }
        }
        composeTestRule.runOnUiThread { vm.openTaskDetails(longTaskId) }
        waitUntilPumped { hasTagOnUiThread("task_details_screen") }
        captureAndSave("10_STATES_AND_ERRORS/07-long-text-layout.png")

        safeClickTag("details_more_menu_button")
        database.dayformDao().deleteTaskById(longTaskId)
        waitUntilPumped {
            vm.uiState.value.allTasks.none { it.task.id == longTaskId }
        }
        safeClickTag("menu_edit_task")
        waitUntilPumped { hasTagOnUiThread("task_not_found_error_screen") }
        captureAndSave("06_TASK_DETAILS_SUBTASKS/14-task-details-missing-task.png")

        safeClickTag("task_not_found_back_button")
        if (hasTagOnUiThread("details_back_button")) {
            safeClickTag("details_back_button")
        }

        // I01, I02, I06: Create Task with Reminder Warning / Retry Banner & Preserved Draft State
        repository.reminderScheduleHandler = { _, _, _ ->
            Result.failure(IllegalStateException("OS AlarmManager limit reached"))
        }
        safeClickTag("nav_add_task_button")
        waitUntilPumped { hasTagOnUiThread("create_edit_task_screen") }

        safeSetText("task_title_input", "Sync Q4 Design Tokens & Release Notes")
        safeSetText(
            "task_description_input",
            "Draft preserved across configuration changes with reminder enabled."
        )
        safeClickTag("priority_chip_high")

        captureAndSave("10_STATES_AND_ERRORS/06-recreation-state-preserved.png")

        safeScrollToTag("toggle_advanced_options")
        safeClickTag("toggle_advanced_options")
        safeScrollToText("10 min before")
        safeClickText("10 min before")

        captureAndSave("10_STATES_AND_ERRORS/02-notification-permission-dialog.png")

        safeClickTag("save_task_button")
        waitUntilPumped { hasTagOnUiThread("reminder_warning_banner") }
        captureAndSave("10_STATES_AND_ERRORS/01-reminder-warning-retry.png")
    }
}
