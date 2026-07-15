# Local Verification Environment

Date: 2026-06-21

This file records what the current VPS environment can verify without an attached Android device.

## Android Tooling

- Android SDK root: `/home/vmadmin/install/opt/android-sdk`
- Installed platform: `android-36`
- Installed system images: none
- Configured Android Virtual Devices: none
- Attached devices: not used for current local completion scope

## Verification Possible Here

- JVM unit tests via `./gradlew testDebugUnitTest`
- Debug APK build via `./gradlew assembleDebug`
- Debug instrumentation APK build via `./gradlew assembleDebugAndroidTest`
- Compiled instrumentation smoke test: `dev.studyshield.MainActivitySmokeTest`
- Android lint via `./gradlew lintDebug`
- APK manifest, permission, badging, and signature inspection via `scripts/release-audit.sh`
- Source and release-document checks for privacy-sensitive APIs, dependency names, Room schema exports, and store submission documents
- Bundled companion package checks for the Codex zip manifest, character PNG, and MP3 audio entry

## Verification Outside Current Scope

Connected device and emulator scenarios are outside the current local completion gate.

Before a public release, copy `docs/verification/DEVICE_MATRIX_TEMPLATE.md` for the release candidate and complete device testing on physical or emulator devices for API 26, 29, 33, 35, and 36.

## Latest Local Audit

Command:

```bash
. /home/vmadmin/install/CACHE/android_env.sh && scripts/release-audit.sh
```

Result: passed on 2026-06-21.

The audit ran:

- `./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`
- APK signature verification with `apksigner verify --verbose`
- APK permission and badging inspection with `aapt2`
- Source checks for prohibited Accessibility, automation, network analytics, advertising, and crash-reporting patterns
- Room schema, release document, and bundled companion pack manifest checks
- Bundled Codex companion zip checks for manifest, character PNG, and MP3 audio entries
- Repository transaction-boundary checks for profile saves, companion-pack imports, deletion, and seed restoration
- Delete-my-data checks for persisted URI read-grant cleanup
- Unit coverage for foreground event handling, audio fallback policy, local-only audio URI validation, sensitive package exclusions, rule matching, overlay layout, companion pack validation, manifest codec, and preview state
- Compiled instrumentation coverage for main activity launch and Accessibility disclosure visibility

Artifacts:

| Artifact | SHA-256 |
|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | `667cf352066f3678ef27e903d6fcc6a4be20be8488b058782a7125fa3a5e60bb` |
| `app/build/outputs/apk/release/app-release.apk` | `9c870dab6c714de281913ed68b24f2ed32ae51cd2129b211ee5b256edf54e1e4` |
| `app/build/outputs/bundle/release/app-release.aab` | `16e0142bce367ed3c1c3f877346b2d03aaa8dbe090323df54931f495b755ae79` |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `c22442d505bd40d585162e7eb5b9a2198eef8e4c1ef9f4cf64759d8e53745619` |
| `app/src/main/assets/companion_packs/codex_cat_girl/codex-cat-girl.studyshield-pack.zip` | `29568a4204798b85bb552f3f274f62cb7d9fc914faa491f0f95037ac8b8671ba` |

APK badging summary:

- Package: `dev.studyshield`
- Version: `0.1.2` (`versionCode` 3)
- Min SDK: 26
- Target SDK: 36
- Accessibility component: present
- Requested dangerous permission: `android.permission.POST_NOTIFICATIONS`
- Requested network permissions: none
- Optional app-op permission declared: `android.permission.PACKAGE_USAGE_STATS`
