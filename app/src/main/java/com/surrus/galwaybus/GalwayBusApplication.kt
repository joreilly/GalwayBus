package com.surrus.galwaybus

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crash.FirebaseCrash
import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.di.DaggerApplicationComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import net.danlew.android.joda.JodaTimeAndroid
import javax.inject.Inject

class GalwayBusApplication : Application(), HasActivityInjector {

    @Inject lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            FirebaseCrash.setCrashCollectionEnabled(false);
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false)
        } else {
            Logger.init().logAdapter(releaseLogAdapter)
        }

        Logger.i("GalwayBusApplication init")

        JodaTimeAndroid.init(this);
        DaggerApplicationComponent
                .builder()
                .application(this)
                .build()
                .inject(this)

    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityDispatchingAndroidInjector
    }


    internal var releaseLogAdapter: LogAdapter = object : LogAdapter {

        override fun d(tag: String, message: String) {}

        override fun e(tag: String, message: String) {
            Log.e(tag, message)
        }

        override fun w(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun i(tag: String, message: String) {
            Log.i(tag, message)
        }

        override fun v(tag: String, message: String) {}

        override fun wtf(tag: String, message: String) {
            Log.wtf(tag, message)
        }
    }
}
