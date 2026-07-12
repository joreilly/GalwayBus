package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.FavouriteStop
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