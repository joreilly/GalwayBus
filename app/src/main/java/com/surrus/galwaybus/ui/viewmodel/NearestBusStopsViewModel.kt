package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import io.reactivex.subscribers.DisposableSubscriber
import javax.inject.Inject


class NearestBusStopsViewModel @Inject constructor(val getNearestBusStopsUseCase: GetNearestBusStopsUseCase) : ViewModel() {

    val busStops: MutableLiveData<List<BusStop>> = MutableLiveData()


    fun fetchNearestBusStops(location: Location) {
        if (busStops.value == null) {
            getNearestBusStopsUseCase.execute(BusStopsSubscriber(), location)
        }
    }


    override fun onCleared() {
        getNearestBusStopsUseCase.dispose()
    }


    inner class BusStopsSubscriber: DisposableSubscriber<List<BusStop>>() {

        override fun onComplete() { }

        override fun onNext(t: List<BusStop>) {
            busStops.postValue(t)
        }

        override fun onError(exception: Throwable) {
        }
    }

}