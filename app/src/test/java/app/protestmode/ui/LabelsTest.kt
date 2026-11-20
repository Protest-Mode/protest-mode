package app.protestmode.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.setting.protectiveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabelsTest {
    private fun exposed(id: SettingId): SettingRow {
        val unprotected = if (id.protectiveState == SettingState.ON) SettingState.OFF else SettingState.ON
        return SettingRow(id, unprotected, confirmed = false)
    }

    @Test
    fun aProtectedSettingShowsItsStateWithoutReason() {
        SettingId.entries.forEach { id ->
            val row = SettingRow(id, id.protectiveState, confirmed = false)
            assertEquals(id.name, R.string.target_ok, row.verdictRes())
            assertNull(id.name, row.whyRes())
        }
    }

    @Test
    fun aSettingToChangeShowsItsOwnActionAndReason() {
        SettingId.entries.forEach { id ->
            val row = exposed(id)
            assertEquals(id.name, SettingCatalog[id].actionRes, row.verdictRes())
            assertEquals(id.name, SettingCatalog[id].whyRes, row.whyRes())
        }
    }

    @Test
    fun actionsNameWhatToDo() {
        mapOf(
            SettingId.AIRPLANE to R.string.target_enable,
            SettingId.WIFI to R.string.target_disable,
            SettingId.ENCRYPTION to R.string.target_enable,
            SettingId.DEVELOPER_OPTIONS to R.string.target_disable,
            SettingId.SCREEN_LOCK to R.string.target_secure,
            SettingId.LOCKSCREEN_NOTIFICATIONS to R.string.target_hide,
            SettingId.SECURITY_PATCH to R.string.target_update,
            SettingId.BIOMETRICS to R.string.target_remove,
            SettingId.LOCK_DELAY to R.string.target_shorten,
            SettingId.DEVICE_NAME to R.string.target_change,
            SettingId.VOICE_ASSISTANT to R.string.target_block
        ).forEach { (id, action) -> assertEquals(id.name, action, exposed(id).verdictRes()) }
    }

    @Test
    fun everySettingHasItsOwnReason() {
        val reasons = SettingId.entries.map { SettingCatalog[it].whyRes }
        assertEquals(reasons.size, reasons.toSet().size)
    }

    @Test
    fun anUnreadableSettingAsksToBeChecked() {
        val row = SettingRow(SettingId.DEVICE_NAME, SettingState.UNKNOWN, confirmed = false)
        assertEquals(R.string.target_unknown, row.verdictRes())
        assertEquals(R.string.why_unknown, row.whyRes())
    }

    @Test
    fun aCheckedUnreadableSettingIsProtected() {
        val row = SettingRow(SettingId.DEVICE_NAME, SettingState.UNKNOWN, confirmed = true)
        assertEquals(R.string.setting_state_confirmed, row.stateRes())
        assertNull(row.whyRes())
    }
}
