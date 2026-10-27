# Architecture

DAYFORM is an offline-first, single-module native Android application built with Kotlin and Jetpack Compose.

## Application flow

`MainActivity` sets up the root Compose navigation and owns the top-level screen composition. `DayformViewModel` coordinates app state and user actions; `data/repository/DayformRepository.kt` handles task operations and JSON import/export logic.

## Storage

- **Room / SQLite:** `DayformDatabase`, `DayformDao`, and entities in `data/model/`. Database filename: `dayform_tasks.db`.
- **Room schema version 2:** `app/schemas/com.example.data.local.DayformDatabase/{1,2}.json`. Migration `MIGRATION_1_2` adds recurrence-related columns without destructive migration. Do not delete historical schemas.
- **DataStore Preferences:** name and preferences, including onboarding completion, notification preferences and date/time display settings.
- **Backups:** user-initiated JSON export/import handled within the app; protect exported files.

## UI

`ui/screens/` contains Home, All Tasks, task detail/editing, calendar, insights, onboarding, and settings. `ui/components/` holds common task cards, bottom navigation and dashboard cards. `ui/theme/` defines color, type, motion and theme tokens.

## Reminders

`ReminderScheduler` uses Android `AlarmManager`; `TaskReminderReceiver` posts valid task reminders. `BootCompletedReceiver` re-establishes eligible reminders after device restart, time adjustments, and package replacement. Users must grant notification/exact-alarm access where required by Android.

## Compatibility rules

The Gradle `applicationId` is `com.designwithrahma.dayform`; the current Kotlin namespace remains `com.example` for source compatibility. Keep the application ID and database name unchanged for updates. Database changes require explicit non-destructive migrations and committed schema JSON files. The app deliberately has `android:allowBackup="false"` and uses user-controlled JSON export/import instead of Android automatic backup.
