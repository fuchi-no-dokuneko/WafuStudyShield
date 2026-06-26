#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f /home/vmadmin/install/CACHE/android_env.sh ]]; then
  # shellcheck disable=SC1091
  . /home/vmadmin/install/CACHE/android_env.sh
fi

fail() {
  printf 'release-audit: %s\n' "$*" >&2
  exit 1
}

command -v aapt2 >/dev/null 2>&1 || fail "aapt2 is not available; source the Android environment first."
command -v apksigner >/dev/null 2>&1 || fail "apksigner is not available; source the Android environment first."
command -v jar >/dev/null 2>&1 || fail "jar is required to inspect bundled companion packages."
command -v rg >/dev/null 2>&1 || fail "ripgrep is required for source audit checks."

./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
ANDROID_TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
[[ -f "$APK" ]] || fail "debug APK not found at $APK"
[[ -f "$ANDROID_TEST_APK" ]] || fail "debug androidTest APK not found at $ANDROID_TEST_APK"

apksigner verify --verbose "$APK" >/tmp/studyshield-audit-apksigner.txt ||
  fail "debug APK signature verification failed."

PERMISSIONS="$(aapt2 dump permissions "$APK")"
printf '%s\n' "$PERMISSIONS"

for permission in \
  "android.permission.INTERNET" \
  "android.permission.ACCESS_NETWORK_STATE" \
  "android.permission.WAKE_LOCK" \
  "android.permission.READ_EXTERNAL_STORAGE" \
  "android.permission.READ_MEDIA_IMAGES" \
  "android.permission.READ_MEDIA_VIDEO" \
  "android.permission.READ_MEDIA_AUDIO" \
  "android.permission.ACCESS_FINE_LOCATION" \
  "android.permission.ACCESS_COARSE_LOCATION" \
  "android.permission.READ_CONTACTS" \
  "android.permission.CALL_PHONE" \
  "android.permission.SYSTEM_ALERT_WINDOW"; do
  if grep -Fq "$permission" <<<"$PERMISSIONS"; then
    fail "unexpected merged APK permission: $permission"
  fi
done

