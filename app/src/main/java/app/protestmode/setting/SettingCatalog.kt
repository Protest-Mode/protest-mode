package app.protestmode.setting

import android.content.Context
import android.content.Intent

object SettingCatalog {
    val all: List<Setting> =
        listOf(
            AirplaneSetting,
            LocationSetting,
            WifiSetting,
            BluetoothSetting,
            WifiScanSetting,
            HotspotSetting,
            MobileDataSetting,
            BleScanSetting,
            NfcSetting,
            UwbSetting,
            ScreenLockSetting,
            EncryptionSetting,
            DeveloperOptionsSetting,
            LockscreenNotificationsSetting,
            SecurityPatchSetting,
            BiometricsSetting,
            LockDelaySetting,
            AdvancedProtectionSetting,
            DeviceNameSetting,
            VoiceAssistantSetting
        )

    private val byId = all.associateBy { it.id }

    operator fun get(id: SettingId): Setting = byId.getValue(id)

    fun guideIntent(setting: Setting, context: Context): Intent =
        IntentResolver.resolve(context.packageManager, setting.guideIntents(context))
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
