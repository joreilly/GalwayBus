package com.surrus.galwaybus.cache

import androidx.room.Room
import com.surrus.galwaybus.cache.db.GalwayBusDatabase
import com.surrus.galwaybus.factory.GalwayBusFactory
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class GalwayBusCacheImplTest {

    private var galwayBusDatabase = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.application,
            GalwayBusDatabase::class.java).allowMainThreadQueries().build()
    private var preferencesHelper = PreferencesHelper(RuntimeEnvironment.application)

    private val databaseHelper = GalwayBusCacheImpl(galwayBusDatabase, preferencesHelper)


    @Test
    fun clearTablesCompletes() = runBlocking {
        databaseHelper.clearBusRoutes()
    }

    @Test
    fun saveBusRoutes() = runBlocking {
        val busRouteCount = 2
        val busRouteList = GalwayBusFactory.makeBusRouteList(busRouteCount)
        databaseHelper.saveBusRoutes(busRouteList)
        val numberOfRows = galwayBusDatabase.galwayBusDao().getBusRoutes().size
        assertEquals(busRouteCount, numberOfRows)
    }


    @Test
    fun getBusRouteList() = runBlocking {
        val busRouteList = GalwayBusFactory.makeBusRouteList(2)
        busRouteList.forEach {
            galwayBusDatabase.galwayBusDao().insertBusRoute(it)
        }

        val brl = databaseHelper.getBusRoutes()
        assert(brl == busRouteList)
    }


}