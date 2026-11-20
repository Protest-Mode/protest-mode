package app.protestmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.setting.SettingCatalog
import app.protestmode.ui.RowStatus
import app.protestmode.ui.SettingRow

@Composable
fun SettingDialog(row: SettingRow, onOpenSettings: () -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val setting = SettingCatalog[row.id]
    val steps = stringResource(setting.instructionRes).lines().filter { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(setting.titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                row.detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(
                            if (steps.size ==
                                1
                            ) {
                                R.string.setting_steps_one
                            } else {
                                R.string.setting_steps_many
                            }
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    steps.forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (steps.size > 1) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (row.status == RowStatus.TO_CHECK) {
                    OutlinedButton(onClick = onConfirm, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}
