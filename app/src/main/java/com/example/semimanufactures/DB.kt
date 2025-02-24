package com.example.semimanufactures

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.MultipartBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object DatabaseManager {
    private var database: Database? = null
    suspend fun connect(context: Context) {
        if (database != null) {
            Log.w("DatabaseManager", "Already connected to the database")
            showToast(context, "Уже подключен к базе данных", 5000)
            return
        }
        withContext(Dispatchers.IO) {
            try {
                database = Database.connect(
                    url = "jdbc:mysql://192.168.200.250:3306/individual_tasks?useSSL=false",
                    user = "root",
                    password = "bitrix"
                )
                Log.d("DatabaseManager", "Database connection successful")
                showToast(context, "Успешное подключение к базе данных", 5000)
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Failed to connect to database: ${e.message}", e)
                showToast(context, "Не удалось подключиться к базе данных", 5000)
                database = null
            }
        }
    }
    fun isConnected(): Boolean {
        return database != null
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    suspend fun fetchData(context: Context): List<CardItem> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        val apiUrl = "http://192.168.200.250/api/get_prp_for_get"
        val cardItems = mutableListOf<CardItem>()
        val request = Request.Builder()
            .url(apiUrl)
            .build()
        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    Log.d("API_RESPONSE", "Response body: $responseBody")
                    val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
                    for (jsonElement in jsonArray) {
                        val jsonObject = jsonElement.jsonObject
                        val name = jsonObject["Название"]?.jsonPrimitive?.content ?: "Не указано"
                        val prosk = jsonObject["Проск"]?.jsonPrimitive?.content ?: "Не указано"
                        val demand = jsonObject["Спрос"]?.jsonPrimitive?.content ?: "Не указано"
                        val quantity = jsonObject["Количество"]?.jsonPrimitive?.content ?: "Не указано"
                        val plot = jsonObject["Участок"]?.jsonPrimitive?.content ?: "Не указано"
                        val dateOfDistribution = jsonObject["Дата Распределения"]?.jsonPrimitive?.content
                        val prp = jsonObject["ПрП"]?.jsonPrimitive?.content ?: "Не указано"
                        val primarydemand_id = jsonObject["primarydemand_id"]?.jsonPrimitive?.content ?: "Не указано"
                        val skladiDataId = jsonObject["skladi_data_id"]?.jsonPrimitive?.content ?: "Не указано"
                        dateOfDistribution?.let {
                            CardItem(name, prosk, demand, quantity, plot,
                                it, prp, skladiDataId, primarydemand_id)
                        }?.let { cardItems.add(it) }
                    }
                }
            } else {
                throw Exception("Ошибка при получении данных: ${response.code}")
            }
        } catch (e: SocketTimeoutException) {
            Log.e("fetchData", "Timeout error: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("fetchData", "Error fetching data: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка при получении данных: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return@withContext cardItems
    }
    suspend fun addToSkladiData(
        context: Context,
        primarydemand_id: String,
        userId: Int,
        userFio: String,
        dateDistribution: String,
        demand: String
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val currentDate = System.currentTimeMillis() / 1000
        val distributionValue = if (dateDistribution.isNullOrEmpty()) "" else dateDistribution
        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("primarydemand_id", primarydemand_id)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("user_fio", userFio)
            .addFormDataPart("date_distribution", if (distributionValue.isNullOrEmpty() || distributionValue == "null") "" else distributionValue)
            .addFormDataPart("action", "1")
            .addFormDataPart("system_name", "mobile")
            .addFormDataPart("date", currentDate.toString())
            .addFormDataPart("prp_name", demand)
            .build()
        val request = Request.Builder()
            .url("http://192.168.200.250/api/create_skladi_data")
            .post(formBody)
            .build()
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                when (response.code) {
                    in 200..299 -> {
                        Log.d("API Success", "Запись успешно добавлена")
                        showToast(context, "ПрП успешно выдана с ПРОСКф", 10000)
                    }
                    400 -> {
                        Log.w("API Warning", "Данная ПрП не была добавлена на ПРОСК")
                        showToast(context, "Данная ПрП не была добавлена на ПРОСК", 10000)
                    }
                    500 -> {
                        Log.w("API Warning", "Данная ПрП была выдана, но неизвестно куда")
                        showToast(context, "Данная ПрП была выдана, но неизвестно куда", 10000)
                    }
                    else -> {
                        Log.w("API Warning", "Не удалось добавить запись: ${response.message}")
                        showToast(context, "Не удалось добавить запись", 10000)
                    }
                }
                response.close()
            } catch (e: SocketTimeoutException) {
                Log.e("API Error", "Timeout error during request: ${e.message}")
                showToast(context, "Попробуйте позже. Сервер не отвечает.", 10000)
            } catch (e: IOException) {
                Log.e("API Error", "Ошибка при выполнении запроса: ${e.message}")
                showToast(context, "Ошибка при добавлении записи", 10000)
            } catch (e: Exception) {
                Log.e("API Error", "Unexpected error: ${e.message}")
                showToast(context, "Неизвестная ошибка при добавлении записи.", 10000)
            }
        }
    }
    suspend fun findCardByIdOrPrp(
        context: Context,
        prp: String? = null
    ): List<CardItem> {
        val methodName = "findCardByIdOrPrp"
        Log.d(methodName, "Starting with prp: $prp")
        val allCards = fetchData(context)
        Log.d(methodName, "Fetched ${allCards.size} cards from API")
        if (prp.isNullOrEmpty()) {
            Log.d(methodName, "ПрП пустое, возвращаем пустой список.")
            return emptyList()
        }
        val localCard = allCards.firstOrNull { it.prp == prp }
        return if (localCard != null) {
            Log.d(methodName, "Card found in fetched data: $localCard")
            listOf(localCard)
        } else {
            Log.d(methodName, "No cards found matching parameters.")
            emptyList()
        }
    }
    suspend fun findPrimaryDemandIdAndDate(context: Context, barcodeValue: String): SearchDataRaspredeleniyaAndSegmentAndPrimaryDemandId? {
        val url = "http://192.168.200.250/api/get_task?title=$barcodeValue"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        Log.d("DatabaseManager", "Finding primaryDemandId, ДатаРаспределения and Сегмент for barcodeValue: $barcodeValue")
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()
                Log.d("API Request", "Executing API request: $url")
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("API Request", "Unexpected code: ${response.code}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string() ?: run {
                        Log.e("API Request", "Response body is null")
                        return@withContext null
                    }
                    Log.d("API Response", "Received response: $responseBody")
                    val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
                    if (jsonArray.isNotEmpty()) {
                        val firstTask = jsonArray[0].jsonObject
                        val primaryDemandId = firstTask["primarydemand_id"]?.jsonPrimitive?.content
                        val dateDistribution = firstTask["ДатаРаспределения"]?.jsonPrimitive?.content
                        val segment = firstTask["Сегмент"]?.jsonPrimitive?.content
                        val demand = firstTask["Спрос"]?.jsonPrimitive?.content
                        Log.d("API Response", "Found primaryDemandId: $primaryDemandId, ДатаРаспределения: $dateDistribution, Сегмент: $segment, Спрос: $demand")
                        return@withContext SearchDataRaspredeleniyaAndSegmentAndPrimaryDemandId(
                            primarydemand_id = primaryDemandId,
                            dateDistribution = dateDistribution,
                            segment = segment,
                            demand = demand
                        )
                    } else {
                        Log.d("API Response", "No tasks found for ПрП = $barcodeValue")
                        return@withContext null
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("API Request", "Timeout error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e("API Request", "Error fetching data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка при получении данных: ${e.message}", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addToSkladiDataPrP(
        context: Context,
        barcodeValue: String,
        userId: Int,
        skladId: String,
        userFio: String,
        textField: TextView
    ): Boolean {
        Log.d("DatabaseManager", "Adding to skladi_data for barcodeValue: $barcodeValue, userId: $userId, skladId: $skladId")
        val searchData = findPrimaryDemandIdAndDate(context, barcodeValue)
        if (searchData == null || searchData.primarydemand_id == null) {
            Log.e("DatabaseManager", "ПрП не найден.")
            return false
        }
        val primaryDemandId = searchData.primarydemand_id
        val dateDistribution = searchData.dateDistribution
        val segment = searchData.segment
        val demand = searchData.demand
        if (skladId.isBlank()) {
            Log.e("DatabaseManager", "Sklad ID не может быть пустым.")
            showToast(context, "Склад не может быть пустым", 10000)
            return false
        }
        val currentDate = LocalDate.now()
        val dateDistributionParsed = when {
            dateDistribution.isNullOrEmpty() -> null
            dateDistribution == "null" -> null
            else -> LocalDate.parse(dateDistribution)
        }
        val daysPlusSeria = fetchDaysToAddSeria(context) ?: 3
        val daysPlusMezhZavod = fetchDaysToAddMezhZavod(context) ?: 90
        val daysPlusOKR = fetchDaysToAddOKR(context) ?: 45
        val daysPlusPosleProd = fetchDaysToAddPosleProdazhnoeObsluzhivanie(context) ?: 45
        if (dateDistributionParsed == null) {
            Log.d("DatabaseManager", "Дата распределения пустая, запись будет добавлена.")
        } else if (segment == "Серия" && dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusSeria))) {
            Log.d("DatabaseManager", "Сегмент и дата распределения допустимые, запись будет добавлена.")
            showToast(context, "Сегмент и дата распределения допустимые, запись будет добавлена: Дата распределения - $dateDistributionParsed и Сегмент - $segment", 10000)
        } else if (segment == "Межзавод" && dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusMezhZavod))) {
            Log.d("DatabaseManager", "Сегмент и дата распределения допустимые, запись будет добавлена.")
            showToast(context, "Сегмент и дата распределения допустимые, запись будет добавлена: Дата распределения - $dateDistributionParsed и Сегмент - $segment", 10000)
        }
        else if (segment == "ОКР" && dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusOKR))) {
            Log.d("DatabaseManager", "Сегмент и дата распределения допустимые, запись будет добавлена.")
            showToast(context, "Сегмент и дата распределения допустимые, запись будет добавлена: Дата распределения - $dateDistributionParsed и Сегмент - $segment", 10000)
        }
        else if (segment == "Послепродажное обслуживание" && dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusPosleProd))) {
            Log.d("DatabaseManager", "Сегмент и дата распределения допустимые, запись будет добавлена.")
            showToast(context, "Сегмент и дата распределения допустимые, запись будет добавлена: Дата распределения - $dateDistributionParsed и Сегмент - $segment", 10000)
        } else {
            Log.e("DatabaseManager", "Не удается добавить запись. Дата распределения и сегмент не соответствуют условиям.")
            showToast(context, "Запись не может быть добавлена: Дата распределения - $dateDistributionParsed и Сегмент - $segment", 10000)
            return false
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val currentTimestamp = Instant.now().epochSecond
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("primarydemand_id", primaryDemandId)
            .addFormDataPart("sklad_id", skladId)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("user_fio", userFio)
            .addFormDataPart("date_distribution", if (dateDistribution.isNullOrEmpty() || dateDistribution == "null") "" else dateDistribution)
            .addFormDataPart("action", "0")
            .addFormDataPart("system_name", "mobile")
            .addFormDataPart("date", currentTimestamp.toString())
            .addFormDataPart("prp_name", demand.toString())
            .build()
        Log.d("DatabaseManager", "Тело запроса: ${requestBody.toString()}")
        val request = Request.Builder()
            .url("http://192.168.200.250/api/create_skladi_data")
            .post(requestBody)
            .build()
        return try {
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                Log.d("DatabaseManager", "Ответ от сервера: ${response.body?.string()}")
                if (!response.isSuccessful) {
                    Log.e("DatabaseManager", "Ошибка при добавлении записи: ${response.message}")
                    when (response.code) {
                        400 -> showToast(context, "Некорректно введены данные для добавления на ПРОСК", 10000)
                        409 -> showToast(context, "Данная ПрП уже есть на складе на ПРОСК", 10000) // Код 409 для конфликта
                        else -> showToast(context, "Произошла ошибка: ${response.message}", 10000)
                    }
                    return@withContext false
                } else {
                    Log.d("DatabaseManager", "Запись успешно добавлена в skladi_data.")
                    showToast(context, "Запись успешно добавлена на ПРОСК пользователем $userId", 10000)
                    true
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e("DatabaseManager", "Timeout error during request: ${e.message}")
            withContext(Dispatchers.Main) {
                showToast(context, "Попробуйте позже. Сервер не отвечает.", 10000)
            }
            false
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Не удалось добавить запись: ${e.message}", e)
            withContext(Dispatchers.Main) {
                showToast(context, "Ошибка при добавлении записи.", 10000)
            }
            false
        }
    }
    suspend fun showToast(context: Context, message: String, duration: Long) {
        withContext(Dispatchers.Main) {
            val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            toast.show()
            Handler(Looper.getMainLooper()).postDelayed({
                toast.cancel()
            }, duration)
        }
    }
    suspend fun findDistributionDateByPrP(context: Context, barcodeValue: String): SearchDataRaspredeleniyaAndSegment? {
        val url = "http://192.168.200.250/api/get_task?title=$barcodeValue"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        Log.d("DatabaseManager", "Finding ДатаРаспределения and Сегмент for barcodeValue: $barcodeValue")
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()
                Log.d("API Request", "Executing API request: $url")
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("API Request", "Unexpected code: ${response.code}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string() ?: run {
                        Log.e("API Request", "Response body is null")
                        return@withContext null
                    }
                    Log.d("API Response", "Received response: $responseBody")
                    val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
                    if (jsonArray.isNotEmpty()) {
                        val firstTask = jsonArray[0].jsonObject
                        val dateDistribution = firstTask["ДатаРаспределения"]?.jsonPrimitive?.let {
                            if (it.isString) it.content else null
                        }
                        val segment = firstTask["Сегмент"]?.jsonPrimitive?.let {
                            if (it.isString) it.content else null
                        }
                        val demand = firstTask["Спрос"]?.jsonPrimitive?.let {
                            if (it.isString) it.content else null
                        }
                        Log.d("API Response", "Найдена Дата Распределения: $dateDistribution, Сегмент: $segment для ПрП = $barcodeValue и $demand")
                        showToast(context, "Найдена Дата Распределения: $dateDistribution, Сегмент: $segment для ПрП = $barcodeValue и $demand", 5000)
                        return@withContext SearchDataRaspredeleniyaAndSegment(dateDistribution, segment, demand)
                    } else {
                        Log.d("API Response", "No ДатаРаспределения and Сегмент found for ПрП = $barcodeValue")
                        showToast(context, "Не найдена Дата Распределения и Сегмент для ПрП = $barcodeValue", 5000)
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                Log.e("API Request", "Error fetching data: ${e.message}", e)
                return@withContext null
            }
            catch (e: SocketTimeoutException) {
                Log.e("API Request", "Timeout error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }
        }
    }
    suspend fun getWarehouseNameById(context: Context, skladiDataId: String): String? {
        val apiUrl = "http://192.168.200.250/api/get_all_skladi"
        Log.d("WarehouseManager", "Найден склад с id: $skladiDataId")
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(apiUrl)
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonData = JSONObject(responseBody)
                        val warehouseData = jsonData.optJSONObject(skladiDataId)
                        if (warehouseData != null) {
                            val warehouseName = warehouseData.getString("Наименование")
                            Log.d("WarehouseManager", "Found warehouse name: $warehouseName for ID = $skladiDataId")
                            withContext(Dispatchers.Main) {
                                showToast(context, "Найден склад с наименованием: $warehouseName", 5000)
                            }
                            return@withContext warehouseName
                        } else {
                            Log.d("WarehouseManager", "No warehouse found for ID = $skladiDataId")
                            withContext(Dispatchers.Main) {
                                showToast(context, "Не найден склад с id: $skladiDataId", 5000)
                            }
                            return@withContext null
                        }
                    }
                } else {
                    Log.e("WarehouseManager", "Ошибка при получении данных: ${response.code}")
                    withContext(Dispatchers.Main) {
                        showToast(context, "Ошибка при получении данных: ${response.message}", 5000)
                    }
                    return@withContext null
                }
            } catch (e: SocketTimeoutException) {
                Log.e("WarehouseManager", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    showToast(context, "Попробуйте позже. Сервер не отвечает.", 5000)
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e("WarehouseManager", "Не удалось получить данные: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast(context, "Не удалось получить данные: ${e.message}", 5000)
                }
                return@withContext null
            }
        }
    }
    suspend fun getAllWarehouses(context: Context): List<Warehouse> {
        val apiUrl = "http://192.168.200.250/api/get_all_skladi"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val warehouses = mutableListOf<Warehouse>()
            val request = Request.Builder()
                .url(apiUrl)
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonData = JSONObject(responseBody)
                        for (key in jsonData.keys()) {
                            val warehouseData = jsonData.getJSONObject(key)
                            val id = warehouseData.getString("id")
                            val name = warehouseData.getString("Наименование")
                            val shelf = warehouseData.getString("Полка")
                            val rack = warehouseData.getString("Стеллаж")
                            val displayName = "$name $rack $shelf"
                            warehouses.add(Warehouse(id, name, displayName))
                        }
                    }
                } else {
                    Log.e("WarehouseManager", "Ошибка при получении данных: ${response.code}")
                    throw Exception("Ошибка при получении данных: ${response.code}")
                }
            } catch (e: SocketTimeoutException) {
                Log.e("WarehouseManager", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.e("WarehouseManager", "Не удалось получить данные: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
                return@withContext emptyList()
            }
            warehouses
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addInventoryRecord(
        context: Context,
        barcodeValue: String,
        userId: Int,
        skladId: String,
        userFio: String,
        textField: TextView
    ): Boolean {
        Log.d("DatabaseManager", "Adding to skladi_data for barcodeValue: $barcodeValue, userId: $userId, skladId: $skladId")
        val searchData = findPrimaryDemandIdAndDate(context, barcodeValue)
        if (searchData == null || searchData.primarydemand_id == null) {
            Log.e("DatabaseManager", "ПрП не найден.")
            return false
        }
        val primaryDemandId = searchData.primarydemand_id
        val dateDistribution = searchData.dateDistribution
        if (skladId.isBlank()) {
            Log.e("DatabaseManager", "Sklad ID не может быть пустым.")
            return false
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val currentTimestamp = Instant.now().epochSecond
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("primarydemand_id", primaryDemandId)
            .addFormDataPart("sklad_id", skladId)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("user_fio", userFio)
            .addFormDataPart("date_distribution", dateDistribution?.takeIf { it.isNotEmpty() } ?: "")
            .addFormDataPart("system_name", "mobile")
            .addFormDataPart("date", currentTimestamp.toString())
            .build()
        val request = Request.Builder()
            .url("http://192.168.200.250/api/create_skladi_inv_data")
            .post(requestBody)
            .build()
        return try {
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("DatabaseManager", "Ошибка при добавлении записи: ${response.message}")
                    if (response.code == 400) {
                        showToast(context, "Данная ПрП проинвентаризирована", 5000)
                    }
                    false
                } else {
                    Log.d("DatabaseManager", "ПрП успешно проинвентаризирована")
                    showToast(context, "ПрП успешно проинвентаризирована сотрудником $userId", 5000)
                    true
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e("DatabaseManager", "Timeout error during request: ${e.message}")
            withContext(Dispatchers.Main) {
                showToast(context, "Попробуйте позже. Сервер не отвечает.", 5000)
            }
            false
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Не удалось добавить запись: ${e.message}", e)
            withContext(Dispatchers.Main) {
                showToast(context, "Ошибка при добавлении записи.", 5000)
            }
            false
        }
    }
    suspend fun fetchDaysToAddSeria(context: Context): Long? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://192.168.200.250/api/get_project_setting/skladi_today_plus_value_Серия")
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseData ->
                        val adapter = moshi.adapter(ApiResponseDateValue::class.java)
                        val apiResponse = adapter.fromJson(responseData)
                        return@let apiResponse?.value?.toLongOrNull()
                    }
                } else {
                    Log.e("ApiError", "Ошибка в ответе сервера: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении значения: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            null
        }
    }
    suspend fun fetchDaysToAddPosleProdazhnoeObsluzhivanie(context: Context): Long? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://192.168.200.250/api/get_project_setting/skladi_today_plus_value_Послепродажное обслуживание")
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseData ->
                        val adapter = moshi.adapter(ApiResponseDateValue::class.java)
                        val apiResponse = adapter.fromJson(responseData)
                        return@let apiResponse?.value?.toLongOrNull()
                    }
                } else {
                    Log.e("ApiError", "Ошибка в ответе сервера: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении значения: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            null
        }
    }
    suspend fun fetchDaysToAddMezhZavod(context: Context): Long? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://192.168.200.250/api/get_project_setting/skladi_today_plus_value_Межзавод")
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseData ->
                        val adapter = moshi.adapter(ApiResponseDateValue::class.java)
                        val apiResponse = adapter.fromJson(responseData)
                        return@let apiResponse?.value?.toLongOrNull()
                    }
                } else {
                    Log.e("ApiError", "Ошибка в ответе сервера: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении значения: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            null
        }
    }
    suspend fun fetchDaysToAddOKR(context: Context): Long? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://192.168.200.250/api/get_project_setting/skladi_today_plus_value_ОКР")
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseData ->
                        val adapter = moshi.adapter(ApiResponseDateValue::class.java)
                        val apiResponse = adapter.fromJson(responseData)
                        return@let apiResponse?.value?.toLongOrNull()
                    }
                } else {
                    Log.e("ApiError", "Ошибка в ответе сервера: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении значения: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            null
        }
    }
    suspend fun fetchMobileVersion(context: Context): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://192.168.200.250/api/get_version")
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.d("ApiResponse", "Response data: $responseData")
                    if (responseData != null) {
                        val jsonObject = JSONObject(responseData)
                        Log.d("ApiResponse", "Parsed JSON: $jsonObject")
                        if (jsonObject.has("version")) {
                            val version = jsonObject.getInt("version")
                            Log.d("ApiResponse", "Извлечённая версия: $version")
                            return@withContext version.toString()
                        } else {
                            Log.e("ApiError", "Ключ 'version' отсутствует в ответе")
                        }
                    } else {
                        Log.e("ApiError", "Response body is null")
                    }
                } else {
                    Log.e("ApiError", "Ошибка в ответе сервера: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении значения: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            null
        }
    }
    suspend fun getOperationsForPrp(context: Context, prpValue: String): List<OperationWithDemand> {
        val apiUrl = "http://192.168.200.250/api/get_task?title=${Uri.encode(prpValue)}"
        Log.d("APIManager", "Fetching operations from API: $apiUrl")
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(OperationWithDemand::class.java)
        return withContext(Dispatchers.IO) {
            val operations = mutableListOf<OperationWithDemand>()
            val uniqueOperations = mutableSetOf<String>()
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .build()
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseData ->
                        val jsonArray = JSONArray(responseData)
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val operation = jsonObject.getString("Операция")
                            if (uniqueOperations.add(operation)) {
                                val operationWithDemand = OperationWithDemand(
                                    operation = operation,
                                    operation2 = jsonObject.getString("Operation"),
                                    demand = jsonObject.getString("Спрос"),
                                    uchastok = jsonObject.getString("Участок"),
                                    podrazd_mdm_code = jsonObject.optString("Подразделение_mdm_code"),
                                    next_podrazd_mdm_code = jsonObject.optString("next_Подразделение_mdm_code"),
                                    zahodNomer = jsonObject.getString("ЗаходНомер"),
                                    status = jsonObject.getString("status")
                                )
                                operations.add(operationWithDemand)
                            }
                        }
                    }
                } else {
                    Log.e("ApiError", "Ошибка в ответе сервера: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("APIManager", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("APIManager", "Error fetching data from API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            Log.d("APIManager", "Returning operations: $operations")
            operations
        }
    }
    private var database2: Database? = null
    suspend fun connect2(context: Context) {
        if (database2 != null) {
            Log.w("DatabaseManager", "Already connected to the database")
            showToast(context, "Уже подключен к базе данных", 5000)
            return
        }
        withContext(Dispatchers.IO) {
            try {
                database2 = Database.connect(
                    url = "jdbc:mysql://192.168.200.250:3306/project?useSSL=false",
                    user = "root",
                    password = "bitrix"
                )
                Log.d("DatabaseManager", "Database connection successful")
                showToast(context, "Успешное подключение к базе данных", 5000)
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Failed to connect to database: ${e.message}", e)
                showToast(context, "Не удалось подключиться к базе данных", 5000)
                database2 = null
            }
        }
    }
    fun isConnected2(): Boolean {
        return database2 != null
    }
    suspend fun getDeliveryLogisticsByDemand(prp: String, operation: String, type: String, context: Context): List<LogisticsDeliveryItem> {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val url = "http://192.168.200.250/api/get_delivery_by_object"
        val formBody = FormBody.Builder()
            .add("object_name", prp)
            .add("operation", operation)
            .add("type", type)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                val gson = Gson()
                val listType = object : TypeToken<List<LogisticsDeliveryItem>>() {}.type
                return@withContext gson.fromJson(responseBody, listType)
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                emptyList()
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении данных: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
                emptyList()
            }
        }
    }
    suspend fun getDeliveryLogisticsByDemandDoc(doc: String, type: String, context: Context): List<LogisticsDeliveryItem> {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val url = "http://192.168.200.250/api/get_delivery_by_object"
        val formBody = FormBody.Builder()
            .add("object_id", doc)
            .add("type", type)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                val gson = Gson()
                val listType = object : TypeToken<List<LogisticsDeliveryItem>>() {}.type
                return@withContext gson.fromJson(responseBody, listType)
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                emptyList()
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении данных: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
                emptyList()
            }
        }
    }
    suspend fun getAllSotrudnikiInfo(context: Context): List<SotrudnikiInfo> {
        val apiUrl = "http://192.168.200.250/api/get_all_sotrudniki"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val sotrudniki = mutableListOf<SotrudnikiInfo>()
            val request = Request.Builder()
                .url(apiUrl)
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonData = JSONObject(responseBody)
                        for (key in jsonData.keys()) {
                            val sotrudnikiData = jsonData.getJSONObject(key)
                            val mdmcode = sotrudnikiData.getString("mdmcode")
                            val fio = sotrudnikiData.getString("Рабочий")
                            sotrudniki.add(SotrudnikiInfo(mdmcode, fio))
                        }
                    }
                } else {
                    Log.e("ApiError", "Ошибка при получении данных: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении данных: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            sotrudniki
        }
    }
    suspend fun getFilteredSotrudnikiInfo(context: Context, fio: String): List<SotrudnikiInfo> {
        val sotrudniki = getAllSotrudnikiInfo(context)
        val filteredSotrudniki = sotrudniki.filter { it.fio.contains(fio, ignoreCase = true) }
        filteredSotrudniki.forEach { sotrudnik ->
//            Log.d("SotrudnikiInfo", "Информация о сотруднике: ID=${sotrudnik.mdmcode}, Рабочий=${sotrudnik.fio}")
//            Log.d("Нужная инфа о сотруднике: ", """
//                        mdmcode: ${sotrudnik.mdmcode}
//                        Рабочий: ${sotrudnik.fio}
//                    """.trimIndent())
        }
        return filteredSotrudniki
    }
    suspend fun getAllWarehousesInfo(context: Context): List<WarehouseInfo> {
        val apiUrl = "http://192.168.200.250/api/get_all_skladi"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val warehouses = mutableListOf<WarehouseInfo>()
            val request = Request.Builder()
                .url(apiUrl)
                .build()
            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonData = JSONObject(responseBody)
                        for (key in jsonData.keys()) {
                            val warehouseData = jsonData.getJSONObject(key)
                            val id = warehouseData.getString("id")
                            val name = warehouseData.getString("Наименование")
                            val shelf = warehouseData.optString("Полка", null)
                            val rack = warehouseData.optString("Стеллаж", null)
                            val address = warehouseData.optString("Адрес", null)
                            val coordinates = warehouseData.optString("Координаты", null)
                            val purpose = warehouseData.optString("Назначение", null)
                            val description = warehouseData.optString("Описание", null)
                            val responsibleMDMCode = warehouseData.optString("ОтветственныйMDMкод", null)
                            val responsibleName = warehouseData.optString("ОтветственныйФИО", null)
                            val responsiblePosition = warehouseData.optString("ОтветственныйДолжность", null)
                            val plannerMDMCode = warehouseData.optString("ПлановикMDMкод", null)
                            val plannerName = warehouseData.optString("ПлановикФИО", null)
                            val plannerPosition = warehouseData.optString("ПлановикДолжность", null)
                            val subdivisionMDMCode = warehouseData.optString("ПодразделениеMDMкод", null)
                            val subdivision = warehouseData.optString("Подразделение", null)
                            val mdmKey = warehouseData.optString("mdm_key", null)
                            val isActive = warehouseData.optString("is_active", null)
                            val displayName = "$name $rack $shelf"
                            warehouses.add(WarehouseInfo(
                                id,
                                name,
                                displayName,
                                shelf,
                                rack,
                                address,
                                coordinates,
                                purpose,
                                description,
                                responsibleMDMCode,
                                responsibleName,
                                responsiblePosition,
                                plannerMDMCode,
                                plannerName,
                                plannerPosition,
                                subdivisionMDMCode,
                                subdivision,
                                mdmKey,
                                isActive
                            ))
                        }
                    }
                } else {
                    Log.e("ApiError", "Ошибка при получении данных: ${response.code}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка при получении данных: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return@withContext emptyList()
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiError", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.e("ApiError", "Ошибка при получении данных: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
                return@withContext emptyList()
            }
            warehouses
        }
    }
    suspend fun getFilteredWarehousesInfo(context: Context): List<WarehouseInfo> {
        val warehouses = getAllWarehousesInfo(context)
        val uniqueWarehouses = mutableListOf<WarehouseInfo>()
        var isProskAdded = false
        for (warehouse in warehouses) {
            if (warehouse.name == "ПРОСК полуфабрикатов" && warehouse.id == "812") {
                if (!isProskAdded) {
                    uniqueWarehouses.add(warehouse)
                    isProskAdded = true
                }
            } else if (warehouse.name != "ПРОСК полуфабрикатов") {
                uniqueWarehouses.add(warehouse)
            }
        }
        uniqueWarehouses.forEach { warehouse ->
            //Log.d("WarehouseInfo", "Информация о складе: ID=${warehouse.id}, Наименование=${warehouse.name}")
        }
        return uniqueWarehouses
    }
    suspend fun fetchInfoPrp(context: Context, prp: String): String? {
        val apiUrl = "http://192.168.200.250/api/get_skladi_inv_data"
        Log.d("ApiManager", "Fetching data for ПрП: $prp")
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return withContext(Dispatchers.IO) {
            val formBody = FormBody.Builder()
                .add("prp", prp)
                .build()
            val request = Request.Builder()
                .url(apiUrl)
                .post(formBody)
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("ApiManager", "Unexpected code $response")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка при получении данных для ПрП = $prp", Toast.LENGTH_LONG).show()
                        }
                        return@withContext null
                    }
                    val jsonResponse = response.body?.string()
                    if (jsonResponse != null) {
                        val jsonArray = JSONArray(jsonResponse)
                        if (jsonArray.length() > 0) {
                            val dataObject = jsonArray.getJSONObject(0)
                            val userFio = dataObject.optString("user_fio", "Неизвестно")
                            val naimenovanie = dataObject.optString("Наименование", "Неизвестно")
                            val formattedDate = dataObject.optString("formatted_date", "Неизвестно")
                            val resultString = "$userFio, $naimenovanie, $formattedDate"
                            Log.d("ApiManager", "Found data: $resultString for ПрП = $prp")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Найдены данные: $resultString для ПрП = $prp", Toast.LENGTH_LONG).show()
                            }
                            return@withContext resultString
                        } else {
                            Log.d("ApiManager", "No data found for ПрП = $prp")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Не найдены данные для ПрП = $prp", Toast.LENGTH_LONG).show()
                            }
                            return@withContext null
                        }
                    } else {
                        Log.e("ApiManager", "Response body is null")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка при получении данных для ПрП = $prp", Toast.LENGTH_LONG).show()
                        }
                        return@withContext null
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ApiManager", "Timeout error during request: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            } catch (e: IOException) {
                Log.e("ApiManager", "Exception occurred: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка сети или сервера", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e("ApiManager", "Unexpected error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }
        }
    }
}
data class OperationWithDemand(
    val operation: String,
    val operation2: String,
    val demand: String,
    val uchastok: String,
    val podrazd_mdm_code: String,
    val next_podrazd_mdm_code: String,
    val zahodNomer: String,
    val status: String
) {
    override fun equals(other: Any?) = other is OperationWithDemand && this.operation == other.operation
    override fun hashCode() = operation.hashCode()
}
data class Warehouse(val id: String, val name: String, val displayName: String) {
    override fun toString(): String {
        return "$id, $name"
    }
}
data class WarehouseInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val shelf: String?,
    val rack: String?,
    val address: String?,
    val coordinates: String?,
    val purpose: String?,
    val description: String?,
    val responsibleMDMCode: String?,
    val responsibleName: String?,
    val responsiblePosition: String?,
    val plannerMDMCode: String?,
    val plannerName: String?,
    val plannerPosition: String?,
    val subdivisionMDMCode: String?,
    val subdivision: String?,
    val mdmKey: String?,
    val isActive: String?
) {
    override fun toString(): String {
        return "id склада = $id, наименование = $name, полка = $shelf, стеллаж = $rack, адрес = $address, координаты = $coordinates, назначение = $purpose, описание = $description, ответственный = $responsibleName ($responsiblePosition)"
    }
}
data class SotrudnikiInfo(
    val mdmcode: String,
    val fio: String
)
@Serializable
data class ApiResponseDateValue(val value: String)
data class SearchDataRaspredeleniyaAndSegmentAndPrimaryDemandId(
    val primarydemand_id: String?,
    val dateDistribution: String?,
    val segment: String?,
    val demand: String?
)
data class SearchDataRaspredeleniyaAndSegment(
    val dateDistribution: String?,
    val segment: String?,
    val demand: String?
)
data class UserInfo(
    val id: Int,
    val username: String,
    val password: String,
    val sotrudnikMdmCode: String,
    val fio: String,
    val roleCheck: String,
    val isDis: String
)
data class UserInfoRoles(
    val id: Int,
    val username: String,
    val password: String,
    val sotrudnikMdmCode: String,
    val fio: String,
    val roleCheck: String,
    val isDis: String,
    val roles: List<String>
)