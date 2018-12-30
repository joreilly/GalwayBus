package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class BusRoutesViewModel constructor(private val getBusRoutesUseCase: GetBusRoutesUseCase, val uiDispatcher: CoroutineDispatcher = Dispatchers.Main)
    : ViewModel(), CoroutineScope {

    private val viewModelJob = Job()
    override val coroutineContext: CoroutineContext
        get() = uiDispatcher + viewModelJob

    private val busRoutes: MutableLiveData<List<BusRouteSchedule>> = MutableLiveData()

    init {
        fetchRoutes()
    }

    fun fetchRoutes() {
        launch {
            val busRoutesData = getBusRoutesUseCase.getBusRoutes()
            busRoutes.postValue(busRoutesData)
        }
    }

    fun getBusRoutes(): LiveData<List<BusRouteSchedule>> = busRoutes
}