package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import kotlinx.coroutines.*


open class GetBusRoutesUseCase constructor(val galwayRepository: GalwayBusRepository) {

     open suspend fun getBusRoutes(): List<BusRouteSchedule>  {

        // TODO: start of switching over to common repository...will need to inject as dependencyu
        val galwayBusRepositoryCommon = com.surrus.galwaybus.common.GalwayBusRepository()
        val routeList = galwayBusRepositoryCommon.fetchBusRoutes()

        val scheduleMap = galwayBusRepositoryCommon.fetchSchedules()

        // merge data
        return routeList
                .filter {  scheduleMap[it.timetableId] != null }
                .map {
                    val schedulePdf = scheduleMap[it.timetableId] ?: ""
                    BusRouteSchedule(it.timetableId, it.shortName, it.longName, schedulePdf)
                }
    }


}