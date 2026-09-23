package dev.johnoreilly.galwaybus

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** How a stop's departures combine /stops with /bus.json, and what happens when either fails. */
class DepartureVehicleTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val stop = "STOP"

    private fun inMinutes(m: Long) = Instant.now().plusSeconds(m * 60).epochSecond.let { Instant.ofEpochSecond(it).toString() }

    private fun departure(trip: String, minutes: Long) =
        """{"display_name":"City","timetable_id":"401","depart_timestamp":"${inMinutes(minutes)}","tripId":"$trip"}"""

    private fun bus(vehicle: String, trip: String) = """
        {"latitude":53.27,"longitude":-9.05,"modified_timestamp":"","trip_duid":"$trip","vehicle_id":"$vehicle",
         "timetable_id":"401","next_stops":[{"stop_ref":"$stop","stop_sequence":3}]}"""

    private fun repository(stops: String?, buses: String?) = GalwayBusRepository(
        HttpClient(MockEngine { request ->
            val body = if (request.url.encodedPath.startsWith("/stops/")) stops else buses
            if (body == null) respondError(HttpStatusCode.InternalServerError) else respond(body, HttpStatusCode.OK, jsonHeaders)
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }
    )

    @Test
    fun `departures still arrive when bus positions can't be fetched`() = runBlocking {
        val times = repository(stops = """{"times":[${departure("T1", 5)}]}""", buses = null)
            .getStopDeparturesWithLive(stop).times
        assertEquals(listOf("T1"), times.map { it.tripId })
        assertNull(times.single().vehicleId, "No positions, so no vehicle — but the departure stands")
    }

    @Test
    fun `a failed departures fetch is an error, not an empty stop`() {
        // Callers keep what is on screen when this throws; an empty list would replace it.
        assertFailsWith<Exception> {
            runBlocking { repository(stops = null, buses = """{"bus":{}}""").getStopDeparturesWithLive(stop) }
        }
    }

    @Test
    fun `a bus running a later departure's trip is not lent to an earlier departure`() = runBlocking {
        // T1's bus isn't in the feed; B2 is running T2, the next departure. B2 has this stop ahead,
        // which used to be enough to attach it to T1 — leaving T2 without its own bus.
        val times = repository(
            stops = """{"times":[${departure("T1", 3)},${departure("T2", 12)}]}""",
            buses = """{"bus":{"401":[${bus("B2", "T2")}]}}"""
        ).getStopDeparturesWithLive(stop).times
        assertEquals(listOf("T1" to null, "T2" to "B2"), times.map { it.tripId to it.vehicleId })
    }

    @Test
    fun `a bus on an unlisted trip still fills in for a departure with no exact match`() = runBlocking {
        // The fallback is still wanted when the bus's trip isn't any listed departure's.
        val times = repository(
            stops = """{"times":[${departure("T1", 3)}]}""",
            buses = """{"bus":{"401":[${bus("B9", "T9")}]}}"""
        ).getStopDeparturesWithLive(stop).times
        assertEquals("B9", times.single().vehicleId)
    }
}
