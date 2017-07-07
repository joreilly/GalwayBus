package com.surrus.galwaybus.util.ext

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations

fun <T> LiveData<T>.observe(owner: LifecycleOwner, observer: (T?) -> Unit) = observe(owner, object : Observer<T> {
  override fun onChanged(v: T?) {
    observer.invoke(v)
  }
})

fun <X, Y> LiveData<X>.switchMap(func: (X) -> LiveData<Y>)
    = Transformations.switchMap(this, func)
