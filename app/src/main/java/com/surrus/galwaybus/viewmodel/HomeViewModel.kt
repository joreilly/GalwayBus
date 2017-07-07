package com.surrus.galwaybus.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.service.GalwayBusService
import com.surrus.galwaybus.util.ext.toLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class HomeViewModel : ViewModel() {

    val galwayBusService = GalwayBusService()

    val routes: LiveData<Map<String,BusRoute>>

    init {
        routes = galwayBusService.getRoutes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toLiveData();
    }

}