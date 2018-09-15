package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import io.reactivex.subscribers.DisposableSubscriber


class BusStopsViewModel constructor(private val getBusStopsUseCase: GetBusStopsUseCase) : ViewModel() {

    val direction: MutableLiveData<Int> = MutableLiveData()

    val busStops = MediatorLiveData<List<BusStop>>().apply {
        this.addSource(direction) {
            if (busStopList.isNotEmpty()) {
                this.value = busStopList[direction.value!!]
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
            busStops.value = t[direction.value!!]
        }

        override fun onError(exception: Throwable) {
        }
    }

    override fun onCleared() {
        getBusStopsUseCase.dispose()
    }

}