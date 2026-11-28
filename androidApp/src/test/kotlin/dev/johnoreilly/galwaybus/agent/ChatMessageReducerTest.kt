package dev.johnoreilly.galwaybus.agent

import com.google.mlkit.genai.agent.AgentEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageReducerTest {

    private val start: List<ChatMessage> = listOf(ChatMessage.User("q"), ChatMessage.Agent())
    private val fn = "dev.johnoreilly.galwaybus.functions.BaseGalwayBusAppFunctionService#"

    private fun List<ChatMessage>.reply() = last() as ChatMessage.Agent

    @Test
    fun `partial text deltas are appended and the complete message replaces them`() {
        val messages = start
            .withAgentEvent(AgentEvent.TextMessage("The next", true))
            .withAgentEvent(AgentEvent.TextMessage(" bus", true))
        assertEquals("The next bus", messages.reply().text)
        assertEquals("Done.", messages.withAgentEvent(AgentEvent.TextMessage("Done.", false)).reply().text)
    }

    @Test
    fun `the model's raw function-call text is not shown`() {
        val messages = start.withAgentEvent(
            AgentEvent.TextMessage("<ctrl42>call:getDepartures{stop:<ctrl46>Eyre Square<ctrl46>}<ctrl43>", false)
        )
        assertEquals("", messages.reply().text)
    }

    @Test
    fun `a function call is recorded by name, and clears text written before it`() {
        val messages = start
            .withAgentEvent(AgentEvent.TextMessage("call:get", true))
            .withAgentEvent(AgentEvent.FunctionCall(fn + "getDepartures", mapOf("stop" to "Eyre Square", "towards" to null)))
        val call = messages.reply().toolCalls.single()
        assertEquals("getDepartures", call.function)
        assertEquals("getDepartures(stop=Eyre Square)", call.raw)
        assertEquals("", messages.reply().text)
    }

    @Test
    fun `departures are read from the function response`() {
        val response = mapOf(
            "result" to arrayListOf(
                mapOf("route" to "405", "destination" to "Rahoon", "stopName" to "Eyre Square", "stopNumber" to "524371",
                    "departs" to "12:02", "minutesAway" to 12L, "minutesLate" to 0L),
                mapOf("route" to "405", "destination" to "Rahoon", "stopName" to "Eyre Square", "stopNumber" to "524371",
                    "departs" to "12:22", "minutesAway" to 31),
                mapOf("destination" to "no route, dropped"),
            )
        )
        val departures = start.withAgentEvent(AgentEvent.FunctionResponse(fn + "getDepartures", response)).reply().departures
        assertEquals(2, departures.size)
        assertEquals(DepartureItem("405", "Rahoon", "Eyre Square", "524371", "12:02", 12, 0), departures[0])
        assertEquals(null, departures[1].minutesLate)
    }

    @Test
    fun `stops are read from a stop search, with one route or several`() {
        val response = mapOf(
            "result" to listOf(
                mapOf("name" to "Galway Cathdrl", "stopNumber" to "522571", "towards" to "Rahoon",
                    "routes" to listOf("402", "404"), "distanceMetres" to 41),
                mapOf("name" to "University Hospl", "stopNumber" to "522551", "routes" to "402"),
            )
        )
        val stops = start.withAgentEvent(AgentEvent.FunctionResponse(fn + "findNearbyStops", response)).reply().stops
        assertEquals(listOf("402", "404"), stops[0].routes)
        assertEquals(41, stops[0].distanceMetres)
        assertEquals(listOf("402"), stops[1].routes)
        assertEquals(null, stops[1].towards)
    }

    @Test
    fun `finishing trims the text and stops progress`() {
        val done = start.withAgentEvent(AgentEvent.TextMessage(" Hi \n", false)).finishAgentReply(null).reply()
        assertEquals("Hi", done.text)
        assertTrue(!done.inProgress)
    }

    @Test
    fun `tool calls read as a person would put them`() {
        assertEquals(
            "Departures · Eyre Square · 405 → Rahoon",
            ToolCall("getDepartures", mapOf("stop" to "Eyre Square", "routeNumber" to "405", "towards" to "Rahoon")).label(),
        )
        assertEquals("Departures · Galway Cathdrl", ToolCall("getDepartures", mapOf("stop" to "523181")).label(mapOf("523181" to "Galway Cathdrl")))
        assertEquals("Stops near you", ToolCall("findNearbyStops", mapOf("nearMe" to true)).label())
        assertEquals("Stops near Galway Cathedral", ToolCall("findNearbyStops", mapOf("place" to "Galway Cathedral")).label())
        assertEquals("Stops matching “Salthill”", ToolCall("findStops", mapOf("query" to "Salthill")).label())
    }
}
