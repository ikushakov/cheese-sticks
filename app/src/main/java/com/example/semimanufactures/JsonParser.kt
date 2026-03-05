package com.example.semimanufactures

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object JsonParser {

    // Универсальный парсер для ответов с полем "data"
    fun parseLogisticsResponse(jsonString: String): LogisticsDetailResult {
        return try {
            // Пробуем стандартный парсинг через Gson
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

            // Проверяем, есть ли поле data
            if (!jsonObject.has("data")) {
                return LogisticsDetailResult(
                    success = jsonObject.get("success")?.asBoolean ?: false,
                    message = jsonObject.get("message")?.asString,
                    data = null,
                    error = "Поле 'data' отсутствует в ответе"
                )
            }

            val dataElement = jsonObject.get("data")
            val logisticsItem: LogisticsItem?
            var isArray = false

            when {
                dataElement.isJsonObject -> {
                    // data - объект
                    logisticsItem = gson.fromJson(dataElement, LogisticsItem::class.java)
                }
                dataElement.isJsonArray -> {
                    // data - массив
                    isArray = true
                    val array = dataElement.asJsonArray
                    logisticsItem = if (array.size() > 0) {
                        gson.fromJson(array.get(0), LogisticsItem::class.java)
                    } else {
                        null
                    }
                }
                else -> {
                    // data - null или другой тип
                    logisticsItem = null
                }
            }

            LogisticsDetailResult(
                success = jsonObject.get("success")?.asBoolean ?: (logisticsItem != null),
                message = jsonObject.get("message")?.asString,
                data = logisticsItem,
                error = if (logisticsItem == null) "Не удалось распарсить данные" else null,
                isDataArray = isArray
            )

        } catch (e: JsonSyntaxException) {
            // Если Gson не справляется, пробуем через JSONObject
            parseWithManualJson(jsonString)
        } catch (e: Exception) {
            LogisticsDetailResult(
                success = false,
                message = null,
                data = null,
                error = "Ошибка парсинга: ${e.message}"
            )
        }
    }

    // Ручной парсинг через JSONObject (запасной вариант)
    private fun parseWithManualJson(jsonString: String): LogisticsDetailResult {
        return try {
            val json = JSONObject(jsonString)
            val success = json.optBoolean("success", false)
            val message = json.optString("message", null)
            val page = json.optInt("page", 0)
            val total = json.optString("total", null)

            val dataElement = json.opt("data")
            val logisticsItem: LogisticsItem?
            var isArray = false

            when (dataElement) {
                is JSONObject -> {
                    // Сначала парсим без docs, потом обрабатываем docs отдельно
                    val dataJson = dataElement.toString()
                    logisticsItem = parseLogisticsItemWithDocs(dataElement)
                }
                is JSONArray -> {
                    isArray = true
                    val array = dataElement as JSONArray
                    logisticsItem = if (array.length() > 0) {
                        val firstElement = array.getJSONObject(0)
                        parseLogisticsItemWithDocs(firstElement)
                    } else {
                        null
                    }
                }
                else -> {
                    logisticsItem = null
                }
            }

            LogisticsDetailResult(
                success = success,
                message = message,
                data = logisticsItem,
                error = if (logisticsItem == null) "Данные отсутствуют или некорректны" else null,
                isDataArray = isArray,
                page = page,
                total = total
            )

        } catch (e: JSONException) {
            LogisticsDetailResult(
                success = false,
                message = null,
                data = null,
                error = "Ошибка JSON: ${e.message}"
            )
        }
    }

    private fun parseLogisticsItemWithDocs(jsonObject: JSONObject): LogisticsItem? {
        return try {
            val jsonCopy = JSONObject(jsonObject.toString())

            // Обрабатываем docs как массив
            val docsArray = jsonObject.optJSONArray("docs")
            val docsList = mutableListOf<Doc>()

            if (docsArray != null) {
                for (i in 0 until docsArray.length()) {
                    try {
                        val docJson = docsArray.optJSONObject(i)
                        if (docJson != null) {
                            val doc = Gson().fromJson(docJson.toString(), Doc::class.java)
                            // Фильтруем только документы с md5Name
                            if (!doc.md5Name.isNullOrBlank()) {
                                docsList.add(doc)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("JsonParser", "Error parsing doc at index $i", e)
                    }
                }
            }

            // Удаляем docs из копии
            jsonCopy.remove("docs")

            // Парсим основной объект
            val baseItem = Gson().fromJson(jsonCopy.toString(), LogisticsItem::class.java)

            // Возвращаем объект с docs (Gson сам заполнит поле)
            // Для этого создаем новый JSON с правильной структурой
            val finalJson = JSONObject(jsonCopy.toString())
            if (docsList.isNotEmpty()) {
                finalJson.put("docs", JSONArray(Gson().toJson(docsList)))
            } else {
                finalJson.put("docs", JSONArray())
            }

            Gson().fromJson(finalJson.toString(), LogisticsItem::class.java)

        } catch (e: Exception) {
            Log.e("JsonParser", "Error parsing LogisticsItem with docs", e)
            null
        }
    }
    // Для парсинга массива заявок (например, в основном списке)
    fun parseLogisticsListResponse(jsonString: String): LogisticsListResult {
        return try {
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

            val dataElement = jsonObject.get("data")
            val items = mutableListOf<LogisticsItem>()
            var isArray = false

            when {
                dataElement.isJsonArray -> {
                    isArray = true
                    val array = dataElement.asJsonArray
                    array.forEach { element ->
                        if (element.isJsonObject) {
                            items.add(gson.fromJson(element, LogisticsItem::class.java))
                        }
                    }
                }
                dataElement.isJsonObject -> {
                    // Если вдруг data - объект, а не массив (редкий случай)
                    items.add(gson.fromJson(dataElement, LogisticsItem::class.java))
                }
            }

            LogisticsListResult(
                success = jsonObject.get("success")?.asBoolean ?: items.isNotEmpty(),
                message = jsonObject.get("message")?.asString,
                data = items,
                draw = jsonObject.get("draw")?.asInt ?: 0,
                recordsTotal = jsonObject.get("recordsTotal")?.asInt ?: 0,
                recordsFiltered = jsonObject.get("recordsFiltered")?.asInt ?: 0,
                error = if (items.isEmpty()) "Список пуст" else null
            )

        } catch (e: Exception) {
            LogisticsListResult(
                success = false,
                message = null,
                data = emptyList(),
                draw = 0,
                recordsTotal = 0,
                recordsFiltered = 0,
                error = "Ошибка парсинга списка: ${e.message}"
            )
        }
    }
}

// Результат парсинга деталей заявки
data class LogisticsDetailResult(
    val success: Boolean,
    val message: String?,
    val data: LogisticsItem?,
    val error: String? = null,
    val isDataArray: Boolean = false,
    val page: Int? = null,
    val total: String? = null
)

// Результат парсинга списка заявок
data class LogisticsListResult(
    val success: Boolean,
    val message: String?,
    val data: List<LogisticsItem>,
    val draw: Int,
    val recordsTotal: Int,
    val recordsFiltered: Int,
    val error: String? = null
)