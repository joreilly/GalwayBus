package com.surrus.galwaybus.cache.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.surrus.galwaybus.cache.dao.GalwayBusDao
import com.surrus.galwaybus.model.BusRoute
import com.surrus.galwaybus.model.BusStop


@Database(entities = arrayOf(BusRoute::class, BusStop::class), version = 3, exportSchema = false)
abstract class GalwayBusDatabase : RoomDatabase() {

    abstract fun galwayBusDao(): GalwayBusDao
}