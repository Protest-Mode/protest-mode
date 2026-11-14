package app.protestmode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.disguise.Disguise
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.SettingId
import app.protestmode.ui.components.CategoryHeader
import app.protestmode.ui.components.SecuredLabels
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    snackbar: SnackbarHostState,
    onRowClick: (SettingRow) -> Unit,
    onConfirm: (SettingId) -> Unit,
    onLabelClick: (ExposureLabel) -> Unit,
    onFeedback: () -> Unit,
    onDisguiseSelected: (Disguise) -> Unit,
    onSendCrash: () -> Unit,
    onDismissCrash: () -> Unit
) {
    var choosingDisguise by rememberSaveable { mutableStateOf(false) }
    if (choosingDisguise) {
        DisguiseDialog(
            current = state.disguise,
            onApply = {
                choosingDisguise = false
                onDisguiseSelected(it)
            },
            onCancel = { choosingDisguise = false }
        )
    }
    if (state.pendingCrash) {
        CrashDialog(onSendCrash, onDismissCrash)
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(state.disguise.labelRes),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { choosingDisguise = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_disguise),
                            contentDescription = stringResource(R.string.disguise_action)
                        )
                    }
                    IconButton(onClick = onFeedback) {
                        Icon(
                            painter = painterResource(R.drawable.ic_feedback),
                            contentDescription = stringResource(R.string.feedback_action)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "banner") {
                StatusBanner(state.exposure, state.remainingAdvice, Modifier.padding(horizontal = 16.dp))
            }
            item(key = "labels") {
                SecuredLabels(
                    secured = state.securedLabels,
                    selected = state.selectedLabel,
                    onToggle = onLabelClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            state.rows.groupBy { it.id.category }.forEach { (category, rows) ->
                item(key = "category_${category.name}") {
                    CategoryHeader(category)
                }
                items(rows, key = { it.id.name }) { row ->
                    SettingRowItem(row = row, onClick = { onRowClick(row) }, onConfirm = { onConfirm(row.id) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CrashDialog(onSend: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.crash_title)) },
        text = { Text(stringResource(R.string.crash_text)) },
        confirmButton = {
            TextButton(onClick = onSend) { Text(stringResource(R.string.crash_send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.crash_dismiss)) }
        }
    )
}

@Composable
private fun DisguiseDialog(current: Disguise, onApply: (Disguise) -> Unit, onCancel: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.disguise_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.disguise_text))
                Column(Modifier.selectableGroup()) {
                    Disguise.entries.forEach { disguise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = disguise == selected,
                                    role = Role.RadioButton,
                                    onClick = { selected = disguise }
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DisguiseIcon(disguise)
                            Text(stringResource(disguise.labelRes), Modifier.weight(1f))
                            RadioButton(selected = disguise == selected, onClick = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected) }) { Text(stringResource(R.string.disguise_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.disguise_cancel)) }
        }
    )
}

@Composable
private fun DisguiseIcon(disguise: Disguise) {
    val iconRes = disguise.iconRes
    if (iconRes != null) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colorResource(R.color.launcher_background))
        ) {
            Image(
                painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().scale(1.25f)
            )
        }
    }
}
