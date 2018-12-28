package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository


open class GetBusStopsUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getBusStops(routeId: String) = galwayRepository.getBusStops(routeId)
}