package com.surrus.galwaybus.domain.interactor

import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.executor.ExecutorThread
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber


/**
 * Abstract class for a UseCase that returns an instance of a [Single].
 */
abstract class FlowableUseCase<T, in Params> constructor(
        private val executorThread: ExecutorThread,
        private val postExecutionThread: PostExecutionThread) {

    private val disposables = CompositeDisposable()

    /**
     * Builds a [Single] which will be used when the current [FlowableUseCase] is executed.
     */
    protected abstract fun buildUseCaseObservable(params: Params? = null): Flowable<T>

    /**
     * Executes the current use case.
     */
    open fun execute(observer: DisposableSubscriber<T>, params: Params? = null) {
        val observable = this.buildUseCaseObservable(params)
                .subscribeOn(executorThread.scheduler)
                .observeOn(postExecutionThread.scheduler) as Flowable<T>
        addDisposable(observable.subscribeWith(observer))
    }

    /**
     * Dispose from current [CompositeDisposable].
     */
    fun dispose() {
        if (disposables.size() > 0 && !disposables.isDisposed) {
            disposables.clear()
        }
    }

    /**
     * Dispose from current [CompositeDisposable].
     */
    private fun addDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }

}