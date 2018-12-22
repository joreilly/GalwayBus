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


class BusStopsViewModel constructor(private val getBusStopsUseCase: GetBusStopsUseCase, val uiDispatcher: CoroutineDispatcher = Dispatchers.Main)
    : ViewModel(), CoroutineScope {

    private val viewModelJob = Job()
    override val coroutineContext: CoroutineContext
        get() = uiDispatcher + viewModelJob


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
            launch {
                val busStopsData = getBusStopsUseCase.getBusStops(routeId)
                busStops.value = busStopsData[direction.value!!]
            }
        }
    }

}