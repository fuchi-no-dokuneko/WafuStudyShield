# Accessibility Disclosure

## In-App Disclosure

StudyShield uses Android Accessibility so it can know which app is currently in the foreground and show a study reminder over apps and times you choose.

StudyShield only compares the foreground app package name with your local rules. It does not read messages, passwords, screen text, contacts, photos, location, browser history, or social content. It does not tap, swipe, fill forms, capture screenshots, or control other apps.

You can skip a reminder, end a focus session, disable the service in Android Settings, edit rules, delete local records, or uninstall StudyShield at any time.

## Play Console Accessibility Declaration

StudyShield is a voluntary student focus reminder. The AccessibilityService is used to detect foreground app package changes and present a full-screen reminder overlay when a user-selected app opens during a user-configured study schedule.

The service does not retrieve window content, inspect nodes, capture screenshots, perform gestures, simulate clicks, block Settings, block emergency calls, collect social content, upload data, or hide from the user. All rules and records are local to the device.

## Service Configuration

The app's accessibility service metadata sets:

- `android:accessibilityEventTypes="typeWindowStateChanged"`
- `android:canRetrieveWindowContent="false"`
- `android:canPerformGestures="false"`
- `android:canTakeScreenshot="false"`

The service code does not call `rootInActiveWindow`, gesture dispatch APIs, or screenshot APIs.
