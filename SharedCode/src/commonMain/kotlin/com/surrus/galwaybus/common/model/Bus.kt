package com.surrus.galwaybus.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

@Serializable
data class DepartureMetadata(val destination: String, val delay: Int)

object MapToList : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val arrayEntries = element.jsonObject.map { (_, value) -> value }
        return JsonArray(arrayEntries)
    }
}

@Serializable
data class Bus(val vehicle_id: String, val modified_timestamp: String, val latitude: Double,
                val longitude: Double,
                val direction: Int = 0,
                val departure_metadata: DepartureMetadata? = null,
                @Serializable(with = MapToList::class)
                val route: List<String>? = null,
                val next_stop_ref: String? = null)