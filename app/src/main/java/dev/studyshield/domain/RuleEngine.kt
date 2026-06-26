package dev.studyshield.domain

import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

class RuleEngine(
    private val clock: Clock,
    private val excludedPackages: Set<String>,
    private val random: Random = Random.Default
) {
    fun evaluate(
        foregroundPackage: String?,
        profiles: List<FocusProfile>,
        skipList: SkipList = SkipList.empty(),
        scene: ReminderScene = ReminderScene.Triggered,
        instant: Instant = clock.instant()
    ): ActiveReminder? {
        if (foregroundPackage.isNullOrBlank()) return null
        if (foregroundPackage in excludedPackages) return null
        if (skipList.isSkipped(foregroundPackage, instant)) return null

        val candidates = profiles
            .asSequence()
            .filter { it.enabled }
            .mapNotNull { profile ->
                val target = profile.targetApps.firstOrNull {
                    it.enabled && it.packageName == foregroundPackage
                } ?: return@mapNotNull null
                val schedule = profile.schedules.firstOrNull { it.isActiveAt(instant) }
                    ?: return@mapNotNull null
                Candidate(profile, target, schedule)
            }
            .sortedWith(
                compareByDescending<Candidate> { it.profile.intensity.priority }
                    .thenBy { it.profile.id }
            )
            .toList()

        val selected = candidates.firstOrNull() ?: return null
        return ActiveReminder(
            profile = selected.profile,
            targetApp = selected.target,
            matchedSchedule = selected.schedule,
            scene = scene,
            eventId = UUID.randomUUID().toString(),
            resolvedSide = resolveSide(selected.profile.layout)
        )
    }

    fun resolveSide(layout: OverlayLayout): ResolvedSide {
        return when (layout.sidePreference) {
            CompanionSide.Left -> ResolvedSide.Left
            CompanionSide.Right -> ResolvedSide.Right
            CompanionSide.PackDefault -> layout.packDefaultSide
            CompanionSide.RandomPerEvent -> if (random.nextBoolean()) ResolvedSide.Left else ResolvedSide.Right
        }
    }

    private data class Candidate(
        val profile: FocusProfile,
        val target: TargetApp,
        val schedule: ScheduleRule
    )

    private val ReminderIntensity.priority: Int
        get() = when (this) {
            ReminderIntensity.Gentle -> 1
            ReminderIntensity.Firm -> 2
            ReminderIntensity.Strict -> 3
        }
}

data class SkipList(
    private val skipsByPackage: Map<String, Instant>
) {
    fun isSkipped(packageName: String, instant: Instant): Boolean {
        val until = skipsByPackage[packageName] ?: return false
        return instant.isBefore(until)
    }

    fun plus(packageName: String, until: Instant): SkipList {
        return SkipList(skipsByPackage + (packageName to until))
    }

    fun plusAll(packageNames: Iterable<String>, until: Instant): SkipList {
        return SkipList(skipsByPackage + packageNames.distinct().associateWith { until })
    }

    companion object {
        fun empty(): SkipList = SkipList(emptyMap())
    }
}
