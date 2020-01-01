package com.surrus.galwaybus.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.surrus.galwaybus.cache.db.GalwayBusDatabaseConstants

@Entity(tableName = GalwayBusDatabaseConstants.BUS_ROUTES_TABLE_NAME)
data class BusRoute(@PrimaryKey val timetableId: String, val longName: String, val shortName: String)

