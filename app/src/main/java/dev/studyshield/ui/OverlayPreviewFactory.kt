package dev.studyshield.ui

import dev.studyshield.domain.ActiveReminder
import dev.studyshield.domain.CompanionSide
import dev.studyshield.domain.FocusProfile
import dev.studyshield.domain.ReminderScene
import dev.studyshield.domain.ResolvedSide
import dev.studyshield.domain.ScheduleRule
import dev.studyshield.domain.TargetApp
import dev.studyshield.domain.TimeRange
import java.time.DayOfWeek
import java.time.LocalTime

fun previewReminder(state: MainUiState): ActiveReminder {
    val editor = state.editor
    val targetPackage = editor.selectedPackages.firstOrNull()
    val target = targetPackage?.let { packageName ->
        val app = state.installedApps.firstOrNull { it.packageName == packageName }
        TargetApp(packageName = packageName, label = app?.label ?: packageName, enabled = true)
    } ?: TargetApp(packageName = "dev.studyshield.preview", label = "Preview app", enabled = true)
    val profile = FocusProfile(
        id = editor.profileId,
        name = editor.name.ifBlank { "Preview profile" },
        enabled = true,
        intensity = editor.intensity,
        companionPackId = editor.companionPackId,
        schedules = listOf(
            ScheduleRule(
                days = DayOfWeek.entries.toSet(),
                range = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59))
            )
        ),
        targetApps = listOf(target),
        layout = editor.layout
    )
    val pack = state.companionPacks.firstOrNull { it.id == editor.companionPackId }
    return ActiveReminder(
        profile = profile,
        targetApp = target,
        matchedSchedule = profile.schedules.first(),
        scene = ReminderScene.Triggered,
        eventId = "preview",
        resolvedSide = previewSide(editor.layout.sidePreference, editor.layout.packDefaultSide),
        companionPack = pack
    )
}

fun previewSide(preference: CompanionSide, packDefault: ResolvedSide): ResolvedSide {
    return when (preference) {
        CompanionSide.Left -> ResolvedSide.Left
        CompanionSide.Right -> ResolvedSide.Right
        CompanionSide.PackDefault -> packDefault
        CompanionSide.RandomPerEvent -> packDefault
    }
}
