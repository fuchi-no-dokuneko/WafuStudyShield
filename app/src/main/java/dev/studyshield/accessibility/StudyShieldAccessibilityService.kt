package dev.studyshield.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import dev.studyshield.StudyShieldApplication
import dev.studyshield.UsageAccessStatus
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration

class StudyShieldAccessibilityService : AccessibilityService() {
    private val pauseDuration = Duration.ofMinutes(5)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: StudyShieldRepository
    private lateinit var overlayController: StudyShieldOverlayController
    private lateinit var ruleEngine: RuleEngine
    private lateinit var usageAccessStatus: UsageAccessStatus
    private lateinit var usageStatsForegroundReader: UsageStatsForegroundReader
    private var skipList = SkipList.empty()
    private var currentReminder: ActiveReminder? = null

    override fun onCreate() {
        super.onCreate()
        val application = application as StudyShieldApplication
        repository = application.repository
        overlayController = StudyShieldOverlayController(this, PromptAudioPlayer(this))
        usageAccessStatus = UsageAccessStatus(this)
        usageStatsForegroundReader = UsageStatsForegroundReader(this)
        ruleEngine = RuleEngine(
            clock = Clock.systemDefaultZone(),
            excludedPackages = SensitivePackagePolicy.excludedPackages(packageName)
        )
        startUsageStatsPolling()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        handleForegroundCandidate(
            packageName = event.packageName?.toString(),
            className = event.className?.toString()
        )
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

    private fun startUsageStatsPolling() {
        scope.launch {
            while (isActive) {
                delay(FOREGROUND_POLL_INTERVAL_MILLIS)
                if (!usageAccessStatus.isUsageAccessEnabled()) continue
                val foreground = withContext(Dispatchers.Default) {
                    usageStatsForegroundReader.currentForegroundApp()
                } ?: continue
                handleForegroundCandidate(
                    packageName = foreground.packageName,
                    className = foreground.className
                )
            }
        }
    }

    private fun handleForegroundCandidate(packageName: String?, className: String?) {
        when (
            ForegroundEventPolicy.actionFor(
                packageName = packageName,
                className = className,
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

    private fun handleOutcome(reminder: ActiveReminder, outcome: UserOutcome) {
        overlayController.hide()
        currentReminder = null
        val now = Clock.systemDefaultZone().instant()
        if (outcome == UserOutcome.PausedFiveMinutes) {
            skipList = skipList.plus(reminder.targetApp.packageName, now.plus(pauseDuration))
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

    private companion object {
        const val FOREGROUND_POLL_INTERVAL_MILLIS = 5_000L
    }
}
