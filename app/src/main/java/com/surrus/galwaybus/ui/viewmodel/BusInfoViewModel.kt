package com.surrus.galwaybus.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surrus.galwaybus.common.GalwayBusRepository
import com.surrus.galwaybus.common.model.Bus
import com.surrus.galwaybus.common.model.Result
import com.surrus.galwaybus.ui.data.Resource
import com.surrus.galwaybus.ui.data.ResourceState
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.fixedRateTimer


class BusInfoViewModel constructor(private val galwaysBusRepository: GalwayBusRepository)
    : ViewModel() {

    val busListForRoute: MutableLiveData<Resource<List<Bus>>> = MutableLiveData()
    private var busLocationTimer: Timer? = null


    fun pollForBusLocations(routeId: String) {
        busLocationTimer?.cancel()
        busLocationTimer = fixedRateTimer("getDepartesTimer", true, 0, POLL_INTERVAL) {
            viewModelScope.launch {
                val result = galwaysBusRepository.fetchBusListForRoute(routeId)
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
        const val POLL_INTERVAL = 15000L
    }
}