package com.gamyam.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.gamyam.app.theme.GamyamTheme
import org.junit.Rule
import org.junit.Test

/** Smoke tests for [MainScreen]. */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainScreen_showsFindBusesButton() {
        composeTestRule.setContent {
            GamyamTheme { MainScreen(onItemClick = {}) }
        }
        composeTestRule.onNodeWithText("Find buses").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsAllThreeTabs() {
        composeTestRule.setContent {
            GamyamTheme { MainScreen(onItemClick = {}) }
        }
        composeTestRule.onNodeWithText("DISTRICT").assertIsDisplayed()
        composeTestRule.onNodeWithText("CITY").assertIsDisplayed()
        composeTestRule.onNodeWithText("AIRPORT").assertIsDisplayed()
    }
}
