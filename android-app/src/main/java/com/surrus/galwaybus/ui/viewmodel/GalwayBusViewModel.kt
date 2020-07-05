package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.Location
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.common.remote.RealtimeBusInformation
import com.surrus.galwaybus.common.remote.Stop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


sealed class UiState<out T: Any> {
    object Loading : UiState<Nothing>()
    data class Success<out T : Any>(val data: T) : UiState<T>()
    data class Error(val exception: Exception) : UiState<Nothing>()
}


sealed class Screen {
    object Home : Screen()
    data class BusStopView(val stopId: String, val stopName: String) : Screen()
}


class GalwayBusViewModel(private val galwayBusRepository: GalwayBusRepository) : ViewModel() {

    val currentScreen = MutableLiveData<Screen>(Screen.Home)
    val uiState = MutableLiveData<UiState<List<Stop>>>()

    val busDepartureList = MutableLiveData<List<RealtimeBusInformation>>()

    val location: MutableLiveData<Location> = MutableLiveData()
    val cameraPosition: MutableLiveData<Location> = MutableLiveData()
    private val zoomLevel: MutableLiveData<Float> = MutableLiveData()

    private var pollingJob: Job? = null


    init {
        location.value = Location(53.2743394, -9.0514163) // default if we can't get location
        //getNearestStops(loc)
    }

    fun setLocation(loc: Location) {
        location.value = loc
    }


    fun getNearestStops(location: Location) {
        viewModelScope.launch {
            val result = galwayBusRepository.getNearestStops(location)
            if (result is Result.Success) {
                uiState.value = UiState.Success(result.data)
            }
        }
    }

    fun getBusStopDepartures(stopId: String) {
        pollingJob?.cancel()
        busDepartureList.value = emptyList()

        pollingJob = viewModelScope.launch {

            while (true) {
                val result = galwayBusRepository.getRealtimeBusInformation(stopId)
                if (result is Result.Success) {
                    busDepartureList.value = result.data
                }
                delay(20000)
            }
        }
    }



    fun navigateTo(screen: Screen) {
        currentScreen.postValue(screen)
    }

    fun onBack(): Boolean {
        val wasHandled = currentScreen.value != Screen.Home
        currentScreen.postValue(Screen.Home)
        return wasHandled
    }


}