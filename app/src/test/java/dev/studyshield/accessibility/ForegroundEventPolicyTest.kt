package dev.studyshield.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundEventPolicyTest {
    @Test
    fun evaluatesNormalForegroundPackages() {
        val action = ForegroundEventPolicy.actionFor(
            packageName = "com.social.app",
            className = "com.social.app.MainActivity",
            ownPackageName = "dev.studyshield",
            hasCurrentReminder = true
        )

        assertEquals(ForegroundEventAction.Evaluate, action)
    }

    @Test
    fun evaluatesOwnPackageWhenNoReminderIsVisible() {
        val action = ForegroundEventPolicy.actionFor(
            packageName = "dev.studyshield",
            className = "dev.studyshield.MainActivity",
            ownPackageName = "dev.studyshield",
            hasCurrentReminder = false
        )

        assertEquals(ForegroundEventAction.Evaluate, action)
    }

    @Test
    fun hidesReminderWhenOwnMainActivityComesForward() {
        val action = ForegroundEventPolicy.actionFor(
            packageName = "dev.studyshield",
            className = "dev.studyshield.MainActivity",
            ownPackageName = "dev.studyshield",
            hasCurrentReminder = true
        )

        assertEquals(ForegroundEventAction.HideCurrentReminder, action)
    }

    @Test
    fun ignoresOwnPackageEventsFromOverlayImplementationClasses() {
        val action = ForegroundEventPolicy.actionFor(
            packageName = "dev.studyshield",
            className = "androidx.compose.ui.platform.ComposeView",
            ownPackageName = "dev.studyshield",
            hasCurrentReminder = true
        )

        assertEquals(ForegroundEventAction.IgnoreOwnOverlayEvent, action)
    }

    @Test
    fun ignoresOwnPackageEventsWithUnknownClassWhileReminderIsVisible() {
        val action = ForegroundEventPolicy.actionFor(
            packageName = "dev.studyshield",
            className = null,
            ownPackageName = "dev.studyshield",
            hasCurrentReminder = true
        )

        assertEquals(ForegroundEventAction.IgnoreOwnOverlayEvent, action)
    }
}
