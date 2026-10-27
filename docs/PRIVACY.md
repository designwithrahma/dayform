# Privacy & local data

This document describes the **inspected source code**; it is not a third-party privacy audit of a released APK.

- The app uses Room/SQLite on-device for tasks, subtasks and categories.
- Android DataStore saves personal preferences, such as your display name, onboarding completion, notification and feedback settings.
- Android local notifications and scheduled alarms may display task content on your device.
- JSON export shares workspace data to a destination chosen by the user; imported JSON may overwrite data if replacement is selected. Treat exported files as personal information.
- The source manifest does not request `android.permission.INTERNET`, and no network API integration is wired into the main app source. The About screen offers an external portfolio link opened in another app when tapped.
- Android automatic backups are disabled (`allowBackup=false`) to avoid accidental sync of local tasks; use manual JSON export if you need a backup.

Do not assume files exported to external apps or cloud drives retain this app's privacy protections. Third-party software on a user's device may collect its own information independently. Review this policy whenever network features, SDKs or analytics are introduced.
