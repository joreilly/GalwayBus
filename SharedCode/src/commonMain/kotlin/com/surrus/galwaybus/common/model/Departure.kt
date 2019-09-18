package com.surrus.galwaybus.common.model

import kotlinx.serialization.Serializable


@Serializable
data class Departure(val timetableId: String, val displayName: String, val departTimestamp: String)

