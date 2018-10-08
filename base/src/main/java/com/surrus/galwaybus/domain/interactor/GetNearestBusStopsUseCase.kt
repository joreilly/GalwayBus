package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location



open class GetNearestBusStopsUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getNearestBusStops(location: Location): List<BusStop> {
        return galwayRepository.getNearestBusStops(location)
    }
}