package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.FavouriteStop
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

    private val repository = GalwayBusRepository()

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
    fun stopsCarryIrishNamesFromTranslations() = kotlinx.coroutines.runBlocking {
        val eyre = repository.getStops().find { it.stop_ref == "8460B522331" }
        assertEquals("Eyre Square", eyre?.long_name)
        assertEquals("An Fhaiche Mhór", eyre?.long_name_ga)
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
        // Eyre Square stop typically has many departures
        val departures = repository.getStopDepartures("8460B522331")
        assertTrue(departures.size <= 5, "Expected at most 5 departures, but got ${departures.size}")
    }

    @Test
    fun getStopsReturnsValidStopIds() = kotlinx.coroutines.runBlocking {
        val stops = repository.getStops()
        assertTrue(stops.isNotEmpty())
        assertTrue(stops.any { it.stop_id.isNotEmpty() && it.stop_id != "0" }, "Expected at least some stops to have a valid stop_id")
        
        // Check a specific well-known stop
        val eyreSquare = stops.find { it.stop_ref == "8460B522331" }
        assertEquals("522331", eyreSquare?.stop_id, "Eyre Square stop_id should be 522331")
    }

    @Test
    fun directionHeadsignsAlignWithStopLists() = kotlinx.coroutines.runBlocking {
        // Route 401 runs Pearse Stadium (Salthill / Dr. Mannix Road) <-> Parkmore (An Phairc Mhor).
        // The snapshot's routeStops order does not follow GTFS direction index, so the label for
        // each stop list must be derived from the trips serving it, not from the list index.
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
}