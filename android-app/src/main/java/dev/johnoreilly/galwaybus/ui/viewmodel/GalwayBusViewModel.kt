package dev.johnoreilly.galwaybus.ui.viewmodel

import androidx.lifecycle.*
import co.touchlab.kermit.Kermit
import com.surrus.galwaybus.common.GalwayBusDeparture
import com.surrus.galwaybus.common.GalwayBusRepository
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
        private val galwayBusRepository: GalwayBusRepository,
        private val logger: Kermit
) : ViewModel() {

    val uiState = MutableLiveData<UiState<List<BusStop>>>()

    val stopRef = MutableLiveData<String>("")
    val busDepartureList = stopRef.switchMap { pollBusDepartures(it).asLiveData() }

    val favorites = MutableStateFlow<Set<String>>(setOf())

    val location: MutableLiveData<Location> = MutableLiveData()
    //val cameraPosition: MutableLiveData<Location> = MutableLiveData()

    init {
        setLocation(Location(53.2743394, -9.0514163)) // default if we can't get location
    }

    fun setLocation(loc: Location) {
        location.value = loc
        getNearestStops(loc)
    }

    fun setStopRef(stopRefValue: String) {
        stopRef.value = stopRefValue
    }

    fun getNearestStops(location: Location) {
        viewModelScope.launch {
            val result = galwayBusRepository.fetchNearestStops(location.latitude, location.longitude)
            when (result) {
                is Result.Success -> {
                    uiState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    uiState.value = UiState.Error(result.exception)
                }
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


    fun toggleFavorite(stopRef: String) {
        val set = favorites.value.toMutableSet()
        if (!set.add(stopRef)) {
            set.remove(stopRef)
        }
        favorites.value = set
    }

    companion object {
        private const val POLL_INTERVAL =  10000L
    }
}