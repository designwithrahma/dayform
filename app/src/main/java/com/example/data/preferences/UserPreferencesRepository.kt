package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dayformDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dayform_user_preferences"
)

@androidx.compose.runtime.Immutable
data class UserSettings(
    val hasCompletedOnboarding: Boolean = false,
    val displayName: String = "",
    val notificationsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val soundFeedbackEnabled: Boolean = true,
    val weekStartsOnMonday: Boolean = true,
    val use24HourFormat: Boolean = false,
    val hideCompletedSubtasks: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val WEEK_STARTS_MONDAY = booleanPreferencesKey("week_starts_monday")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val HIDE_COMPLETED_SUBTASKS = booleanPreferencesKey("hide_completed_subtasks")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dayformDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserSettings(
                hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
                displayName = prefs[Keys.DISPLAY_NAME]?.trim() ?: "",
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
                soundFeedbackEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                weekStartsOnMonday = prefs[Keys.WEEK_STARTS_MONDAY] ?: true,
                use24HourFormat = prefs[Keys.USE_24_HOUR] ?: false,
                hideCompletedSubtasks = prefs[Keys.HIDE_COMPLETED_SUBTASKS] ?: false
            )
        }

    suspend fun completeOnboarding(displayName: String) {
        val cleanName = displayName.trim()
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = true
            prefs[Keys.DISPLAY_NAME] = cleanName
        }
    }

    suspend fun updateDisplayName(name: String) {
        val cleanName = name.trim()
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = cleanName
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setSoundFeedbackEnabled(enabled: Boolean) {
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setWeekStartsOnMonday(monday: Boolean) {
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.WEEK_STARTS_MONDAY] = monday
        }
    }

    suspend fun setUse24HourFormat(use24Hour: Boolean) {
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.USE_24_HOUR] = use24Hour
        }
    }

    suspend fun setHideCompletedSubtasks(hide: Boolean) {
        context.dayformDataStore.edit { prefs ->
            prefs[Keys.HIDE_COMPLETED_SUBTASKS] = hide
        }
    }
}
