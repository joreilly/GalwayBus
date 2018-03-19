package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase

open class NearestBusStopsViewModelFactory(private val getNearestBusStopsUseCase: GetNearestBusStopsUseCase) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NearestBusStopsViewModel::class.java)) {
            return NearestBusStopsViewModel(getNearestBusStopsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}