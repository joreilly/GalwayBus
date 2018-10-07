package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.model.BusStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.launch


class BusStopsViewModel constructor(private val getBusStopsUseCase: GetBusStopsUseCase) : ViewModel() {

    val direction: MutableLiveData<Int> = MutableLiveData()

    val busStops = MediatorLiveData<List<BusStop>>().apply {
        this.addSource(direction) {
            if (busStopList.isNotEmpty()) {
                this.value = busStopList[direction.value!!]
            }
        }
    }

    private var busStopList: List<List<BusStop>> = emptyList()

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main + viewModelJob)

    init {
        direction.value = 0
    }

    fun setDirection(dir: Int) {
        direction.value = dir
    }

    fun fetchBusStops(routeId: String) {
        if (busStops.value == null) {
            uiScope.launch {
                val busStopsData = getBusStopsUseCase.getBusStops(routeId).await()
                busStops.value = busStopsData[direction.value!!]
            }
        }
    }

}