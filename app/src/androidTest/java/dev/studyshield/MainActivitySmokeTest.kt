package dev.studyshield

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsVoluntaryAccessibilityDisclosure() {
        composeRule.onNodeWithText("StudyShield").assertIsDisplayed()
        composeRule.onNodeWithText("Focus service").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The service compares only the foreground package name with apps and times you choose. " +
                "It does not read messages, passwords, screen text, contacts, photos, location, " +
                "browser history, or Accessibility node content."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Profiles").assertExists()
        composeRule.onNodeWithText("Target apps").assertExists()
    }
}
