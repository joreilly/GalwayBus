package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class BusStopsViewModel constructor(private val getBusStopsUseCase: GetBusStopsUseCase, private val uiDispatcher: CoroutineDispatcher = Dispatchers.Main)
    : ViewModel(), CoroutineScope {

    private val viewModelJob = Job()
    override val coroutineContext: CoroutineContext
        get() = uiDispatcher + viewModelJob


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
            launch {
                busStopList = getBusStopsUseCase.getBusStops(routeIdString)
                busStops.value = busStopList[direction.value!!]
            }
        }
    }
}
