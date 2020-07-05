package com.surrus.galwaybus.common.model

import kotlin.math.sqrt

data class Location(val latitude: Double, val longitude: Double) {
    fun distance(that: Location): Double {
        val distanceLat = this.latitude - that.latitude
        val distanceLong = this.longitude - that.longitude

        return sqrt(distanceLat * distanceLat + distanceLong * distanceLong)
    }
}