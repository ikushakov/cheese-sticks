package com.example.semimanufactures.map.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Модель предмета
 */
@JsonClass(generateAdapter = true)
data class Item(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "coordinates")
    val coordinates: List<Double>? = null // [lat, lon]
)

