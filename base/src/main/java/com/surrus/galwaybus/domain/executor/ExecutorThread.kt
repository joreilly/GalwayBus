package com.surrus.galwaybus.domain.executor

import io.reactivex.Scheduler


interface ExecutorThread {
    val scheduler: Scheduler
}