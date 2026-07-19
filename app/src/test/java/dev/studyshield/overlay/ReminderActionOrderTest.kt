package dev.studyshield.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderActionOrderTest {
    @Test
    fun disabledUsesStableDefaultOrder() {
        val order = ReminderActionOrder.resolve(randomize = false) { true }

        assertEquals(
            listOf(ReminderAction.ReturnHome, ReminderAction.PauseFiveMinutes),
            order
        )
    }

    @Test
    fun enabledCanSwapButtonPositions() {
        val order = ReminderActionOrder.resolve(randomize = true) { true }

        assertEquals(
            listOf(ReminderAction.PauseFiveMinutes, ReminderAction.ReturnHome),
            order
        )
    }

    @Test
    fun enabledCanKeepDefaultButtonPositions() {
        val order = ReminderActionOrder.resolve(randomize = true) { false }

        assertEquals(
            listOf(ReminderAction.ReturnHome, ReminderAction.PauseFiveMinutes),
            order
        )
    }
}
