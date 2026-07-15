package dev.studyshield.storage

import dev.studyshield.domain.BackgroundMode
import dev.studyshield.domain.BubbleAlignment
import dev.studyshield.domain.BubbleAnimation
import dev.studyshield.domain.CompanionPack
import dev.studyshield.domain.CompanionSide
import dev.studyshield.domain.DialogueCue
import dev.studyshield.domain.FocusProfile
import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ReminderIntensity
import dev.studyshield.domain.ReminderScene
import dev.studyshield.domain.ResolvedSide
import dev.studyshield.domain.ScheduleRule
import dev.studyshield.domain.TargetApp
import dev.studyshield.domain.TimeRange
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

fun FocusProfileEntity.toDomain(
    schedules: List<ScheduleRuleEntity>,
    targets: List<TargetAppEntity>
): FocusProfile {
    return FocusProfile(
        id = id,
        name = name,
        enabled = enabled,
        intensity = enumValueOf(intensity),
        companionPackId = companionPackId,
        schedules = schedules.map { it.toDomain() },
        targetApps = targets.map { it.toDomain() },
        layout = toLayout()
    )
}

fun CompanionPackEntity.toDomain(): CompanionPack {
    return CompanionPack(
        id = id,
        slug = slug,
        name = name,
        author = author,
        license = license,
        language = language,
        contentRating = contentRating,
        compatibleVersion = compatibleVersion,
        characterImageUri = characterImageUri,
        wallpaperUri = wallpaperUri,
        layout = toLayout()
    )
}

fun DialogueCueEntity.toDomain(): DialogueCue {
    return DialogueCue(
        id = id,
        packId = packId,
        scene = enumValueOf(scene),
        text = text,
        audioUri = audioUri,
        transcript = transcript
    )
}

fun ScheduleRuleEntity.toDomain(): ScheduleRule {
    return ScheduleRule(
        id = id,
        profileId = profileId,
        days = daysCsv.split(",").filter { it.isNotBlank() }.map { DayOfWeek.of(it.toInt()) }.toSet(),
        range = TimeRange(LocalTime.of(startMinute / 60, startMinute % 60), LocalTime.of(endMinute / 60, endMinute % 60)),
        zoneId = ZoneId.of(zoneId)
    )
}

fun TargetAppEntity.toDomain(): TargetApp {
    return TargetApp(packageName = packageName, label = label, enabled = enabled)
}

fun FocusProfile.toEntity(): FocusProfileEntity {
    return FocusProfileEntity(
        id = id,
        name = name,
        enabled = enabled,
        intensity = intensity.name,
        companionPackId = companionPackId,
        sidePreference = layout.sidePreference.name,
        packDefaultSide = layout.packDefaultSide.name,
        characterAnchorX = layout.characterAnchorX,
        characterAnchorY = layout.characterAnchorY,
        characterScale = layout.characterScale,
        characterOpacity = layout.characterOpacity,
        bubbleAnchorX = layout.bubbleAnchorX,
        bubbleAnchorY = layout.bubbleAnchorY,
        bubbleAlignment = layout.bubbleAlignment.name,
        animation = layout.animation.name,
        backgroundMode = layout.backgroundMode.name,
        overlayDim = layout.overlayDim,
        wallpaperScale = layout.wallpaperScale,
        wallpaperOffsetX = layout.wallpaperOffsetX,
        wallpaperOffsetY = layout.wallpaperOffsetY,
        reduceMotion = layout.reduceMotion
    )
}

fun ScheduleRule.toEntity(profileId: Long): ScheduleRuleEntity {
    return ScheduleRuleEntity(
        id = id,
        profileId = profileId,
        daysCsv = days.sorted().joinToString(",") { it.value.toString() },
        startMinute = range.start.hour * 60 + range.start.minute,
        endMinute = range.end.hour * 60 + range.end.minute,
        zoneId = zoneId.id
    )
}

fun TargetApp.toEntity(profileId: Long): TargetAppEntity {
    return TargetAppEntity(
        profileId = profileId,
        packageName = packageName,
        label = label,
        enabled = enabled
    )
}

fun CompanionPack.toEntity(): CompanionPackEntity {
    return CompanionPackEntity(
        id = id,
        slug = slug,
        name = name,
        author = author,
        license = license,
        language = language,
        contentRating = contentRating,
        compatibleVersion = compatibleVersion,
        characterImageUri = characterImageUri,
        wallpaperUri = wallpaperUri,
        sidePreference = layout.sidePreference.name,
        packDefaultSide = layout.packDefaultSide.name,
        characterAnchorX = layout.characterAnchorX,
        characterAnchorY = layout.characterAnchorY,
        characterScale = layout.characterScale,
        characterOpacity = layout.characterOpacity,
        bubbleAnchorX = layout.bubbleAnchorX,
        bubbleAnchorY = layout.bubbleAnchorY,
        bubbleAlignment = layout.bubbleAlignment.name,
        animation = layout.animation.name,
        backgroundMode = layout.backgroundMode.name,
        overlayDim = layout.overlayDim,
        wallpaperScale = layout.wallpaperScale,
        wallpaperOffsetX = layout.wallpaperOffsetX,
        wallpaperOffsetY = layout.wallpaperOffsetY,
        reduceMotion = layout.reduceMotion
    )
}

