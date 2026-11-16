package app.protestmode.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.provider.Settings
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.protestmode.R
import app.protestmode.disguise.DisguiseWindow
import app.protestmode.feedback.FeedbackMail

@Composable
fun ProtestModeApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    // Closing the notification shade or a floating panel gives the focus back without ON_RESUME.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) {
        if (focused) viewModel.onResume()
    }

    LaunchedEffect(state.disguise) {
        (context as? Activity)?.let { DisguiseWindow.apply(it, state.disguise) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.OpenScreen ->
                    if (!context.startSafely(event.intent)) context.startSafely(Intent(Settings.ACTION_SETTINGS))
                is UiEvent.SendMail ->
                    if (!context.startSafely(event.intent)) offerSupportAddress(context, resources, snackbar)
            }
        }
    }

    val actions = remember(viewModel) {
        MainActions(
            onRowClick = viewModel::onRowTapped,
            onConfirm = viewModel::confirmRow,
            onLabelClick = viewModel::toggleLabel,
            onFeedback = viewModel::onFeedback,
            onDisguiseSelected = viewModel::onDisguiseSelected,
            onSendCrash = viewModel::onSendCrash,
            onDismissCrash = viewModel::onDismissCrash
        )
    }

    MainScreen(state = state, snackbar = snackbar, actions = actions)
}

/** Opens another app's screen; returns false when no app can handle it or the system refuses. */
private fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

/** Without an email app, the support address is shown with an action to copy it. */
private suspend fun offerSupportAddress(context: Context, resources: Resources, snackbar: SnackbarHostState) {
    val result = snackbar.showSnackbar(
        message = resources.getString(R.string.feedback_no_mail_app, FeedbackMail.SUPPORT_EMAIL),
        actionLabel = resources.getString(R.string.feedback_copy),
        duration = SnackbarDuration.Long
    )
    if (result == SnackbarResult.ActionPerformed) {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(FeedbackMail.SUPPORT_EMAIL, FeedbackMail.SUPPORT_EMAIL))
    }
}
