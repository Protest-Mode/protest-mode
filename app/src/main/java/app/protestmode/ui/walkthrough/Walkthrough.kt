package app.protestmode.ui.walkthrough

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.protestmode.R
import app.protestmode.exposure.ExposureStatus
import app.protestmode.ui.color
import app.protestmode.ui.descriptionRes
import app.protestmode.ui.titleRes

/** The parts of the screen the first-launch tour points at, in order. */
enum class WalkStep(@StringRes val textRes: Int) {
    BANNER(R.string.walkthrough_banner),
    LABELS(R.string.walkthrough_labels),
    SETTING(R.string.walkthrough_setting)
}

/** Where each target sits on screen, reported by the targets themselves as they are laid out. */
@Stable
class WalkthroughTargets {
    val bounds = mutableStateMapOf<WalkStep, Rect>()
}

val LocalWalkthroughTargets = compositionLocalOf<WalkthroughTargets?> { null }

/** Marks this element as the target of a step of the tour. */
fun Modifier.walkthroughTarget(step: WalkStep): Modifier = composed {
    val targets = LocalWalkthroughTargets.current
    if (targets == null) this else onGloballyPositioned { targets.bounds[step] = it.boundsInRoot() }
}

/** Remembers that the tour was seen; nothing else is stored, and nothing leaves the phone. */
class WalkthroughStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("walkthrough", Context.MODE_PRIVATE)

    fun seen() = prefs.getBoolean(KEY, false)

    fun markSeen() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        const val KEY = "seen"
    }
}

/** The three levels of the banner: a dot and the name in its colour, then what it means. */
@Composable
private fun StatusLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposureStatus.entries.forEach { status ->
            val title = stringResource(status.titleRes())
            val description = stringResource(status.descriptionRes())
            val color = status.color()
            // The dot sits in line with the first line of its paragraph.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.padding(top = 4.dp).size(12.dp).background(color, CircleShape))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(title) }
                        // A dash reads the same in every language, unlike a colon (spaced in French only).
                        append(" – ")
                        append(description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Asks before the tour starts: it is only shown to whoever wants it. */
@Composable
fun WalkthroughOffer(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.walkthrough_ask_title)) },
        text = { Text(stringResource(R.string.walkthrough_ask_text)) },
        confirmButton = { Button(onClick = onAccept) { Text(stringResource(R.string.walkthrough_ask_yes)) } },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(R.string.walkthrough_ask_no)) } }
    )
}

/**
 * Dims the screen except for the current target, with a short explanation next to it. Touches outside
 * the buttons are swallowed, so the screen underneath cannot be used until the tour ends.
 */
@Composable
fun WalkthroughOverlay(
    steps: List<WalkStep>,
    current: Int,
    targets: WalkthroughTargets,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val step = steps[current]
    val density = LocalDensity.current
    val margin = with(density) { 8.dp.toPx() }
    val target = targets.bounds[step]?.let {
        Rect(
            it.left - margin,
            it.top - margin,
            it.right + margin,
            it.bottom + margin
        )
    }
    val spot by animateRectAsState(target ?: Rect.Zero, label = "spotlight")
    var screenHeight by remember { mutableIntStateOf(0) }
    var bubbleHeight by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeight = it.height }
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // Below the target when there is room for the bubble, above it otherwise.
        val gap = with(density) { 12.dp.toPx() }
        val below = target == null || target.bottom + gap + bubbleHeight < screenHeight
        val y = when {
            target == null -> (screenHeight - bubbleHeight) / 2f
            below -> target.bottom + gap
            else -> target.top - gap - bubbleHeight
        }
        val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            if (target != null) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(spot.left, spot.top),
                    size = Size(spot.width, spot.height),
                    cornerRadius = CornerRadius(with(density) { 20.dp.toPx() }),
                    blendMode = BlendMode.Clear
                )
                // A tip on the edge of the bubble, in line with the middle of the target.
                val half = 12.dp.toPx()
                val x = target.center.x.coerceIn(28.dp.toPx() + half, size.width - 28.dp.toPx() - half)
                val edge = if (below) y else y + bubbleHeight
                val tip = if (below) edge - gap else edge + gap
                drawPath(
                    Path().apply {
                        moveTo(x - half, edge)
                        lineTo(x, tip)
                        lineTo(x + half, edge)
                        close()
                    },
                    bubbleColor
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
            modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .offset { IntOffset(0, y.toInt()) }
                .onSizeChanged { bubbleHeight = it.height }
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(step.textRes), style = MaterialTheme.typography.bodyLarge)
                if (step == WalkStep.BANNER) StatusLegend()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${current + 1}/${steps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (current < steps.lastIndex) {
                        TextButton(onClick = onSkip) { Text(stringResource(R.string.walkthrough_skip)) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onNext) {
                        Text(
                            stringResource(
                                if (current < steps.lastIndex) R.string.walkthrough_next else R.string.walkthrough_done
                            )
                        )
                    }
                }
            }
        }
    }
}
