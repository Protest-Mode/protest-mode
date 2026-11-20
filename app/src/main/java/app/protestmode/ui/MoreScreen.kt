package app.protestmode.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.ui.theme.LocalListColors

private class MoreEntry(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onBack: () -> Unit,
    onHide: () -> Unit,
    onTour: () -> Unit,
    onWrite: () -> Unit,
    onContribute: () -> Unit
) {
    BackHandler(onBack = onBack)
    val listColors = LocalListColors.current
    val entries =
        listOf(
            MoreEntry(R.drawable.ic_disguise, R.string.menu_hide_app, R.string.more_hide_app_summary, onHide),
            MoreEntry(R.drawable.ic_tour, R.string.menu_replay_tour, R.string.more_tour_summary, onTour),
            MoreEntry(R.drawable.ic_feedback, R.string.feedback_action, R.string.more_write_summary, onWrite),
            MoreEntry(R.drawable.ic_code, R.string.menu_contribute, R.string.more_contribute_summary, onContribute)
        )
    Scaffold(
        containerColor = listColors.screen,
        topBar = {
            TopAppBar(
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = listColors.screen,
                    scrolledContainerColor = listColors.screen
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.more_back)
                        )
                    }
                },
                title = { Text(stringResource(R.string.more_title), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(top = 8.dp)) {
            itemsIndexed(entries) { index, entry ->
                val outer = 20.dp
                val inner = 4.dp
                val shape =
                    RoundedCornerShape(
                        topStart = if (index == 0) outer else inner,
                        topEnd = if (index == 0) outer else inner,
                        bottomStart = if (index == entries.lastIndex) outer else inner,
                        bottomEnd = if (index == entries.lastIndex) outer else inner
                    )
                Row(
                    modifier =
                    Modifier
                        .padding(start = 12.dp, end = 12.dp, bottom = 2.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(listColors.card)
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
