package dev.studyshield.media

enum class DeviceSoundMode {
    Normal,
    Silent,
    Vibrate,
    Unknown
}

object PromptAudioPolicy {
    fun fallbackReason(
        audioUri: String?,
        mediaVolume: Int,
        soundMode: DeviceSoundMode
    ): String? {
        val uri = audioUri?.trim().orEmpty()
        if (uri.isBlank()) return "No audio configured."
        if (mediaVolume <= 0) return "Media volume is zero."
        if (soundMode == DeviceSoundMode.Silent) return "Device is in silent mode."
        if (soundMode == DeviceSoundMode.Vibrate) return "Device is in vibrate mode."
        if (!isSupportedLocalAudioUri(uri)) return "Only local audio files are supported."
        return null
    }

    fun isSupportedLocalAudioUri(uri: String): Boolean {
        val scheme = uri.substringBefore(':', missingDelimiterValue = "")
            .trim()
            .lowercase()
        return scheme in supportedLocalSchemes
    }

    private val supportedLocalSchemes = setOf("content", "file", "android.resource")
}
