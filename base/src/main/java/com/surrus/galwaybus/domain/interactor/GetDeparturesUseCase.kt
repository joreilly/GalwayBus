package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.Departure
import kotlinx.coroutines.Deferred

open class GetDeparturesUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getDepartures(stopRef: String): Deferred<List<Departure>> {
        return galwayRepository.getDepartures(stopRef)
    }
}