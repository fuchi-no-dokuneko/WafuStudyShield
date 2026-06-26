# Contributing

Contributions must preserve StudyShield's voluntary, offline-first, student-controlled design.

## Accepted Work

- Kotlin Android code.
- Tests for rule matching, overlay layout, storage, service lifecycle, audio fallback, and companion-pack validation.
- Translations for disclosures and UI labels.
- Original or redistributable companion packs with manifest license metadata.
- Device compatibility reports covering screen sizes, densities, orientation, Android versions, and accessibility settings.

## Not Accepted

- Server-side tracking.
- Advertising identifiers.
- Analytics SDKs.
- Hidden monitoring.
- Reading another app's text content.
- Accessibility automation for tapping, swiping, payments, form filling, bypassing restrictions, or impersonating system UI.
- Features that block Settings, emergency calls, permission screens, uninstall, or service disablement.
- Companion packs containing unauthorized assets, sexual content, shame, threats, or coercive student-retention copy.

## Test Matrix

Before release, verify:

- API 26, 29, 33, 35, and 36 where available.
- Small phone, large phone, tablet, portrait, landscape, and split screen.
- Media volume zero, focus denied, missing MP3, and voice disabled.
- Accessibility service disabled, enabled, interrupted, and app uninstalled.
- Network unavailable.

Record release candidate evidence with `docs/verification/DEVICE_MATRIX_TEMPLATE.md`. Run `scripts/release-audit.sh` before submitting a release or store metadata update.

## Issues

Reports should include Android version, device model, StudyShield version, whether accessibility is enabled, selected package count, and steps to reproduce. Do not attach private screenshots of messages or other app content.
