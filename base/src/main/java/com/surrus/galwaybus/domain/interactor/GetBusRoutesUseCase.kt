package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.RouteSchedule
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


open class GetBusRoutesUseCase constructor(val galwayRepository: GalwayBusRepository) {

     suspend fun getBusRoutes(): Deferred<List<BusRouteSchedule>> = coroutineScope {

        async {
            val routeList = galwayRepository.getBusRoutes().await()
            val scheduleMap = galwayRepository.getSchedules().await()
            mergeRouteSchedule(routeList, scheduleMap)
        }
    }


    fun mergeRouteSchedule(routeList: List<BusRoute>, scheduleMap: Map<String, RouteSchedule>) : List<BusRouteSchedule> {

        return routeList.map {
            val schedulePdf = scheduleMap[it.timetableId]!!.pdfUrl
            BusRouteSchedule(it.timetableId, it.shortName, it.longName, schedulePdf)
        }
    }
}