# Architecture

## Principles

StudyShield is offline-first, student-controlled, and explicit about exit paths. The accessibility service is used only to identify foreground package changes. It does not inspect Accessibility node trees, perform gestures, automate other apps, or hide itself.

## Runtime Flow

```text
Profile saved in Room
  -> AccessibilityService receives TYPE_WINDOW_STATE_CHANGED
  -> RuleEngine compares package name, profile schedules, day, enabled target apps, and skips
  -> Repository attaches the selected companion pack and scene dialogue
  -> OverlayController creates TYPE_ACCESSIBILITY_OVERLAY
  -> PromptAudioPlayer requests transient accessibility audio focus
  -> Compose overlay shows profile, target, dialogue, selected images, wallpaper, and actions
  -> Outcome is recorded in Room
  -> Overlay and audio are removed
```

## Module Boundaries

- `domain`: pure Kotlin model objects and `RuleEngine`. This layer is covered by JVM tests and has no Android dependencies.
- `storage`: Room entities and mappers. Primitive persisted fields keep the schema reviewable.
- `accessibility`: platform event source. It passes only package names and loaded profiles into the rule engine.
- `overlay`: WindowManager ownership and overlay UI. It is responsible for always removing views and stopping audio.
- `media`: Media3 and `AudioFocusRequest` integration. It never loops, raises volume, plays remote audio, or plays without focus. If media volume is zero or the device is silent/vibrate, the overlay stays visual and shows the transcript.
- `companion`: manifest parsing, validation, and zip archive import/export for local companion packs.
- `ui`: Compose application UI and theme.

## Data Model

Room stores:

- `focus_profiles`: profile name, enabled state, intensity, companion pack reference, and overlay layout values.
- `schedule_rules`: repeatable local start/end ranges, active days, and time zone.
- `target_apps`: user-selected package names and labels.
- `companion_packs`: manifest metadata, optional character/wallpaper URI indexes, and default layout.
- `dialogue_cues`: scene text, optional audio URI, and required transcript.
- `focus_events`: local reflection records for reminders and outcomes.

## Overlay Lifecycle

The service keeps at most one overlay view. New reminders call `hide()` before showing a replacement. `hide()` stops audio, removes the view immediately, clears lifecycle owners, and is called from service interrupt, destroy, non-matching foreground changes, the StudyShield activity coming to the foreground, and every user outcome. Own-package overlay implementation events are ignored so the overlay does not dismiss itself immediately after being added.

## Overlay Preview

The setup UI includes a full-screen preview path that builds a synthetic `ActiveReminder` from the current editor state and renders it through the same `FocusOverlayContent` composable used by the accessibility overlay. Preview mode does not request Accessibility permission, play audio, record events, or affect skip state.

## Excluded Packages

The service excludes StudyShield, Android system, Settings, permission controller, System UI, package installer, intent resolver, dialer, incall, phone, telecom, emergency, Play Store, Google Play services, NFC, and Google Wallet packages. These exclusions prevent reminder overlays on permission, install/uninstall, phone, payment, and core system flows.

## No Network Design

The app manifest intentionally omits `android.permission.INTERNET`. Any future feature requiring servers, analytics, ads, remote chat, or cross-device sync conflicts with the project scope unless the product plan is explicitly revised.

Companion pack sharing is file-based. Import copies zip-contained assets into app-private storage, export writes a new zip containing `manifest.json` plus readable image and MP3 assets, and no network transport is implemented.

## Picker-Based Assets

Character images and wallpapers are selected through Android Photo Picker. MP3 voice prompts are selected through Android's document picker. The app persists only the returned URI strings plus the system-granted read access; it does not request broad media-library permissions or scan storage. Zip-imported companion assets are copied into app-private storage.
