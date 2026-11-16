package app.protestmode.data

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.setting.Reading
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfirmationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val unknown = Reading(SettingState.UNKNOWN)
    private fun named(name: String) = Reading(SettingState.UNKNOWN, confirmationKey = name)

    @Test
    fun aComparableConfirmationSurvivesARelaunchAsAFingerprint() {
        Confirmations(context, SavedStateHandle()).confirm(SettingId.DEVICE_NAME, "Pixel de Sam")

        val relaunched = Confirmations(context, SavedStateHandle())
        assertEquals(
            setOf(SettingId.DEVICE_NAME),
            relaunched.valid(mapOf(SettingId.DEVICE_NAME to named("Pixel de Sam")))
        )
        val stored = context.getSharedPreferences(Confirmations.FILE, Context.MODE_PRIVATE).all.values.toString()
        assertFalse(stored, "Pixel de Sam" in stored)
    }

    @Test
    fun aConfirmationIsDroppedWhenTheValueChanges() {
        val confirmations = Confirmations(context, SavedStateHandle())
        confirmations.confirm(SettingId.DEVICE_NAME, "Pixel de Sam")

        assertEquals(emptySet<SettingId>(), confirmations.valid(mapOf(SettingId.DEVICE_NAME to named("Galaxy S21"))))
        assertEquals(emptySet<SettingId>(), confirmations.valid(mapOf(SettingId.DEVICE_NAME to named("Pixel de Sam"))))
    }

    @Test
    fun aConfirmationWithoutValueOnlyLastsForTheSession() {
        val session = SavedStateHandle()
        Confirmations(context, session).confirm(SettingId.UWB, null)

        assertEquals(setOf(SettingId.UWB), Confirmations(context, session).valid(mapOf(SettingId.UWB to unknown)))
        assertEquals(
            emptySet<SettingId>(),
            Confirmations(context, SavedStateHandle()).valid(
                mapOf(
                    SettingId.UWB to unknown
                )
            )
        )
    }

    @Test
    fun fingerprintsAreSha256AndEmptyWithoutValue() {
        assertEquals("", Confirmations.fingerprint(null))
        assertEquals(64, Confirmations.fingerprint("x").length)
        assertTrue(Confirmations.fingerprint("x") != Confirmations.fingerprint("y"))
    }
}
