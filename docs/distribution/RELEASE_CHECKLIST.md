# Release Checklist

## Build

- Run `. /home/vmadmin/install/CACHE/android_env.sh && ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`.
- Run `scripts/release-audit.sh`.
- Verify `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk` contains only `android.permission.PACKAGE_USAGE_STATS`, `android.permission.POST_NOTIFICATIONS`, and the AndroidX dynamic receiver permission, with no network, location, contacts, broad media, screenshot, package-inventory, or overlay-management permission.
- Verify `aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk` reports package `dev.studyshield`, min SDK 26, target SDK 36, launchable `MainActivity`, and `provides-component:'accessibility'`.
- Verify `apksigner verify --verbose app/build/outputs/apk/debug/app-debug.apk` succeeds for the installable APK.
- Verify the target SDK matches the highest installed and tested Android SDK for the release environment.
- Migrate Room schema intentionally and commit exported schemas.

## Device Verification

- Copy `docs/verification/DEVICE_MATRIX_TEMPLATE.md` for the release candidate and fill it with device evidence.
- Test API 26, 29, 33, 35, and 36 where available.
- Test small phone, large phone, tablet, portrait, landscape, and split-screen.
- Test Accessibility disabled, enabled, interrupted, and service process restart.
- Test the required-permission facade, close confirmation, five-minute setup notification reminders, Usage Access disabled event-only detection, Usage Access enabled five-second detection for already-open target apps, selected target app trigger, non-target app no-trigger, Settings/permission/dialer exclusion, five-minute pause, and delete data including persisted file read-grant cleanup.
- Test media volume zero, silent/vibrate mode, audio focus denied, missing MP3 URI, invalid image URI, remote audio URI rejection, companion-pack zip import/export, and companion-pack import failure.
- Test no-network mode.
- Run the compiled `MainActivitySmokeTest` as part of connected tests; it verifies the main screen launches and shows the voluntary Accessibility disclosure.

## Store Materials

- Confirm `docs/distribution/PLAY_CONSOLE_ACCESSIBILITY_DECLARATION.md` matches the app behavior.
- Confirm `docs/distribution/PLAY_DATA_SAFETY.md` matches the merged manifest and dependency graph.
- Ensure official companion packs contain only original or redistributable assets and license metadata.
