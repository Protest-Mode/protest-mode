package app.protestmode.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.disguise.Disguise
import app.protestmode.ui.components.CategoryHeader
import app.protestmode.ui.components.SecuredLabels
import app.protestmode.ui.components.SettingRowItem
import app.protestmode.ui.components.StatusBanner

@Composable
fun DisguiseDialog(current: Disguise, onApply: (Disguise) -> Unit, onCancel: () -> Unit) {
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
