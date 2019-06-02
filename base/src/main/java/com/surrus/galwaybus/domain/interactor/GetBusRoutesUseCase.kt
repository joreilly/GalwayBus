package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import kotlinx.coroutines.*


open class GetBusRoutesUseCase constructor(val galwayRepository: GalwayBusRepository) {

     open suspend fun getBusRoutes(): List<BusRouteSchedule>  {

        return withContext(Dispatchers.IO) {

            // TODO: start of switching over to common repository...will need to inject as dependencyu
            val galwayBusRepositoryCommon = com.surrus.galwaybus.common.GalwayBusRepository()
            val routeList = galwayBusRepositoryCommon.fetchBusRoutes()

            val scheduleMap = galwayRepository.getSchedules()

            // merge data
            routeList
                    .filter {  scheduleMap[it.timetableId] != null }
                    .map {
                        val schedulePdf = scheduleMap[it.timetableId]!!.pdfUrl
                        BusRouteSchedule(it.timetableId, it.shortName, it.longName, schedulePdf)
                    }
        }
    }


}