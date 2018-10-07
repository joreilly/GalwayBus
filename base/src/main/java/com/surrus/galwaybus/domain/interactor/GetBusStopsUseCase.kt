package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusStop
import kotlinx.coroutines.Deferred

open class GetBusStopsUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getBusStops(routeId: String): Deferred<List<List<BusStop>>> {
        return galwayRepository.getBusStops(routeId)
    }
}