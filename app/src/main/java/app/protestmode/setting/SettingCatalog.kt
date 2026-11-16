package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.os.Build

/** The implementation of each setting; the exhaustive `when` keeps it in step with [SettingId]. */
val SettingId.setting: Setting
    get() = when (this) {
        SettingId.AIRPLANE -> AirplaneSetting
        SettingId.LOCATION -> LocationSetting
        SettingId.WIFI -> WifiSetting
        SettingId.BLUETOOTH -> BluetoothSetting
        SettingId.WIFI_SCAN -> WifiScanSetting
        SettingId.HOTSPOT -> HotspotSetting
        SettingId.MOBILE_DATA -> MobileDataSetting
        SettingId.BLE_SCAN -> BleScanSetting
        SettingId.NFC -> NfcSetting
        SettingId.UWB -> UwbSetting
        SettingId.SCREEN_LOCK -> ScreenLockSetting
        SettingId.ENCRYPTION -> EncryptionSetting
        SettingId.DEVELOPER_OPTIONS -> DeveloperOptionsSetting
        SettingId.LOCKSCREEN_NOTIFICATIONS -> LockscreenNotificationsSetting
        SettingId.SECURITY_PATCH -> SecurityPatchSetting
        SettingId.BIOMETRICS -> BiometricsSetting
        SettingId.LOCK_DELAY -> LockDelaySetting
        SettingId.ADVANCED_PROTECTION -> AdvancedProtectionSetting
        SettingId.DEVICE_NAME -> DeviceNameSetting
        SettingId.VOICE_ASSISTANT -> VoiceAssistantSetting
    }

object SettingCatalog {
    val all: List<Setting> = SettingId.entries.map { it.setting }

    operator fun get(id: SettingId): Setting = id.setting

    fun guideIntent(setting: Setting, context: Context): Intent =
        IntentResolver.resolve(context.packageManager, setting.guideIntents(context))
}

/** A Settings Panel on API 29+, then the full screens, from the most specific to the most general. */
internal fun panelThen(panelAction: String, vararg fallbacks: String): List<Intent> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Intent(panelAction))
    fallbacks.forEach { add(Intent(it)) }
}

fun interface SettingSource {
    fun readAll(context: Context): Map<SettingId, Reading>

    companion object {
        val Device = SettingSource { context -> readSafely(SettingCatalog.all, context) }

        // A setting that fails to read (vendor bug, system service restarting) must not take the whole
        // screen down: it is shown as unknown, never as protected.
        internal fun readSafely(settings: List<Setting>, context: Context): Map<SettingId, Reading> {
            val probe = Probe(context)
            return settings
                .filter { setting -> runCatching { setting.isAvailable(context) }.getOrDefault(true) }
                .sortedBy { it.id.ordinal }
                .associate { setting ->
                    setting.id to runCatching { setting.read(probe) }.getOrElse { Reading(SettingState.UNKNOWN) }
                }
        }
    }
}
