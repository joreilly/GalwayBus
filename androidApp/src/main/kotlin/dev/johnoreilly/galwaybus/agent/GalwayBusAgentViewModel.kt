package dev.johnoreilly.galwaybus.agent

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.agent.Agent
import com.google.mlkit.genai.agent.AgentEvent
import com.google.mlkit.genai.agent.AgentRunner
import com.google.mlkit.genai.agent.AgentSession
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Content
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "Ask GalwayBus": an on-device agent (ML Kit GenAI agent API, Gemini Nano via AICore) whose tools
 * are the app's own AppFunctions in [dev.johnoreilly.galwaybus.functions.BaseGalwayBusAppFunctionService],
 * the same functions the system exposes to assistants.
 *
 * Needs Android 16 (AppFunctions), a device that supports Gemini Nano custom generation, and a
 * Google account enrolled in the AICore Developer Preview. Anywhere else [availability] ends up
 * [Availability.Unsupported] and the UI hides the entry point.
 */
class GalwayBusAgentViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface Availability {
        data object Checking : Availability
        /** Gemini Nano is downloading; [bytes] so far, when known. */
        data class Downloading(val bytes: Long?) : Availability
        data object Ready : Availability
        data class Unsupported(val reason: String) : Availability
    }

    private val _availability = MutableStateFlow<Availability>(Availability.Checking)
    val availability: StateFlow<Availability> = _availability.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val agent = Agent.builder()
        .setName("GalwayBus")
        .setInstructions(INSTRUCTIONS)
        // Tools are discovered from the app's AppFunctions (getDepartures, findStops,
        // findNearbyStops, listRoutes, listSavedStops); nothing is declared twice.
        .enableAppFunctions(true)
        .setMaxTurns(8)
        .build()

    // Created on first use, after the model is known to be available. The session keeps the
    // conversation history; living in the ViewModel, it survives rotation.
    private var runner: AgentRunner? = null
    private var session: AgentSession? = null

    init {
        if (Build.VERSION.SDK_INT < 36) {
            unsupported("AppFunctions need Android 16 (API 36); this is API ${Build.VERSION.SDK_INT}")
        } else {
            viewModelScope.launch { prepareModel() }
        }
    }

    private suspend fun prepareModel() {
        // Everything ML Kit, getClient() included, stays inside the try: this runs at app start,
        // and a failure here must hide "Ask", never crash the app (BikeShare 1.0.17 did, when R8
        // broke ML Kit's initialisation).
        var model: GenerativeModel? = null
        try {
            model = Generation.getClient()
            when (val status = model.checkStatus()) {
                FeatureStatus.AVAILABLE -> _availability.value = Availability.Ready
                FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                    _availability.value = Availability.Downloading(null)
                    model.download().collect { download ->
                        when (download) {
                            is DownloadStatus.DownloadStarted -> _availability.value = Availability.Downloading(null)
                            is DownloadStatus.DownloadProgress ->
                                _availability.value = Availability.Downloading(download.totalBytesDownloaded)
                            DownloadStatus.DownloadCompleted -> _availability.value = Availability.Ready
                            is DownloadStatus.DownloadFailed ->
                                unsupported("Gemini Nano download failed: ${download.e.message}")
                        }
                    }
                }
                else -> unsupported(
                    "Gemini Nano isn't available here (status $status). The agent API needs a supported " +
                        "device and a Google account in the AICore Developer Preview."
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            unsupported("Couldn't check for Gemini Nano: ${e.message}")
        } finally {
            model?.close()
        }
    }

    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty() || _busy.value || _availability.value != Availability.Ready) return
        _busy.value = true
        _messages.update { it + ChatMessage.User(text) + ChatMessage.Agent() }
        viewModelScope.launch {
            try {
                val session = session ?: AgentRunner(getApplication(), agent).let { newRunner ->
                    runner = newRunner
                    newRunner.getOrCreateSession(SESSION_ID).also { session = it }
                }
                session.runAsync(Content.builder().text(text).build()).collect { event ->
                    _messages.update { it.withAgentEvent(event) }
                }
                _messages.update { it.finishAgentReply(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Agent run failed", e)
                _messages.update { it.finishAgentReply(error = e.message ?: e.javaClass.simpleName) }
            } finally {
                _busy.value = false
            }
        }
    }

    /** Forgets the conversation; the next question starts a fresh session. */
    fun clear() {
        if (_busy.value) return
        val current = session
        session = null
        _messages.value = emptyList()
        viewModelScope.launch { runCatching { current?.delete() } }
    }

    private fun unsupported(reason: String) {
        Log.i(TAG, "Ask GalwayBus unavailable: $reason")
        _availability.value = Availability.Unsupported(reason)
    }

    override fun onCleared() {
        session?.close()
        runner?.close()
    }

    private companion object {
        const val TAG = "GalwayBusAgent"
        const val SESSION_ID = "ask-galwaybus"
        val INSTRUCTIONS = """
            You help people catch Galway city buses using GalwayBus's tools.
            Never invent stops, routes or times: look them up with the tools.
            For when buses leave a stop, call getDepartures with the stop's name, number or a
            nearby place as stop. Only add routeNumber when the user names a route, and only add
            towards when they say where they're going; otherwise leave them out.
            For a stop from an earlier answer, pass its stopNumber as stop.
            For stops near a place, call findNearbyStops with that place's name as place. Never
            guess latitude and longitude.
            For stops near the user ("near me", "closest to me"), call findNearbyStops with
            nearMe true. For the next bus near them, then call getDepartures with the nearest
            stop's stopNumber.
            For the user's saved stops, call listSavedStops, then getDepartures with each stop's
            stopNumber.
            Your answer appears under a list of everything the tools returned, so never list
            departures or stops yourself. Reply with one sentence about the first one only:
            for a bus, its route, where it's going and minutesAway (and minutesLate when it
            isn't 0 or null); for stops, the nearest stop's name.
        """.trimIndent()
    }
}