fun DialogueCue.toEntity(packId: Long): DialogueCueEntity {
    return DialogueCueEntity(
        id = id,
        packId = packId,
        scene = scene.name,
        text = text,
        audioUri = audioUri,
        transcript = transcript
    )
}

private fun FocusProfileEntity.toLayout(): OverlayLayout {
    return OverlayLayout(
        sidePreference = enumValueOf(sidePreference),
        packDefaultSide = enumValueOf(packDefaultSide),
        characterAnchorX = characterAnchorX,
        characterAnchorY = characterAnchorY,
        characterScale = characterScale,
        characterOpacity = characterOpacity,
        bubbleAnchorX = bubbleAnchorX,
        bubbleAnchorY = bubbleAnchorY,
        bubbleAlignment = enumValueOf(bubbleAlignment),
        animation = enumValueOf(animation),
        backgroundMode = enumValueOf(backgroundMode),
        overlayDim = overlayDim,
        wallpaperScale = wallpaperScale,
        wallpaperOffsetX = wallpaperOffsetX,
        wallpaperOffsetY = wallpaperOffsetY,
        reduceMotion = reduceMotion
    )
}

private fun CompanionPackEntity.toLayout(): OverlayLayout {
    return OverlayLayout(
        sidePreference = enumValueOf(sidePreference),
        packDefaultSide = enumValueOf(packDefaultSide),
        characterAnchorX = characterAnchorX,
        characterAnchorY = characterAnchorY,
        characterScale = characterScale,
        characterOpacity = characterOpacity,
        bubbleAnchorX = bubbleAnchorX,
        bubbleAnchorY = bubbleAnchorY,
        bubbleAlignment = enumValueOf(bubbleAlignment),
        animation = enumValueOf(animation),
        backgroundMode = enumValueOf(backgroundMode),
        overlayDim = overlayDim,
        wallpaperScale = wallpaperScale,
        wallpaperOffsetX = wallpaperOffsetX,
        wallpaperOffsetY = wallpaperOffsetY,
        reduceMotion = reduceMotion
    )
}

fun sampleProfile(): FocusProfile {
    return FocusProfile(
        name = "Self-study",
        enabled = false,
        intensity = ReminderIntensity.Gentle,
        companionPackId = null,
        schedules = listOf(
            ScheduleRule(
                days = DayOfWeek.entries.toSet(),
                range = TimeRange(LocalTime.of(18, 0), LocalTime.of(21, 0))
            )
        ),
        targetApps = emptyList(),
        layout = OverlayLayout(
            sidePreference = CompanionSide.PackDefault,
            packDefaultSide = ResolvedSide.Right,
            bubbleAlignment = BubbleAlignment.Start,
            animation = BubbleAnimation.Fade,
            backgroundMode = BackgroundMode.Solid
        )
    )
}

fun samplePack(): CompanionPack {
    return CompanionPack(
        slug = "study-guide",
        name = "Study Guide",
        author = "StudyShield project",
        license = "CC0-1.0",
        language = "en",
        contentRating = "general",
        compatibleVersion = 1,
        layout = OverlayLayout(
            sidePreference = CompanionSide.PackDefault,
            packDefaultSide = ResolvedSide.Right,
            characterAnchorX = 0.78f,
            characterAnchorY = 0.62f,
            bubbleAnchorX = 0.22f,
            bubbleAnchorY = 0.30f
        )
    )
}

fun sampleDialogue(packId: Long): List<DialogueCueEntity> {
    return listOf(
        DialogueCue(
            packId = packId,
            scene = ReminderScene.Triggered,
            text = "This is your study window. Take one breath before continuing.",
            transcript = "This is your study window. Take one breath before continuing."
        ).toEntity(packId),
        DialogueCue(
            packId = packId,
            scene = ReminderScene.FirstSkip,
            text = "Paused for five minutes. This record stays local.",
            transcript = "Paused for five minutes. This record stays local."
        ).toEntity(packId),
        DialogueCue(
            packId = packId,
            scene = ReminderScene.SessionEnded,
            text = "Focus session ended. Your local record is ready.",
            transcript = "Focus session ended. Your local record is ready."
        ).toEntity(packId)
    )
}
