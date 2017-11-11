package org.buffer.android.boilerplate.data.executor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

open class JobExecutorThread @Inject constructor(): ExecutorThread {

    override val scheduler: Scheduler
        get() = Schedulers.io()

}