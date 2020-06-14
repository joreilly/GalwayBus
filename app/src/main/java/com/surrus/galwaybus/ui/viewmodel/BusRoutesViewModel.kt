package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.BusRoute
import kotlinx.coroutines.launch


open class BusRoutesViewModel constructor(private val galwayBusRepository: GalwayBusRepository)
    : ViewModel() {

    val busRoutes: MutableLiveData<List<BusRoute>> = MutableLiveData()

    init {
        fetchRoutes()
    }

    fun fetchRoutes() {
        viewModelScope.launch {
            val busRoutesList = galwayBusRepository.fetchBusRoutes()
            busRoutes.postValue(busRoutesList)
        }
    }
}