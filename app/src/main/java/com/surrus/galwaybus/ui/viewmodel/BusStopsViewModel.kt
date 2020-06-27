package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.BusStop
import com.surrus.galwaybus.common.model.Result
import kotlinx.coroutines.launch


class BusStopsViewModel constructor(private val galwayBusRepository: GalwayBusRepository)
    : ViewModel() {

    private val routeId: MutableLiveData<String> = MutableLiveData()
    private val direction: MutableLiveData<Int> = MutableLiveData(0)

    val busStops = MediatorLiveData<Result<List<BusStop>>>().apply {
        this.addSource(direction) {
            if (busStopList.isNotEmpty()) {
                this.value = Result.Success(busStopList[direction.value!!])
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

            viewModelScope.launch {
                val result = galwayBusRepository.fetchRouteStops(routeIdString)
                busStops.postValue(when (result) {
                    is Result.Success -> {
                        busStopList = result.data

                        Result.Success(busStopList[direction.value!!])
                    }
                    is Result.Error -> Result.Error(result.exception)
                })
            }
        }
    }
}
