package dev.studyshield.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

class RuleEngineTest {
    private val zone = ZoneId.of("UTC")
    private val clock = Clock.fixed(Instant.parse("2026-06-20T19:00:00Z"), zone)

    @Test
    fun overnightRuleMatchesAfterMidnightFromPreviousSelectedDay() {
        val rule = ScheduleRule(
            days = setOf(DayOfWeek.FRIDAY),
            range = TimeRange(LocalTime.of(22, 0), LocalTime.of(2, 0)),
            zoneId = zone
        )

        val active = rule.isActiveAt(Instant.parse("2026-06-20T01:30:00Z"))

        assertEquals(true, active)
    }

    @Test
    fun overlappingProfilesChooseStrictestIntensity() {
        val gentle = profile(
            id = 1,
            intensity = ReminderIntensity.Gentle,
            packageName = "com.social.app"
        )
        val strict = profile(
            id = 2,
            intensity = ReminderIntensity.Strict,
            packageName = "com.social.app"
        )
        val engine = RuleEngine(clock, excludedPackages = emptySet(), random = Random(1))

        val reminder = engine.evaluate("com.social.app", listOf(gentle, strict))

        assertEquals(2L, reminder?.profile?.id)
    }

    @Test
    fun profileCanMatchAnyOfMultipleTimeRanges() {
        val morning = ScheduleRule(
            days = setOf(DayOfWeek.SATURDAY),
            range = TimeRange(LocalTime.of(8, 0), LocalTime.of(10, 0)),
            zoneId = zone
        )
        val evening = ScheduleRule(
            days = setOf(DayOfWeek.SATURDAY),
            range = TimeRange(LocalTime.of(18, 0), LocalTime.of(21, 0)),
            zoneId = zone
        )
        val profile = profile(packageName = "com.social.app").copy(schedules = listOf(morning, evening))
        val engine = RuleEngine(clock, excludedPackages = emptySet())

        val reminder = engine.evaluate("com.social.app", listOf(profile))

        assertEquals(evening, reminder?.matchedSchedule)
    }

    @Test
    fun excludedPackageDoesNotTrigger() {
        val engine = RuleEngine(clock, excludedPackages = setOf("com.android.settings"))

        val reminder = engine.evaluate(
            foregroundPackage = "com.android.settings",
            profiles = listOf(profile(packageName = "com.android.settings"))
        )

        assertNull(reminder)
    }

    @Test
    fun skippedPackageDoesNotTriggerBeforeExpiry() {
        val engine = RuleEngine(clock, excludedPackages = emptySet())
        val skipList = SkipList.empty().plus("com.social.app", clock.instant().plusSeconds(60))

        val reminder = engine.evaluate(
            foregroundPackage = "com.social.app",
            profiles = listOf(profile(packageName = "com.social.app")),
            skipList = skipList
        )

        assertNull(reminder)
    }

    @Test
    fun plusAllSkipsEveryEnabledTargetWhenEndingFocusSession() {
        val skipList = SkipList.empty().plusAll(
            listOf("com.social.app", "com.video.app", "com.social.app"),
            clock.instant().plusSeconds(60)
        )

        assertEquals(true, skipList.isSkipped("com.social.app", clock.instant()))
        assertEquals(true, skipList.isSkipped("com.video.app", clock.instant()))
        assertEquals(false, skipList.isSkipped("com.notes.app", clock.instant()))
    }

    private fun profile(
        id: Long = 1,
        intensity: ReminderIntensity = ReminderIntensity.Gentle,
        packageName: String
    ): FocusProfile {
        return FocusProfile(
            id = id,
            name = "Study",
            enabled = true,
            intensity = intensity,
            companionPackId = null,
            schedules = listOf(
                ScheduleRule(
                    days = setOf(DayOfWeek.SATURDAY),
                    range = TimeRange(LocalTime.of(18, 0), LocalTime.of(21, 0)),
                    zoneId = zone
                )
            ),
            targetApps = listOf(TargetApp(packageName, "Social", enabled = true)),
            layout = OverlayLayout()
        )
    }
}
