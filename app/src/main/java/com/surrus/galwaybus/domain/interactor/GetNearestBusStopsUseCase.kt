package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import io.reactivex.Flowable
import javax.inject.Inject

open class GetNearestBusStopsUseCase @Inject constructor(val galwayRepository: GalwayBusRepository,
                                                         executorThread: ExecutorThread, postExecutionThread: PostExecutionThread):
        FlowableUseCase<List<BusStop>, Location>(executorThread, postExecutionThread) {

    override fun buildUseCaseObservable(location: Location?): Flowable<List<BusStop>> {
        return galwayRepository.getNearestBusStops(location!!)
    }
}