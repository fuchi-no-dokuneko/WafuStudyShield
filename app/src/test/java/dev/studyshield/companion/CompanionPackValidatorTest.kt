package dev.studyshield.companion

import dev.studyshield.domain.ReminderScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPackValidatorTest {
    @Test
    fun acceptsRedistributablePackWithTriggeredCue() {
        val json = """
            {
              "slug": "study-guide",
              "name": "Study Guide",
              "author": "StudyShield project",
              "license": "CC0-1.0",
              "language": "en",
              "contentRating": "general",
              "compatibleVersion": 1,
              "characterImageUri": "content://local/character.webp",
              "wallpaperUri": "content://local/wallpaper.jpg",
              "layout": {
                "wallpaperScale": 1.4,
                "wallpaperOffsetX": 0.2,
                "wallpaperOffsetY": -0.1
              },
              "dialogue": [
                {
                  "scene": "Triggered",
                  "text": "Take one breath and return to the worksheet.",
                  "transcript": "Take one breath and return to the worksheet."
                }
              ]
            }
        """.trimIndent()

        val result = CompanionPackValidator().validate(json)

        assertTrue(result is CompanionPackValidationResult.Valid)
        val valid = result as CompanionPackValidationResult.Valid
        assertEquals("study-guide", valid.bundle.pack.slug)
        assertEquals("content://local/character.webp", valid.bundle.pack.characterImageUri)
        assertEquals("content://local/wallpaper.jpg", valid.bundle.pack.wallpaperUri)
        assertEquals(1.4f, valid.bundle.pack.layout.wallpaperScale, 0.0001f)
        assertEquals(ReminderScene.Triggered, valid.bundle.dialogue.first().scene)
    }

    @Test
    fun rejectsAudioCueWithoutTranscriptAndAdultRating() {
        val json = """
            {
              "slug": "bad-pack",
              "name": "Bad Pack",
              "author": "Unknown",
              "license": "unknown",
              "language": "en",
              "contentRating": "adult",
              "compatibleVersion": 1,
              "dialogue": [
                {
                  "scene": "Triggered",
                  "text": "Feel shame if you stop.",
                  "audioUri": "file:///voice.mp3",
                  "transcript": ""
                }
              ]
            }
        """.trimIndent()

        val result = CompanionPackValidator().validate(json)

        assertTrue(result is CompanionPackValidationResult.Invalid)
        val errors = (result as CompanionPackValidationResult.Invalid).errors.joinToString("\n")
        assertTrue(errors.contains("contentRating"))
        assertTrue(errors.contains("license"))
        assertTrue(errors.contains("transcript"))
        assertTrue(errors.contains("coercive"))
    }

    @Test
    fun rejectsInvalidLayoutWithoutThrowing() {
        val json = """
            {
              "slug": "bad-layout",
              "name": "Bad Layout",
              "author": "StudyShield project",
              "license": "CC0-1.0",
              "language": "en",
              "contentRating": "general",
              "compatibleVersion": 1,
              "layout": {
                "characterAnchorX": 2.0,
                "bubbleAnchorY": -0.5,
                "wallpaperScale": 0.5
              },
              "dialogue": [
                {
                  "scene": "Triggered",
                  "text": "Return to the study plan.",
                  "transcript": "Return to the study plan."
                }
              ]
            }
        """.trimIndent()

        val result = CompanionPackValidator().validate(json)

        assertTrue(result is CompanionPackValidationResult.Invalid)
        val errors = (result as CompanionPackValidationResult.Invalid).errors.joinToString("\n")
        assertTrue(errors.contains("layout is invalid"))
    }

    @Test
    fun rejectsRemoteAudioUri() {
        val json = """
            {
              "slug": "remote-audio",
              "name": "Remote Audio",
              "author": "StudyShield project",
              "license": "CC0-1.0",
              "language": "en",
              "contentRating": "general",
              "compatibleVersion": 1,
              "dialogue": [
                {
                  "scene": "Triggered",
                  "text": "Return to the study plan.",
                  "audioUri": "https://example.invalid/voice.mp3",
                  "transcript": "Return to the study plan."
                }
              ]
            }
        """.trimIndent()

        val result = CompanionPackValidator().validate(json)

        assertTrue(result is CompanionPackValidationResult.Invalid)
        val errors = (result as CompanionPackValidationResult.Invalid).errors.joinToString("\n")
        assertTrue(errors.contains("audioUri"))
        assertTrue(errors.contains("local"))
    }
}
