package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import io.reactivex.subscribers.DisposableSubscriber


class BusRoutesViewModel constructor(val getBusRoutesUseCase: GetBusRoutesUseCase) : ViewModel() {

    private val busRoutes: MutableLiveData<List<BusRouteSchedule>> = MutableLiveData()

    init {
        getBusRoutesUseCase.execute(BusRouteSubscriber())
    }

    fun fetchRoutes() {
        getBusRoutesUseCase.execute(BusRouteSubscriber())
    }

    fun getBusRoutes() :LiveData<List<BusRouteSchedule>> {
        return busRoutes
    }


    override fun onCleared() {
        getBusRoutesUseCase.dispose()
    }


    inner class BusRouteSubscriber: DisposableSubscriber<List<BusRouteSchedule>>() {

        override fun onComplete() { }

        override fun onNext(t: List<BusRouteSchedule>) {
            busRoutes.postValue(t)
        }

        override fun onError(exception: Throwable) {
        }
    }

}