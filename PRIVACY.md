# Privacy

StudyShield is designed to work offline. Core focus reminders, rules, companion metadata, and local records are stored on the device in the app database.

## What The Service Observes

The AccessibilityService receives foreground window state changes and reads the event package name. It uses that package name to decide whether a user-selected reminder rule applies.

## What StudyShield Does Not Read

StudyShield does not read:

- Message text.
- Passwords.
- Screen text.
- Accessibility node trees.
- Contacts.
- Location.
- Full photo library contents.
- Browser history.
- Social content.

## Network And Tracking

The merged Android manifest has no internet permission, network-state permission, or wake-lock permission. The app contains no analytics SDK, ad SDK, cloud API client, or background upload path.

## Local Records

Focus events store the timestamp, profile name, selected target app package/label, and the user's outcome. These records are for self-reflection only.

## Deleting Data

The app includes a "Delete my data" action. It clears local profiles, selected app records, companion pack indexes, dialogue cues, focus events, and persisted read grants for files selected through Android system pickers, then restores the bundled sample profile so the app remains usable. User-owned original files selected through system pickers are outside the app database and should be deleted by the user from their storage location if desired.

## Companion Packs

Official packs must include license metadata and redistributable assets. User-imported packs stay local. Character images and wallpapers are selected through Photo Picker, MP3 prompts are selected through Android's document picker, and the app stores only the chosen URI strings. Users and contributors must not submit copyrighted character art, voice clips, wallpapers, or other assets without permission.
