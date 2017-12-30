package com.surrus.galwaybus.di

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.surrus.galwaybus.cache.GalwayBusCacheImpl
import com.surrus.galwaybus.cache.PreferencesHelper
import com.surrus.galwaybus.cache.db.GalwayBusDatabase
import com.surrus.galwaybus.data.GalwayBusDataRepository
import com.surrus.galwaybus.data.repository.GalwayBusCache
import com.surrus.galwaybus.data.repository.GalwayBusRemote
import com.surrus.galwaybus.data.source.GalwayBusDataStoreFactory
import com.surrus.galwaybus.di.scopes.PerApplication
import com.surrus.galwaybus.domain.executor.ExecutorThread
import com.surrus.galwaybus.domain.executor.PostExecutionThread
import com.surrus.galwaybus.domain.repository.GalwayBusRepository
import com.surrus.galwaybus.remote.GalwayBusRemoteImpl
import com.surrus.galwaybus.remote.GalwayBusService
import com.surrus.galwaybus.ui.UiThread
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.buffer.android.boilerplate.data.executor.JobExecutorThread
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


@Module
open class ApplicationModule {

    @Provides
    @PerApplication
    fun provideContext(application: Application): Context {
        return application
    }

    @Provides
    @PerApplication
    internal fun providePreferencesHelper(context: Context): PreferencesHelper {
        return PreferencesHelper(context)
    }

    @Provides
    @PerApplication
    fun provideOkHttpClient(application: Application): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val cacheDir = File(application.cacheDir, UUID.randomUUID().toString())
        // 10MB cache
        val cache = Cache(cacheDir, 10 * 1024 * 1024)

        return OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .addNetworkInterceptor( StethoInterceptor())
                .build()
    }

    @Provides
    @PerApplication
    internal fun provideGalwayBusService(okHttpClient: OkHttpClient) : GalwayBusService {
        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl("http://galwaybus.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()

        return retrofit.create(GalwayBusService::class.java)
    }


    @Provides
    @PerApplication
    internal fun provideGalwayBusRepository(factory: GalwayBusDataStoreFactory): GalwayBusRepository {
        return GalwayBusDataRepository(factory)
    }


    @Provides
    @PerApplication
    internal fun provideGalwayBusCache(database: GalwayBusDatabase, preferencesHelper: PreferencesHelper): GalwayBusCache {
        return GalwayBusCacheImpl(database, preferencesHelper)
    }

    @Provides
    @PerApplication
    internal fun provideGalwayBusRemote(galwayBusService: GalwayBusService): GalwayBusRemote {
        return GalwayBusRemoteImpl(galwayBusService)
    }


    @Provides
    @PerApplication
    internal fun provideThreadExecutor(jobExecutor: JobExecutorThread): ExecutorThread {
        return jobExecutor
    }

    @Provides
    @PerApplication
    internal fun providePostExecutionThread(uiThread: UiThread): PostExecutionThread {
        return uiThread
    }


    @Provides
    @PerApplication
    internal fun provideGalwayBusDatabase(application: Application): GalwayBusDatabase {
        return Room.databaseBuilder(application.applicationContext,
                GalwayBusDatabase::class.java, "galway_bus.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}