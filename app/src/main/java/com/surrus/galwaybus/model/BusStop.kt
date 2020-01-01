package com.surrus.galwaybus.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.surrus.galwaybus.cache.db.GalwayBusDatabaseConstants


@Entity(tableName = GalwayBusDatabaseConstants.BUS_STOPS_TABLE_NAME)
data class BusStop(@PrimaryKey var stopId: Int, var shortName: String, var longName: String = "", var stopRef: String = "",
                   var irishShortName: String = "", var irishLongName: String = "", var latitude: Double = 0.toDouble(),
                   var longitude: Double = 0.toDouble(), @Ignore var times: List<Departure> = emptyList()) {

    constructor() : this(0, "", "", "", "", "",
            0.toDouble(), 0.toDouble(), emptyList())

}


