package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository

open class GetBusInfoUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getBusListForRoute(routeId: String) = galwayRepository.getBusStopListForRoute(routeId)
}