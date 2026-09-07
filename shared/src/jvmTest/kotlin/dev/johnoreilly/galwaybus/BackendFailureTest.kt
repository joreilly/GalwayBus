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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Routes and stops used to be read from a GTFS snapshot bundled in the app, which could not fail,
 * so the coroutines loading them were written unguarded. Once that data moved to the backend, a
 * transient failure — the service scaling from zero, a dropped connection — threw inside a bare
 * `launch`, and an unhandled exception in a coroutine terminates the app. It has to degrade.
 */
class BackendFailureTest {

    // A real dispatcher, not a test one: the repository retries with `delay`, and virtual time
    // would never advance here, so the work would simply never finish.
    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() = Dispatchers.setMain(Dispatchers.Default)

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun failingBackend(): HttpClient {
        val engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }
    }

    private fun viewModelOnFailingBackend() =
        GalwayBusViewModel(GalwayBusRepository(injectedHttpClient = failingBackend()))

    /** Long enough for the repository's retry ladder (0.4s + 0.8s) to run out. */
    private suspend fun waitForRetries() = delay(3_000)

    @Test
    fun `a backend outage at startup leaves the app running`() {
        runBlocking {
            val viewModel = viewModelOnFailingBackend()
            waitForRetries()
            assertTrue(viewModel.routes.value.isEmpty(), "No routes should be loaded")
            assertNotNull(viewModel.errorMessage, "The failure should be reported, not swallowed")
        }
    }

    @Test
    fun `asking for nearby stops during an outage reports rather than crashes`() {
        runBlocking {
            val viewModel = viewModelOnFailingBackend()
            waitForRetries()
            viewModel.loadNearby()
            waitForRetries()
            assertEquals(NearbyState.Unavailable, viewModel.nearbyState)
        }
    }

    @Test
    fun `a backend that answers with nonsense is survivable too`() {
        // The guards catch network failures; this is the other shape — a 200 whose body doesn't
        // parse, which throws deeper inside and from a different coroutine. Nothing should escape:
        // on iOS an exception leaving a launch takes the whole app down.
        val engine = MockEngine {
            respond(
                content = "not json at all",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }
        runBlocking {
            val viewModel = GalwayBusViewModel(GalwayBusRepository(injectedHttpClient = client))
            viewModel.selectRoute("401")
            viewModel.loadNearby()
            waitForRetries()
            assertTrue(viewModel.routes.value.isEmpty(), "Nothing should have parsed")
            assertNotNull(viewModel.errorMessage, "The failure should surface rather than vanish")
        }
    }

    @Test
    fun `a stop's departures survive an outage`() {
        // This path was already guarded; the assertion pins it down so it stays that way.
        runBlocking {
            val viewModel = viewModelOnFailingBackend()
            viewModel.selectStop("8460B522331")
            waitForRetries()
            assertTrue(viewModel.stopDepartures.value.isEmpty())
        }
    }
}
