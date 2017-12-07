package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import io.reactivex.subscribers.DisposableSubscriber
import javax.inject.Inject
import io.reactivex.disposables.CompositeDisposable




class BusStopsViewModel @Inject constructor(val getBusStopsUseCase: GetBusStopsUseCase) : ViewModel() {

    val busStops: MutableLiveData<List<List<BusStop>>> = MutableLiveData()


    fun fetchBusStops(routeId: String) {
        if (busStops.value == null) {
            getBusStopsUseCase.execute(BusStopsSubscriber(), routeId)
        }
    }

    override fun onCleared() {
        getBusStopsUseCase.dispose()
    }


    inner class BusStopsSubscriber: DisposableSubscriber<List<List<BusStop>>>() {

        override fun onComplete() { }

        override fun onNext(t: List<List<BusStop>>) {
            busStops.postValue(t)
        }

        override fun onError(exception: Throwable) {
        }
    }

}