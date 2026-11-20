package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.provider.Settings
import app.protestmode.R

object NfcSetting : Setting {
    override val id = SettingId.NFC
    override val titleRes = R.string.setting_nfc
    override val iconRes = R.drawable.ic_nfc
    override val whyRes = R.string.why_nfc
    override val instructionRes = R.string.instruction_nfc

    override fun isAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) && adapter(context) != null

    override fun read(probe: Probe) = Reading(adapter(probe.context)?.isEnabled.toSettingState())

    override fun guideIntents(context: Context): List<Intent> =
        panelThen(Settings.Panel.ACTION_NFC, Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)

    private fun adapter(context: Context): NfcAdapter? = NfcAdapter.getDefaultAdapter(context.applicationContext)
}
