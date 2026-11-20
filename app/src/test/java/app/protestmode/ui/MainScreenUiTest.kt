package app.protestmode.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.ui.theme.ProtestModeTheme
import app.protestmode.ui.walkthrough.rememberWalkthroughState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val recorded = RecordedActions()

    private fun show(state: MainUiState) = compose.setContent {
        ProtestModeTheme(dynamicColor = false) {
            MainScreen(state, SnackbarHostState(), recorded.actions, rememberWalkthroughState())
        }
    }

    private fun title(id: SettingId) = text(SettingCatalog[id].titleRes)

    private fun inDialog(value: String) = hasText(value) and hasAnyAncestor(isDialog())

    private fun hasNoNode(value: String) = compose.onAllNodes(hasText(value)).fetchSemanticsNodes().isEmpty()

    @Test
    fun nothingIsShownBeforeTheFirstReading() {
        show(MainUiState(rows = listOf(exposedRow(SettingId.WIFI))))
        assertTrue(hasNoNode(text(R.string.status_exposed)))
        assertTrue(hasNoNode(title(SettingId.WIFI)))
    }

    @Test
    fun aProtectedSettingOpensItsScreenAtOnce() {
        show(loadedState(listOf(protectedRow(SettingId.WIFI))))
        compose.onNodeWithText(title(SettingId.WIFI)).performClick()
        assertEquals(listOf(SettingId.WIFI), recorded.opened.map { it.id })
        assertTrue(hasNoNode(text(R.string.action_open_settings)))
    }

    @Test
    fun aSettingToChangeExplainsItsNumberedStepsBeforeOpeningTheScreen() {
        show(loadedState(listOf(exposedRow(SettingId.LOCK_DELAY))))
        compose.onNodeWithText(title(SettingId.LOCK_DELAY)).performClick()
        assertTrue(recorded.opened.isEmpty())
        compose.onNode(inDialog(text(R.string.setting_steps_many))).assertExists()
        compose.onNode(inDialog("3.")).assertExists()
        compose.onNode(inDialog(text(R.string.action_open_settings))).performClick()
        assertEquals(listOf(SettingId.LOCK_DELAY), recorded.opened.map { it.id })
        assertTrue(hasNoNode(text(R.string.action_open_settings)))
    }

    @Test
    fun aSingleStepIsNotNumbered() {
        show(loadedState(listOf(exposedRow(SettingId.AIRPLANE))))
        compose.onNodeWithText(title(SettingId.AIRPLANE)).performClick()
        compose.onNode(inDialog(text(R.string.setting_steps_one))).assertExists()
        assertTrue(compose.onAllNodes(inDialog("1.")).fetchSemanticsNodes().isEmpty())
        compose.onNode(inDialog(text(R.string.action_close))).performClick()
        assertTrue(recorded.opened.isEmpty())
    }

    @Test
    fun anUnreadableSettingCanBeCheckedFromItsDialog() {
        show(loadedState(listOf(SettingRow(SettingId.DEVICE_NAME, SettingState.UNKNOWN, confirmed = false))))
        compose.onNodeWithText(title(SettingId.DEVICE_NAME)).performClick()
        compose.onNode(inDialog(text(R.string.action_confirm))).performClick()
        assertEquals(listOf(SettingId.DEVICE_NAME), recorded.confirmed)
        assertTrue(compose.onAllNodes(inDialog(text(R.string.action_open_settings))).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theRiskLevelIsReadOnceForEachSetting() {
        show(loadedState(listOf(exposedRow(SettingId.SCREEN_LOCK))))
        val risk = text(R.string.risk_critical_a11y)
        val description =
            compose.onNode(hasText(title(SettingId.SCREEN_LOCK)))
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
                .joinToString(" ")
        assertEquals(description, 1, description.windowed(risk.length).count { it == risk })
    }

    @Test
    fun aCategoryHeaderReadsAsAHeadingWithItsCount() {
        show(loadedState(listOf(protectedRow(SettingId.AIRPLANE), exposedRow(SettingId.WIFI))))
        val tracking = text(R.string.category_tracking)
        val count = text(R.string.category_count_a11y, 1, 2)
        val heading = compose.onNode(isHeading() and hasText(tracking)).fetchSemanticsNode()
        val description = heading.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertTrue(description.toString(), description.any { it == "$tracking, $count" })
    }
}
