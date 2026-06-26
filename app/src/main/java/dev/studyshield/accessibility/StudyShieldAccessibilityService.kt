package dev.studyshield.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import dev.studyshield.StudyShieldApplication
import dev.studyshield.domain.ActiveReminder
import dev.studyshield.domain.RuleEngine
import dev.studyshield.domain.SkipList
import dev.studyshield.domain.UserOutcome
import dev.studyshield.media.PromptAudioPlayer
import dev.studyshield.overlay.StudyShieldOverlayController
import dev.studyshield.storage.StudyShieldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration

class StudyShieldAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: StudyShieldRepository
    private lateinit var overlayController: StudyShieldOverlayController
    private lateinit var ruleEngine: RuleEngine
    private var skipList = SkipList.empty()
    private var currentReminder: ActiveReminder? = null

    override fun onCreate() {
        super.onCreate()
        val application = application as StudyShieldApplication
        repository = application.repository
        overlayController = StudyShieldOverlayController(this, PromptAudioPlayer(this))
        ruleEngine = RuleEngine(
            clock = Clock.systemDefaultZone(),
            excludedPackages = SensitivePackagePolicy.excludedPackages(packageName)
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString()
        when (
            ForegroundEventPolicy.actionFor(
                packageName = packageName,
                className = event.className?.toString(),
                ownPackageName = this.packageName,
                hasCurrentReminder = currentReminder != null
            )
        ) {
            ForegroundEventAction.HideCurrentReminder -> {
                overlayController.hide()
                currentReminder = null
                return
            }
            ForegroundEventAction.IgnoreOwnOverlayEvent -> return
            ForegroundEventAction.Evaluate -> Unit
        }
        scope.launch {
            val reminder = ruleEngine.evaluate(
                foregroundPackage = packageName,
                profiles = repository.listProfiles(),
                skipList = skipList
            )
            if (reminder == null) {
                if (currentReminder != null) {
                    overlayController.hide()
                    currentReminder = null
                }
                return@launch
            }

            if (currentReminder?.targetApp?.packageName == reminder.targetApp.packageName &&
                currentReminder?.profile?.id == reminder.profile.id
            ) {
                return@launch
            }

            val reminderWithPack = reminder.copy(
                companionPack = repository.companionPack(reminder.profile.companionPackId)
            )
            currentReminder = reminderWithPack
            val cue = repository.firstDialogueFor(reminderWithPack)
            overlayController.show(reminderWithPack, cue) { outcome ->
                handleOutcome(reminder, outcome)
            }
        }
    }

    override fun onInterrupt() {
        overlayController.hide()
        currentReminder = null
    }

    override fun onDestroy() {
        overlayController.hide()
        scope.cancel()
        super.onDestroy()
    }

    private fun handleOutcome(reminder: ActiveReminder, outcome: UserOutcome) {
        overlayController.hide()
        currentReminder = null
        val now = Clock.systemDefaultZone().instant()
        if (outcome == UserOutcome.SkippedOnce) {
            skipList = skipList.plus(reminder.targetApp.packageName, now.plus(Duration.ofMinutes(5)))
        }
        if (outcome == UserOutcome.EndedFocus) {
            skipList = skipList.plusAll(
                reminder.profile.targetApps.filter { it.enabled }.map { it.packageName },
                now.plus(Duration.ofHours(12))
            )
        }
        if (outcome == UserOutcome.ReturnedHome) {
            val homeIntent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(homeIntent) }
        }
        scope.launch {
            repository.recordOutcome(reminder, outcome)
        }
    }
}
