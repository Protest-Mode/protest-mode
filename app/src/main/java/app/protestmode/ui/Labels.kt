package app.protestmode.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.protestmode.R
import app.protestmode.exposure.ExposureStatus
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.RiskLevel
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingCategory
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.setting.labelRes
import app.protestmode.ui.theme.LocalStatusColors

@StringRes
fun ExposureStatus.titleRes() = when (this) {
    ExposureStatus.EXPOSED -> R.string.status_exposed
    ExposureStatus.PARTIAL -> R.string.status_partial
    ExposureStatus.DISCREET -> R.string.status_discreet
}

@StringRes
fun ExposureStatus.descriptionRes() = when (this) {
    ExposureStatus.EXPOSED -> R.string.status_exposed_desc
    ExposureStatus.PARTIAL -> R.string.status_partial_desc
    ExposureStatus.DISCREET -> R.string.status_discreet_desc
}

@Composable
fun ExposureStatus.color(): Color = when (this) {
    ExposureStatus.EXPOSED -> LocalStatusColors.current.exposed
    ExposureStatus.PARTIAL -> LocalStatusColors.current.partial
    ExposureStatus.DISCREET -> LocalStatusColors.current.discreet
}

@Composable
fun ExposureStatus.onColor(): Color = when (this) {
    ExposureStatus.EXPOSED -> LocalStatusColors.current.onExposed
    ExposureStatus.PARTIAL -> LocalStatusColors.current.onPartial
    ExposureStatus.DISCREET -> LocalStatusColors.current.onDiscreet
}

@StringRes
fun ExposureLabel.titleRes() = when (this) {
    ExposureLabel.DEVICE_ACCESS -> R.string.label_device_access
    ExposureLabel.LOCATION_TRACKING -> R.string.label_location_tracking
    ExposureLabel.FINGERPRINTING -> R.string.label_fingerprinting
    ExposureLabel.ACTIVITY_HISTORY -> R.string.label_activity_history
}

@StringRes
fun SettingRow.stateRes() = if (state == SettingState.UNKNOWN && confirmed) {
    R.string.setting_state_confirmed
} else {
    SettingCatalog[id].labelRes(state)
}

@StringRes
fun SettingRow.verdictRes() = when (status) {
    RowStatus.PROTECTED -> R.string.target_ok
    RowStatus.TO_CHECK -> R.string.target_unknown
    RowStatus.EXPOSED -> when (id) {
        SettingId.SCREEN_LOCK -> R.string.target_secure
        SettingId.LOCKSCREEN_NOTIFICATIONS -> R.string.target_hide
        SettingId.SECURITY_PATCH -> R.string.target_update
        SettingId.BIOMETRICS -> R.string.target_remove
        SettingId.LOCK_DELAY -> R.string.target_shorten
        SettingId.DEVICE_NAME -> R.string.target_change
        SettingId.VOICE_ASSISTANT -> R.string.target_block
        else -> if (id.desiredEnabled) R.string.target_enable else R.string.target_disable
    }
}

@StringRes
fun SettingRow.whyRes(): Int? = when (status) {
    RowStatus.PROTECTED -> null
    RowStatus.TO_CHECK -> R.string.why_unknown
    RowStatus.EXPOSED -> when (id) {
        SettingId.AIRPLANE -> R.string.why_airplane
        SettingId.LOCATION -> R.string.why_location
        SettingId.WIFI -> R.string.why_wifi
        SettingId.BLUETOOTH -> R.string.why_bluetooth
        SettingId.WIFI_SCAN -> R.string.why_wifi_scan
        SettingId.HOTSPOT -> R.string.why_hotspot
        SettingId.MOBILE_DATA -> R.string.why_mobile_data
        SettingId.BLE_SCAN -> R.string.why_ble_scan
        SettingId.NFC -> R.string.why_nfc
        SettingId.UWB -> R.string.why_uwb
        SettingId.SCREEN_LOCK -> R.string.why_screen_lock
        SettingId.ENCRYPTION -> R.string.why_encryption
        SettingId.DEVELOPER_OPTIONS -> R.string.why_developer_options
        SettingId.LOCKSCREEN_NOTIFICATIONS -> R.string.why_lockscreen_notifications
        SettingId.SECURITY_PATCH -> R.string.why_security_patch
        SettingId.BIOMETRICS -> R.string.why_biometrics
        SettingId.LOCK_DELAY -> R.string.why_lock_delay
        SettingId.ADVANCED_PROTECTION -> R.string.why_advanced_protection
        SettingId.DEVICE_NAME -> R.string.why_device_name
        SettingId.VOICE_ASSISTANT -> R.string.why_voice_assistant
    }
}

@StringRes
fun SettingCategory.titleRes() = when (this) {
    SettingCategory.TRACKING -> R.string.category_tracking
    SettingCategory.SECURITY -> R.string.category_security
    SettingCategory.ANONYMITY -> R.string.category_anonymity
}

@StringRes
fun SettingCategory.descriptionRes() = when (this) {
    SettingCategory.TRACKING -> R.string.category_tracking_desc
    SettingCategory.SECURITY -> R.string.category_security_desc
    SettingCategory.ANONYMITY -> R.string.category_anonymity_desc
}

@StringRes
fun RiskLevel.labelRes() = when (this) {
    RiskLevel.CRITICAL -> R.string.risk_critical
    RiskLevel.HIGH -> R.string.risk_high
    RiskLevel.MODERATE -> R.string.risk_moderate
}

@StringRes
fun RiskLevel.a11yRes() = when (this) {
    RiskLevel.CRITICAL -> R.string.risk_critical_a11y
    RiskLevel.HIGH -> R.string.risk_high_a11y
    RiskLevel.MODERATE -> R.string.risk_moderate_a11y
}

@Composable
fun RiskLevel.colors(): Pair<Color, Color> {
    val colors = LocalStatusColors.current
    return when (this) {
        RiskLevel.CRITICAL -> colors.exposed to colors.onExposed
        RiskLevel.HIGH -> colors.partial to colors.onPartial
        RiskLevel.MODERATE -> colors.caution to colors.onCaution
    }
}
