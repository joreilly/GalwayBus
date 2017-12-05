package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase

open class BusRoutesViewModelFactory(private val getBusRoutesUseCase: GetBusRoutesUseCase) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusRoutesViewModel::class.java)) {
            return BusRoutesViewModel(getBusRoutesUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}