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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


sealed class UiState<out T: Any> {
    object Loading : UiState<Nothing>()
    data class Success<out T : Any>(val data: T) : UiState<T>()
    data class Error(val exception: Exception) : UiState<Nothing>()
}


class GalwayBusViewModel(
        application: Application,
        private val galwayBusRepository: GalwayBusRepository,
        private val logger: Kermit
) : AndroidViewModel(application) {

    val busStopListState = MutableLiveData<UiState<List<BusStop>>>()

    val stopRef = MutableLiveData<String>("")
    val busDepartureList = stopRef.switchMap { pollBusDepartures(it).asLiveData() }

    val routeId = MutableLiveData<String>("")
    val busInfoList = routeId.switchMap { pollBusInfoForRoute(it).asLiveData() }

    //var busStops = listOf<BusStop>()
    val busStops = galwayBusRepository.getBusStopsFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())



    val location: MutableLiveData<Location> = MutableLiveData()

    val cameraPosition: MutableLiveData<Location> = MutableLiveData()
    private val zoomLevel: MutableLiveData<Float> = MutableLiveData(15.0f)

    private val context = application
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val favorites: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
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

    fun setStopRef(stopRefValue: String) {
        stopRef.value = stopRefValue
    }

    fun setRouteId(routeIdValue: String) {
        routeId.value = routeIdValue
    }

    fun getBusStop(stopRef: String): BusStop? {
        val busStop = busStops.value.firstOrNull { it.stop_id == stopRef }
        logger.i {  "getBusStop, stopRef = $stopRef, busStop = $busStop, list size = ${busStops.value.size}" }
        return busStop
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