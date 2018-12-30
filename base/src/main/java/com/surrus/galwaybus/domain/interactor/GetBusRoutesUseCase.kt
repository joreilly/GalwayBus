package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import kotlinx.coroutines.*


open class GetBusRoutesUseCase constructor(val galwayRepository: GalwayBusRepository) {

     open suspend fun getBusRoutes(): List<BusRouteSchedule>  {

        return withContext(Dispatchers.IO) {
            val routeList = galwayRepository.getBusRoutes()
            val scheduleMap = galwayRepository.getSchedules()

            // merge data
            routeList.map {
                val schedulePdf = scheduleMap[it.timetableId]!!.pdfUrl
                BusRouteSchedule(it.timetableId, it.shortName, it.longName, schedulePdf)
            }
        }
    }


}