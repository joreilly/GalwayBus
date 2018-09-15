package com.surrus.galwaybus.di.koin

import androidx.room.Room
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.surrus.galwaybus.cache.GalwayBusCacheImpl
import com.surrus.galwaybus.cache.PreferencesHelper
import com.surrus.galwaybus.cache.db.GalwayBusDatabase
import com.surrus.galwaybus.data.GalwayBusDataRepository
import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusDataStore
import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.data.source.GalwayBusCacheDataStore
import com.surrus.galwaybus.data.source.GalwayBusDataStoreFactory
import com.surrus.galwaybus.data.source.GalwayBusRemoteDataStore
import com.surrus.galwaybus.di.koin.DatasourceProperties.SERVER_URL
import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.interactor.GetBusRoutesUseCase
import com.surrus.galwaybus.domain.interactor.GetBusStopsUseCase
import com.surrus.galwaybus.domain.interactor.GetDeparturesUseCase
import com.surrus.galwaybus.domain.interactor.GetNearestBusStopsUseCase
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.remote.GalwayBusRemoteImpl
import com.surrus.galwaybus.remote.GalwayBusService
import com.surrus.galwaybus.ui.UiThread
import com.surrus.galwaybus.ui.viewmodel.BusRoutesViewModel
import com.surrus.galwaybus.ui.viewmodel.BusStopsViewModel
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.buffer.android.boilerplate.data.executor.JobExecutorThread
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object DatasourceProperties {
    const val SERVER_URL = "https://galwaybus.herokuapp.com/"
}



val galwayBusAppModuile = module(definition = {

    viewModel { NearestBusStopsViewModel(get()) }
    viewModel { BusRoutesViewModel(get()) }
    viewModel { BusStopsViewModel(get()) }

    single { PreferencesHelper(get()) }

    single { GetNearestBusStopsUseCase(get(), get(), get())  }
    single { GetBusStopsUseCase(get(), get(), get())  }
    single { GetDeparturesUseCase(get(), get(), get())  }
    single { GetBusRoutesUseCase(get(), get(), get())  }

    single { JobExecutorThread() as ExecutorThread }
    single { UiThread() as PostExecutionThread }

    single { GalwayBusDataRepository(get()) as GalwayBusRepository }

    single { GalwayBusDataStoreFactory(get(), get(), get()) }
    single { GalwayBusRemoteDataStore(get()) }
    single { GalwayBusCacheDataStore(get()) }

    single { GalwayBusCacheImpl(get(), get()) as GalwayBusCache }
    single { GalwayBusRemoteDataStore(get()) as GalwayBusDataStore }
    single { GalwayBusRemoteImpl(get()) as GalwayBusRemote }
    single { createGalwayBusDatabase(androidContext()) }

    single { createFirebaseAnalytics(get()) }
})


val remoteDatasourceModule = module(definition = {

    single { createOkHttpClient() }
    single { createWebService<GalwayBusService>(get(), SERVER_URL) }
})


fun createOkHttpClient(): OkHttpClient {
    val interceptor = HttpLoggingInterceptor()
    interceptor.level = HttpLoggingInterceptor.Level.BODY

//    val cacheDir = File(application.cacheDir, UUID.randomUUID().toString())
//    // 10MB cache
//    val cache = Cache(cacheDir, 10 * 1024 * 1024)

    return OkHttpClient.Builder()
            //.cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            //.addNetworkInterceptor( StethoInterceptor())
            .build()
}

inline fun <reified T> createWebService(okHttpClient: OkHttpClient, url: String): T {
    val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
            .create()

    val retrofit = Retrofit.Builder()
            .baseUrl(SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

    return retrofit.create(T::class.java)

}

internal fun createGalwayBusDatabase(context: Context): GalwayBusDatabase {
    return Room.databaseBuilder(context,
            GalwayBusDatabase::class.java, "galway_bus.db")
            .fallbackToDestructiveMigration()
            .build()
}

internal fun createFirebaseAnalytics(context: Context): FirebaseAnalytics {
    return FirebaseAnalytics.getInstance(context)
}


// Gather all app modules
val appModule = listOf(galwayBusAppModuile, remoteDatasourceModule)