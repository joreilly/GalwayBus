package dev.johnoreilly.galwaybus.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import co.touchlab.kermit.Kermit
import com.surrus.galwaybus.common.GalwayBusDeparture
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Location
import com.surrus.galwaybus.common.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


sealed class UiState<out T: Any> {
    object Loading : UiState<Nothing>()
    data class Success<out T : Any>(val data: T) : UiState<T>()
    data class Error(val exception: Exception) : UiState<Nothing>()
}


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


@ExperimentalCoroutinesApi
class GalwayBusViewModel(
        application: Application,
        private val galwayBusRepository: GalwayBusRepository,
        private val logger: Kermit
) : AndroidViewModel(application) {

    val busStopListState = MutableStateFlow<UiState<List<BusStop>>>(UiState.Loading)

    var currentBusStop =  MutableStateFlow<BusStop?>(null)

    val busDepartureList = currentBusStop.filterNotNull().flatMapLatest { pollBusDepartures(it.stopRef) }

    val routeId = MutableStateFlow<String>("")
    val busInfoList = routeId.flatMapLatest { pollBusInfoForRoute(it)  }

    val location = MutableStateFlow<Location?>(null)

    val cameraPosition = MutableStateFlow<Location?>(null)
    private val zoomLevel = MutableStateFlow<Float>(15.0f)

    private val context = application
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    val favorites: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY] ?: emptySet()
    }

    val favoriteBusStopList = galwayBusRepository.getBusStopsFlow().combine(favorites) { busStops, favorites ->
        favorites.map { favorite -> busStops.firstOrNull { it.stop_id == favorite } }.filterNotNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    fun fetchAndStoreBusStops() {
        viewModelScope.launch {
            galwayBusRepository.fetchAndStoreBusStops()
        }
    }

    fun setLocation(loc: Location) {
        location.value = loc
        getNearestStops(loc)
    }

    fun setZoomLevel(zl: Float) {
        zoomLevel.value = zl
    }

    fun getZoomLevel(): Float {
        return zoomLevel.value ?: 15.0f
    }

    fun setCameraPosition(loc: Location) {
        cameraPosition.value = loc
    }

    fun setCurrentStop(busStop: BusStop) {
        currentBusStop.value = busStop
    }

    fun setRouteId(routeIdValue: String) {
        routeId.value = routeIdValue
    }

    private fun getNearestStops(location: Location) {
        viewModelScope.launch {
            val result = galwayBusRepository.fetchNearestStops(location.latitude, location.longitude)
            busStopListState.value = when (result) {
                is Result.Success -> UiState.Success(result.data)
                is Result.Error -> UiState.Error(result.exception)
            }
        }
    }

    private fun pollBusDepartures(stopRef: String): Flow<List<GalwayBusDeparture>> = flow {
        emit(emptyList())
        while (true) {
            val result = galwayBusRepository.fetchBusStopDepartures(stopRef)
            if (result is Result.Success) {
                logger.d("GalwayBusViewModel") { result.data.toString() }
                emit(result.data)
            }
            delay(POLL_INTERVAL)
        }
    }

    fun pollBusInfoForRoute(routeId: String): Flow<List<Bus>> = flow {
        if (routeId.isNotEmpty()) {
            emit(emptyList())
            while (true) {
                val result = galwayBusRepository.fetchBusListForRoute(routeId)
                if (result is Result.Success) {
                    logger.d("GalwayBusViewModel") { result.data.toString() }
                    emit(result.data)
                }
                delay(POLL_INTERVAL)
            }
        }
    }


    fun toggleFavorite(stopRef: String) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                val currentFavorites = settings[FAVORITES_KEY] ?: emptySet()
                val newFavorites = currentFavorites.toMutableSet()
                if (!newFavorites.add(stopRef)) {
                    newFavorites.remove(stopRef)
                }
                settings[FAVORITES_KEY] = newFavorites
            }
        }
    }

    fun centerInEyreSquare() {
        setLocation(Location(53.2743394, -9.0514163))
    }

    companion object {
        private const val POLL_INTERVAL =  10000L
    }
}