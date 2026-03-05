package com.example.semimanufactures.map.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Модель сотрудника
 */
@JsonClass(generateAdapter = true)
data class Employee(
    @Json(name = "id")
    val id: String,
    @Json(name = "fio")
    val fio: String? = null,
    @Json(name = "position")
    val position: String? = null,
    @Json(name = "mdmcode")
    val mdmcode: String? = null
)

