package app.protestmode.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.SettingCategory
import app.protestmode.setting.SettingId
import app.protestmode.ui.components.CategoryHeader
import app.protestmode.ui.components.CrashDialog
import app.protestmode.ui.components.ScreenTopBar
import app.protestmode.ui.components.SecuredLabels
import app.protestmode.ui.components.SettingDialog
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner
import app.protestmode.ui.components.groupedCard
import app.protestmode.ui.theme.LocalListColors
import app.protestmode.ui.walkthrough.WalkStep
import app.protestmode.ui.walkthrough.Walkthrough
import app.protestmode.ui.walkthrough.WalkthroughState
import app.protestmode.ui.walkthrough.walkthroughTarget

data class MainActions(
    val onRowClick: (SettingRow) -> Unit,
    val onConfirm: (SettingId) -> Unit,
    val onLabelClick: (ExposureLabel) -> Unit,
    val onSendCrash: () -> Unit,
    val onDismissCrash: () -> Unit,
    val onOpenAdvanced: () -> Unit,
    val onTourDone: () -> Unit
)

/** One item of the list each: the list index of a setting is its index here. */
private sealed interface ListEntry {
    data object Banner : ListEntry

    data object Labels : ListEntry

    data class Header(val category: SettingCategory, val protectedCount: Int, val total: Int) : ListEntry

    data class Setting(val row: SettingRow, val index: Int, val lastIndex: Int) : ListEntry

    data class End(val category: SettingCategory) : ListEntry
}

private fun listEntries(rows: List<SettingRow>): List<ListEntry> = buildList {
    add(ListEntry.Banner)
    add(ListEntry.Labels)
    rows.groupBy { it.id.category }.forEach { (category, group) ->
        add(ListEntry.Header(category, group.count { it.isProtected }, group.size))
        group.forEachIndexed { index, row -> add(ListEntry.Setting(row, index, group.lastIndex)) }
        add(ListEntry.End(category))
    }
}

@Composable
fun MainScreen(state: MainUiState, snackbar: SnackbarHostState, actions: MainActions, tour: WalkthroughState) {
    var detailFor by rememberSaveable { mutableStateOf<SettingId?>(null) }
    state.rows.firstOrNull { it.id == detailFor }?.let { row ->
        SettingDialog(
            row = row,
            onOpenSettings = {
                detailFor = null
                actions.onRowClick(row)
            },
            onConfirm = {
                detailFor = null
                actions.onConfirm(row.id)
            },
            onDismiss = { detailFor = null }
        )
    }
    if (state.pendingCrash) {
        CrashDialog(actions.onSendCrash, actions.onDismissCrash)
    }
    val listState = rememberLazyListState()
    val listColors = LocalListColors.current
    // Recomputed when the readings change, not on every frame of a scroll.
    val entries = remember(state.rows) { listEntries(state.rows) }
    val tourSetting = remember(entries) {
        entries.indexOfFirst { it is ListEntry.Setting && !it.row.isProtected }.takeIf { it >= 0 }
    }
    // Scrolled to the header of its category: the row itself would end up under the sticky header.
    val tourScroll = tourSetting?.let { setting -> (setting downTo 0).first { entries[it] is ListEntry.Header } }
    val targets = tour.activeTargets
    Box {
        Scaffold(
            containerColor = listColors.screen,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                ScreenTopBar(
                    title = stringResource(state.disguise.labelRes),
                    actions = {
                        IconButton(onClick = actions.onOpenAdvanced) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = stringResource(R.string.advanced_open)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
                // Nothing is shown before the first reading, rather than a level that is not known yet.
                if (!state.loaded) return@LazyColumn
                entries.forEachIndexed { position, entry ->
                    when (entry) {
                        ListEntry.Banner -> item(key = "banner", contentType = "banner") {
                            StatusBanner(
                                state.exposure,
                                state.remainingAdvice,
                                Modifier.padding(horizontal = 16.dp).walkthroughTarget(targets, WalkStep.BANNER)
                            )
                        }
                        ListEntry.Labels -> item(key = "labels", contentType = "labels") {
                            SecuredLabels(
                                secured = state.securedLabels,
                                selected = state.selectedLabel,
                                onToggle = actions.onLabelClick,
                                modifier =
                                Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                                    .walkthroughTarget(targets, WalkStep.LABELS)
                            )
                        }
                        is ListEntry.Header -> stickyHeader(key = "header_${entry.category}", contentType = "header") {
                            CategoryHeader(entry.category, entry.protectedCount, entry.total)
                        }
                        is ListEntry.Setting -> item(key = entry.row.id.name, contentType = "setting") {
                            val row = entry.row
                            val tourTarget = if (position == tourSetting) targets else null
                            SettingRowItem(
                                row = row,
                                onClick = { if (row.isProtected) actions.onRowClick(row) else detailFor = row.id },
                                onConfirm = { actions.onConfirm(row.id) },
                                modifier =
                                Modifier
                                    .groupedCard(entry.index, entry.lastIndex, listColors.card)
                                    .walkthroughTarget(tourTarget, WalkStep.SETTING)
                            )
                        }
                        is ListEntry.End -> item(key = "end_${entry.category}", contentType = "spacer") {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        if (state.loaded) {
            Walkthrough(
                state = tour,
                offer = state.offerTour,
                listState = listState,
                settingIndex = tourScroll,
                onDone = actions.onTourDone
            )
        }
    }
}
