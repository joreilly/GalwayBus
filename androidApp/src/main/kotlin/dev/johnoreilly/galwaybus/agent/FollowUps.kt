package dev.johnoreilly.galwaybus.agent

/**
 * Questions worth asking next, built from what a finished reply found: its tool calls and their
 * results. Built here rather than asked of the model, which would mean a second generation after
 * every answer, and from a small model, suggestions the tools can't answer. Each is phrased the
 * way the agent already handles well (a stop by name or number, an explicit destination), and
 * none repeats [lastQuestion].
 */
internal fun followUpPrompts(reply: ChatMessage.Agent, lastQuestion: String?): List<String> {
    if (reply.inProgress || reply.error != null) return emptyList()
    val lastCall = reply.toolCalls.lastOrNull()
    val prompts = when {
        reply.departures.isNotEmpty() -> departureFollowUps(reply.departures, lastCall)
        reply.stops.isNotEmpty() -> stopFollowUps(reply.stops)
        // Nothing found, or the agent had to ask (no location, an unknown place): offer the
        // questions that work anywhere in the city, without repeating the one that just failed.
        else -> listOfNotNull(
            NEAREST_ME.takeUnless { lastCall?.function == "findNearbyStops" && lastCall.arg("place") == null },
            "When's the next bus from Eyre Square?",
        )
    }
    return prompts
        .filterNot { it.equals(lastQuestion?.trim(), ignoreCase = true) }
        .distinct()
        .take(MAX_FOLLOW_UPS)
}

private fun departureFollowUps(departures: List<DepartureItem>, call: ToolCall?): List<String> {
    // The stop as the user knows it. From one stop, its name and number: the name alone covers
    // both sides of the road, so a follow-up by name could answer for the other one. From
    // several stops, the name that was asked about, which covers them all ("Salthill" is six
    // stops, "Eyre Square" seven).
    val asked = call?.arg("stop")
    val first = departures.first()
    val stop = when {
        departures.map { it.stopNumber }.distinct().size == 1 && first.stopName.isNotBlank() ->
            "${first.stopName}, stop ${first.stopNumber}"
        asked != null -> asked
        else -> first.stopName.ifBlank { return emptyList() }
    }
    val towards = call?.arg("towards")
    val route = call?.arg("routeNumber")
    val routes = departures.map { it.route }.distinct()
    return buildList {
        if (towards != null) add("When's the next bus from $towards to $stop?")
        if (route == null && routes.size > 1) {
            // The routes in the order they're due, so the first is the soonest.
            routes.take(2).forEach { add("When's the next $it from $stop?") }
        }
        if (route != null || towards != null) add("When's the next bus from $stop?")
        if (towards != null) add("Which bus stops are near $towards?")
    }
}

private fun stopFollowUps(stops: List<StopItem>): List<String> {
    val nearest = stops.first()
    return buildList {
        // The same question tapping its row asks.
        add("When are the next buses from ${nearest.name}, stop ${nearest.stopNumber}?")
        // No "which of these does the 407 stop at?": Gemini Nano read it as departures for the
        // 407 at the nearest stop, and answered that there were none.
        // The other side of the road: the nearest stop's namesake, heading the other way.
        stops.drop(1).firstOrNull { it.name == nearest.name && it.towards != nearest.towards }?.let { other ->
            add("When are the next buses from ${other.name}, stop ${other.stopNumber}?")
        }
    }
}

private fun ToolCall.arg(name: String): String? = args[name]?.toString()?.takeIf { it.isNotBlank() }

private const val NEAREST_ME = "Which bus stop is nearest me?"
private const val MAX_FOLLOW_UPS = 3
