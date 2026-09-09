package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.FavouriteStop
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.toDuration
import galwaybus.shared.generated.resources.Res
import galwaybus.shared.generated.resources.next_stop
import galwaybus.shared.generated.resources.your_stop
import org.jetbrains.compose.resources.getString
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicDesktopTest {

    // Routes, stops and route detail used to be read from a GTFS snapshot bundled in the app, so
    // these tests ran offline against real data. They now come from the backend, so the API is
    // stubbed rather than reached: the assertions below are about how the client maps and caps
    // that data, and the correctness of the data itself is tested backend-side.
    private val repository = GalwayBusRepository(injectedHttpClient = stubbedBackend())

    private fun stubbedBackend(): HttpClient {
        val engine = MockEngine { request ->
            val body = when {
                request.url.encodedPath.endsWith("/routes.json") -> ROUTES_JSON
                request.url.encodedPath.endsWith("/stops.json") -> STOPS_JSON
                request.url.encodedPath.startsWith("/routes/") -> ROUTE_401_JSON
                request.url.encodedPath.startsWith("/stops/") -> STOP_TIMES_JSON
                request.url.encodedPath.endsWith("/bus.json") -> """{"bus":{}}"""
                else -> "{}"
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        // Restore a clean persisted state so the test leaves no residue.
        repository.saveFavouriteStops(emptyList())
    }

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    // Verifies the localization pipeline the desktop language switcher relies on: Compose
    // Resources resolves strings from java.util.Locale.getDefault(), which LocalAppLocale (jvm)
    // sets when the user picks a language.
    @Test
    fun stringResourcesResolvePerLocale() = kotlinx.coroutines.runBlocking {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ga"))
            assertEquals("An chéad stad eile", getString(Res.string.next_stop))
            assertEquals("Do stad-sa", getString(Res.string.your_stop))

            Locale.setDefault(Locale.forLanguageTag("en"))
            assertEquals("Next stop", getString(Res.string.next_stop))
            assertEquals("Your stop", getString(Res.string.your_stop))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun localizedNameFollowsLocale() {
        val eyre = dev.johnoreilly.galwaybus.model.Stop(
            stop_ref = "8460B522331", stop_id = "522331",
            long_name = "Eyre Square", long_name_ga = "An Fhaiche Mhór",
            short_name = "Eyre Square", latitude = 0.0, longitude = 0.0
        )
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ga"))
            assertEquals("An Fhaiche Mhór", eyre.localizedName())
            Locale.setDefault(Locale.forLanguageTag("en"))
            assertEquals("Eyre Square", eyre.localizedName())
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun stopsCarryIrishNamesFromTranslations() = kotlinx.coroutines.runBlocking {
        // The API calls it irish_long_name; the UI reads long_name_ga.
        val eyre = repository.getStops().find { it.stop_ref == "8460B522331" }
        assertEquals("Eyre Square", eyre?.long_name)
        assertEquals("An Fhaiche Mhór", eyre?.long_name_ga)
    }

    @Test
    fun stopsCarryTheirDirection() = kotlinx.coroutines.runBlocking {
        // Distinguishes the two stops of a pair sharing a name and route; derived backend-side now.
        val eyre = repository.getStops().find { it.stop_ref == "8460B522331" }
        assertEquals("An Phairc Mhor", eyre?.direction)
    }

    @Test
    fun favouritesRoundTripThroughPersistence() {
        val favourites = listOf(
            FavouriteStop(stopRef = "8460B522333", name = "Eyre Square"),
            FavouriteStop(stopRef = "8460B520101", name = "NUIG")
        )

        repository.saveFavouriteStops(favourites)

        assertEquals(favourites, repository.getFavouriteStops())
    }

    @Test
    fun savingEmptyFavouritesClearsPersistedValue() {
        repository.saveFavouriteStops(listOf(FavouriteStop("8460B522333", "Eyre Square")))
        repository.saveFavouriteStops(emptyList())

        assertTrue(repository.getFavouriteStops().isEmpty())
    }

    @Test
    fun getStopDeparturesReturnsAtMost5() = kotlinx.coroutines.runBlocking {
        // The backend returns ten; the stop card shows five.
        val departures = repository.getStopDeparturesWithLive("8460B522331").times
        assertTrue(departures.size <= 5, "Expected at most 5 departures, but got ${departures.size}")
        assertTrue(departures.isNotEmpty(), "Expected the backend's departures to come through")
    }

    @Test
    fun getStopsReturnsValidStopIds() = kotlinx.coroutines.runBlocking {
        val stops = repository.getStops()
        assertTrue(stops.isNotEmpty())
        assertTrue(stops.any { it.stop_id.isNotEmpty() && it.stop_id != "0" }, "Expected at least some stops to have a valid stop_id")

        // stop_id arrives as a JSON number and is searched and displayed as text.
        // Check a specific well-known stop
        val eyreSquare = stops.find { it.stop_ref == "8460B522331" }
        assertEquals("522331", eyreSquare?.stop_id, "Eyre Square stop_id should be 522331")
    }

    @Test
    fun directionHeadsignsAlignWithStopLists() = kotlinx.coroutines.runBlocking {
        // Route 401 runs Pearse Stadium (Salthill / Dr. Mannix Road) <-> Parkmore (An Phairc Mhor).
        // Working out which label belongs to which stop list is the backend's job now (it takes
        // both from the same representative trip); what matters here is that the client keeps them
        // index-aligned through two separate calls onto one cached response.
        val stopLists = repository.getStopsForRoute("401")
        val headsigns = repository.getDirectionHeadsigns("401")
        assertEquals(2, stopLists.size)
        assertEquals(stopLists.size, headsigns.size, "One headsign per direction, aligned by index")

        // The label must describe where that direction is heading (its last stop), not where it starts.
        stopLists.forEachIndexed { i, stops ->
            val destination = stops.last().long_name
            val label = headsigns[i]
            // Stop list ending at Pearse Stadium is the Salthill-bound "Dr. Mannix Road" direction;
            // the one ending at the tech park is the Parkmore-bound "An Phairc Mhor" direction.
            if (destination.contains("Pearse", ignoreCase = true)) {
                assertEquals("Dr. Mannix Road", label, "Salthill-bound list mislabelled")
            } else {
                assertEquals("An Phairc Mhor", label, "Parkmore-bound list mislabelled")
            }
        }
    }

    @Test
    fun viewModelMigratesExistingFavourites() = kotlinx.coroutines.runBlocking {
        // Prepare old-style favourite without stopId (or "0" after type change)
        val oldFavourite = FavouriteStop(stopRef = "8460B522331", name = "Eyre Square", stopId = "0")
        repository.saveFavouriteStops(listOf(oldFavourite))

        val viewModel = GalwayBusViewModel(repository)
        
        // Wait for migration to run in viewModelScope
        var migrated = false
        for (i in 1..20) {
            if (viewModel.favourites.value.any { it.stopId == "522331" }) {
                migrated = true
                break
            }
            kotlinx.coroutines.delay(100)
        }
        
        assertTrue(migrated, "ViewModel should have migrated the favourite stopId to 522331")
        assertEquals("522331", viewModel.favourites.value.first().stopId)
    }

    private companion object {
        const val ROUTES_JSON = """
            {"401":{"timetable_id":401,"long_name":"Parkmore Road - Doctor Mannix Road","short_name":"401"}}
        """

        const val STOPS_JSON = """
            [{"stop_ref":"8460B522331","stop_id":522331,"long_name":"Eyre Square",
              "irish_long_name":"An Fhaiche Mhór","short_name":"Eyre Square","latitude":53.2743,
              "longitude":-9.0489,"routes":["401","409"],"direction":"An Phairc Mhor","galway":true}]
        """

        const val ROUTE_401_JSON = """
            {"route":{"timetable_id":401,"long_name":"Parkmore Road - Doctor Mannix Road","short_name":"401"},
             "stops":[
               [{"stop_ref":"8460B522331","stop_id":522331,"long_name":"Eyre Square","latitude":53.2743,"longitude":-9.0489},
                {"stop_ref":"8460B635561","stop_id":635561,"long_name":"Galway Tc Pk","latitude":53.299,"longitude":-8.9869}],
               [{"stop_ref":"8460B635561","stop_id":635561,"long_name":"Galway Tc Pk","latitude":53.299,"longitude":-8.9869},
                {"stop_ref":"8460B522011","stop_id":522011,"long_name":"Pearse Stadium","latitude":53.2618,"longitude":-9.0832}]],
             "direction_headsigns":["An Phairc Mhor","Dr. Mannix Road"],
             "shapes":[[[53.2743,-9.0489],[53.299,-8.9869]],[[53.299,-8.9869],[53.2618,-9.0832]]]}
        """

        /** Ten departures, as /stops/{ref} returns. */
        val STOP_TIMES_JSON = buildString {
            append("""{"stop":{"stop_ref":"8460B522331","stop_id":522331,"long_name":"Eyre Square","short_name":"Eyre Square","latitude":53.2743,"longitude":-9.0489},"times":[""")
            append((0 until 10).joinToString(",") { i ->
                val at = kotlin.time.Clock.System.now() +
                    ((i + 1) * 5).toDuration(kotlin.time.DurationUnit.MINUTES)
                """{"display_name":"Parkmore","timetable_id":"401","depart_timestamp":"$at","delaySeconds":0,"tripId":"trip_$i"}"""
            })
            append("]}")
        }
    }
}
