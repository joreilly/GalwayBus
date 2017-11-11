package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusRoute
import io.reactivex.Flowable
import javax.inject.Inject

open class GetBusRoutesUseCase @Inject constructor(val galwayRepository: GalwayBusRepository,
                                                   executorThread: ExecutorThread, postExecutionThread: PostExecutionThread):
        FlowableUseCase<List<BusRoute>, Void?>(executorThread, postExecutionThread) {

    public override fun buildUseCaseObservable(params: Void?): Flowable<List<BusRoute>> {
        return galwayRepository.getBusRoutes()
    }

}