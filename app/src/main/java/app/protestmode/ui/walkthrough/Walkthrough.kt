package app.protestmode.ui.walkthrough

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

enum class WalkStep(@StringRes val textRes: Int) {
    BANNER(R.string.walkthrough_banner),
    LABELS(R.string.walkthrough_labels),
    SETTING(R.string.walkthrough_setting)
}

@Stable
class WalkthroughTargets {
    val bounds = mutableStateMapOf<WalkStep, Rect>()
}

/** Reports where this element sits on screen; does nothing when [targets] is null. */
fun Modifier.walkthroughTarget(targets: WalkthroughTargets?, step: WalkStep): Modifier =
    if (targets == null) this else onGloballyPositioned { targets.bounds[step] = it.boundsInRoot() }

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

@Stable
class WalkthroughState internal constructor(initialStep: Int) {
    var step by mutableIntStateOf(initialStep)
        internal set
    val targets = WalkthroughTargets()
    val running get() = step >= 0

    /** Targets only report their position while the tour runs: no work while the list scrolls otherwise. */
    val activeTargets get() = targets.takeIf { running }

    fun start() {
        step = 0
    }

    internal fun stop() {
        step = -1
    }

    internal companion object {
        val Saver = Saver<WalkthroughState, Int>(save = { it.step }, restore = { WalkthroughState(it) })
    }
}

@Composable
fun rememberWalkthroughState(): WalkthroughState =
    rememberSaveable(saver = WalkthroughState.Saver) { WalkthroughState(initialStep = -1) }

/**
 * The first-launch offer, then the tour itself. [settingIndex] is the list index to scroll to for the
 * setting step; without one, that step is left out.
 */
@Composable
fun Walkthrough(
    state: WalkthroughState,
    offer: Boolean,
    listState: LazyListState,
    settingIndex: Int?,
    onDone: () -> Unit
) {
    val steps = WalkStep.entries.filter { it != WalkStep.SETTING || settingIndex != null }
    val end = {
        state.stop()
        onDone()
    }
    if (offer && !state.running) WalkthroughOffer(onAccept = state::start, onDecline = end)
    if (!state.running) return
    val current = state.step.coerceAtMost(steps.lastIndex)
    BackHandler(onBack = end)
    LaunchedEffect(steps[current], settingIndex) {
        when (steps[current]) {
            WalkStep.BANNER, WalkStep.LABELS -> listState.animateScrollToItem(0)
            WalkStep.SETTING -> settingIndex?.let { listState.animateScrollToItem(it) }
        }
    }
    WalkthroughOverlay(
        steps = steps,
        current = current,
        targets = state.targets,
        onNext = { if (current < steps.lastIndex) state.step = current + 1 else end() },
        onSkip = end
    )
}

@Composable
private fun WalkthroughOffer(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.walkthrough_ask_title)) },
        text = { Text(stringResource(R.string.walkthrough_ask_text)) },
        confirmButton = { Button(onClick = onAccept) { Text(stringResource(R.string.walkthrough_ask_yes)) } },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(R.string.walkthrough_ask_no)) } }
    )
}

@Composable
private fun StatusLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposureStatus.entries.forEach { status ->
            val title = stringResource(status.titleRes())
            val description = stringResource(status.descriptionRes())
            val color = status.color()
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

/** Above the spotlight when the bubble does not fit below it; centred when there is no target. */
private fun bubbleTop(spot: Rect?, bubbleHeight: Float, screenHeight: Float, gap: Float): Float = when {
    spot == null -> (screenHeight - bubbleHeight) / 2f
    spot.bottom + gap + bubbleHeight < screenHeight -> spot.bottom + gap
    else -> spot.top - gap - bubbleHeight
}

/**
 * Touches outside the buttons are swallowed: the screen underneath stays out of reach during the tour.
 * The spotlight, the bubble and its tip are placed from the animated spot, in the layout and draw
 * phases, so that they move together without recomposing.
 */
@Composable
private fun WalkthroughOverlay(
    steps: List<WalkStep>,
    current: Int,
    targets: WalkthroughTargets,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val step = steps[current]
    val density = LocalDensity.current
    val margin = with(density) { 8.dp.toPx() }
    val gap = with(density) { 12.dp.toPx() }
    val target = targets.bounds[step]?.let {
        Rect(
            it.left - margin,
            it.top - margin,
            it.right + margin,
            it.bottom + margin
        )
    }
    val spot = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        if (placed) {
            spot.animateTo(target)
        } else {
            spot.snapTo(target)
            placed = true
        }
    }
    var screenHeight by remember { mutableFloatStateOf(0f) }
    var bubbleHeight by remember { mutableFloatStateOf(0f) }
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeight = it.height.toFloat() }
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            if (!placed) return@Canvas
            val s = spot.value
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(s.left, s.top),
                size = Size(s.width, s.height),
                cornerRadius = CornerRadius(20.dp.toPx()),
                blendMode = BlendMode.Clear
            )
            val top = bubbleTop(s, bubbleHeight, size.height, gap)
            val below = top > s.top
            val half = 12.dp.toPx()
            val x = s.center.x.coerceIn(28.dp.toPx() + half, size.width - 28.dp.toPx() - half)
            val edge = if (below) top else top + bubbleHeight
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
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
            modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .offset {
                    IntOffset(0, bubbleTop(spot.value.takeIf { placed }, bubbleHeight, screenHeight, gap).toInt())
                }
                .onSizeChanged { bubbleHeight = it.height.toFloat() }
                // Hidden until its height is known, so that it is never drawn at a wrong place first.
                .graphicsLayer { alpha = if (bubbleHeight > 0f) 1f else 0f }
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
                    val last = current == steps.lastIndex
                    if (!last) {
                        TextButton(onClick = onSkip) { Text(stringResource(R.string.walkthrough_skip)) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onNext) {
                        Text(stringResource(if (last) R.string.walkthrough_done else R.string.walkthrough_next))
                    }
                }
            }
        }
    }
}
