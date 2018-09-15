package com.surrus.galwaybus.cache

import androidx.room.Room
import com.surrus.galwaybus.cache.db.GalwayBusDatabase
import com.surrus.galwaybus.factory.GalwayBusFactory
import junit.framework.Assert.assertEquals
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
    fun clearTablesCompletes() {
        val testObserver = databaseHelper.clearBusRoutes().test()
        testObserver.assertComplete()
    }

    @Test
    fun saveBusRoutes() {
        val busRouteCount = 2
        val busRouteList = GalwayBusFactory.makeBusRouteList(busRouteCount)
        val testObserver = databaseHelper.saveBusRoutes(busRouteList).test()
        val numberOfRows = galwayBusDatabase.galwayBusDao().getBusRoutes().size
        testObserver.assertComplete()
        assertEquals(busRouteCount, numberOfRows)
    }


    @Test
    fun getBusRouteList() {
        val busRouteList = GalwayBusFactory.makeBusRouteList(2)
        busRouteList.forEach {
            galwayBusDatabase.galwayBusDao().insertBusRoute(it)
        }

        val testObserver = databaseHelper.getBusRoutes().test()
        testObserver.assertValue(busRouteList)
    }


}