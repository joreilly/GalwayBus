package dev.johnoreilly.galwaybus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.johnoreilly.galwaybus.location.LocationProvider
import dev.johnoreilly.galwaybus.location.LocationResult
import dev.johnoreilly.galwaybus.location.NearbyStop
import dev.johnoreilly.galwaybus.location.UserLocation
import dev.johnoreilly.galwaybus.location.nearestTo
import dev.johnoreilly.galwaybus.map.MapPoint
import dev.johnoreilly.galwaybus.map.toMapPoints
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.DepartureTime
import dev.johnoreilly.galwaybus.model.FavouriteStop
import dev.johnoreilly.galwaybus.model.Route
import dev.johnoreilly.galwaybus.model.Stop
import dev.johnoreilly.galwaybus.scan.StopMatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** State of the "stops near me" screen. */
sealed interface NearbyState {
    data object Idle : NearbyState
    data object Loading : NearbyState
    /** A device fix was obtained; [nearbyStops] are ordered around [location]. */
    data class Located(val location: UserLocation) : NearbyState
    /** Location permission was refused. */
    data object PermissionDenied : NearbyState
    /** No device fix (e.g. desktop); [nearbyStops] fall back to Galway city centre. */
    data object Unavailable : NearbyState
}

class GalwayBusViewModel(private val repository: GalwayBusRepository) : ViewModel() {

    private val autoRefreshIntervalMs = 30_000L

    // Legacy safety net for an empty /bus.json the backend does not explain. The backend now
    // reports `stale`, and holds its own last-known data behind it, so this timer only covers
    // backends too old to send the flag — see [busPositionsForDisplay]. It can go once every
    // deployment is new enough.
    private val busPositionsGraceMs = 120_000L
    private var lastNonEmptyBusesMs = 0L
    private var lastNonEmptyRouteBusesMs = 0L

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    var selectedRouteNum by mutableStateOf<String?>(null)
        private set

    /** In-app UI language override (BCP-47 tag, e.g. "en"/"ga"); null follows the device locale. */
    var appLanguage: String? by mutableStateOf(readPref(LANGUAGE_PREF_KEY)?.ifBlank { null })
        private set

    fun selectAppLanguage(tag: String?) {
        appLanguage = tag
        writePref(LANGUAGE_PREF_KEY, tag ?: "")
    }

    /** UI theme override; defaults to following the system. */
    var themeMode: ThemeMode by mutableStateOf(
        readPref(THEME_PREF_KEY)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    )
        private set

    fun selectThemeMode(mode: ThemeMode) {
        themeMode = mode
        writePref(THEME_PREF_KEY, mode.name)
    }

    /**
     * True when the backend last told us the live feed was unreachable. Both bus maps read the
     * same /bus.json (one cached fetch behind the repository), so one flag covers both — the UI
     * uses it to say the feed is down rather than implying no buses are running.
     */
    private val _busFeedStale = MutableStateFlow(false)
    val busFeedStale: StateFlow<Boolean> = _busFeedStale.asStateFlow()

    private val _busPositions = MutableStateFlow<List<BusLocation>>(emptyList())
    val busPositions: StateFlow<List<BusLocation>> = _busPositions.asStateFlow()

    private val _routeStops = MutableStateFlow<List<List<Stop>>>(emptyList())
    val routeStops: StateFlow<List<List<Stop>>> = _routeStops.asStateFlow()

    // Road geometry per direction for the selected route, index-aligned with routeStops, and the
    // tracked trip's own line (which can be a variant of its direction's).
    private val _routeShapes = MutableStateFlow<List<List<MapPoint>>>(emptyList())
    val routeShapes: StateFlow<List<List<MapPoint>>> = _routeShapes.asStateFlow()

    private val _trackedShape = MutableStateFlow<List<MapPoint>>(emptyList())
    val trackedShape: StateFlow<List<MapPoint>> = _trackedShape.asStateFlow()

    // One vehicle picked out of the several running a route, so the route map can show that trip's
    // own path and times rather than the direction's generic line.
    var selectedRouteBus by mutableStateOf<BusLocation?>(null)
        private set

    private val _selectedBusShape = MutableStateFlow<List<MapPoint>>(emptyList())
    val selectedBusShape: StateFlow<List<MapPoint>> = _selectedBusShape.asStateFlow()

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

