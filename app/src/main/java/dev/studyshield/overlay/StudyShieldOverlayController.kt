package dev.studyshield.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.studyshield.domain.ActiveReminder
import dev.studyshield.domain.DialogueCue
import dev.studyshield.domain.UserOutcome
import dev.studyshield.media.PromptAudioPlayer
import dev.studyshield.media.PromptAudioResult

class StudyShieldOverlayController(
    private val context: Context,
    private val audioPlayer: PromptAudioPlayer = PromptAudioPlayer(context)
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var owner: OverlayTreeOwner? = null
    private val layoutResolver = LayoutResolver()

    fun show(
        reminder: ActiveReminder,
        cue: DialogueCue?,
        onOutcome: (UserOutcome) -> Unit
    ) {
        hide()
        val text = cue?.transcript?.takeIf { it.isNotBlank() }
            ?: "This is ${reminder.profile.name}. Return to your study task when you are ready."
        val audioResult = mutableStateOf<PromptAudioResult>(
            PromptAudioResult.TextFallback("No audio configured.")
        )
        audioResult.value = audioPlayer.playIfAllowed(cue?.audioUri) { reason ->
            overlayView?.post {
                audioResult.value = PromptAudioResult.TextFallback(reason)
            }
        }
        val resolvedLayout = layoutResolver.resolve(reminder.profile.layout, reminder.resolvedSide)

        val treeOwner = OverlayTreeOwner()
        treeOwner.onCreate()
        owner = treeOwner

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(treeOwner)
            setViewTreeViewModelStoreOwner(treeOwner)
            setViewTreeSavedStateRegistryOwner(treeOwner)
            setContent {
                FocusOverlayContent(
                    reminder = reminder,
                    dialogueText = text,
                    audioResult = audioResult.value,
                    layout = resolvedLayout,
                    onReturnHome = { onOutcome(UserOutcome.ReturnedHome) },
                    onPauseFiveMinutes = { onOutcome(UserOutcome.PausedFiveMinutes) }
                )
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "StudyShield reminder"
        }

        overlayView = view
        windowManager.addView(view, params)
    }

    fun hide() {
        audioPlayer.stop()
        overlayView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        overlayView = null
        owner?.onDestroy()
        owner = null
    }
}

private class OverlayTreeOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
