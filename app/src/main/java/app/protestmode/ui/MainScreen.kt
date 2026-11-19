package app.protestmode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.protestmode.ui.components.SettingDialog
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner
import app.protestmode.ui.theme.LocalListColors

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
    // A protected setting opens its screen at once; any other one first explains what to change.
    var detailFor by rememberSaveable { mutableStateOf<String?>(null) }
    state.rows.firstOrNull { it.id.name == detailFor }?.let { row ->
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
    val listColors = LocalListColors.current
    Scaffold(
        containerColor = listColors.screen,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = listColors.screen,
                    scrolledContainerColor = listColors.screen
                ),
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
        LazyColumn(modifier = Modifier.padding(padding)) {
            item(key = "banner") {
                StatusBanner(state.exposure, state.remainingAdvice, Modifier.padding(horizontal = 16.dp))
            }
            item(key = "labels") {
                SecuredLabels(
                    secured = state.securedLabels,
                    selected = state.selectedLabel,
                    onToggle = actions.onLabelClick,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }
            state.rows.groupBy { it.id.category }.forEach { (category, rows) ->
                stickyHeader(key = "category_${category.name}") {
                    CategoryHeader(category, protectedCount = rows.count { it.isProtected }, total = rows.size)
                }
                // One card per setting, the cards of a category forming one rounded group, as in the system settings.
                itemsIndexed(rows, key = { _, row -> row.id.name }) { index, row ->
                    val outer = 20.dp
                    val inner = 4.dp
                    val shape =
                        RoundedCornerShape(
                            topStart = if (index == 0) outer else inner,
                            topEnd = if (index == 0) outer else inner,
                            bottomStart = if (index == rows.lastIndex) outer else inner,
                            bottomEnd = if (index == rows.lastIndex) outer else inner
                        )
                    SettingRowItem(
                        row = row,
                        onClick = { if (row.isProtected) actions.onRowClick(row) else detailFor = row.id.name },
                        onConfirm = { actions.onConfirm(row.id) },
                        modifier =
                        Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 2.dp)
                            .clip(shape)
                            .background(listColors.card)
                    )
                }
                item(key = "category_end_${category.name}") { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}
