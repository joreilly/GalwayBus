package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Departure
import com.surrus.galwaybus.model.Location
import io.reactivex.Flowable
import javax.inject.Inject

open class GetDeparturesUseCase @Inject constructor(val galwayRepository: GalwayBusRepository,
                                                    executorThread: ExecutorThread, postExecutionThread: PostExecutionThread):
        FlowableUseCase<List<Departure>, String>(executorThread, postExecutionThread) {

    override fun buildUseCaseObservable(stopRef: String?): Flowable<List<Departure>> {
        return galwayRepository.getDepartures(stopRef!!)
    }
}