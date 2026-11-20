package app.protestmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.protestmode.ui.theme.LocalListColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val color = LocalListColors.current.screen
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = color, scrolledContainerColor = color),
        navigationIcon = navigationIcon,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        actions = actions
    )
}

/** One card of a group: rounded at both ends of the group, nearly square between its cards. */
fun Modifier.groupedCard(index: Int, lastIndex: Int, color: Color): Modifier {
    val outer = 20.dp
    val inner = 4.dp
    val shape =
        RoundedCornerShape(
            topStart = if (index == 0) outer else inner,
            topEnd = if (index == 0) outer else inner,
            bottomStart = if (index == lastIndex) outer else inner,
            bottomEnd = if (index == lastIndex) outer else inner
        )
    return padding(start = 12.dp, end = 12.dp, bottom = 2.dp).clip(shape).background(color)
}
