package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ui.theme.DeadlineOrange
import com.example.ui.theme.LavenderPurple
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.SecondaryText

enum class TaskPriority(
    val label: String,
    val sortWeight: Int,
    val badgeColor: Color
) {
    LOW("Low", 1, SecondaryText),
    MEDIUM("Medium", 2, LavenderPurple),
    HIGH("High", 3, DeadlineOrange),
    URGENT("Urgent", 4, OverdueRed);

    companion object {
        fun fromName(name: String?): TaskPriority = when (name) {
            "LOW" -> LOW
            "MEDIUM" -> MEDIUM
            "HIGH" -> HIGH
            "URGENT" -> URGENT
            else -> entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
        }

        fun isValidName(name: String?): Boolean {
            return !name.isNullOrBlank() && entries.any { it.name.equals(name, ignoreCase = true) }
        }
    }
}

enum class ReminderOffset(
    val label: String,
    val minutesBefore: Long?
) {
    NONE("No reminder", null),
    AT_DUE_TIME("At due time", 0L),
    TEN_MIN_BEFORE("10 minutes before", 10L),
    THIRTY_MIN_BEFORE("30 minutes before", 30L),
    ONE_HOUR_BEFORE("1 hour before", 60L),
    ONE_DAY_BEFORE("1 day before", 1440L),
    CUSTOM("Custom time", null);

    companion object {
        fun fromName(name: String?): ReminderOffset = when (name) {
            "NONE" -> NONE
            "AT_DUE_TIME" -> AT_DUE_TIME
            "TEN_MIN_BEFORE" -> TEN_MIN_BEFORE
            "THIRTY_MIN_BEFORE" -> THIRTY_MIN_BEFORE
            "ONE_HOUR_BEFORE" -> ONE_HOUR_BEFORE
            "ONE_DAY_BEFORE" -> ONE_DAY_BEFORE
            "CUSTOM" -> CUSTOM
            else -> entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }

        fun isValidName(name: String?): Boolean {
            return !name.isNullOrBlank() && entries.any { it.name.equals(name, ignoreCase = true) }
        }
    }
}

enum class RecurrenceType(val label: String) {
    NEVER("Never"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM_WEEKDAYS("Custom weekdays");

    companion object {
        fun fromName(name: String?): RecurrenceType = when (name) {
            "NEVER" -> NEVER
            "DAILY" -> DAILY
            "WEEKLY" -> WEEKLY
            "MONTHLY" -> MONTHLY
            "CUSTOM_WEEKDAYS" -> CUSTOM_WEEKDAYS
            else -> entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NEVER
        }

        fun isValidName(name: String?): Boolean {
            return !name.isNullOrBlank() && entries.any { it.name.equals(name, ignoreCase = true) }
        }
    }
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}

enum class TaskSortOption(val label: String) {
    DUE_DATE("Due date"),
    PRIORITY("Priority"),
    NEWEST("Newest"),
    ALPHABETICAL("Alphabetical")
}

@Immutable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val sortOrder: Int = 0
)

@Immutable
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["isCompleted"]),
        Index(value = ["categoryId"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDateEpochDay: Long,
    val dueTimeMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val categoryId: Long? = null,
    val reminderOffset: String = ReminderOffset.NONE.name,
    val customReminderMillis: Long? = null,
    val recurrenceType: String = RecurrenceType.NEVER.name,
    val recurrenceWeekdays: String = "", // Comma-separated ISO weekdays 1..7
    val sortOrder: Int = 0,
    val isSampleData: Boolean = false,
    val recurrenceSeriesId: String? = null,
    val spawnedNextTaskId: Long? = null,
    val recurrenceAnchorDay: Int? = null
) {
    val priorityEnum: TaskPriority
        get() = TaskPriority.fromName(priority)

    val reminderEnum: ReminderOffset
        get() = ReminderOffset.fromName(reminderOffset)

    val recurrenceEnum: RecurrenceType
        get() = RecurrenceType.fromName(recurrenceType)
}

@Immutable
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

@Immutable
data class TaskWithDetails(
    val task: TaskEntity,
    val subtasks: List<SubtaskEntity>,
    val category: CategoryEntity?
) {
    val completedSubtasksCount: Int = subtasks.count { it.isCompleted }

    val totalSubtasksCount: Int = subtasks.size
}

@Immutable
data class SubtaskDraft(
    val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false
)

@Immutable
data class DayActivityStat(
    val epochDay: Long,
    val dayShortLabel: String,
    val completedCount: Int,
    val plannedCount: Int,
    val isToday: Boolean
)

@Immutable
data class CategoryTaskStat(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val totalCount: Int,
    val completedCount: Int
)

@Immutable
data class InsightsSummary(
    val completedToday: Int = 0,
    val completedThisWeek: Int = 0,
    val plannedThisWeek: Int = 0,
    val currentDailyStreak: Int = 0,
    val totalCompletedAllTime: Int = 0,
    val totalTasksAllTime: Int = 0,
    val overallCompletionRatePercent: Int = 0,
    val weeklyProgressFraction: Float = 0f,
    val sevenDayActivity: List<DayActivityStat> = emptyList(),
    val categoryBreakdown: List<CategoryTaskStat> = emptyList()
)
