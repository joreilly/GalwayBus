package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.surrus.galwaybus.domain.interactor.GetBusInfoUseCase
import com.surrus.galwaybus.model.Bus
import com.surrus.galwaybus.model.Result
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.coroutines.CoroutineContext


class BusInfoViewModel constructor(private val getBusInfoUseCase: GetBusInfoUseCase, val uiDispatcher: CoroutineDispatcher = Dispatchers.Main)
    : ViewModel(), CoroutineScope {

    private val viewModelJob = Job()
    override val coroutineContext: CoroutineContext
        get() = uiDispatcher + viewModelJob

    val busListForRoute: MutableLiveData<Resource<List<Bus>>> = MutableLiveData()
    private var busLocationTimer: Timer? = null


    fun pollForBusLocations(routeId: String) {
        busLocationTimer?.cancel()
        busLocationTimer = fixedRateTimer("getDepartesTimer", true, 0, POLL_INTERVAL) {
            launch {
                val result = getBusInfoUseCase.getBusListForRoute(routeId)
                busListForRoute.postValue(when (result) {
                    is Result.Success -> Resource(ResourceState.SUCCESS, result.data, null)
                    is Result.Error -> Resource(ResourceState.ERROR, null, result.exception.message)
                })
            }
        }
    }

    fun stopPolling() {
        busLocationTimer?.cancel()
    }


    override fun onCleared() {
        stopPolling()
    }

    companion object {
        const val POLL_INTERVAL = 30000L
    }
}