package app.protestmode.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import app.protestmode.disguise.Disguise
import app.protestmode.ui.theme.ProtestModeTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    @Before
    fun show() {
        compose.setContent {
            ProtestModeTheme(dynamicColor = false) {
                AdvancedScreen(
                    disguise = Disguise.ORIGINAL,
                    snackbar = SnackbarHostState(),
                    onDisguiseSelected = { calls += "disguise:$it" },
                    onBack = { calls += "back" },
                    onTour = { calls += "tour" },
                    onContact = { calls += "contact" },
                    onContribute = { calls += "contribute" }
                )
            }
        }
    }

    private fun tap(id: Int) = compose.onNodeWithText(text(id)).performClick()

    @Test
    fun listsTheFourEntriesWithTheirSummary() {
        listOf(
            R.string.advanced_hide,
            R.string.advanced_hide_summary,
            R.string.advanced_tour,
            R.string.advanced_tour_summary,
            R.string.advanced_contact,
            R.string.advanced_contact_summary,
            R.string.advanced_contribute,
            R.string.advanced_contribute_summary
        ).forEach { compose.onNodeWithText(text(it)).assertExists() }
    }

    @Test
    fun hidingTheAppOpensTheDisguiseChoice() {
        tap(R.string.advanced_hide)
        compose.onNodeWithText(text(R.string.disguise_title)).assertExists()
        compose.onNodeWithText(text(R.string.disguise_weather)).performClick()
        tap(R.string.disguise_apply)
        assertEquals(listOf("disguise:${Disguise.WEATHER}"), calls)
    }

    @Test
    fun eachEntryCallsItsAction() {
        tap(R.string.advanced_tour)
        tap(R.string.advanced_contact)
        tap(R.string.advanced_contribute)
        assertEquals(listOf("tour", "contact", "contribute"), calls)
    }

    @Test
    fun theArrowAndTheBackKeyGoBack() {
        compose.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(listOf("back", "back"), calls)
    }
}
