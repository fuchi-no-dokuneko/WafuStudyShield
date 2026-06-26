package dev.studyshield.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

sealed interface PromptAudioResult {
    data object Started : PromptAudioResult
    data class TextFallback(val reason: String) : PromptAudioResult
}

class PromptAudioPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: ExoPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    fun playIfAllowed(
        audioUri: String?,
        onFallback: (String) -> Unit = {}
    ): PromptAudioResult {
        PromptAudioPolicy.fallbackReason(
            audioUri = audioUri,
            mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
            soundMode = audioManager.ringerMode.toDeviceSoundMode()
        )?.let { return PromptAudioResult.TextFallback(it) }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    onFallback("Audio focus was lost.")
                    stop()
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(request)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return PromptAudioResult.TextFallback("Audio focus was denied.")
        }

        stopPlayerOnly()
        focusRequest = request
        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer
        return try {
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.setMediaItem(MediaItem.fromUri(requireNotNull(audioUri).toUri()))
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onFallback(error.message ?: "Audio playback failed.")
                    stop()
                }
            })
            exoPlayer.prepare()
            exoPlayer.play()
            PromptAudioResult.Started
        } catch (exception: RuntimeException) {
            stop()
            PromptAudioResult.TextFallback(exception.message ?: "Audio could not be decoded.")
        }
    }

    fun stop() {
        stopPlayerOnly()
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun stopPlayerOnly() {
        player?.run {
            stop()
            release()
        }
        player = null
    }

    private fun Int.toDeviceSoundMode(): DeviceSoundMode {
        return when (this) {
            AudioManager.RINGER_MODE_NORMAL -> DeviceSoundMode.Normal
            AudioManager.RINGER_MODE_SILENT -> DeviceSoundMode.Silent
            AudioManager.RINGER_MODE_VIBRATE -> DeviceSoundMode.Vibrate
            else -> DeviceSoundMode.Unknown
        }
    }
}
