package app.protestmode

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingCategory
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.setting.protectiveState
import app.protestmode.ui.MainActions
import app.protestmode.ui.MainScreen
import app.protestmode.ui.MainUiState
import app.protestmode.ui.SettingRow
import app.protestmode.ui.theme.ProtestModeTheme
import app.protestmode.ui.walkthrough.rememberWalkthroughState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** On a real screen, each bubble sits next to the element it explains, never over it. */
@RunWith(AndroidJUnit4::class)
class WalkthroughDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = compose.activity.getString(id)

    private fun row(id: SettingId, protected: Boolean): SettingRow {
        val on = protected == (id.protectiveState == SettingState.ON)
        return SettingRow(id, if (on) SettingState.ON else SettingState.OFF, confirmed = false)
    }

    private val actions = MainActions({}, {}, {}, {}, {}, {}, {})

    @Test
    fun eachBubbleSitsNextToItsTarget() {
        // Tracking is protected: the setting step points at the first security setting.
        val rows = SettingId.entries.map { row(it, protected = it.category == SettingCategory.TRACKING) }
        val state = MainUiState(rows = rows, loaded = true, offerTour = true)
        compose.setContent {
            ProtestModeTheme(dynamicColor = false) {
                MainScreen(state, SnackbarHostState(), actions, rememberWalkthroughState())
            }
        }
        compose.onNodeWithText(string(R.string.walkthrough_ask_yes)).performClick()
        compose.waitForIdle()

        val banner = compose.onNodeWithText(string(R.string.status_exposed)).getBoundsInRoot()
        val first = compose.onNode(hasText(string(R.string.walkthrough_banner))).getBoundsInRoot()
        assertTrue("$first should be below $banner", first.top >= banner.bottom)

        compose.onNodeWithText(string(R.string.walkthrough_next)).performClick()
        compose.onNodeWithText(string(R.string.walkthrough_next)).performClick()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()

        val target = string(SettingCatalog[SettingId.SCREEN_LOCK].titleRes)
        val header = compose.onNode(hasText(string(R.string.category_security))).getBoundsInRoot()
        val setting = compose.onNode(hasText(target)).getBoundsInRoot()
        val third = compose.onNode(hasText(string(R.string.walkthrough_setting))).getBoundsInRoot()
        assertTrue("$setting is hidden under the header $header", setting.top >= header.bottom)
        assertTrue("$third covers $setting", third.top >= setting.bottom || third.bottom <= setting.top)
    }
}
