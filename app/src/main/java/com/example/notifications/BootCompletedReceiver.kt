package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.DayformDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!isSupportedAction(action)) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleSystemReconciliation(context.applicationContext, action)
            } catch (_: Exception) {
                // Ignore background errors
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        fun isSupportedAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED
        }

        suspend fun handleSystemReconciliation(context: Context, action: String?): Int {
            if (!isSupportedAction(action)) return 0
            val prefs = UserPreferencesRepository(context.applicationContext).userSettingsFlow.first()
            val dao = DayformDatabase.getInstance(context.applicationContext).dayformDao()
            val tasks = dao.getAllTasksOnce()
            return ReminderScheduler.reconcileReminders(
                context = context.applicationContext,
                tasks = tasks,
                notificationsEnabled = prefs.notificationsEnabled
            )
        }
    }
}
