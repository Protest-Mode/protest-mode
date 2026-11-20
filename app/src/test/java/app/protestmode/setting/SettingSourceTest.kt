package app.protestmode.setting

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingSourceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakeSetting(
        override val id: SettingId,
        private val available: () -> Boolean = { true },
        private val reading: () -> Reading
    ) : Setting {
        override val titleRes = R.string.setting_wifi
        override val iconRes = R.drawable.ic_wifi
        override val instructionRes = R.string.instruction_wifi
        override val whyRes = R.string.why_wifi

        override fun isAvailable(context: Context) = available()

        override fun read(probe: Probe) = reading()

        override fun guideIntents(context: Context) = emptyList<Intent>()
    }

    @Test
    fun aFailingReadShowsAsUnknownWithoutStoppingTheOthers() {
        val readings = SettingSource.readSafely(
            listOf(
                FakeSetting(SettingId.WIFI) { throw IllegalStateException("vendor bug") },
                FakeSetting(SettingId.AIRPLANE) { Reading(SettingState.ON) },
                FakeSetting(SettingId.NFC) { throw SecurityException("restricted") }
            ),
            context
        )

        assertEquals(SettingState.ON, readings.getValue(SettingId.AIRPLANE).state)
        assertEquals(SettingState.UNKNOWN, readings.getValue(SettingId.WIFI).state)
        assertEquals(SettingState.UNKNOWN, readings.getValue(SettingId.NFC).state)
    }

    @Test
    fun aFailingAvailabilityCheckKeepsTheSettingAsUnknown() {
        val readings = SettingSource.readSafely(
            listOf(
                FakeSetting(SettingId.UWB, available = {
                    throw RuntimeException("dead service")
                }) { error("no read") }
            ),
            context
        )

        assertEquals(mapOf(SettingId.UWB to SettingState.UNKNOWN), readings.mapValues { it.value.state })
    }

    @Test
    fun unavailableSettingsAreLeftOutAndTheOrderIsKept() {
        val readings = SettingSource.readSafely(
            listOf(
                FakeSetting(SettingId.NFC) { Reading(SettingState.OFF) },
                FakeSetting(SettingId.HOTSPOT, available = { false }) { Reading(SettingState.ON) },
                FakeSetting(SettingId.AIRPLANE) { Reading(SettingState.ON) }
            ),
            context
        )

        assertEquals(listOf(SettingId.AIRPLANE, SettingId.NFC).sortedBy { it.ordinal }, readings.keys.toList())
    }
}
