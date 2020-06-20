package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.fixedRateTimer



class NearestBusStopsViewModel constructor(private val galwayBusRepository: GalwayBusRepository)
    : ViewModel() {

    var busStops: MutableLiveData<Resource<List<BusStop>>> = MutableLiveData()
    val location: MutableLiveData<Location> = MutableLiveData()
    val cameraPosition: MutableLiveData<Location> = MutableLiveData()
    private val zoomLevel: MutableLiveData<Float> = MutableLiveData()

    private var departureTimer: Timer? = null

    init {
        zoomLevel.value = 15.0f
    }

    fun pollForNearestBusStopTimes() {
        if (location.value != null) {
            departureTimer?.cancel()
            departureTimer = fixedRateTimer("getDepartesTimer", true, 0, 30000) {
                viewModelScope.launch {

                    location.value?.let {
                        val result = galwayBusRepository.fetchNearestStops(it.latitude, it.longitude)
                        busStops.postValue(when (result) {
                            is Result.Success -> Resource(ResourceState.SUCCESS, result.data, null)
                            is Result.Error -> Resource(ResourceState.ERROR, null, result.exception.message)
                        })
                    }
                }
            }
        }
    }

    fun stopPolling() {
        departureTimer?.cancel()
    }

    fun setCameraPosition(loc: Location) {
        cameraPosition.value = loc
    }

    fun setLocation(loc: Location) {
        location.value = loc
    }

    fun setZoomLevel(zl: Float) {
        zoomLevel.value = zl
    }

    fun getLocation(): Location? {
        return location.value
    }

    fun getZoomLevel(): Float {
        return zoomLevel.value!!
    }


    override fun onCleared() {
        Logger.d("NearestBusStopsViewModel.onCleared")
        stopPolling()
    }
}