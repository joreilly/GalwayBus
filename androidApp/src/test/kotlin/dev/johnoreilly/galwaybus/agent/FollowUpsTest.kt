package dev.johnoreilly.galwaybus.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpsTest {

    private fun departure(route: String, stop: String = "Eyre Square", number: String = "522571") =
        DepartureItem(route, "Somewhere", stop, number, "12:00", 5, 0)

    private fun reply(
        call: ToolCall? = null,
        departures: List<DepartureItem> = emptyList(),
        stops: List<StopItem> = emptyList(),
    ) = ChatMessage.Agent(toolCalls = listOfNotNull(call), departures = departures, stops = stops, inProgress = false)

    @Test
    fun `departures towards a place offer the return trip and stops near it`() {
        val prompts = followUpPrompts(
            reply(ToolCall("getDepartures", mapOf("stop" to "Eyre Square", "towards" to "Salthill")), listOf(departure("401"))),
            "When's the next bus from Eyre Square to Salthill?",
        )
        assertEquals(
            listOf(
                "When's the next bus from Salthill to Eyre Square, stop 522571?",
                "When's the next bus from Eyre Square, stop 522571?",
                "Which bus stops are near Salthill?",
            ),
            prompts,
        )
    }

    @Test
    fun `departures on several routes offer the first two by route, by stop name`() {
        val prompts = followUpPrompts(
            reply(ToolCall("getDepartures", mapOf("stop" to "522571")), listOf(departure("404", "Galway Cathdrl"), departure("405", "Galway Cathdrl"), departure("402", "Galway Cathdrl"))),
            null,
        )
        assertEquals(
            listOf("When's the next 404 from Galway Cathdrl, stop 522571?", "When's the next 405 from Galway Cathdrl, stop 522571?"),
            prompts,
        )
    }

    @Test
    fun `departures from several stops keep the name that was asked about`() {
        val prompts = followUpPrompts(
            reply(
                ToolCall("getDepartures", mapOf("stop" to "Salthill", "towards" to "Eyre Square")),
                listOf(departure("401", "Salthill GAA", "522171"), departure("401", "Salthill Prom", "522211")),
            ),
            null,
        )
        assertEquals("When's the next bus from Eyre Square to Salthill?", prompts.first())
        assertTrue(prompts.none { "GAA" in it })
    }

    @Test
    fun `a route filter offers every route`() {
        val prompts = followUpPrompts(
            reply(ToolCall("getDepartures", mapOf("stop" to "Eyre Square", "routeNumber" to "405")), listOf(departure("405"))),
            null,
        )
        assertEquals(listOf("When's the next bus from Eyre Square, stop 522571?"), prompts)
    }

    @Test
    fun `a name that covers several stops stays a name`() {
        val prompts = followUpPrompts(
            reply(
                ToolCall("getDepartures", mapOf("stop" to "Eyre Square")),
                listOf(departure("404", number = "522611"), departure("405", number = "524371")),
            ),
            null,
        )
        assertEquals(listOf("When's the next 404 from Eyre Square?", "When's the next 405 from Eyre Square?"), prompts)
    }

    @Test
    fun `stops offer the nearest one's buses and the other side of the road`() {
        val stops = listOf(
            StopItem("Galway Cathdrl", "522571", "Rahoon", listOf("402", "404", "405"), 41),
            StopItem("Galway Cathdrl", "523181", "Ballybane", listOf("402", "404", "405"), 77),
            StopItem("St Francis Street", "523021", "Rahoon", listOf("402", "404", "405", "407"), 251),
        )
        val prompts = followUpPrompts(reply(ToolCall("findNearbyStops", mapOf("place" to "Galway Cathedral")), stops = stops), null)
        assertEquals(
            listOf(
                "When are the next buses from Galway Cathdrl, stop 522571?",
                "When are the next buses from Galway Cathdrl, stop 523181?",
            ),
            prompts,
        )
    }

    @Test
    fun `no results offer questions that work anywhere, without repeating a failed near-me`() {
        val afterNearMe = followUpPrompts(reply(ToolCall("findNearbyStops", mapOf("nearMe" to true))), null)
        assertEquals(listOf("When's the next bus from Eyre Square?"), afterNearMe)
        val afterNothing = followUpPrompts(reply(), "hello")
        assertTrue("Which bus stop is nearest me?" in afterNothing)
    }

    @Test
    fun `never repeats the question just asked, and nothing while answering or after an error`() {
        val asked = "When's the next bus from Eyre Square?"
        assertTrue(asked !in followUpPrompts(reply(), asked))
        assertEquals(emptyList<String>(), followUpPrompts(ChatMessage.Agent(), null))
        assertEquals(emptyList<String>(), followUpPrompts(ChatMessage.Agent(inProgress = false, error = "boom"), null))
    }
}
