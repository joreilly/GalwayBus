package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


open class BusRoutesViewModel constructor(private val getBusRoutesUseCase: GetBusRoutesUseCase)
    : ViewModel() {


    private val busRoutes: MutableLiveData<List<BusRouteSchedule>> = MutableLiveData()

    init {
        fetchRoutes()
    }

    fun fetchRoutes() {
        viewModelScope.launch {
            val busRoutesData = getBusRoutesUseCase.getBusRoutes()
            busRoutes.postValue(busRoutesData)
        }
    }

    open fun getBusRoutes(): LiveData<List<BusRouteSchedule>> = busRoutes
}