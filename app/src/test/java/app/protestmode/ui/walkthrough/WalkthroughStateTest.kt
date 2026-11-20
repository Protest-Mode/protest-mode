package app.protestmode.ui.walkthrough

import android.content.Context
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalkthroughStateTest {
    private val saverScope = SaverScope { true }

    @Test
    fun notRunningUntilStarted() {
        val state = WalkthroughState(initialStep = -1)
        assertFalse(state.running)
        state.start()
        assertTrue(state.running)
        assertEquals(0, state.step)
        state.stop()
        assertFalse(state.running)
    }

    @Test
    fun targetsOnlyReportDuringTheTour() {
        val state = WalkthroughState(initialStep = -1)
        assertNull(state.activeTargets)
        state.start()
        assertSame(state.targets, state.activeTargets)
    }

    @Test
    fun theStepSurvivesARecreation() {
        val state = WalkthroughState(initialStep = 2)
        val saved = with(WalkthroughState.Saver) { saverScope.save(state) }
        assertNotNull(saved)
        assertEquals(2, WalkthroughState.Saver.restore(saved!!)!!.step)
    }

    @Test
    fun aTargetWithoutTourAddsNothing() {
        assertSame(Modifier, Modifier.walkthroughTarget(null, WalkStep.BANNER))
    }

    @Test
    fun theTourIsRememberedAsSeenOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WalkthroughStore(context)
        assertFalse(store.seen())
        store.markSeen()
        assertTrue(WalkthroughStore(context).seen())
        assertEquals(setOf("seen"), context.getSharedPreferences("walkthrough", Context.MODE_PRIVATE).all.keys)
    }
}
