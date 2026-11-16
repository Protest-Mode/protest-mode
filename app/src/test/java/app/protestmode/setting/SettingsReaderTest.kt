package app.protestmode.setting

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsReaderTest {
    private val resolver get() = context.contentResolver

    @Test
    fun missingKeysReadAsNull() {
        assertNull(SettingsReader.global(resolver, "missing_global"))
        assertNull(SettingsReader.secure(resolver, "missing_secure"))
        assertNull(SettingsReader.secureLong(resolver, "missing_secure_long"))
        assertNull(SettingsReader.system(resolver, "missing_system"))
        assertNull(SettingsReader.globalString(resolver, "missing_string"))
    }

    @Test
    fun presentKeysAreRead() {
        Settings.Global.putInt(resolver, "test_global", 1)
        Settings.Secure.putLong(resolver, "test_secure_long", 5_000L)
        Settings.System.putInt(resolver, "test_system", 30_000)
        Settings.Global.putString(resolver, "test_string", "Galaxy S21")

        assertEquals(1, SettingsReader.global(resolver, "test_global"))
        assertEquals(5_000L, SettingsReader.secureLong(resolver, "test_secure_long"))
        assertEquals(30_000, SettingsReader.system(resolver, "test_system"))
        assertEquals("Galaxy S21", SettingsReader.globalString(resolver, "test_string"))
    }

    @Test
    fun valuesMapToStates() {
        assertEquals(SettingState.UNKNOWN, SettingsReader.toState(null))
        assertEquals(SettingState.OFF, SettingsReader.toState(0))
        assertEquals(SettingState.ON, SettingsReader.toState(1))
    }
}
