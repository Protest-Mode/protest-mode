package app.protestmode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.disguise.Disguise
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.SettingId
import app.protestmode.ui.components.CategoryHeader
import app.protestmode.ui.components.CrashDialog
import app.protestmode.ui.components.DisguiseDialog
import app.protestmode.ui.components.SecuredLabels
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner

/** What the screen can ask for; built once from the ViewModel. */
data class MainActions(
    val onRowClick: (SettingRow) -> Unit,
    val onConfirm: (SettingId) -> Unit,
    val onLabelClick: (ExposureLabel) -> Unit,
    val onFeedback: () -> Unit,
    val onDisguiseSelected: (Disguise) -> Unit,
    val onSendCrash: () -> Unit,
    val onDismissCrash: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: MainUiState, snackbar: SnackbarHostState, actions: MainActions) {
    var choosingDisguise by rememberSaveable { mutableStateOf(false) }
    if (choosingDisguise) {
        DisguiseDialog(
            current = state.disguise,
            onApply = {
                choosingDisguise = false
                actions.onDisguiseSelected(it)
            },
            onCancel = { choosingDisguise = false }
        )
    }
    if (state.pendingCrash) {
        CrashDialog(actions.onSendCrash, actions.onDismissCrash)
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
                    IconButton(onClick = actions.onFeedback) {
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
                    onToggle = actions.onLabelClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            state.rows.groupBy { it.id.category }.forEach { (category, rows) ->
                item(key = "category_${category.name}") {
                    CategoryHeader(category)
                }
                items(rows, key = { it.id.name }) { row ->
                    SettingRowItem(row = row, onClick = {
                        actions.onRowClick(row)
                    }, onConfirm = { actions.onConfirm(row.id) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
