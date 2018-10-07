package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.launch


class BusRoutesViewModel constructor(private val getBusRoutesUseCase: GetBusRoutesUseCase) : ViewModel() {

    private val busRoutes: MutableLiveData<List<BusRouteSchedule>> = MutableLiveData()

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main + viewModelJob)

    init {
        fetchRoutes()
    }

    fun fetchRoutes() {
        uiScope.launch {
            val busRoutesData = getBusRoutesUseCase.getBusRoutes().await()
            busRoutes.postValue(busRoutesData)
        }
    }

    fun getBusRoutes() :LiveData<List<BusRouteSchedule>> {
        return busRoutes
    }
}