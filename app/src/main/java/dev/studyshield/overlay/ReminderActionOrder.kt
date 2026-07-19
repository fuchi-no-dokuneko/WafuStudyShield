package dev.studyshield.overlay

import kotlin.random.Random

internal enum class ReminderAction {
    ReturnHome,
    PauseFiveMinutes
}

internal object ReminderActionOrder {
    val Default = listOf(ReminderAction.ReturnHome, ReminderAction.PauseFiveMinutes)

    fun resolve(
        randomize: Boolean,
        nextBoolean: () -> Boolean = Random.Default::nextBoolean
    ): List<ReminderAction> {
        if (!randomize) return Default
        return if (nextBoolean()) {
            listOf(ReminderAction.PauseFiveMinutes, ReminderAction.ReturnHome)
        } else {
            Default
        }
    }
}
