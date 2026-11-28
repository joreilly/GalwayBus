package dev.johnoreilly.galwaybus.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.johnoreilly.galwaybus.RouteBadge
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * An "Ask" button for the top app bar that opens a chat with the on-device agent. Shows nothing
 * until the agent is known to work on this device.
 */
@Composable
fun AskGalwayBusAction(viewModel: GalwayBusAgentViewModel) {
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }

    // Only once the model is known to be usable, so the button doesn't flash up and vanish on the
    // many devices where it isn't. In the app bar rather than a floating button, which would sit
    // over the bottom navigation and the map's own refresh button.
    if (availability == GalwayBusAgentViewModel.Availability.Ready ||
        availability is GalwayBusAgentViewModel.Availability.Downloading
    ) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "Ask GalwayBus")
        }
    }
    if (open) AskGalwayBusSheet(viewModel, onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskGalwayBusSheet(viewModel: GalwayBusAgentViewModel, onDismiss: () -> Unit) {
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val ready = availability == GalwayBusAgentViewModel.Availability.Ready
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastReply = messages.lastOrNull() as? ChatMessage.Agent

    // Animate to a new message, but follow a streaming reply without animating: restarting an
    // animation on every chunk of text made the list jitter.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(lastReply?.text?.length, lastReply?.departures, lastReply?.stops) {
        if (messages.isNotEmpty() && !listState.isScrollInProgress) listState.scrollToItem(messages.lastIndex)
    }
    // Once the reply is complete, keep its end in view while it settles: the scroll above is
    // skipped while another is animating, and the reply grows after it finishes, when the
    // Markdown render replaces the plain text. Either left the answer below the input.
    LaunchedEffect(lastReply?.inProgress) {
        if (messages.isEmpty()) return@LaunchedEffect
        withTimeoutOrNull(1_500) {
            snapshotFlow { listState.canScrollForward }.collect { more ->
                if (more) listState.scrollToItem(messages.lastIndex, scrollOffset = Int.MAX_VALUE)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        // Put the keyboard away once a question is asked: left open, it takes most of the sheet
        // and only the first line or two of the answer is visible. Read inside the sheet, which
        // is its own window: from outside, this acts on the activity's window instead. Focus
        // stays put: clearing it moved focus to the drag handle, which then showed a grey box.
        val keyboard = LocalSoftwareKeyboardController.current
        val ask = { question: String ->
            keyboard?.hide()
            viewModel.ask(question)
        }
        Column(Modifier.fillMaxHeight(0.85f).imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Ask GalwayBus", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "On-device Gemini Nano, using GalwayBus's AppFunctions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::clear, enabled = messages.isNotEmpty() && !busy) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear conversation")
                }
            }

            when (val a = availability) {
                GalwayBusAgentViewModel.Availability.Checking ->
                    Status("Checking for Gemini Nano…", progress = true)
                is GalwayBusAgentViewModel.Availability.Downloading ->
                    Status("Downloading Gemini Nano" + (a.bytes?.let { " (${it / 1_000_000} MB)" } ?: "") + "…", progress = true)
                is GalwayBusAgentViewModel.Availability.Unsupported -> Status(a.reason, progress = false)
                GalwayBusAgentViewModel.Availability.Ready -> Unit
            }
            HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (messages.isEmpty() && ready) {
                    item { Suggestions(onPick = ask) }
                }
                itemsIndexed(messages) { index, message ->
                    when (message) {
                        is ChatMessage.User -> UserBubble(message)
                        is ChatMessage.Agent -> AgentReply(
                            message,
                            // Only the latest reply's stops ask a follow-up; older ones are history.
                            onStopClick = if (index == messages.lastIndex && !busy) {
                                { stop -> ask("When are the next buses from ${stop.name}, stop ${stop.stopNumber}?") }
                            } else null,
                        )
                    }
                }
            }

            val send = {
                if (input.isNotBlank() && !busy && ready) {
                    ask(input)
                    input = ""
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                enabled = ready,
                placeholder = { Text("Ask about Galway buses") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                trailingIcon = {
                    IconButton(onClick = send, enabled = input.isNotBlank() && !busy && ready) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun Status(text: String, progress: Boolean) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (progress) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Try asking", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            // Each is one or two calls. The places work because the functions look them up
            // themselves; models asked for coordinates guess them.
            Icons.Filled.Schedule to "When's the next bus from Eyre Square?",
            Icons.Filled.DirectionsBus to "When does the next 405 leave Eyre Square for Rahoon?",
            Icons.Filled.Place to "Which bus stops are near Galway Cathedral?",
            Icons.Filled.NearMe to "Which bus stop is nearest me?",
        ).forEach { (icon, suggestion) ->
            AssistChip(
                onClick = { onPick(suggestion) },
                label = { Text(suggestion) },
                leadingIcon = { Icon(icon, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
            )
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage.User) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(message.text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun AgentReply(message: ChatMessage.Agent, onStopClick: ((StopItem) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Which AppFunctions the agent called: shows the tools are doing the work, not the model's
        // memory. Tapping one shows the call exactly as the model made it.
        // A stop the model passed by number reads better by name, which the results carry.
        val stopNames = message.departures.associate { it.stopNumber to it.stopName } +
            message.stops.associate { it.stopNumber to it.name }
        ToolCalls(message.toolCalls, stopNames)

        // The tool results themselves, as the app shows them, so the times on screen come from
        // the data rather than from the model's retelling of it.
        when {
            message.departures.isNotEmpty() -> DeparturesCard(message.departures)
            message.stops.isNotEmpty() -> StopsCard(message.stops, onStopClick)
        }

        when {
            // Gemini Nano answers in Markdown (bold names, numbered and bulleted lists). The reply
            // streams in, and each change is re-parsed: retainState keeps the last render on
            // screen meanwhile, where the default blanks it to a loading state, which flashed.
            message.text.isNotEmpty() -> Markdown(
                rememberMarkdownState(message.text, retainState = true),
                // The default for both is an empty box: a reply that hadn't parsed yet, or
                // didn't parse, drew nothing at all (seen on a reply with no tool calls).
                loading = { Text(message.text, it) },
                error = { Text(message.text, it) },
            )
            message.inProgress -> Working(message.toolCalls.lastOrNull())
        }
        message.error?.let {
            Text("Something went wrong: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolCalls(calls: List<ToolCall>, stopNames: Map<String, String>) {
    if (calls.isEmpty()) return
    var showRaw by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        calls.forEach { call ->
            AssistChip(
                onClick = { showRaw = !showRaw },
                label = {
                    Text(
                        if (showRaw) call.raw else call.label(stopNames),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = if (showRaw) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(call.icon(), contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                modifier = Modifier.semantics { contentDescription = "Called ${call.raw}" },
            )
        }
    }
}

/** What the agent is doing while there's no answer yet. */
@Composable
private fun Working(lastCall: ToolCall?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            when (lastCall?.function) {
                null -> "Thinking…"
                "getDepartures" -> "Checking departures…"
                "listRoutes" -> "Looking up routes…"
                "listSavedStops" -> "Looking up your saved stops…"
                else -> "Finding stops…"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ToolCall.arg(name: String): String? = args[name]?.toString()?.takeIf { it.isNotBlank() }

/** A tool call as a person would put it, e.g. "Departures · Eyre Square → Salthill". */
internal fun ToolCall.label(stopNames: Map<String, String> = emptyMap()): String = when (function) {
    "getDepartures" -> buildString {
        val stop = arg("stop")
        append("Departures · ").append(stop?.let { stopNames[it] ?: it } ?: "stop")
        arg("routeNumber")?.let { append(" · ").append(it) }
        arg("towards")?.let { append(" → ").append(it) }
    }
    "findNearbyStops" -> when {
        args["nearMe"] == true || arg("place") == null && arg("latitude") == null -> "Stops near you"
        arg("place") != null -> "Stops near ${arg("place")}"
        else -> "Stops near a location"
    }
    "findStops" -> "Stops matching “${arg("query").orEmpty()}”"
    "listRoutes" -> "Routes"
    "listSavedStops" -> "Your saved stops"
    else -> function
}

private fun ToolCall.icon(): ImageVector = when (function) {
    "getDepartures" -> Icons.Filled.Schedule
    "findNearbyStops" -> if (arg("place") == null) Icons.Filled.NearMe else Icons.Filled.Place
    "findStops" -> Icons.Filled.Search
    "listRoutes" -> Icons.AutoMirrored.Filled.List
    "listSavedStops" -> Icons.Filled.Star
    else -> Icons.Filled.Build
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun DeparturesCard(departures: List<DepartureItem>) {
    // Name the stop on each row only when they leave from different stops (a name like Eyre
    // Square covers several); otherwise it's the same on every row.
    val severalStops = departures.map { it.stopName }.distinct().size > 1
    ResultCard {
        departures.take(MAX_CARD_ROWS).forEachIndexed { index, d ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                RouteBadge(d.route, large = index == 0)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.destination, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val detail = listOfNotNull(
                        d.stopName.takeIf { severalStops }?.let { "from $it" },
                        d.lateness(),
                    ).joinToString(" · ")
                    if (detail.isNotEmpty()) {
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DepartureTime(d, emphasised = index == 0)
            }
        }
    }
}

/** "Due", a countdown, or the clock time further out, as the app's own lists show it. */
@Composable
private fun DepartureTime(d: DepartureItem, emphasised: Boolean) {
    val live = d.minutesLate != null
    val color = when {
        !live -> MaterialTheme.colorScheme.onSurfaceVariant // timetable only
        d.minutesLate!! > 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (live) {
            Icon(Icons.Filled.RssFeed, contentDescription = "Live", tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            when {
                d.minutesAway <= 0 -> "Due"
                d.minutesAway < CLOCK_TIME_FROM_MINUTES -> "${d.minutesAway} min"
                else -> d.departs
            },
            style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
        )
    }
}

private fun DepartureItem.lateness(): String? = when {
    minutesLate == null -> "Timetabled"
    minutesLate > 0 -> "$minutesLate min late"
    minutesLate < 0 -> "${-minutesLate} min early"
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StopsCard(stops: List<StopItem>, onStopClick: ((StopItem) -> Unit)?) {
    ResultCard {
        stops.take(MAX_CARD_ROWS).forEachIndexed { index, stop ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onStopClick != null) Modifier.clickable { onStopClick(stop) } else Modifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stop.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        listOfNotNull(stop.towards?.let { "Towards $it" }, "Stop ${stop.stopNumber}", stop.distanceMetres?.let(::distanceLabel))
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (stop.routes.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                            stop.routes.forEach { RouteBadge(it, large = false) }
                        }
                    }
                }
                if (onStopClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next buses from ${stop.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun distanceLabel(metres: Int): String =
    if (metres < 1_000) "$metres m" else String.format(Locale.getDefault(), "%.1f km", metres / 1_000.0)

private const val MAX_CARD_ROWS = 6

// Matches the app's own lists (DepartureDisplay.kt): further out than this, a clock time is
// easier to plan with than a countdown.
private const val CLOCK_TIME_FROM_MINUTES = 20
