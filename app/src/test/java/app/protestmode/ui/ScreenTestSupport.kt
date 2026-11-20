package app.protestmode.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.protestmode.exposure.ExposureStatus
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.setting.protectiveState

internal val testContext: Context get() = ApplicationProvider.getApplicationContext()

internal fun text(id: Int, vararg args: Any): String = testContext.getString(id, *args)

internal fun protectedRow(id: SettingId) = SettingRow(id, id.protectiveState, confirmed = false)

internal fun exposedRow(id: SettingId): SettingRow {
    val unprotected = if (id.protectiveState == SettingState.ON) SettingState.OFF else SettingState.ON
    return SettingRow(id, unprotected, confirmed = false)
}

internal fun loadedState(rows: List<SettingRow>, offerTour: Boolean = false) = MainUiState(
    exposure = ExposureStatus.EXPOSED,
    rows = rows,
    loaded = true,
    offerTour = offerTour
)

/** Records what the screen asks for. */
internal class RecordedActions {
    val opened = mutableListOf<SettingRow>()
    val confirmed = mutableListOf<SettingId>()
    var advancedOpened = 0
    var toursDone = 0

    val actions =
        MainActions(
            onRowClick = { opened += it },
            onConfirm = { confirmed += it },
            onLabelClick = {},
            onSendCrash = {},
            onDismissCrash = {},
            onOpenAdvanced = { advancedOpened++ },
            onTourDone = { toursDone++ }
        )
}
