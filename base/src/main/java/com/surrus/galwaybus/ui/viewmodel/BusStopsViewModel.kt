package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.*
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import io.reactivex.subscribers.DisposableSubscriber
import javax.inject.Inject
import io.reactivex.disposables.CompositeDisposable




class BusStopsViewModel @Inject constructor(val getBusStopsUseCase: GetBusStopsUseCase) : ViewModel() {

    val direction: MutableLiveData<Int> = MutableLiveData()

    val busStops = MediatorLiveData<List<BusStop>>().apply {
        this.addSource(direction) {
            if (busStopList.size > 0) {
                this.value = busStopList.get(direction.value!!)
            }
        }
    }

    private var busStopList: List<List<BusStop>> = emptyList()


    init {
        direction.value = 0
    }

    fun setDirection(dir: Int) {
        direction.value = dir
    }

    fun fetchBusStops(routeId: String) {
        if (busStops.value == null) {
            getBusStopsUseCase.execute(BusStopsSubscriber(), routeId)
        }
    }

    inner class BusStopsSubscriber: DisposableSubscriber<List<List<BusStop>>>() {

        override fun onComplete() { }

        override fun onNext(t: List<List<BusStop>>) {
            busStopList = t
            busStops.value = t.get(direction.value!!)
        }

        override fun onError(exception: Throwable) {
        }
    }

    override fun onCleared() {
        getBusStopsUseCase.dispose()
    }

}