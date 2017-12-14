package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import io.reactivex.subscribers.DisposableSubscriber
import javax.inject.Inject


class NearestBusStopsViewModel @Inject constructor(val getNearestBusStopsUseCase: GetNearestBusStopsUseCase) : ViewModel() {

    var busStops: MutableLiveData<Resource<List<BusStop>>> = MutableLiveData()
    val location: MutableLiveData<Location> = MutableLiveData()
    val cameraPosition: MutableLiveData<Location> = MutableLiveData()
    private val zoomLevel: MutableLiveData<Float> = MutableLiveData()

    init {
        zoomLevel.value = 15.0f
    }


    fun pollForNearestBusStopTimes() {
        if (location.value != null) {
            getNearestBusStopsUseCase.dispose()
            getNearestBusStopsUseCase.execute(BusStopsSubscriber(), location.value)
        }
    }

    fun stopPolling() {
        getNearestBusStopsUseCase.dispose()
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


    inner class BusStopsSubscriber: DisposableSubscriber<List<BusStop>>() {

        override fun onComplete() { }

        override fun onNext(t: List<BusStop>) {
            busStops.postValue(Resource(ResourceState.SUCCESS, t, null))
        }

        override fun onError(exception: Throwable) {
            busStops.postValue(Resource(ResourceState.ERROR, null, exception.message))
        }
    }


    override fun onCleared() {
        Logger.d("NearestBusStopsViewModel.onCleared")
        getNearestBusStopsUseCase.dispose()
    }

}