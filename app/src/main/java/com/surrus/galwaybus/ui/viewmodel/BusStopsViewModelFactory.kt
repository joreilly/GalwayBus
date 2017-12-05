package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase

open class BusStopsViewModelFactory(private val getBusStopsUseCase: GetBusStopsUseCase) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusStopsViewModel::class.java)) {
            return BusStopsViewModel(getBusStopsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}