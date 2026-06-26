package dev.studyshield.companion

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class BundledCompanionPackArchiveTest {
    @Test
    fun codexCatGirlPackageContainsManifestImageAndMp3() {
        val packageFile = assetFile(
            "companion_packs/codex_cat_girl/codex-cat-girl.studyshield-pack.zip"
        )

        ZipFile(packageFile).use { zip ->
            val manifestEntry = zip.getEntry("manifest.json")
            val characterEntry = zip.getEntry("assets/character.png")
            val voiceEntry = zip.getEntry("assets/voice.mp3")

            assertNotNull(manifestEntry)
            assertNotNull(characterEntry)
            assertNotNull(voiceEntry)

            val manifest = JSONObject(zip.getInputStream(manifestEntry).readBytes().toString(Charsets.UTF_8))
            assertEquals("codex-cat-girl", manifest.getString("slug"))
            assertEquals("zh-Hant", manifest.getString("language"))
            assertEquals("assets/character.png", manifest.getString("characterImageUri"))

            val triggeredCue = manifest.getJSONArray("dialogue").getJSONObject(0)
            assertEquals("Triggered", triggeredCue.getString("scene"))
            assertEquals("assets/voice.mp3", triggeredCue.getString("audioUri"))
            assertTrue(triggeredCue.getString("text").contains("該讀書囉"))

            val pngBytes = zip.getInputStream(characterEntry).readBytes()
            assertArrayEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), pngBytes.take(4).toByteArray())

            val mp3Bytes = zip.getInputStream(voiceEntry).readBytes()
            assertTrue(mp3Bytes.size > 1024)
            assertTrue(mp3Bytes[0] == 0xFF.toByte() || mp3Bytes.take(3).toByteArray().contentEquals(byteArrayOf(0x49, 0x44, 0x33)))
        }
    }

    private fun assetFile(path: String): File {
        return sequenceOf(
            File("src/main/assets", path),
            File("app/src/main/assets", path)
        ).first { it.isFile }
    }
}
