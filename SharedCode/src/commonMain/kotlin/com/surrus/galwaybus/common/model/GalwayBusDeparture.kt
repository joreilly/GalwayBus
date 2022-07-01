package com.surrus.galwaybus.common.model

import kotlin.time.Duration

data class GalwayBusDeparture(
    val timetableId: String,
    val displayName: String,
    val departTimestamp: String,
    val durationUntilDeparture: Duration,
    val minutesUntilDeparture: Long
)
