package dev.studyshield.accessibility

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

data class ForegroundUsageEvent(
    val packageName: String,
    val className: String?
)

class UsageStatsForegroundReader(context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var lastForeground: ForegroundUsageEvent? = null
    private var lastQueryEndMillis: Long? = null

    fun currentForegroundApp(
        nowMillis: Long = System.currentTimeMillis(),
        lookbackMillis: Long = DEFAULT_LOOKBACK_MILLIS
    ): ForegroundUsageEvent? {
        val queryStartMillis = lastQueryEndMillis?.minus(RECENT_EVENT_OVERLAP_MILLIS)
            ?: (nowMillis - lookbackMillis)
        val events = try {
            usageStatsManager.queryEvents(
                queryStartMillis.coerceAtLeast(0L),
                nowMillis
            )
        } catch (_: SecurityException) {
            lastQueryEndMillis = null
            return null
        }
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when {
                event.isForegroundTransition() -> {
                    val packageName = event.packageName.orEmpty()
                    if (packageName.isNotBlank()) {
                        lastForeground = ForegroundUsageEvent(
                            packageName = packageName,
                            className = event.className
                        )
                    }
                }
                event.isBackgroundTransition() && event.packageName == lastForeground?.packageName -> {
                    lastForeground = null
                }
            }
        }
        lastQueryEndMillis = nowMillis
        return lastForeground
    }

    @Suppress("DEPRECATION")
    private fun UsageEvents.Event.isForegroundTransition(): Boolean {
        return eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && eventType == UsageEvents.Event.ACTIVITY_RESUMED)
    }

    @Suppress("DEPRECATION")
    private fun UsageEvents.Event.isBackgroundTransition(): Boolean {
        return eventType == UsageEvents.Event.MOVE_TO_BACKGROUND ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && eventType == UsageEvents.Event.ACTIVITY_PAUSED)
    }

    private companion object {
        const val DEFAULT_LOOKBACK_MILLIS = 6L * 60L * 60L * 1000L
        const val RECENT_EVENT_OVERLAP_MILLIS = 1_000L
    }
}
