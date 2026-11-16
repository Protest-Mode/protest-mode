package app.protestmode.ui

import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingRowTest {
    @Test
    fun theStatusFollowsTheStateAndTheConfirmation() {
        assertEquals(RowStatus.PROTECTED, SettingRow(SettingId.WIFI, SettingState.OFF, confirmed = false).status)
        assertEquals(RowStatus.EXPOSED, SettingRow(SettingId.WIFI, SettingState.ON, confirmed = false).status)
        assertEquals(RowStatus.TO_CHECK, SettingRow(SettingId.UWB, SettingState.UNKNOWN, confirmed = false).status)
        assertEquals(RowStatus.PROTECTED, SettingRow(SettingId.UWB, SettingState.UNKNOWN, confirmed = true).status)
        assertEquals(RowStatus.EXPOSED, SettingRow(SettingId.AIRPLANE, SettingState.OFF, confirmed = false).status)
    }
}
