package dev.johnoreilly.galwaybus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.DepartureTime
import dev.johnoreilly.galwaybus.model.FavouriteStop
import dev.johnoreilly.galwaybus.model.Route
import dev.johnoreilly.galwaybus.model.Stop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GalwayBusViewModel(private val repository: GalwayBusRepository) : ViewModel() {

    private val autoRefreshIntervalMs = 30_000L

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    var selectedRouteNum by mutableStateOf<String?>(null)
        private set

    private val _busPositions = MutableStateFlow<List<BusLocation>>(emptyList())
    val busPositions: StateFlow<List<BusLocation>> = _busPositions.asStateFlow()

    private val _routeStops = MutableStateFlow<List<List<Stop>>>(emptyList())
    val routeStops: StateFlow<List<List<Stop>>> = _routeStops.asStateFlow()

    private val _directionHeadsigns = MutableStateFlow<List<String>>(emptyList())
    val directionHeadsigns: StateFlow<List<String>> = _directionHeadsigns.asStateFlow()

    var selectedDirection by mutableStateOf(0)
        private set

    var isLoadingPositions by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var lastUpdatedEpochMs by mutableStateOf<Long?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedStopRef by mutableStateOf<String?>(null)
        private set

    private val _stopDepartures = MutableStateFlow<List<DepartureTime>>(emptyList())
    val stopDepartures: StateFlow<List<DepartureTime>> = _stopDepartures.asStateFlow()

    var isLoadingDepartures by mutableStateOf(false)
        private set

    // ── Favourites ──────────────────────────────────────────────────────────
    private val _favourites = MutableStateFlow<List<FavouriteStop>>(emptyList())
    val favourites: StateFlow<List<FavouriteStop>> = _favourites.asStateFlow()

    /** Upcoming departures per favourite stop, keyed by stop ref. */
    private val _favouriteDepartures = MutableStateFlow<Map<String, List<DepartureTime>>>(emptyMap())
    val favouriteDepartures: StateFlow<Map<String, List<DepartureTime>>> = _favouriteDepartures.asStateFlow()

    var isLoadingFavourites by mutableStateOf(false)
        private set

    /** When the favourite departures were last refreshed (for per-card "updated Xs ago"). */
    var favouritesUpdatedMs by mutableStateOf<Long?>(null)
        private set

    var trackedTripId by mutableStateOf<String?>(null)
        private set

    var trackedStopRef by mutableStateOf<String?>(null)
        private set

    var trackedDeparture by mutableStateOf<DepartureTime?>(null)
        private set

    private val _allBusPositions = MutableStateFlow<List<BusLocation>>(emptyList())
    val allBusPositions: StateFlow<List<BusLocation>> = _allBusPositions.asStateFlow()

    private val _allStops = MutableStateFlow<List<Stop>>(emptyList())
    val allStops: StateFlow<List<Stop>> = _allStops.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var allBusesJob: Job? = null
    private var favouritesJob: Job? = null
    private var stopDeparturesJob: Job? = null

    init {
        _favourites.value = repository.getFavouriteStops()
        viewModelScope.launch {
            _allStops.value = repository.getStops()

            // Fix for existing favourites that might have missing stopId (showing as 0 or empty)
            val current = _favourites.value
            if (current.isNotEmpty() && (current.any { it.stopId == "0" || it.stopId.isEmpty() })) {
                val allStops = repository.getStops()
                val updated = current.map { fav ->
                    if (fav.stopId == "0" || fav.stopId.isEmpty()) {
                        val stop = allStops.find { it.stop_ref == fav.stopRef }
                        if (stop != null) fav.copy(stopId = stop.stop_id) else fav
                    } else fav
                }
                if (updated != current) {
                    _favourites.value = updated
                    repository.saveFavouriteStops(updated)
                }
            }

            _routes.value = repository.getRoutes().values
                .sortedBy { it.short_name.toIntOrNull() ?: Int.MAX_VALUE }
        }
        refreshFavouriteDepartures()
        startFavouritesPolling()
        startAllBusesPolling()
    }

    private fun startAllBusesPolling() {
        allBusesJob?.cancel()
        allBusesJob = viewModelScope.launch {
            while (isActive) {
                refreshAllBusPositions()
                delay(autoRefreshIntervalMs)
            }
        }
    }

    fun refreshAllBusPositions() {
        viewModelScope.launch {
            try {
                val allBuses = repository.getBusPositions()
                _allBusPositions.value = allBuses.values.flatten()
            } catch (_: Exception) {}
        }
    }

    fun isFavourite(stopRef: String): Boolean =
        _favourites.value.any { it.stopRef == stopRef }

    /** Adds or removes the given stop from favourites and persists the change. */
    fun toggleFavourite(stop: Stop) {
        val current = _favourites.value
        val updated = if (current.any { it.stopRef == stop.stop_ref }) {
            current.filterNot { it.stopRef == stop.stop_ref }
        } else {
            current + FavouriteStop(stopRef = stop.stop_ref, name = stop.long_name, stopId = stop.stop_id)
        }
        _favourites.value = updated
        repository.saveFavouriteStops(updated)
        refreshFavouriteDepartures()
    }

    /** Removes the stop with [stopRef] from favourites and persists the change. */
    fun removeFavourite(stopRef: String) {
        val current = _favourites.value
        val updated = current.filterNot { it.stopRef == stopRef }
        if (updated.size != current.size) {
            _favourites.value = updated
            repository.saveFavouriteStops(updated)
            refreshFavouriteDepartures()
        }
    }

    fun refreshFavouriteDepartures() {
        viewModelScope.launch {
            refreshFavouriteDeparturesInternal(showLoading = true)
         }
     }

    private suspend fun refreshFavouriteDeparturesInternal(showLoading: Boolean = false) {
        val favs = _favourites.value
        if (favs.isEmpty()) {
            _favouriteDepartures.value = emptyMap()
            return
        }

        if (showLoading) isLoadingFavourites = true
        
        // Update each stop independently to show data as it arrives
        for (fav in favs) {
            try {
                val (departures, _) = repository.getStopDeparturesWithLive(fav.stopRef)
                val current = _favouriteDepartures.value.toMutableMap()
                current[fav.stopRef] = departures
                _favouriteDepartures.value = current
            } catch (_: Exception) {
                // If it fails, we keep the previous data if any, or set empty if none
                if (!_favouriteDepartures.value.containsKey(fav.stopRef)) {
                    val current = _favouriteDepartures.value.toMutableMap()
                    current[fav.stopRef] = emptyList()
                    _favouriteDepartures.value = current
                }
            }
        }
        
        favouritesUpdatedMs = nowEpochMilliseconds()
        if (showLoading) isLoadingFavourites = false
    }

    private fun startFavouritesPolling() {
        favouritesJob?.cancel()
        favouritesJob = viewModelScope.launch {
            while (isActive) {
                delay(autoRefreshIntervalMs)
                refreshFavouriteDeparturesInternal(showLoading = false)
            }
        }
    }

    fun setTrackedDeparture(departure: DepartureTime, stopRef: String) {
        trackedTripId = departure.tripId
        trackedStopRef = stopRef
        trackedDeparture = departure
        selectRouteInternal(departure.timetable_id)
    }

    fun clearTrackedDeparture() {
        trackedTripId = null
        trackedStopRef = null
        trackedDeparture = null
    }

    fun selectRoute(routeNum: String) {
        clearTrackedDeparture()
        selectRouteInternal(routeNum)
    }

    private fun selectRouteInternal(routeNum: String) {
        selectedRouteNum = routeNum
        selectedDirection = 0
        errorMessage = null
        selectedStopRef = null
        _stopDepartures.value = emptyList()
        _busPositions.value = emptyList()
        viewModelScope.launch {
            isLoadingPositions = true
            try {
                _routeStops.value = repository.getStopsForRoute(routeNum)
                _directionHeadsigns.value = repository.getDirectionHeadsigns(routeNum)
                _busPositions.value = repository.getBusPositions(routeNum)
                lastUpdatedEpochMs = nowEpochMilliseconds()
            } catch (e: Exception) {
                errorMessage = e.message ?: e::class.simpleName
                _busPositions.value = emptyList()
            }
            isLoadingPositions = false
        }
        startAutoRefresh(routeNum)
    }

    fun clearRoute() {
        autoRefreshJob?.cancel()
        stopDeparturesJob?.cancel()
        clearTrackedDeparture()
        selectedRouteNum = null
        selectedStopRef = null
        _stopDepartures.value = emptyList()
        _busPositions.value = emptyList()
        errorMessage = null
        lastUpdatedEpochMs = null
    }

    fun selectDirection(index: Int) {
        val count = _routeStops.value.size
        if (index in 0 until count) selectedDirection = index
    }

    fun toggleDirection() {
        val count = _routeStops.value.size
        if (count > 1) selectedDirection = (selectedDirection + 1) % count
    }

    fun selectStop(stopRef: String) {
        if (stopRef == selectedStopRef) {
            selectedStopRef = null
            stopDeparturesJob?.cancel()
            _stopDepartures.value = emptyList()
            return
         }
        selectedStopRef = stopRef
        _stopDepartures.value = emptyList()
        viewModelScope.launch {
            refreshStopDeparturesInternal(stopRef, showLoading = true)
        }
        startStopDeparturesPolling(stopRef)
    }

    private suspend fun refreshStopDeparturesInternal(stopRef: String, showLoading: Boolean = false) {
        if (showLoading) isLoadingDepartures = true
        try {
            val (departures, _) = repository.getStopDeparturesWithLive(stopRef)
            // Only apply if this stop is still the selected one
            if (selectedStopRef == stopRef) _stopDepartures.value = departures
        } catch (_: Exception) {
            if (selectedStopRef == stopRef && _stopDepartures.value.isEmpty()) {
                _stopDepartures.value = emptyList()
            }
        }
        if (showLoading) isLoadingDepartures = false
    }

    private fun startStopDeparturesPolling(stopRef: String) {
        stopDeparturesJob?.cancel()
        stopDeparturesJob = viewModelScope.launch {
            while (isActive) {
                delay(autoRefreshIntervalMs)
                refreshStopDeparturesInternal(stopRef, showLoading = false)
            }
        }
    }

    fun refreshPositions() {
        val route = selectedRouteNum ?: return
        viewModelScope.launch { refreshInternal(route, force = true) }
    }

    private fun startAutoRefresh(routeNum: String) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(autoRefreshIntervalMs)
                refreshInternal(routeNum, force = false)
            }
        }
    }

    /** Refreshes positions without clearing the current list. */
    private suspend fun refreshInternal(routeNum: String, force: Boolean) {
        if (isLoadingPositions || isRefreshing) return
        isRefreshing = true
        try {
            _busPositions.value = repository.getBusPositions(routeNum, forceRefresh = force)
            lastUpdatedEpochMs = nowEpochMilliseconds()
            errorMessage = null
        } catch (e: Exception) {
            // Keep showing the stale list; only surface the error if there's nothing on screen
            if (_busPositions.value.isEmpty()) {
                errorMessage = e.message ?: e::class.simpleName
            }
        }
        isRefreshing = false
    }
}
