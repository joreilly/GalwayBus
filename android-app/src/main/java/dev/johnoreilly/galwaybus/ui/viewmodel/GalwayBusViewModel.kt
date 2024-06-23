@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.johnoreilly.galwaybus.ui.viewmodel

import androidx.lifecycle.*
import co.touchlab.kermit.Logger
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.GalwayBusDeparture
import com.surrus.galwaybus.common.model.Location
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.common.remote.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


sealed class UiState<out T : Any> {
    object Loading : UiState<Nothing>()
    data class Success<out T : Any>(val data: T) : UiState<T>()
    data class Error(val exception: Exception) : UiState<Nothing>()
}


class GalwayBusViewModel(
    private val repository: GalwayBusRepository
) : ViewModel() {

    val busStopListState = MutableStateFlow<UiState<List<BusStop>>>(UiState.Loading)

    var currentBusStop = MutableStateFlow<BusStop?>(null)

    val busDepartureList = currentBusStop.filterNotNull().flatMapLatest { pollBusDepartures(it.stopRef) }

    val routeId = MutableStateFlow<String>("")
    val busInfoList = routeId.flatMapLatest { pollBusInfoForRoute(it) }

    val eyreSquare = Location(53.2743394, -9.0514163)
    val location = MutableStateFlow<Location>(eyreSquare)

    val cameraPosition = MutableStateFlow<Location?>(null)
    private val zoomLevel = MutableStateFlow<Float>(15.0f)

    val favorites = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteBusStopList = repository.favoriteBusStopList
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allBusStops = repository.busStops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val stations = MutableStateFlow<List<Station>>(emptyList())
    val isRefreshing = MutableStateFlow(false)


    fun fetchAndStoreBusStops() {
        viewModelScope.launch {
            repository.fetchAndStoreBusStops()
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
        if (loc != cameraPosition.value) {
            cameraPosition.value = loc
            getNearestStops(loc)
        }
    }

    fun setCurrentStop(busStop: BusStop) {
        currentBusStop.value = busStop
    }

    fun setRouteId(routeIdValue: String) {
        routeId.value = routeIdValue
    }

    private fun getNearestStops(location: Location) {
        println("getNearestStops, location = $location")
        viewModelScope.launch {
            val result = repository.fetchNearestStops(location.latitude, location.longitude)
            busStopListState.value = when (result) {
                is Result.Success -> UiState.Success(result.data)
                is Result.Error -> UiState.Error(result.exception)
            }
        }
    }

    private fun pollBusDepartures(stopRef: String): Flow<List<GalwayBusDeparture>> = flow {
        emit(emptyList())
        while (true) {
            val result = repository.fetchBusStopDepartures(stopRef)
            if (result is Result.Success) {
                Logger.d { result.data.toString() }
                emit(result.data)
            }
            delay(POLL_INTERVAL)
        }
    }

    private fun pollBusInfoForRoute(routeId: String): Flow<List<Bus>> = flow {
        if (routeId.isNotEmpty()) {
            emit(emptyList())
            while (true) {
                val result = repository.fetchBusListForRoute(routeId)
                if (result is Result.Success) {
                    Logger.d { result.data.toString() }
                    emit(result.data.sortedBy { it.vehicle_id })
                }
                delay(POLL_INTERVAL)
            }
        }
    }


    fun fetchStations() {
        viewModelScope.launch {
            stations.value = repository.fetchBikeShareInfo("galway").sortedBy { it.name }
        }
    }

    fun toggleFavorite(stopRef: String) {
        repository.toggleFavorite(stopRef)
    }

    fun centerInEyreSquare() {
        setLocation(eyreSquare)
    }

    companion object {
        private const val POLL_INTERVAL = 10000L
    }
}