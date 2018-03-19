package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop
import io.reactivex.Flowable
import javax.inject.Inject

open class GetBusStopsUseCase @Inject constructor(val galwayRepository: GalwayBusRepository,
                                                  executorThread: ExecutorThread, postExecutionThread: PostExecutionThread):
        FlowableUseCase<List<List<BusStop>>, String>(executorThread, postExecutionThread) {

    override fun buildUseCaseObservable(routeId: String?): Flowable<List<List<BusStop>>> {
        return galwayRepository.getBusStops(routeId!!)
    }
}