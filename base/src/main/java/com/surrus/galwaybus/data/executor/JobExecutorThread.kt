package org.buffer.android.boilerplate.data.executor

import com.surrus.galwaybus.domain.executor.ExecutorThread
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers


open class JobExecutorThread: ExecutorThread {

    override val scheduler: Scheduler
        get() = Schedulers.io()

}