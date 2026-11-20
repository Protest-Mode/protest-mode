package app.protestmode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import app.protestmode.ui.walkthrough.LocalWalkthroughTargets
import app.protestmode.ui.walkthrough.WalkStep
import app.protestmode.ui.walkthrough.WalkthroughOffer
import app.protestmode.ui.walkthrough.WalkthroughOverlay
import app.protestmode.ui.walkthrough.WalkthroughStore
import app.protestmode.ui.walkthrough.WalkthroughTargets
import app.protestmode.ui.walkthrough.walkthroughTarget

/** What the screen can ask for; built once from the ViewModel. */
data class MainActions(
    val onRowClick: (SettingRow) -> Unit,
    val onConfirm: (SettingId) -> Unit,
    val onLabelClick: (ExposureLabel) -> Unit,
    val onFeedback: () -> Unit,
    val onDisguiseSelected: (Disguise) -> Unit,
    val onSendCrash: () -> Unit,
    val onDismissCrash: () -> Unit,
    val onContribute: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: MainUiState, snackbar: SnackbarHostState, actions: MainActions) {
    var choosingDisguise by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
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
    val listState = rememberLazyListState()
    // First launch: a short tour over the real screen. The setting step points at the first setting to
    // change, and is left out when there is none.
    val context = LocalContext.current
    val store = remember { WalkthroughStore(context) }
    val targets = remember { WalkthroughTargets() }
    // Recomputed when the readings change, not on every frame of a scroll.
    val groups = remember(state.rows) { state.rows.groupBy { it.id.category } }
    val firstToChange = remember(state.rows) { state.rows.firstOrNull { !it.isProtected } }
    val tourSteps = WalkStep.entries.filter { it != WalkStep.SETTING || firstToChange != null }
    // OFFER: the app asks first; DONE: declined, skipped or finished; otherwise the index of the step.
    var tourStep by rememberSaveable { mutableIntStateOf(if (store.seen()) TOUR_DONE else TOUR_OFFER) }
    val touring = tourStep in tourSteps.indices && state.rows.isNotEmpty()
    // Brings the target of the step into view: the top of the list, or the category of the setting.
    LaunchedEffect(tourStep, touring) {
        if (!touring) return@LaunchedEffect
        when (tourSteps[tourStep]) {
            WalkStep.BANNER, WalkStep.LABELS -> listState.animateScrollToItem(0)
            WalkStep.SETTING -> {
                var index = 2
                for ((category, rows) in groups) {
                    if (category == firstToChange?.id?.category) break
                    index += rows.size + 2
                }
                listState.animateScrollToItem(index)
            }
        }
    }
    val endTour = {
        tourStep = TOUR_DONE
        store.markSeen()
    }
    val listColors = LocalListColors.current
    if (showMore) {
        MoreScreen(
            onBack = { showMore = false },
            onHide = { choosingDisguise = true },
            onTour = {
                showMore = false
                tourStep = 0
            },
            onWrite = actions.onFeedback,
            onContribute = actions.onContribute
        )
        return
    }
    // Targets only report their position during the tour: no work while the list scrolls otherwise.
    CompositionLocalProvider(LocalWalkthroughTargets provides targets.takeIf { touring }) {
        Box {
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
                        // One entry point for everything besides the checks.
                        actions = {
                            IconButton(onClick = { showMore = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more),
                                    contentDescription = stringResource(R.string.menu_more)
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
                    item(key = "banner", contentType = "banner") {
                        StatusBanner(
                            state.exposure,
                            state.remainingAdvice,
                            Modifier.padding(horizontal = 16.dp).walkthroughTarget(WalkStep.BANNER)
                        )
                    }
                    item(key = "labels", contentType = "labels") {
                        SecuredLabels(
                            secured = state.securedLabels,
                            selected = state.selectedLabel,
                            onToggle = actions.onLabelClick,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp
                            ).walkthroughTarget(WalkStep.LABELS)
                        )
                    }
                    groups.forEach { (category, rows) ->
                        stickyHeader(key = "category_${category.name}", contentType = "header") {
                            CategoryHeader(category, protectedCount = rows.count { it.isProtected }, total = rows.size)
                        }
                        // One card per setting, the cards of a category forming one rounded group, as in the system settings.
                        itemsIndexed(rows, key = { _, row ->
                            row.id.name
                        }, contentType = { _, _ -> "setting" }) { index, row ->
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
                                    .then(
                                        if (row ==
                                            firstToChange
                                        ) {
                                            Modifier.walkthroughTarget(WalkStep.SETTING)
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                        item(key = "category_end_${category.name}", contentType = "spacer") { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
            if (tourStep == TOUR_OFFER && state.rows.isNotEmpty()) {
                WalkthroughOffer(onAccept = { tourStep = 0 }, onDecline = endTour)
            }
            if (touring) {
                WalkthroughOverlay(
                    steps = tourSteps,
                    current = tourStep,
                    targets = targets,
                    onNext = { if (tourStep < tourSteps.lastIndex) tourStep++ else endTour() },
                    onSkip = endTour
                )
            }
        }
    }
}

private const val TOUR_OFFER = -2
private const val TOUR_DONE = -1
