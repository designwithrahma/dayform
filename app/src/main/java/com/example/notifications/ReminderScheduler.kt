package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.ReminderOffset
import com.example.data.model.TaskEntity
import com.example.util.DateTimeUtil

enum class ReminderScheduleStatus {
    NONE_REQUESTED,
    SCHEDULED,
    SKIPPED_PAST_TRIGGER,
    BLOCKED_APP_PREFERENCE,
    BLOCKED_OS_PERMISSION,
    FAILED_RETRYABLE
}

data class ReminderScheduleReport(
    val status: ReminderScheduleStatus,
    val triggerMillis: Long? = null,
    val userMessage: String? = null
) {
    val isScheduled: Boolean
        get() = status == ReminderScheduleStatus.SCHEDULED
}

object ReminderScheduler {

    const val CHANNEL_ID = "dayform_task_reminders"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESCRIPTION = "extra_task_description"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!runtimeGranted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun computeReminderTriggerMillis(task: TaskEntity): Long? {
        if (task.isCompleted) return null
        val offset = task.reminderEnum
        if (offset == ReminderOffset.NONE) return null
        if (offset == ReminderOffset.CUSTOM) {
            return task.customReminderMillis
        }
        val minutesBefore = offset.minutesBefore ?: return null
        // Default due time to 9:00 AM if task only has a due date without specific time
        val effectiveDueMinutes = task.dueTimeMinutes ?: (9 * 60)
        val baseDueMillis = DateTimeUtil.epochDayAndMinutesToMillis(
            task.dueDateEpochDay,
            effectiveDueMinutes
        )
        return baseDueMillis - (minutesBefore * 60_000L)
    }

    fun scheduleTaskReminderDetailed(
        context: Context,
        task: TaskEntity,
        notificationsEnabled: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): ReminderScheduleReport {
        cancelTaskReminder(context, task.id)
        if (task.isCompleted || task.reminderEnum == ReminderOffset.NONE) {
            return ReminderScheduleReport(status = ReminderScheduleStatus.NONE_REQUESTED)
        }
        if (!notificationsEnabled) {
            return ReminderScheduleReport(
                status = ReminderScheduleStatus.BLOCKED_APP_PREFERENCE,
                userMessage = "Notifications are disabled in DAYFORM settings."
            )
        }

        val triggerMillis = computeReminderTriggerMillis(task)
            ?: return ReminderScheduleReport(
                status = ReminderScheduleStatus.FAILED_RETRYABLE,
                userMessage = "Reminder time could not be calculated for this task."
            )

        if (triggerMillis <= nowMillis) {
            return ReminderScheduleReport(
                status = ReminderScheduleStatus.SKIPPED_PAST_TRIGGER,
                triggerMillis = triggerMillis,
                userMessage = "Reminder time has already passed, so no alarm was scheduled."
            )
        }

        if (!hasNotificationPermission(context)) {
            return ReminderScheduleReport(
                status = ReminderScheduleStatus.BLOCKED_OS_PERMISSION,
                triggerMillis = triggerMillis,
                userMessage = "Android notification permission is disabled. Grant notification permission to receive reminders."
            )
        }

        return try {
            ensureNotificationChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return ReminderScheduleReport(
                    status = ReminderScheduleStatus.FAILED_RETRYABLE,
                    triggerMillis = triggerMillis,
                    userMessage = "System AlarmManager service is unavailable."
                )

            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_TASK_DESCRIPTION, task.description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } else {
                        // Graceful fallback when exact alarm permission is not granted
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
                ReminderScheduleReport(
                    status = ReminderScheduleStatus.SCHEDULED,
                    triggerMillis = triggerMillis
                )
            } catch (_: SecurityException) {
                // Fallback to inexact window alarm if exact alarm throws SecurityException
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    5 * 60_000L,
                    pendingIntent
                )
                ReminderScheduleReport(
                    status = ReminderScheduleStatus.SCHEDULED,
                    triggerMillis = triggerMillis
                )
            }
        } catch (e: Exception) {
            ReminderScheduleReport(
                status = ReminderScheduleStatus.FAILED_RETRYABLE,
                triggerMillis = triggerMillis,
                userMessage = e.localizedMessage ?: "Reminder alarm could not be registered with the system."
            )
        }
    }

    fun scheduleTaskReminder(
        context: Context,
        task: TaskEntity,
        notificationsEnabled: Boolean = true
    ): Result<Boolean> {
        val report = scheduleTaskReminderDetailed(
            context = context,
            task = task,
            notificationsEnabled = notificationsEnabled
        )
        return when (report.status) {
            ReminderScheduleStatus.SCHEDULED -> Result.success(true)
            ReminderScheduleStatus.NONE_REQUESTED,
            ReminderScheduleStatus.SKIPPED_PAST_TRIGGER -> Result.success(false)
            ReminderScheduleStatus.BLOCKED_APP_PREFERENCE,
            ReminderScheduleStatus.BLOCKED_OS_PERMISSION -> Result.failure(
                IllegalStateException(report.userMessage ?: "Reminder blocked by notification settings.")
            )
            ReminderScheduleStatus.FAILED_RETRYABLE -> Result.failure(
                IllegalArgumentException(report.userMessage ?: "Reminder could not be scheduled.")
            )
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.toInt())
    }

    fun cancelAllTaskReminders(context: Context, taskIds: Iterable<Long>) {
        taskIds.forEach { taskId ->
            cancelTaskReminder(context, taskId)
        }
        dismissAllDisplayedNotifications(context)
    }

    fun dismissAllDisplayedNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
    }

    fun reconcileReminders(
        context: Context,
        tasks: List<TaskEntity>,
        notificationsEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (!notificationsEnabled) {
            cancelAllTaskReminders(context, tasks.map { it.id })
            return 0
        }
        var scheduledCount = 0
        tasks.forEach { task ->
            val report = scheduleTaskReminderDetailed(
                context = context,
                task = task,
                notificationsEnabled = true,
                nowMillis = nowMillis
            )
            if (report.isScheduled) {
                scheduledCount++
            }
        }
        return scheduledCount
    }
}
