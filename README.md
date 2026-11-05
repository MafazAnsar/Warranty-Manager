# Warranty Manager (Android)

A native Kotlin + Jetpack Compose starter app for tracking purchases, warranties and guarantees.

## Included
- Local Room database (offline-first)
- Categorised items, brand/model/serial number and supplier details
- Multiple warranty records per item in the database
- Product/receipt photo URI attachments
- Warranty expiry reminder scheduling with WorkManager
- Android notification permission handling
- Search and category filters
- Claim/repair notes
- LKR purchase price field

## Build
1. Install Android Studio (Hedgehog or newer) and Android SDK 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync.
4. Connect an Android phone with USB debugging enabled, or create an emulator.
5. Select `app` and Run.

Command line: `./gradlew assembleDebug` (requires Gradle wrapper; Android Studio can generate it if not present).

## Important current limitations
This is the first source-code build, not a signed APK.
- The dashboard's "Expiring" tile is currently a placeholder; the item-level warranty detail shows expiry dates.
- Reminder scheduling is implemented for the default 90/30/7 days, but custom reminder editing and rescheduling after edits are not yet exposed in the UI.
- Photo URIs are stored locally as references. Android may revoke access if the original source is moved or removed; a production release should copy attachments into app-private storage.
- Backup/export and cloud synchronisation are not implemented in this first cut. The Backup button is currently a placeholder. The next build should implement a ZIP backup/restore through Android's Storage Access Framework, allowing the user to choose Google Drive or local storage.
- The current UI creates one warranty record at item registration. The data model supports multiple records; a dedicated add/edit warranty screen is a next-step enhancement.
- For Android 13+, allow notification permission when prompted. Battery optimisation may delay reminders on some manufacturers' devices.

## Suggested next release
1. Reliable attachment copying to private app storage.
2. ZIP backup/restore containing database data and all photos/documents.
3. Google Drive backup using the Android document picker (no API key required).
4. Full warranty/guarantee editor and custom reminder schedule.
5. Expiry dashboard and CSV export.
