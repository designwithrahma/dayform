package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.DayformDatabase
import com.example.data.model.ReminderOffset
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminderBroadcast(context.applicationContext, intent)
            } catch (_: Exception) {
                // Ignore background errors
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        suspend fun handleReminderBroadcast(context: Context, intent: Intent): Boolean {
            val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
            if (taskId <= 0L) return false

            if (!ReminderScheduler.hasNotificationPermission(context)) {
                return false
            }

            val prefs = UserPreferencesRepository(context.applicationContext).userSettingsFlow.first()
            if (!prefs.notificationsEnabled) {
                ReminderScheduler.cancelTaskReminder(context, taskId)
                return false
            }

            val dao = DayformDatabase.getInstance(context.applicationContext).dayformDao()
            val task = dao.getTaskById(taskId)
            if (task == null || task.isCompleted || task.reminderEnum == ReminderOffset.NONE) {
                ReminderScheduler.cancelTaskReminder(context, taskId)
                return false
            }

            val title = task.title.takeIf { it.isNotBlank() } ?: "Upcoming Task"
            val description = task.description.takeIf { it.isNotBlank() }
                ?: "Tap to open task details in DAYFORM"

            ReminderScheduler.ensureNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            }

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                taskId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(taskId.toInt(), notification)
            return true
        }
    }
}
