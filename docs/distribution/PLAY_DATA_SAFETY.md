# Play Data Safety Notes

## Data Collection

StudyShield does not collect or transmit user data off device.

## Data Shared

No data is shared with third parties.

## Network Use

The merged Android manifest does not request `android.permission.INTERNET`. The app contains no analytics SDK, advertising SDK, cloud API client, crash upload SDK, or background upload path.

## Local Data Stored

The app stores the following data locally in its Room database:

- Focus profile names and schedule rules.
- User-selected target app package names and labels.
- Companion pack metadata and selected content URI strings.
- Dialogue cue text, optional MP3 URI strings, and transcripts.
- Local focus event records: timestamp, profile name, selected target app package/label, and user outcome.

When the user grants Android Usage Access, StudyShield can read foreground app usage events. It uses those events only on device to infer the current foreground package every five seconds and does not store raw usage history.

When the user grants notification permission, StudyShield uses it only to send local setup reminders every five minutes while Accessibility or Usage Access remains disabled. No notification content is sent off device.

## Deletion

The in-app "Delete my data" action clears local profiles, selected app records, companion pack indexes, dialogue cues, focus event records, and persisted read grants for files selected through Android system pickers, then restores the bundled sample profile. Original user-owned files selected through Android system pickers remain in the user's storage provider and are deleted outside StudyShield.

## Sensitive Permissions

StudyShield uses Android Accessibility and optional Usage Access only for foreground package-name detection and an accessibility overlay. Notification permission is used only for local setup reminders. It does not read Accessibility node content, screenshots, typed text, passwords, messages, contacts, location, browser history, or full media-library contents.
