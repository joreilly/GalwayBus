package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.model.BusRouteSchedule
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.RouteSchedule
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import javax.inject.Inject

open class GetBusRoutesUseCase @Inject constructor(val galwayRepository: GalwayBusRepository,
                                                   executorThread: ExecutorThread, postExecutionThread: PostExecutionThread):
        FlowableUseCase<List<BusRouteSchedule>, Void?>(executorThread, postExecutionThread) {

    public override fun buildUseCaseObservable(params: Void?): Flowable<List<BusRouteSchedule>> {
        return Flowables.zip(galwayRepository.getBusRoutes(), galwayRepository.getSchedules()) {
            routeList, scheduleMap -> mergeRouteSchedule(routeList, scheduleMap)
        }
    }


    fun mergeRouteSchedule(routeList: List<BusRoute>, scheduleMap: Map<String, RouteSchedule>) : List<BusRouteSchedule> {

        return routeList.map {
            val schedulePdf = scheduleMap[it.timetableId]!!.pdfUrl
            BusRouteSchedule(it.timetableId, it.shortName, it.longName, schedulePdf)
        }
    }
}