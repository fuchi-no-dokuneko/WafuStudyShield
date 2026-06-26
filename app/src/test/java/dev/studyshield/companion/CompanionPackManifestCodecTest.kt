package dev.studyshield.companion

import dev.studyshield.domain.CompanionPack
import dev.studyshield.domain.DialogueCue
import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ReminderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPackManifestCodecTest {
    @Test
    fun exportedManifestRoundTripsThroughValidator() {
        val bundle = CompanionPackBundle(
            pack = CompanionPack(
                slug = "study-guide",
                name = "Study Guide",
                author = "StudyShield project",
                license = "CC0-1.0",
                language = "en",
                contentRating = "general",
                compatibleVersion = 1,
                characterImageUri = "content://local/character.webp",
                wallpaperUri = "content://local/wallpaper.jpg",
                layout = OverlayLayout(wallpaperScale = 1.5f)
            ),
            dialogue = listOf(
                DialogueCue(
                    scene = ReminderScene.Triggered,
                    text = "Return to the study plan.",
                    audioUri = "content://local/voice.mp3",
                    transcript = "Return to the study plan."
                )
            )
        )

        val manifest = CompanionPackManifestCodec().encode(bundle)
        val result = CompanionPackValidator().validate(manifest)

        assertTrue(result is CompanionPackValidationResult.Valid)
        val valid = result as CompanionPackValidationResult.Valid
        assertEquals("study-guide", valid.bundle.pack.slug)
        assertEquals("content://local/character.webp", valid.bundle.pack.characterImageUri)
        assertEquals("content://local/wallpaper.jpg", valid.bundle.pack.wallpaperUri)
        assertEquals("content://local/voice.mp3", valid.bundle.dialogue.first().audioUri)
        assertEquals(1.5f, valid.bundle.pack.layout.wallpaperScale, 0.0001f)
    }
}
