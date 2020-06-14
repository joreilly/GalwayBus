package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.BusStop
import kotlinx.coroutines.launch


class BusStopsViewModel constructor(private val galwayBusRepository: GalwayBusRepository)
    : ViewModel() {

    private val routeId: MutableLiveData<String> = MutableLiveData()
    private val direction: MutableLiveData<Int> = MutableLiveData()

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
        routeId.value = ""
    }

    fun setDirection(dir: Int) {
        direction.value = dir
    }

    fun setRouteId(routeIdString: String) {
        if (routeId.value != routeIdString) {
            routeId.value = routeIdString

            busStops.value = emptyList()
            viewModelScope.launch {
                busStopList = galwayBusRepository.fetchRouteStops(routeIdString)
                if (busStopList.size >= 2) {
                    busStops.value = busStopList[direction.value!!]
                }
            }
        }
    }
}
