# Device Verification Record

Use one copy of this template per release candidate.

## Build Under Test

- Version name:
- Version code:
- APK SHA-256:
- Commit or source snapshot:
- Tester:
- Date:

## Device Matrix

| Device | Android API | Form factor | Orientation / mode | Result | Notes |
|---|---:|---|---|---|---|
|  | 26 | Small phone | Portrait | Not run |  |
|  | 29 | Phone | Landscape | Not run |  |
|  | 33 | Large phone | Portrait | Not run |  |
|  | 35 | Tablet | Split screen | Not run |  |
|  | 36 | Phone | Portrait | Not run |  |

## Required Scenarios

Record pass/fail, device, and notes for each item.

- Accessibility disabled: app lets user edit rules and preview without claiming active blocking.
- Accessibility enabled: selected target app opens and StudyShield overlay appears.
- Non-target app opens and no overlay appears.
- Android Settings, permission screens, dialer, telecom, and system UI do not show blocking overlays.
- Return home, skip once, and end focus remove overlay and stop audio.
- Service interrupted or disabled removes overlay and stops audio.
- Media volume zero, silent mode, and vibrate mode show full transcript fallback.
- Missing or invalid MP3 URI shows full transcript fallback.
- Invalid character or wallpaper URI does not crash and keeps fallback UI visible.
- Imported companion zip with image and MP3 assets saves the pack and plays or falls back to its transcript.
- Imported invalid companion package reports validation error and does not modify existing pack.
- Delete my data clears local profiles, target app records, companion indexes, dialogue, focus events, and persisted file read grants, then restores bundled sample data.
- No-network mode: core rules, preview, overlay trigger, and local records still work.

## Evidence

- Attach screenshots or screen recordings only from StudyShield or non-sensitive test apps.
- Do not attach private messages, social feeds, passwords, contacts, or other app content.