    /** True once the first all-buses fetch attempt has finished (success or not). */
    var hasLoadedAllBuses by mutableStateOf(false)
        private set

    /** When we last actually received bus positions (not just polled) — null until the first one. */
    var allBusesUpdatedMs by mutableStateOf<Long?>(null)
        private set

    private val _allStops = MutableStateFlow<List<Stop>>(emptyList())
    val allStops: StateFlow<List<Stop>> = _allStops.asStateFlow()

    // ── Stop selected on the map (departures bottom sheet) ─────────────────
    var mapStop by mutableStateOf<Stop?>(null)
        private set

    private val _mapStopDepartures = MutableStateFlow<List<DepartureTime>>(emptyList())
    val mapStopDepartures: StateFlow<List<DepartureTime>> = _mapStopDepartures.asStateFlow()

    var isLoadingMapStop by mutableStateOf(false)
        private set

    // ── Nearby stops (device location) ─────────────────────────────────────
    private val locationProvider = LocationProvider()

    var nearbyState by mutableStateOf<NearbyState>(NearbyState.Idle)
        private set

    private val _nearbyStops = MutableStateFlow<List<NearbyStop>>(emptyList())
    val nearbyStops: StateFlow<List<NearbyStop>> = _nearbyStops.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var allBusesJob: Job? = null
    private var favouritesJob: Job? = null
    private var stopDeparturesJob: Job? = null

    /**
     * Last resort for background work. An exception escaping a `launch` is fatal on iOS — it takes
     * the whole app down with a stack of nothing but coroutine machinery, which says nothing about
     * what actually failed. Guarding each launch individually was tried twice and missed a case
     * both times, so this catches whatever is left: the user sees a message instead of the app
     * vanishing, and the log names the exception so the next report is diagnosable.
     *
     * It is a net, not a substitute for handling failure where it happens — anything that can fail
     * predictably should still say something useful about it locally.
     */
    private val coroutineFailureNet = CoroutineExceptionHandler { context, throwable ->
        println("GalwayBus: unhandled failure in $context — ${throwable::class.simpleName}: ${throwable.message}")
        errorMessage = throwable.message ?: throwable::class.simpleName
    }

