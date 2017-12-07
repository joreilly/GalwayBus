package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.model.BusRoute
import io.reactivex.subscribers.DisposableSubscriber
import javax.inject.Inject


class BusRoutesViewModel @Inject constructor(val getBusRoutesUseCase: GetBusRoutesUseCase) : ViewModel() {

    private val busRoutes: MutableLiveData<List<BusRoute>> = MutableLiveData()

    init {
        getBusRoutesUseCase.execute(BusRouteSubscriber())
    }

    fun getBusRoutes() :LiveData<List<BusRoute>> {
        return busRoutes
    }


    override fun onCleared() {
        getBusRoutesUseCase.dispose()
    }


    inner class BusRouteSubscriber: DisposableSubscriber<List<BusRoute>>() {

        override fun onComplete() { }

        override fun onNext(t: List<BusRoute>) {
            busRoutes.postValue(t)
        }

        override fun onError(exception: Throwable) {
        }
    }

}