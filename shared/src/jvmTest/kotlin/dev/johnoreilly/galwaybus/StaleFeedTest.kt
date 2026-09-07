package dev.johnoreilly.galwaybus

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The backend can now distinguish "no buses are running" from "we cannot reach NTA", and says so
 * with `stale`. These check the flag survives the wire into the repository, and — importantly —
 * that a backend which has never heard of the field still behaves exactly as it used to.
 */
class StaleFeedTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun repository(body: String) = GalwayBusRepository(
        HttpClient(MockEngine { respond(body, HttpStatusCode.OK, jsonHeaders) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }
    )

    private val oneBus = """
        {"latitude":53.27,"longitude":-9.05,"modified_timestamp":"","trip_duid":"T1","timetable_id":"401"}
    """.trimIndent()

    @Test
    fun `a stale bus feed is reported as stale`() = runBlocking {
        val feed = repository("""{"bus":{"401":[$oneBus]},"stale":true}""").getBusPositions()
        assertTrue(feed.stale, "The backend said this was not live data")
        assertEquals(1, feed.all.size, "Stale positions are still the best guess available, so they come through")
    }

    @Test
    fun `a live bus feed is not stale`() = runBlocking {
        val feed = repository("""{"bus":{"401":[$oneBus]},"stale":false}""").getBusPositions()
        assertFalse(feed.stale)
        assertEquals(1, feed.all.size)
    }

    @Test
    fun `an older backend that omits the flag is treated as live`() = runBlocking {
        // The field is absent from every response the deployed backend sends today. Defaulting it
        // to false is what keeps this change safe to ship before the backend is redeployed.
        val feed = repository("""{"bus":{"401":[$oneBus]}}""").getBusPositions()
        assertFalse(feed.stale, "A missing flag must not read as an outage")
        assertEquals(1, feed.all.size)
    }

    @Test
    fun `an empty stale feed still carries the flag`() = runBlocking {
        val feed = repository("""{"bus":{},"stale":true}""").getBusPositions()
        assertTrue(feed.stale, "An outage with nothing to show is exactly the case the banner exists for")
        assertTrue(feed.all.isEmpty())
    }

    @Test
    fun `forRoute picks one route out of the feed`() = runBlocking {
        val feed = repository("""{"bus":{"401":[$oneBus]},"stale":true}""").getBusPositions("401")
        assertEquals(1, feed.forRoute("401").size)
        assertTrue(feed.forRoute("409").isEmpty())
        assertTrue(feed.stale, "Staleness is a property of the feed, not of the route asked for")
    }
}