    /** [viewModelScope.launch] with the net attached. Use this rather than launching directly. */
    private fun launchSafely(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(coroutineFailureNet, block = block)

    init {
        _favourites.value = repository.getFavouriteStops()
        // Stops and routes come from the backend now; before they were read from a file bundled in
        // the app and could not fail, so this ran unguarded. An unhandled failure in a launch takes
        // the whole app down, so a backend hiccup must degrade to an empty list and a message.
        launchSafely {
            try {
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
            } catch (e: Exception) {
                // Nothing is cached on failure, so any later call (opening Near me, picking a
                // route) retries rather than leaving the app permanently empty.
                errorMessage = e.message ?: e::class.simpleName
            }
        }
        refreshFavouriteDepartures()
        startFavouritesPolling()
        startAllBusesPolling()
        // Restore the route the user was viewing last session.
        repository.getLastViewedRoute()?.let { selectRouteInternal(it) }
    }

    private fun startAllBusesPolling() {
        allBusesJob?.cancel()
        allBusesJob = launchSafely {
            while (isActive) {
                refreshAllBusPositions()
                delay(autoRefreshIntervalMs)
            }
        }
    }

    fun refreshAllBusPositions() {
        launchSafely {
            try {
                val feed = repository.getBusPositions()
                val fetched = feed.all
                val now = nowEpochMilliseconds()
                val previous = _allBusPositions.value
                val msSinceLastNonEmpty = now - lastNonEmptyBusesMs
                val displayed = busPositionsForDisplay(
                    fetched = fetched,
                    current = previous,
                    msSinceLastNonEmpty = msSinceLastNonEmpty,
                    graceMs = busPositionsGraceMs,
                    feedStale = feed.stale
                )
                _busFeedStale.value = feed.stale
                println(
                    "BusFeed: allBuses fetched=${fetched.size} previous=${previous.size} stale=${feed.stale} " +
                        "msSinceLastNonEmpty=$msSinceLastNonEmpty graceMs=$busPositionsGraceMs -> displayed=${displayed.size}"
                )
                _allBusPositions.value = displayed
                if (fetched.isNotEmpty()) {
                    lastNonEmptyBusesMs = now
                    allBusesUpdatedMs = now
                }
            } catch (e: Exception) {
                // Network/parse failure: keep whatever we last showed rather than blanking, but
                // stop presenting it as live — from here the map is as unreachable as a dead feed.
                _busFeedStale.value = true
                println("BusFeed: allBuses fetch FAILED ${e::class.simpleName}: ${e.message}")
            } finally {
                hasLoadedAllBuses = true
            }
        }
    }

    /**
     * Resolves OCR text scanned off a stop plate to a known stop (by its printed stop code),
     * or null if the text holds no recognisable stop code. The scan screen opens the departures
     * sheet for a match via [selectMapStop].
     */
    fun matchScannedStop(recognizedText: String): Stop? =
        StopMatcher.match(recognizedText, _allStops.value)

    /** Shows the departures sheet for a stop tapped on the map. */
    fun selectMapStop(stop: Stop) {
        mapStop = stop
        _mapStopDepartures.value = emptyList()
        launchSafely {
            isLoadingMapStop = true
            try {
                val departures = repository.getStopDeparturesWithLive(stop.stop_ref).times
                if (mapStop?.stop_ref == stop.stop_ref) _mapStopDepartures.value = departures
            } catch (_: Exception) {
            }
            isLoadingMapStop = false
        }
    }

    fun clearMapStop() {
        mapStop = null
        _mapStopDepartures.value = emptyList()
    }

    /**
     * Requests the device location (prompting for permission if needed) and lists the nearest
     * stops. With no fix available (permission refused, or desktop) it falls back to showing the
     * stops around Galway city centre so the screen is never empty.
     */
    fun loadNearby() {
        launchSafely {
            nearbyState = NearbyState.Loading
            // Stops may not have loaded at startup (no network then, or the backend was waking up),
            // so this is also the retry. A failure here must not escape the coroutine.
            val stops = try {
                _allStops.value.ifEmpty { repository.getStops().also { _allStops.value = it } }
            } catch (e: Exception) {
                errorMessage = e.message ?: e::class.simpleName
                nearbyState = NearbyState.Unavailable
                return@launchSafely
            }
            when (val result = locationProvider.currentLocation()) {
                is LocationResult.Available -> {
                    _nearbyStops.value = stops.nearestTo(result.location, NEARBY_STOP_LIMIT)
                    nearbyState = NearbyState.Located(result.location)
                }
                LocationResult.PermissionDenied -> nearbyState = NearbyState.PermissionDenied
                LocationResult.Unavailable -> {
                    _nearbyStops.value = stops.nearestTo(GALWAY_CENTRE, NEARBY_STOP_LIMIT)
                    nearbyState = NearbyState.Unavailable
                }
            }
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
        launchSafely {
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
                val departures = repository.getStopDeparturesWithLive(fav.stopRef).times
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
        favouritesJob = launchSafely {
            while (isActive) {
                delay(autoRefreshIntervalMs)
                refreshFavouriteDeparturesInternal(showLoading = false)
            }
        }
    }

    /**
     * Loads the tracked bus's own geometry once its vehicle appears in the feed. Trips on a route
     * run several shape variants (short workings, diversions), so the direction's line is only a
     * stand-in until the bus tells us which one it is driving.
     */
    fun loadTrackedShape(shapeId: String?) {
        if (shapeId == null || shapeId == loadedTrackedShapeId) return
        loadedTrackedShapeId = shapeId
        launchSafely {
            _trackedShape.value = repository.getShape(shapeId).toMapPoints()
        }
    }

    private var loadedTrackedShapeId: String? = null

    /**
     * Picks a bus on the route map. Tapping the same one again clears it, so the map goes back to
     * showing the direction as a whole.
     */
    fun selectRouteBus(bus: BusLocation) {
        if (selectedRouteBus?.trip_duid == bus.trip_duid) {
            clearRouteBusSelection()
            return
        }
        selectedRouteBus = bus
        _selectedBusShape.value = emptyList()
        val shapeId = bus.shape_id ?: return
        launchSafely { _selectedBusShape.value = repository.getShape(shapeId).toMapPoints() }
    }

    fun clearRouteBusSelection() {
        selectedRouteBus = null
        _selectedBusShape.value = emptyList()
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
        loadedTrackedShapeId = null
        _trackedShape.value = emptyList()
        trackedDeparture = null
    }

    fun selectRoute(routeNum: String) {
        clearTrackedDeparture()
        selectRouteInternal(routeNum)
    }

    private fun selectRouteInternal(routeNum: String) {
        clearRouteBusSelection()
        repository.saveLastViewedRoute(routeNum)
        selectedRouteNum = routeNum
        selectedDirection = 0
        errorMessage = null
        selectedStopRef = null
        _stopDepartures.value = emptyList()
        _busPositions.value = emptyList()
        lastNonEmptyRouteBusesMs = 0L
        launchSafely {
            isLoadingPositions = true
            try {
                _routeStops.value = repository.getStopsForRoute(routeNum)
                _directionHeadsigns.value = repository.getDirectionHeadsigns(routeNum)
                _routeShapes.value = repository.getRouteShapes(routeNum).map { it.toMapPoints() }
                val feed = repository.getBusPositions(routeNum)
                val fetched = feed.forRoute(routeNum)
                _busPositions.value = fetched
                _busFeedStale.value = feed.stale
                if (fetched.isNotEmpty()) lastNonEmptyRouteBusesMs = nowEpochMilliseconds()
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
        repository.saveLastViewedRoute(null)
        autoRefreshJob?.cancel()
        stopDeparturesJob?.cancel()
        clearTrackedDeparture()
        selectedRouteNum = null
        selectedStopRef = null
        _stopDepartures.value = emptyList()
        _busPositions.value = emptyList()
        lastNonEmptyRouteBusesMs = 0L
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
        launchSafely {
            refreshStopDeparturesInternal(stopRef, showLoading = true)
        }
        startStopDeparturesPolling(stopRef)
    }

    private suspend fun refreshStopDeparturesInternal(stopRef: String, showLoading: Boolean = false) {
        if (showLoading) isLoadingDepartures = true
        try {
            val departures = repository.getStopDeparturesWithLive(stopRef).times
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
        stopDeparturesJob = launchSafely {
            while (isActive) {
                delay(autoRefreshIntervalMs)
                refreshStopDeparturesInternal(stopRef, showLoading = false)
            }
        }
    }

    fun refreshPositions() {
        val route = selectedRouteNum ?: return
        launchSafely { refreshInternal(route, force = true) }
    }

    private fun startAutoRefresh(routeNum: String) {
        autoRefreshJob?.cancel()
        autoRefreshJob = launchSafely {
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
            val feed = repository.getBusPositions(routeNum, forceRefresh = force)
            val fetched = feed.forRoute(routeNum)
            val now = nowEpochMilliseconds()
            val previous = _busPositions.value
            val msSinceLastNonEmpty = now - lastNonEmptyRouteBusesMs
            val displayed = busPositionsForDisplay(
                fetched = fetched,
                current = previous,
                msSinceLastNonEmpty = msSinceLastNonEmpty,
                graceMs = busPositionsGraceMs,
                feedStale = feed.stale
            )
            _busFeedStale.value = feed.stale
            println(
                "BusFeed: route=$routeNum force=$force fetched=${fetched.size} previous=${previous.size} stale=${feed.stale} " +
                    "msSinceLastNonEmpty=$msSinceLastNonEmpty graceMs=$busPositionsGraceMs -> displayed=${displayed.size}"
            )
            _busPositions.value = displayed
            // Keep the picked-out bus pointing at its latest position, so its times tick along
            // with the feed rather than freezing at whatever they were when it was tapped.
            selectedRouteBus?.let { selected ->
                displayed.firstOrNull { it.trip_duid == selected.trip_duid }?.let { selectedRouteBus = it }
            }
            if (fetched.isNotEmpty()) lastNonEmptyRouteBusesMs = now
            lastUpdatedEpochMs = now
            errorMessage = null
        } catch (e: Exception) {
            // Keep showing the stale list; only surface the error if there's nothing on screen
            _busFeedStale.value = true
            println("BusFeed: route=$routeNum fetch FAILED ${e::class.simpleName}: ${e.message}")
            if (_busPositions.value.isEmpty()) {
                errorMessage = e.message ?: e::class.simpleName
            }
        }
        isRefreshing = false
    }

    private companion object {
        const val NEARBY_STOP_LIMIT = 20
        val GALWAY_CENTRE = UserLocation(53.2743, -9.0488)
    }
}
