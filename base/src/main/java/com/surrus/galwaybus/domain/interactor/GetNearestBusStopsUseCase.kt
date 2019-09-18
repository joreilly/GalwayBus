package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.model.Result


open class GetNearestBusStopsUseCase constructor(private val galwayRepository: GalwayBusRepository) {

    suspend fun getNearestBusStops(location: Location) : Result<List<BusStop>> {

        // TODO: start of switching over to common repository...will need to inject as dependencyu
        val galwayBusRepositoryCommon = com.surrus.galwaybus.common.GalwayBusRepository()
        val nearestStops = galwayBusRepositoryCommon.getNearestStops(location.latitude, location.longitude)
        return Result.Success(nearestStops.map {
            BusStop(it.stop_id, it.short_name, it.long_name)
        })

        //return galwayRepository.getNearestBusStops(location)
    }
}