mapfile -t USES_PERMISSIONS < <(sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" <<<"$PERMISSIONS")
if [[ "${#USES_PERMISSIONS[@]}" -ne 1 ||
      "${USES_PERMISSIONS[0]}" != "dev.studyshield.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" ]]; then
  fail "merged APK must request only the AndroidX dynamic receiver permission."
fi

BADGING="$(aapt2 dump badging "$APK")"
grep -Fq "package: name='dev.studyshield'" <<<"$BADGING" ||
  fail "debug APK package name is not dev.studyshield."
grep -Fq "minSdkVersion:'26'" <<<"$BADGING" ||
  fail "debug APK minSdkVersion is not 26."
grep -Fq "targetSdkVersion:'36'" <<<"$BADGING" ||
  fail "debug APK targetSdkVersion is not 36."
grep -Fq "application-label:'StudyShield'" <<<"$BADGING" ||
  fail "debug APK application label is not StudyShield."
grep -Fq "launchable-activity: name='dev.studyshield.MainActivity'" <<<"$BADGING" ||
  fail "debug APK launchable activity is missing or unexpected."
grep -Fq "provides-component:'accessibility'" <<<"$BADGING" ||
  fail "debug APK does not advertise an accessibility component."

MANIFEST="app/src/main/AndroidManifest.xml"
grep -Fq 'android:allowBackup="false"' "$MANIFEST" ||
  fail "AndroidManifest.xml must disable cloud backup."
grep -Fq 'android:fullBackupContent="false"' "$MANIFEST" ||
  fail "AndroidManifest.xml must disable full backup content."
grep -Fq 'android:dataExtractionRules="@xml/data_extraction_rules"' "$MANIFEST" ||
  fail "AndroidManifest.xml must define data extraction rules."

DATA_RULES="app/src/main/res/xml/data_extraction_rules.xml"
for domain in database sharedpref file; do
  grep -Fq "domain=\"$domain\"" "$DATA_RULES" ||
    fail "data extraction rules must exclude $domain data."
done

SERVICE_XML="app/src/main/res/xml/study_shield_accessibility_service.xml"
grep -Fq 'android:accessibilityEventTypes="typeWindowStateChanged"' "$SERVICE_XML" ||
  fail "accessibility service must subscribe only to window state changes."
grep -Fq 'android:canRetrieveWindowContent="false"' "$SERVICE_XML" ||
  fail "accessibility service must not retrieve window content."
grep -Fq 'android:canPerformGestures="false"' "$SERVICE_XML" ||
  fail "accessibility service must not perform gestures."
grep -Fq 'android:canTakeScreenshot="false"' "$SERVICE_XML" ||
  fail "accessibility service must not take screenshots."

PROHIBITED_SOURCE_PATTERNS=(
  "rootInActiveWindow"
  "dispatchGesture"
  "performGlobalAction"
  "takeScreenshot"
  "AccessibilityNodeInfo"
  "GLOBAL_ACTION"
  "ACTION_CALL"
  "Firebase"
  "firebase"
  "Analytics"
  "analytics"
  "AdMob"
  "admob"
  "Crashlytics"
  "crashlytics"
)

for pattern in "${PROHIBITED_SOURCE_PATTERNS[@]}"; do
  if rg -n --glob '!build/**' --glob '!docs/**' --glob '!*.md' --glob '!app/build/**' "$pattern" app/src/main build.gradle.kts app/build.gradle.kts settings.gradle.kts >/tmp/studyshield-audit-match.txt; then
    cat /tmp/studyshield-audit-match.txt >&2
    fail "prohibited source/dependency pattern found: $pattern"
  fi
done

for schema in \
  "app/schemas/dev.studyshield.storage.StudyShieldDatabase/1.json" \
  "app/schemas/dev.studyshield.storage.StudyShieldDatabase/2.json"; do
  [[ -s "$schema" ]] || fail "Room schema export missing or empty: $schema"
done

for required_doc in \
  "README.md" \
  "ARCHITECTURE.md" \
  "PRIVACY.md" \
  "ACCESSIBILITY_DISCLOSURE.md" \
  "COMPANION_PACK_SPEC.md" \
  "CONTRIBUTING.md" \
  "docs/distribution/PLAY_CONSOLE_ACCESSIBILITY_DECLARATION.md" \
  "docs/distribution/PLAY_DATA_SAFETY.md" \
  "docs/distribution/RELEASE_CHECKLIST.md" \
  "docs/verification/LOCAL_ENVIRONMENT.md" \
  "docs/verification/DEVICE_MATRIX_TEMPLATE.md"; do
  [[ -s "$required_doc" ]] || fail "required release document missing or empty: $required_doc"
done

[[ -s "app/src/main/assets/companion_packs/study_guide/manifest.json" ]] ||
  fail "bundled companion pack manifest is missing or empty."
for bundled_asset in \
  "app/src/main/assets/companion_packs/codex_cat_girl/manifest.json" \
  "app/src/main/assets/companion_packs/codex_cat_girl/character.png" \
  "app/src/main/assets/companion_packs/codex_cat_girl/voice.mp3" \
  "app/src/main/assets/companion_packs/codex_cat_girl/codex-cat-girl.studyshield-pack.zip"; do
  [[ -s "$bundled_asset" ]] || fail "bundled Codex companion package asset missing or empty: $bundled_asset"
done
CODEX_PACK_CONTENTS="$(jar tf app/src/main/assets/companion_packs/codex_cat_girl/codex-cat-girl.studyshield-pack.zip)"
for zip_entry in "manifest.json" "assets/character.png" "assets/voice.mp3"; do
  grep -Fxq "$zip_entry" <<<"$CODEX_PACK_CONTENTS" ||
    fail "bundled Codex companion package missing zip entry: $zip_entry"
done

REPOSITORY_SOURCE="app/src/main/java/dev/studyshield/storage/StudyShieldRepository.kt"
for function_name in saveProfile saveCompanionPack clearUserData ensureSeedData; do
  if ! perl -0ne "exit(/suspend fun $function_name\\([^)]*\\).*?database\\.withTransaction/s ? 0 : 1)" "$REPOSITORY_SOURCE"; then
    fail "StudyShieldRepository.$function_name must use a Room transaction."
  fi
done

PLATFORM_HELPERS_SOURCE="app/src/main/java/dev/studyshield/PlatformHelpers.kt"
grep -Fq "releasePersistableUriPermission" "$PLATFORM_HELPERS_SOURCE" ||
  fail "Delete-my-data privacy path must be able to release persisted URI read grants."
grep -Fq "persistedUriPermissions" "$PLATFORM_HELPERS_SOURCE" ||
  fail "Persisted URI permission cleanup must enumerate app-held grants."

VIEW_MODEL_SOURCE="app/src/main/java/dev/studyshield/ui/MainViewModel.kt"
perl -0ne 'exit(/fun clearUserData\(\).*?releaseAllReadPermissions/s ? 0 : 1)' "$VIEW_MODEL_SOURCE" ||
  fail "MainViewModel.clearUserData must release persisted URI read grants."

ACCESSIBILITY_SERVICE_SOURCE="app/src/main/java/dev/studyshield/accessibility/StudyShieldAccessibilityService.kt"
grep -Fq "ForegroundEventPolicy.actionFor" "$ACCESSIBILITY_SERVICE_SOURCE" ||
  fail "Accessibility service must route own-package foreground events through ForegroundEventPolicy."
grep -Fq "ForegroundEventAction.HideCurrentReminder" "$ACCESSIBILITY_SERVICE_SOURCE" ||
  fail "Accessibility service must hide reminders when StudyShield itself comes forward."

PROMPT_AUDIO_PLAYER_SOURCE="app/src/main/java/dev/studyshield/media/PromptAudioPlayer.kt"
grep -Fq "PromptAudioPolicy.fallbackReason" "$PROMPT_AUDIO_PLAYER_SOURCE" ||
  fail "PromptAudioPlayer must use PromptAudioPolicy before playback."
PROMPT_AUDIO_POLICY_SOURCE="app/src/main/java/dev/studyshield/media/PromptAudioPolicy.kt"
grep -Fq "DeviceSoundMode.Silent" "$PROMPT_AUDIO_POLICY_SOURCE" ||
  fail "Prompt audio policy must fall back in silent mode."
grep -Fq "DeviceSoundMode.Vibrate" "$PROMPT_AUDIO_POLICY_SOURCE" ||
  fail "Prompt audio policy must fall back in vibrate mode."
grep -Fq "android.resource" "$PROMPT_AUDIO_POLICY_SOURCE" ||
  fail "Prompt audio policy must explicitly allow local android.resource audio."

COMPANION_VALIDATOR_SOURCE="app/src/main/java/dev/studyshield/companion/CompanionPackValidator.kt"
grep -Fq "PromptAudioPolicy.isSupportedLocalAudioUri" "$COMPANION_VALIDATOR_SOURCE" ||
  fail "Companion pack validator must reject remote audio URIs."

printf 'release-audit: passed\n'