/** One entry in the conversation. */
sealed interface ChatMessage {
    data class User(val text: String) : ChatMessage

    data class Agent(
        val text: String = "",
        /** AppFunctions called while answering, in order. */
        val toolCalls: List<ToolCall> = emptyList(),
        /** Departures from the latest getDepartures result, shown as cards. */
        val departures: List<DepartureItem> = emptyList(),
        /** Stops from the latest stop search, shown as cards when there are no departures. */
        val stops: List<StopItem> = emptyList(),
        val inProgress: Boolean = true,
        val error: String? = null,
    ) : ChatMessage
}

/** An AppFunction call: [function] is the bare name, such as `getDepartures`. */
data class ToolCall(val function: String, val args: Map<String, Any?>) {
    /** The call as the model made it, e.g. `getDepartures(stop=Eyre Square)`. */
    val raw: String get() = "$function(" + args.entries.joinToString { "${it.key}=${it.value}" } + ")"
}

/** One row of a departure card, read from a [dev.johnoreilly.galwaybus.functions.DepartureResponse]. */
data class DepartureItem(
    val route: String,
    val destination: String,
    val stopName: String,
    val stopNumber: String,
    val departs: String,
    val minutesAway: Int,
    val minutesLate: Int?,
)

/** One row of a stop card, read from a [dev.johnoreilly.galwaybus.functions.StopResponse]. */
data class StopItem(
    val name: String,
    val stopNumber: String,
    val towards: String?,
    val routes: List<String>,
    val distanceMetres: Int?,
)

/**
 * Folds one [AgentEvent] into the reply being built (the last message). Partial text may arrive as
 * deltas or as the text so far, so a partial that extends what we have replaces it, any other is
 * appended; a complete message replaces the partials before it.
 */
internal fun List<ChatMessage>.withAgentEvent(event: AgentEvent): List<ChatMessage> {
    val reply = lastOrNull() as? ChatMessage.Agent ?: return this
    val updated = when (event) {
        is AgentEvent.FunctionCall -> reply.copy(
            toolCalls = reply.toolCalls + ToolCall(event.name.functionName(), event.args.filterValues { it != null }),
            // The text so far was the model deciding what to call, not an answer.
            text = "",
        )
        is AgentEvent.FunctionResponse -> {
            val results = (event.response["result"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
            when (event.name.functionName()) {
                "getDepartures" -> reply.copy(departures = results.mapNotNull { it.toDeparture() })
                "findStops", "findNearbyStops", "listSavedStops" -> reply.copy(stops = results.mapNotNull { it.toStop() })
                else -> reply
            }
        }
        // Gemini Nano's own function-call syntax arrives as text before each FunctionCall
        // ("<ctrl42>call:getDepartures{...}"); shown, it flashed up as the answer.
        is AgentEvent.TextMessage -> if (event.text.contains("<ctrl")) reply else reply.copy(
            text = when {
                !event.isPartial -> event.text
                event.text.startsWith(reply.text) -> event.text
                else -> reply.text + event.text
            }
        )
        else -> reply
    }
    return dropLast(1) + updated
}

internal fun List<ChatMessage>.finishAgentReply(error: String?): List<ChatMessage> {
    val reply = lastOrNull() as? ChatMessage.Agent ?: return this
    return dropLast(1) + reply.copy(text = reply.text.trim(), inProgress = false, error = error)
}

// AppFunction ids look like "package.Class#function".
private fun String.functionName() = substringAfterLast('#').substringAfterLast('.')

private fun Map<*, *>.string(key: String): String? = this[key]?.toString()?.takeIf { it.isNotBlank() }
private fun Map<*, *>.int(key: String): Int? = when (val v = this[key]) {
    is Number -> v.toInt()
    is String -> v.toIntOrNull()
    else -> null
}

internal fun Map<*, *>.toDeparture(): DepartureItem? = DepartureItem(
    route = string("route") ?: return null,
    destination = string("destination").orEmpty(),
    stopName = string("stopName").orEmpty(),
    stopNumber = string("stopNumber").orEmpty(),
    departs = string("departs").orEmpty(),
    minutesAway = int("minutesAway") ?: return null,
    minutesLate = int("minutesLate"),
)

internal fun Map<*, *>.toStop(): StopItem? = StopItem(
    name = string("name") ?: return null,
    stopNumber = string("stopNumber") ?: return null,
    towards = string("towards"),
    routes = when (val r = this["routes"]) {
        is List<*> -> r.mapNotNull { it?.toString() }
        null -> emptyList()
        else -> listOf(r.toString())
    },
    distanceMetres = int("distanceMetres"),
)
