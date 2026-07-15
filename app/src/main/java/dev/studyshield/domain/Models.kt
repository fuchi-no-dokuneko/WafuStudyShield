package dev.studyshield.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class ReminderIntensity {
    Gentle,
    Firm,
    Strict
}

enum class ReminderScene {
    Triggered,
    FirstSkip,
    SessionEnded
}

enum class UserOutcome {
    ReturnedHome,
    PausedFiveMinutes,
    DismissedByRuleChange,
    DismissedByExcludedScreen
}

enum class CompanionSide {
    Left,
    Right,
    RandomPerEvent,
    PackDefault
}

enum class ResolvedSide {
    Left,
    Right
}

enum class BubbleAlignment {
    Start,
    Center,
    End
}

enum class BubbleAnimation {
    None,
    Fade,
    FadeDown
}

enum class BackgroundMode {
    Solid,
    DimmedWallpaper
}

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime
) {
    init {
        require(start != end) { "A time range must have a non-zero duration." }
    }

    fun contains(time: LocalTime): Boolean {
        return if (start < end) {
            !time.isBefore(start) && time.isBefore(end)
        } else {
            !time.isBefore(start) || time.isBefore(end)
        }
    }
}

data class ScheduleRule(
    val id: Long = 0,
    val profileId: Long = 0,
    val days: Set<DayOfWeek>,
    val range: TimeRange,
    val zoneId: ZoneId = ZoneId.systemDefault()
) {
    init {
        require(days.isNotEmpty()) { "A schedule rule needs at least one day." }
    }

    fun isActiveAt(instant: Instant): Boolean {
        val local = LocalDateTime.ofInstant(instant, zoneId)
        val today = local.dayOfWeek
        val yesterday = today.minus(1)
        return if (range.start < range.end) {
            today in days && range.contains(local.toLocalTime())
        } else {
            (today in days && !local.toLocalTime().isBefore(range.start)) ||
                (yesterday in days && local.toLocalTime().isBefore(range.end))
        }
    }
}

data class TargetApp(
    val packageName: String,
    val label: String,
    val enabled: Boolean
)

data class OverlayLayout(
    val sidePreference: CompanionSide = CompanionSide.PackDefault,
    val packDefaultSide: ResolvedSide = ResolvedSide.Right,
    val characterAnchorX: Float = 0.78f,
    val characterAnchorY: Float = 0.62f,
    val characterScale: Float = 1.0f,
    val characterOpacity: Float = 1.0f,
    val bubbleAnchorX: Float = 0.22f,
    val bubbleAnchorY: Float = 0.30f,
    val bubbleAlignment: BubbleAlignment = BubbleAlignment.Start,
    val animation: BubbleAnimation = BubbleAnimation.Fade,
    val backgroundMode: BackgroundMode = BackgroundMode.Solid,
    val overlayDim: Float = 0.72f,
    val wallpaperScale: Float = 1.0f,
    val wallpaperOffsetX: Float = 0.0f,
    val wallpaperOffsetY: Float = 0.0f,
    val reduceMotion: Boolean = false
) {
    init {
        listOf(
            characterAnchorX,
            characterAnchorY,
            characterScale,
            characterOpacity,
            bubbleAnchorX,
            bubbleAnchorY,
            overlayDim,
            wallpaperScale,
            wallpaperOffsetX,
            wallpaperOffsetY
        ).forEach { require(it.isFinite()) { "Layout values must be finite." } }
        require(characterAnchorX in 0f..1f)
        require(characterAnchorY in 0f..1f)
        require(characterScale in 0.35f..2.5f)
        require(characterOpacity in 0f..1f)
        require(bubbleAnchorX in 0f..1f)
        require(bubbleAnchorY in 0f..1f)
        require(overlayDim in 0f..1f)
        require(wallpaperScale in 1f..3f)
        require(wallpaperOffsetX in -1f..1f)
        require(wallpaperOffsetY in -1f..1f)
    }
}

data class CompanionPack(
    val id: Long = 0,
    val slug: String,
    val name: String,
    val author: String,
    val license: String,
    val language: String,
    val contentRating: String,
    val compatibleVersion: Int,
    val characterImageUri: String? = null,
    val wallpaperUri: String? = null,
    val layout: OverlayLayout
)

data class DialogueCue(
    val id: Long = 0,
    val packId: Long = 0,
    val scene: ReminderScene,
    val text: String,
    val audioUri: String? = null,
    val transcript: String = text
) {
    init {
        require(text.isNotBlank()) { "Dialogue text cannot be blank." }
        if (audioUri != null) {
            require(transcript.isNotBlank()) { "Every audio cue requires an editable transcript." }
        }
    }
}

data class FocusProfile(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val intensity: ReminderIntensity,
    val companionPackId: Long?,
    val schedules: List<ScheduleRule>,
    val targetApps: List<TargetApp>,
    val layout: OverlayLayout
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank." }
    }
}

data class ActiveReminder(
    val profile: FocusProfile,
    val targetApp: TargetApp,
    val matchedSchedule: ScheduleRule,
    val scene: ReminderScene,
    val eventId: String,
    val resolvedSide: ResolvedSide,
    val companionPack: CompanionPack? = null
)
