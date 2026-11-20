package app.protestmode.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.disguise.Disguise
import app.protestmode.ui.components.DisguiseDialog
import app.protestmode.ui.components.ScreenTopBar
import app.protestmode.ui.components.groupedCard
import app.protestmode.ui.theme.LocalListColors

private class AdvancedEntry(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val onClick: () -> Unit
)

@Composable
fun AdvancedScreen(
    disguise: Disguise,
    snackbar: SnackbarHostState,
    onDisguiseSelected: (Disguise) -> Unit,
    onBack: () -> Unit,
    onTour: () -> Unit,
    onContact: () -> Unit,
    onContribute: () -> Unit
) {
    BackHandler(onBack = onBack)
    var choosingDisguise by rememberSaveable { mutableStateOf(false) }
    if (choosingDisguise) {
        DisguiseDialog(
            current = disguise,
            onApply = {
                choosingDisguise = false
                onDisguiseSelected(it)
            },
            onCancel = { choosingDisguise = false }
        )
    }
    val listColors = LocalListColors.current
    val entries =
        listOf(
            AdvancedEntry(R.drawable.ic_disguise, R.string.advanced_hide, R.string.advanced_hide_summary) {
                choosingDisguise = true
            },
            AdvancedEntry(R.drawable.ic_tour, R.string.advanced_tour, R.string.advanced_tour_summary, onTour),
            AdvancedEntry(
                R.drawable.ic_feedback,
                R.string.advanced_contact,
                R.string.advanced_contact_summary,
                onContact
            ),
            AdvancedEntry(
                R.drawable.ic_code,
                R.string.advanced_contribute,
                R.string.advanced_contribute_summary,
                onContribute
            )
        )
    Scaffold(
        containerColor = listColors.screen,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.advanced_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(top = 8.dp)) {
            itemsIndexed(entries) { index, entry ->
                Row(
                    modifier =
                    Modifier
                        .groupedCard(index, entries.lastIndex, listColors.card)
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = entry.onClick)
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(painterResource(entry.icon), contentDescription = null, modifier = Modifier.size(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(entry.title), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(entry.summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
