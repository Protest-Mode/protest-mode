package app.protestmode.ui.walkthrough

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import app.protestmode.exposure.ExposureStatus
import app.protestmode.setting.SettingId
import app.protestmode.ui.MainScreen
import app.protestmode.ui.MainUiState
import app.protestmode.ui.RecordedActions
import app.protestmode.ui.exposedRow
import app.protestmode.ui.loadedState
import app.protestmode.ui.protectedRow
import app.protestmode.ui.text
import app.protestmode.ui.theme.ProtestModeTheme
import app.protestmode.ui.titleRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalkthroughUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val recorded = RecordedActions()
    private val rows = listOf(protectedRow(SettingId.AIRPLANE), exposedRow(SettingId.SCREEN_LOCK))

    private fun show(
        state: MainUiState,
        tour: WalkthroughState = WalkthroughState(initialStep = -1)
    ): WalkthroughState {
        compose.setContent {
            ProtestModeTheme(dynamicColor = false) { MainScreen(state, SnackbarHostState(), recorded.actions, tour) }
        }
        return tour
    }

    private fun exists(value: String) =
        compose.onAllNodes(hasText(value, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun tap(id: Int) = compose.onNodeWithText(text(id)).performClick()

    @Test
    fun declinedOfferIsNotShownAgain() {
        show(loadedState(rows, offerTour = true))
        tap(R.string.walkthrough_ask_no)
        assertEquals(1, recorded.toursDone)
        assertTrue(!exists("1/3"))
    }

    @Test
    fun theTourGoesThroughEachStepThenEnds() {
        show(loadedState(rows, offerTour = true))
        tap(R.string.walkthrough_ask_yes)
        assertTrue(exists("1/3"))
        ExposureStatus.entries.forEach { assertTrue(exists(text(it.titleRes()))) }
        tap(R.string.walkthrough_next)
        assertTrue(exists("2/3"))
        assertTrue(exists(text(R.string.walkthrough_labels)))
        tap(R.string.walkthrough_next)
        assertTrue(exists("3/3"))
        assertTrue(exists(text(R.string.walkthrough_setting)))
        tap(R.string.walkthrough_done)
        assertEquals(1, recorded.toursDone)
        assertTrue(!exists("3/3"))
    }

    @Test
    fun skippingEndsTheTour() {
        show(loadedState(rows, offerTour = true))
        tap(R.string.walkthrough_ask_yes)
        tap(R.string.walkthrough_skip)
        assertEquals(1, recorded.toursDone)
        assertTrue(!exists("1/3"))
    }

    @Test
    fun theBackKeyEndsTheTourWithoutLeaving() {
        val tour = show(loadedState(rows))
        compose.runOnIdle { tour.start() }
        assertTrue(exists("1/3"))
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(1, recorded.toursDone)
        assertTrue(!exists("1/3"))
        assertTrue(!compose.activity.isFinishing)
    }

    @Test
    fun theSettingStepIsLeftOutWhenEverythingIsProtected() {
        val tour = show(loadedState(listOf(protectedRow(SettingId.AIRPLANE))))
        compose.runOnIdle { tour.start() }
        assertTrue(exists("1/2"))
    }

    @Test
    fun aReplayStartsAtTheFirstStepWithoutAsking() {
        val tour = show(loadedState(rows, offerTour = false))
        assertTrue(!exists(text(R.string.walkthrough_ask_title)))
        compose.runOnIdle { tour.start() }
        assertTrue(exists("1/3"))
        assertTrue(!exists(text(R.string.walkthrough_ask_title)))
    }
}
