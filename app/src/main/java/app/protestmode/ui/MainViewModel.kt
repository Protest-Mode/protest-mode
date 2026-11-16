package app.protestmode.ui

import android.app.Application
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.protestmode.data.Confirmations
import app.protestmode.disguise.Disguise
import app.protestmode.disguise.DisguiseManager
import app.protestmode.exposure.Exposure
import app.protestmode.exposure.ExposureStatus
import app.protestmode.feedback.CrashStore
import app.protestmode.feedback.FeedbackMail
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.Reading
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingSource
import app.protestmode.setting.SettingState
import app.protestmode.setting.enumByName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RowStatus { PROTECTED, TO_CHECK, EXPOSED }

data class SettingRow(val id: SettingId, val state: SettingState, val confirmed: Boolean, val detail: String? = null) {
    val status: RowStatus = when {
        Exposure.isProtected(id, state, confirmed) -> RowStatus.PROTECTED
        state == SettingState.UNKNOWN -> RowStatus.TO_CHECK
        else -> RowStatus.EXPOSED
    }
    val isProtected get() = status == RowStatus.PROTECTED
}

/** One-off actions the screen carries out on behalf of the ViewModel. */
sealed interface UiEvent {
    /** A system screen that guides the user to protect a setting. */
    data class OpenScreen(val intent: Intent) : UiEvent

    /** An email to open in the user's email app. */
    data class SendMail(val intent: Intent) : UiEvent
}

data class MainUiState(
    val exposure: ExposureStatus = ExposureStatus.EXPOSED,
    val remainingAdvice: Int = 0,
    val securedLabels: Set<ExposureLabel> = emptySet(),
    val selectedLabel: ExposureLabel? = null,
    val rows: List<SettingRow> = emptyList(),
    /** A crash report from the previous run waits for the user's decision. */
    val pendingCrash: Boolean = false,
    val disguise: Disguise = Disguise.ORIGINAL
)

class MainViewModel
@JvmOverloads
constructor(
    application: Application,
    private val savedState: SavedStateHandle,
    private val readDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val source: SettingSource = SettingSource.Device
) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)

    /** Consumed once by the UI. */
    val events: Flow<UiEvent> = eventChannel.receiveAsFlow()

    private val crashStore = CrashStore(application)
    private val disguiseManager = DisguiseManager(application)
    private var disguise: Disguise = disguiseManager.current()
    private var crashTrace: String? = crashStore.pending()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var readings: Map<SettingId, Reading> = emptyMap()

    private val confirmations = Confirmations(application, savedState)

    private var selectedLabel: ExposureLabel?
        get() = enumByName<ExposureLabel>(savedState.get<String>(KEY_LABEL))
        set(value) {
            savedState[KEY_LABEL] = value?.name
        }

    private var refreshJob: Job? = null

    // Quick settings and floating panels change settings without pausing the activity, so the keys
    // read by the settings are observed directly.
    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refresh(OBSERVER_DELAY_MS)
            }
        }

    init {
        // A key a vendor refuses to observe is still read again on resume and on focus.
        SettingCatalog.all.flatMap { it.observedUris }.distinct().forEach {
            runCatching { context.contentResolver.registerContentObserver(it, false, settingsObserver) }
        }
        refresh()
    }

    override fun onCleared() {
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    /** Called on ON_RESUME and window focus gain, which often follow each other: both share one read. */
    fun onResume() {
        refresh(RESUME_DELAY_MS)
    }

    fun onRowTapped(row: SettingRow) {
        val setting = SettingCatalog[row.id]
        viewModelScope.launch {
            val protectedAutomatically =
                withContext(readDispatcher) {
                    row.state != SettingState.UNKNOWN && runCatching { setting.tryProtect(context) }.getOrDefault(false)
                }
            if (!protectedAutomatically) {
                val intent = withContext(readDispatcher) {
                    runCatching {
                        SettingCatalog.guideIntent(setting, context)
                    }.getOrElse { Intent(Settings.ACTION_SETTINGS) }
                }
                eventChannel.trySend(UiEvent.OpenScreen(intent))
            }
            refresh()
        }
    }

    fun confirmRow(id: SettingId) {
        val reading = readings[id] ?: return
        if (reading.state != SettingState.UNKNOWN) return
        confirmations.confirm(id, reading.confirmationKey)
        publish()
    }

    fun toggleLabel(label: ExposureLabel) {
        selectedLabel = if (selectedLabel == label) null else label
        publish()
    }

    fun onDisguiseSelected(target: Disguise) {
        if (disguiseManager.apply(target)) disguise = target
        publish()
    }

    fun onFeedback() {
        eventChannel.trySend(UiEvent.SendMail(FeedbackMail.feedback(context)))
    }

    fun onSendCrash() {
        crashTrace?.let { eventChannel.trySend(UiEvent.SendMail(FeedbackMail.crashReport(context, it))) }
        dropCrash()
    }

    fun onDismissCrash() = dropCrash()

    private fun dropCrash() {
        crashStore.clear()
        crashTrace = null
        publish()
    }

    private fun refresh(delayMs: Long = 0) {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                if (delayMs > 0) delay(delayMs)
                readings = withContext(readDispatcher) { source.readAll(context) }
                publish()
            }
    }

    private fun publish() {
        val confirmedIds = confirmations.valid(readings)
        val exposure = Exposure.evaluate(readings.mapValues { it.value.state }, confirmedIds)
        val label = selectedLabel
        _state.value =
            MainUiState(
                exposure = exposure.status,
                remainingAdvice = exposure.remainingAdvice,
                securedLabels = exposure.securedLabels,
                selectedLabel = label,
                pendingCrash = crashTrace != null,
                disguise = disguise,
                rows =
                readings
                    .filterKeys { label == null || label in it.labels }
                    .map { (id, reading) -> SettingRow(id, reading.state, id in confirmedIds, reading.detail) }
            )
    }

    private companion object {
        const val KEY_LABEL = "label"
        const val OBSERVER_DELAY_MS = 300L
        const val RESUME_DELAY_MS = 100L
    }
}
