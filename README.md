# StudyShield

StudyShield is an open-source Android focus reminder for students. Students choose distracting apps, study times, and a reminder style. During an active rule, the AccessibilityService observes only the foreground package name and shows a full-screen `TYPE_ACCESSIBILITY_OVERLAY` reminder when a selected app opens.

StudyShield is not a hidden blocker and does not claim to be impossible to bypass. Users can disable the service, change rules, skip once, end a focus session, or uninstall the app.

## Current Build

- Native Kotlin Android app.
- Jetpack Compose settings UI and overlay UI.
- Room database for local profiles, schedules, selected apps, companion packs, dialogue, and local focus events.
- Multiple focus profiles, multiple time ranges per profile, and filtered installed-app selection.
- AccessibilityService that subscribes to window-state changes only.
- WindowManager overlay with return-home, skip-once, and end-focus actions.
- Media3 audio playback path with audio-focus checks and visible transcript fallback.
- Companion-pack validator, pack selector, zip import/export with embedded images and MP3 files, legacy JSON manifest import, Photo Picker image/wallpaper selection, MP3 document picker, and URI-backed overlay rendering.
- Bundled Codex Cat Girl demo companion package with Traditional Chinese dialogue and MP3 audio.
- Full-screen in-app reminder preview that reuses the production overlay renderer before Accessibility is enabled.
- JVM tests for rule matching, layout safety, and pack validation.

## Build

```bash
. /home/vmadmin/install/CACHE/android_env.sh
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The current debug build targets Android SDK 36. The merged APK requests no network, media-library, contacts, location, screenshot, or overlay-management permission; AndroidX adds only the app-local dynamic receiver permission.

Distribution notes and store disclosure drafts live under `docs/distribution/`.

For a release audit, run:

```bash
scripts/release-audit.sh
```

This runs the local build/test/lint gate and checks the merged APK and source for the project's no-network, no-tracking, no-node-content, no-gesture, and no-screenshot invariants.

## Android Limitations

Android requires users to manually enable accessibility services in system settings. StudyShield cannot and should not enable itself. Accessibility overlays are shown only after a selected app becomes the foreground app. System settings, permission screens, dialer, telecom, system UI, and StudyShield itself are excluded.

## Permissions And Data Boundaries

StudyShield does not request internet permission. It does not read messages, passwords, screen text, contacts, photos, location, browsing history, or Accessibility node trees. The service compares package names against user-selected rules.

Media and wallpaper customization use system pickers. The app stores selected content URI strings and requests read access to those specific files only. Imported companion-pack zip assets are copied into app-private storage for sharing-safe reuse, and users are responsible for ensuring they have rights to non-official assets.

## Repository Map

- `app/src/main/java/dev/studyshield/domain`: rule models and matching engine.
- `app/src/main/java/dev/studyshield/storage`: Room schema, DAOs, mappers, repository.
- `app/src/main/java/dev/studyshield/accessibility`: AccessibilityService event pipeline.
- `app/src/main/java/dev/studyshield/overlay`: overlay WindowManager and Compose renderer.
- `app/src/main/java/dev/studyshield/media`: audio focus and playback fallback.
- `app/src/main/java/dev/studyshield/companion`: companion-pack validation and zip package import/export.
- `app/src/test`: JVM tests for core behavior.
