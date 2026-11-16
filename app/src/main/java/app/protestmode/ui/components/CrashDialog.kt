package app.protestmode.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.protestmode.R
import app.protestmode.ui.components.CategoryHeader
import app.protestmode.ui.components.SecuredLabels
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner

@Composable
fun CrashDialog(onSend: () -> Unit, onDismiss: () -> Unit) {
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
