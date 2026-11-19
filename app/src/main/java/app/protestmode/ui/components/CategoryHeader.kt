package app.protestmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.setting.SettingCategory
import app.protestmode.ui.descriptionRes
import app.protestmode.ui.theme.LocalListColors
import app.protestmode.ui.theme.LocalStatusColors
import app.protestmode.ui.titleRes

/** Stays at the top of the list, with what the category covers, while its settings scroll. */
@Composable
fun CategoryHeader(category: SettingCategory, protectedCount: Int, total: Int, modifier: Modifier = Modifier) {
    val title = stringResource(category.titleRes())
    val count = stringResource(R.string.category_count_a11y, protectedCount, total)
    Column(modifier.fillMaxWidth().background(LocalListColors.current.screen)) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { heading() }
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "$title, $count" }
            )
            CountPill(protectedCount, total)
        }
        Text(
            text = stringResource(category.descriptionRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )
    }
}

@Composable
private fun CountPill(protectedCount: Int, total: Int) {
    val colors = LocalStatusColors.current
    val done = protectedCount == total
    Text(
        text = "$protectedCount/$total",
        style = MaterialTheme.typography.labelLarge,
        color = if (done) colors.onDiscreet else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
        Modifier
            .clearAndSetSemantics { }
            .background(
                if (done) colors.discreet else LocalListColors.current.card,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 2.dp)
    )
}
