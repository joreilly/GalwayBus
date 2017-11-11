package com.surrus.galwaybus.ui.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase

open class HomeViewModelFactory(private val getBusRoutesUseCase: GetBusRoutesUseCase) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(getBusRoutesUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}