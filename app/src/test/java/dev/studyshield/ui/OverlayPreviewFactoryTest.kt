package dev.studyshield.ui

import dev.studyshield.InstalledAppInfo
import dev.studyshield.domain.CompanionSide
import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ResolvedSide
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPreviewFactoryTest {
    @Test
    fun previewUsesSelectedTargetAppLabelWhenAvailable() {
        val reminder = previewReminder(
            MainUiState(
                installedApps = listOf(InstalledAppInfo("com.video.app", "Video App")),
                editor = ProfileEditorState(selectedPackages = setOf("com.video.app"))
            )
        )

        assertEquals("com.video.app", reminder.targetApp.packageName)
        assertEquals("Video App", reminder.targetApp.label)
    }

    @Test
    fun previewUsesFallbackTargetWhenNoAppSelected() {
        val reminder = previewReminder(MainUiState())

        assertEquals("dev.studyshield.preview", reminder.targetApp.packageName)
        assertEquals("Preview app", reminder.targetApp.label)
    }

    @Test
    fun randomPreviewSideUsesPackDefaultToAvoidMovingDuringPreview() {
        val reminder = previewReminder(
            MainUiState(
                editor = ProfileEditorState(
                    layout = OverlayLayout(
                        sidePreference = CompanionSide.RandomPerEvent,
                        packDefaultSide = ResolvedSide.Left
                    )
                )
            )
        )

        assertEquals(ResolvedSide.Left, reminder.resolvedSide)
    }
}
