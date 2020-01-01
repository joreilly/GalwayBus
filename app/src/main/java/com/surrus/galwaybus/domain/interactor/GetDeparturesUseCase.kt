package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.Departure


open class GetDeparturesUseCase constructor(val galwayRepository: GalwayBusRepository) {

    suspend fun getDepartures(stopRef: String): List<Departure> {
        return galwayRepository.getDepartures(stopRef)
    }
}