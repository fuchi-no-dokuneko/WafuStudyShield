# Play Console Accessibility Declaration

StudyShield is a voluntary student focus reminder. The AccessibilityService is used only to detect foreground app package changes and show a full-screen study reminder when a user-selected app opens during a user-configured study schedule.

The service:

- Does not retrieve window content.
- Does not inspect Accessibility nodes.
- Does not capture screenshots.
- Does not perform gestures, taps, swipes, form filling, or automation.
- Does not block Android Settings, emergency calls, permission screens, uninstall, or service disablement.
- Does not collect message text, passwords, social content, contacts, location, browser history, or media-library contents.
- Does not upload data or use analytics, advertising, cloud, or remote-chat services.

All focus profiles, selected package names, companion pack indexes, dialogue metadata, and focus event records remain local to the device. Users can skip a reminder, end a focus session, disable the AccessibilityService in Android Settings, edit rules, delete local records, or uninstall the app at any time.
