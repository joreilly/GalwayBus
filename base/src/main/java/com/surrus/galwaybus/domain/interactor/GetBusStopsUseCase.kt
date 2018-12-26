package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Bus

open class GetBusStopsUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getBusStops(routeId: String): List<List<BusStop>> {
        return galwayRepository.getBusStops(routeId)
    }


    suspend fun getBusListForRoute(routeId: String): List<Bus> {
        return galwayRepository.getBusStopListForRoute(routeId)
    }

}