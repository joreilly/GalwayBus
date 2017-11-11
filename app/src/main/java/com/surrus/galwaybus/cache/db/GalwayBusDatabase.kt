package com.surrus.galwaybus.cache.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.surrus.galwaybus.cache.dao.GalwayBusDao
import com.surrus.galwaybus.model.BusRoute
import javax.inject.Inject

@Database(entities = arrayOf(BusRoute::class), version = 1)
abstract class GalwayBusDatabase : RoomDatabase() {

    abstract fun galwayBusDao(): GalwayBusDao
}