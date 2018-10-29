package com.surrus.galwaybus.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import com.surrus.galwaybus.model.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.fixedRateTimer


class NearestBusStopsViewModel constructor(private val getNearestBusStopsUseCase: GetNearestBusStopsUseCase) : ViewModel() {

    var busStops: MutableLiveData<Resource<List<BusStop>>> = MutableLiveData()
    val location: MutableLiveData<Location> = MutableLiveData()
    val cameraPosition: MutableLiveData<Location> = MutableLiveData()
    private val zoomLevel: MutableLiveData<Float> = MutableLiveData()


    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main + viewModelJob)

    private var departureTimer: Timer? = null

    init {
        zoomLevel.value = 15.0f
    }

    fun pollForNearestBusStopTimes() {
        if (location.value != null) {
            departureTimer?.cancel()
            departureTimer = fixedRateTimer("getDepartesTimer", true, 0, 3000) {
                uiScope.launch {
                    val result = getNearestBusStopsUseCase.getNearestBusStops(location.value!!)
                    busStops.postValue(when (result) {
                        is Result.Success -> Resource(ResourceState.SUCCESS, result.data, null)
                        is Result.Error -> Resource(ResourceState.ERROR, null, result.exception.message)
                    })
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