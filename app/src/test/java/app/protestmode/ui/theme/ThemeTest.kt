package app.protestmode.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ThemeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun colours(dynamicColor: Boolean): Pair<Color, StatusColors> {
        var primary = Color.Unspecified
        var status: StatusColors? = null
        compose.setContent {
            ProtestModeTheme(darkTheme = false, dynamicColor = dynamicColor) {
                primary = MaterialTheme.colorScheme.primary
                status = LocalStatusColors.current
            }
        }
        compose.waitForIdle()
        return primary to status!!
    }

    @Test
    fun fixedColoursWhenDynamicColourIsOff() {
        assertEquals(Color(0xFF1B3A4B), colours(dynamicColor = false).first)
    }

    @Test
    fun statusColoursStayFixedWithDynamicColour() {
        val status = colours(dynamicColor = true).second
        assertEquals(Color(0xFFB3261E), status.exposed)
        assertEquals(Color(0xFF1B6E20), status.discreet)
    }

    @Test
    fun theLaunchScreenTakesTheIconYellow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val theme = context.resources.newTheme().apply { applyStyle(R.style.Theme_ProtestMode, true) }
        val attributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
        val colour = attributes.getColor(0, 0)
        attributes.recycle()
        assertEquals(Color(0xFFFDD25B).toArgb(), colour)
    }
}
