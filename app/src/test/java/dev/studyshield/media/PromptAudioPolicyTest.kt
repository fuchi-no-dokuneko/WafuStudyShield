package dev.studyshield.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAudioPolicyTest {
    @Test
    fun fallsBackWhenAudioIsMissing() {
        val reason = PromptAudioPolicy.fallbackReason(
            audioUri = null,
            mediaVolume = 5,
            soundMode = DeviceSoundMode.Normal
        )

        assertEquals("No audio configured.", reason)
    }

    @Test
    fun fallsBackWhenMediaVolumeIsZero() {
        val reason = PromptAudioPolicy.fallbackReason(
            audioUri = "content://local/voice.mp3",
            mediaVolume = 0,
            soundMode = DeviceSoundMode.Normal
        )

        assertEquals("Media volume is zero.", reason)
    }

    @Test
    fun fallsBackWhenDeviceIsSilentOrVibrate() {
        val silent = PromptAudioPolicy.fallbackReason(
            audioUri = "content://local/voice.mp3",
            mediaVolume = 5,
            soundMode = DeviceSoundMode.Silent
        )
        val vibrate = PromptAudioPolicy.fallbackReason(
            audioUri = "content://local/voice.mp3",
            mediaVolume = 5,
            soundMode = DeviceSoundMode.Vibrate
        )

        assertEquals("Device is in silent mode.", silent)
        assertEquals("Device is in vibrate mode.", vibrate)
    }

    @Test
    fun fallsBackForRemoteAudioUris() {
        val reason = PromptAudioPolicy.fallbackReason(
            audioUri = "https://example.invalid/voice.mp3",
            mediaVolume = 5,
            soundMode = DeviceSoundMode.Normal
        )

        assertEquals("Only local audio files are supported.", reason)
    }

    @Test
    fun allowsSupportedLocalAudioUris() {
        assertNull(PromptAudioPolicy.fallbackReason("content://local/voice.mp3", 5, DeviceSoundMode.Normal))
        assertNull(PromptAudioPolicy.fallbackReason("file:///sdcard/Download/voice.mp3", 5, DeviceSoundMode.Normal))
        assertNull(PromptAudioPolicy.fallbackReason("android.resource://dev.studyshield/raw/voice", 5, DeviceSoundMode.Normal))
    }

    @Test
    fun localAudioSchemesAreCaseInsensitive() {
        assertTrue(PromptAudioPolicy.isSupportedLocalAudioUri("CONTENT://local/voice.mp3"))
    }
}
