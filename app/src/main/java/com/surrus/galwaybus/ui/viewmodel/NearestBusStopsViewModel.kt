package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import io.reactivex.subscribers.DisposableSubscriber
import javax.inject.Inject


class NearestBusStopsViewModel @Inject constructor(val getNearestBusStopsUseCase: GetNearestBusStopsUseCase) : ViewModel() {

    var busStops: MutableLiveData<List<BusStop>> = MutableLiveData()

    private val location: MutableLiveData<Location> = MutableLiveData()
    private val zoomLevel: MutableLiveData<Float> = MutableLiveData()


//    init {
//        busStops = Transformations.switchMap(location) {
//            id -> {
//                getNearestBusStopsUseCase.execute(BusStopsSubscriber(), id)
//            }
//            busStops
//        }
//        }
//    }


    fun fetchNearestBusStops(location: Location) {
        //if (busStops.value == null) {
            getNearestBusStopsUseCase.dispose()
            getNearestBusStopsUseCase.execute(BusStopsSubscriber(), location)
        //}
    }

    fun setLocationZoomLevel(loc: Location, zl: Float) {
        location.value = loc
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
            busStops.postValue(t)
        }

        override fun onError(exception: Throwable) {
        }
    }


    override fun onCleared() {
        getNearestBusStopsUseCase.dispose()
    }

}