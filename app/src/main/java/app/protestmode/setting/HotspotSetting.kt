package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.provider.Settings
import app.protestmode.R

object HotspotSetting : Setting {
    override val id = SettingId.HOTSPOT
    override val titleRes = R.string.setting_hotspot
    override val iconRes = R.drawable.ic_hotspot
    override val whyRes = R.string.why_hotspot
    override val instructionRes = R.string.instruction_hotspot

    override fun isAvailable(context: Context) = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

    // No public API exposes the hotspot state. WifiManager.isWifiApEnabled() is hidden and may be
    // blocked by non-SDK interface restrictions: the reflection error then reads as unknown (readSafely).
    override fun read(probe: Probe): Reading {
        val manager = probe.context.applicationContext.getSystemService(WifiManager::class.java)
        val enabled = manager?.let { WifiManager::class.java.getMethod("isWifiApEnabled").invoke(it) as? Boolean }
        return Reading(enabled.toSettingState())
    }

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(HiddenActions.TETHER), Intent(Settings.ACTION_WIRELESS_SETTINGS))
}
