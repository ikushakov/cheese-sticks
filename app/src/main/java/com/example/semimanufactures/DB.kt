package com.example.semimanufactures

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.service_mode.ServiceModeException
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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object DatabaseManager {
//    private var database: Database? = null
//    suspend fun connect(context: Context) {
//        if (database != null) {
//            Log.w("DatabaseManager", "Already connected to the database")
//            showToast(context, "Уже подключен к базе данных", 5000)
//            return
//        }
//        withContext(Dispatchers.IO) {
//            try {
//                database = Database.connect(
//                    url = "jdbc:mysql://192.168.200.250:3306/individual_tasks?useSSL=false",
//                    user = "root",
//                    password = "bitrix"
//                )
//                Log.d("DatabaseManager", "Database connection successful")
//                showToast(context, "Успешное подключение к базе данных", 5000)
//            } catch (e: Exception) {
//                Log.e("DatabaseManager", "Failed to connect to database: ${e.message}", e)
//                showToast(context, "Не удалось подключиться к базе данных", 5000)
//                database = null
//            }
//        }
//    }
//    fun isConnected(): Boolean {
//        return database != null
//    }
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    suspend fun fetchData(context: Context): List<CardItem> = withContext(Dispatchers.IO) {
        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_prp_for_get",
            "https://api.gkmmz.ru/api/get_prp_for_get"
        )

        val items = mutableListOf<CardItem>()
        var success = false

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.code == 429) return@use // попробуем следующий URL

                    if (!resp.isSuccessful) {
                        throw IOException("HTTP ${resp.code} ${resp.message}")
                    }

                    val body = resp.body?.string().orEmpty()
                    Log.d("API_RESPONSE", "Response body: $body")

                    val jsonArray = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonArray
                    for (el in jsonArray) {
                        val o = el.jsonObject
                        val name   = o["Название"]?.jsonPrimitive?.content ?: "Не указано"
                        val prosk  = o["Проск"]?.jsonPrimitive?.content ?: "Не указано"
                        val demand = o["Спрос"]?.jsonPrimitive?.content ?: "Не указано"
                        val qty    = o["Количество"]?.jsonPrimitive?.content ?: "Не указано"
                        val plot   = o["Участок"]?.jsonPrimitive?.content ?: "Не указано"
                        val date   = o["Дата Распределения"]?.jsonPrimitive?.content
                        val prp    = o["ПрП"]?.jsonPrimitive?.content ?: "Не указано"
                        val primId = o["primarydemand_id"]?.jsonPrimitive?.content ?: "Не указано"
                        val sklId  = o["skladi_data_id"]?.jsonPrimitive?.content ?: "Не указано"

                        if (date != null) {
                            items += CardItem(name, prosk, demand, qty, plot, date, prp, sklId, primId)
                        }
                    }
                    success = true
                }

                if (success) break
            } catch (e: ServiceModeException) {
                // сервисный режим, экран уже откроется через интерсептор
                return@withContext emptyList()
            } catch (e: SocketTimeoutException) {
                Log.e("fetchData", "Timeout: ${e.message}")
                if (i == urls.lastIndex) withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("fetchData", "Error: ${e.message}", e)
                if (i == urls.lastIndex) withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка при получении данных: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        items
    }
    private fun getUnsafeSSLSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(getUnsafeTrustManager())
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }

    private fun getUnsafeTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }
    // 2) Добавление записи в skladi_data
    suspend fun addToSkladiData(
        context: Context,
        primarydemand_id: String,
        userId: Int,
        userFio: String,
        dateDistribution: String,
        demand: String
    ) = withContext(Dispatchers.IO) {
        if (userId == 0 || userFio.isEmpty()) {
            withContext(Dispatchers.Main) { showToast(context, "Вернитесь в настройки, затем попробуйте снова", 10000) }
            return@withContext
        }

        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_skladi_data",
            "https://api.gkmmz.ru/api/create_skladi_data"
        )

        val nowSec = System.currentTimeMillis() / 1000
        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("primarydemand_id", primarydemand_id)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("user_fio", userFio)
            .addFormDataPart("action", "1")
            .addFormDataPart("system_name", version_name)
            .addFormDataPart("date", nowSec.toString())
            .addFormDataPart("prp_name", demand)

        if (dateDistribution.isNotEmpty() && dateDistribution != "null") {
            bodyBuilder.addFormDataPart("date_distribution", dateDistribution)
        }
        val reqBody = bodyBuilder.build()

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            Log.d("API Success", "Запись успешно добавлена")
                            withContext(Dispatchers.Main) { showToast(context, "ПрП успешно выдана с ПРОСКф", 10000) }
                            return@withContext
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("API Warning", "429 на $url — пробуем резервный…")
                            return@use // идём к следующему URL
                        }
                        resp.code == 400 -> {
                            Log.w("API Warning", "Данная ПрП не была добавлена на ПРОСК")
                            withContext(Dispatchers.Main) { showToast(context, "Данная ПрП не была добавлена на ПРОСК", 10000) }
                            return@withContext
                        }
                        resp.code == 500 -> {
                            Log.w("API Warning", "Данная ПрП была выдана, но неизвестно куда")
                            withContext(Dispatchers.Main) { showToast(context, "Данная ПрП была выдана, но неизвестно куда", 10000) }
                            return@withContext
                        }
                        else -> {
                            Log.w("API Warning", "Не удалось добавить запись: ${resp.code} ${resp.message}")
                            if (i == urls.lastIndex) {
                                withContext(Dispatchers.Main) { showToast(context, "Не удалось добавить запись", 10000) }
                                return@withContext
                            }
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                return@withContext
            } catch (e: SocketTimeoutException) {
                Log.e("API Error", "Timeout on $url: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { showToast(context, "Попробуйте позже. Сервер не отвечает.", 10000) }
                    return@withContext
                }
            } catch (e: Exception) {
                Log.e("API Error", "Request error on $url: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { showToast(context, "Ошибка при добавлении записи", 10000) }
                    return@withContext
                }
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
    // 3) Поиск primaryDemandId / даты распределения / сегмента по штрихкоду
    suspend fun findPrimaryDemandIdAndDate(
        context: Context,
        barcodeValue: String
    ): SearchDataRaspredeleniyaAndSegmentAndPrimaryDemandId? = withContext(Dispatchers.IO) {

        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_task?title=$barcodeValue",
            "https://api.gkmmz.ru/api/get_task?title=$barcodeValue"
        )

        Log.d("DatabaseManager", "Поиск данных для штрихкода: $barcodeValue")

        var found: SearchDataRaspredeleniyaAndSegmentAndPrimaryDemandId? = null

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                Log.d("API Request", "GET $url")

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            val body = resp.body?.string().orEmpty()
                            Log.d("API Response", "Ответ: $body")

                            val arr = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonArray
                            val first = arr.firstOrNull()?.jsonObject
                            if (first != null) {
                                found = SearchDataRaspredeleniyaAndSegmentAndPrimaryDemandId(
                                    primarydemand_id = first["primarydemand_id"]?.jsonPrimitive?.content,
                                    dateDistribution = first["ДатаРаспределения"]?.jsonPrimitive?.content,
                                    segment = first["Сегмент"]?.jsonPrimitive?.content,
                                    demand = first["Спрос"]?.jsonPrimitive?.content
                                )
                                Log.d("API Response", "Найдены данные: $found")
                            } else {
                                Log.d("API Response", "Нет данных для ПрП = $barcodeValue")
                            }
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("API Warning", "429 на $url — пробуем резервный…")
                            return@use
                        }
                        else -> {
                            Log.e("API Request", "Ошибка сервера: ${resp.code}")
                            if (i == urls.lastIndex) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ошибка сервера: ${resp.code}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                if (found != null) break
            } catch (e: ServiceModeException) {
                return@withContext null
            } catch (e: SocketTimeoutException) {
                Log.e("API Request", "Таймаут: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Сервер не отвечает. Попробуйте позже.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("API Request", "Ошибка: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка получения данных: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        found
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addToSkladiDataPrP(
        context: Context,
        barcodeValue: String,
        userId: Int,
        skladId: String,
        userFio: String,
        textField: TextView
    ): Boolean = withContext(Dispatchers.IO) {

        Log.d("DatabaseManager", "Adding to skladi_data for barcode: $barcodeValue, userId=$userId, skladId=$skladId")

        // 1) Проверки входных
        if (userId == 0 || userFio.isEmpty()) {
            withContext(Dispatchers.Main) { showToast(context, "Вернитесь в настройки, затем попробуйте снова", 10000) }
            return@withContext false
        }
        if (skladId.isBlank()) {
            withContext(Dispatchers.Main) { showToast(context, "Склад не может быть пустым", 10000) }
            return@withContext false
        }

        // 2) Достаём данные по ПрП
        val searchData = findPrimaryDemandIdAndDate(context, barcodeValue)
        val primaryDemandId = searchData?.primarydemand_id
        val dateDistribution = searchData?.dateDistribution
        val segment = searchData?.segment
        val demand = searchData?.demand

        if (primaryDemandId.isNullOrEmpty()) {
            Log.e("DatabaseManager", "ПрП не найден.")
            return@withContext false
        }

        // 3) Бизнес-правила по дате распределения/сегменту
        val currentDate = LocalDate.now()
        val dateDistributionParsed: LocalDate? = try {
            if (dateDistribution.isNullOrEmpty() || dateDistribution == "null") null
            else LocalDate.parse(dateDistribution)
        } catch (_: Exception) { null }

        val daysPlusSeria      = fetchDaysToAddSeria(context) ?: 3
        val daysPlusMezhZavod  = fetchDaysToAddMezhZavod(context) ?: 210
        val daysPlusOKR        = fetchDaysToAddOKR(context) ?: 60
        val daysPlusPosleProd  = fetchDaysToAddPosleProdazhnoeObsluzhivanie(context) ?: 45

        if (dateDistributionParsed != null) {
            val allowed = when (segment) {
                "Серия" -> dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusSeria.toLong()))
                "Межзавод" -> dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusMezhZavod.toLong()))
                "ОКР" -> dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusOKR.toLong()))
                "Послепродажное обслуживание" -> dateDistributionParsed.isAfter(currentDate.plusDays(daysPlusPosleProd.toLong()))
                else -> false
            }
            if (!allowed) {
                withContext(Dispatchers.Main) {
                    showToast(
                        context,
                        "Запись не может быть добавлена: Дата распределения - $dateDistributionParsed и Сегмент - $segment",
                        10000
                    )
                }
                return@withContext false
            }
        }

        // 4) HTTP клиент
        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_skladi_data",
            "https://api.gkmmz.ru/api/create_skladi_data"
        )

        val nowSec = Instant.now().epochSecond
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("primarydemand_id", primaryDemandId)
            .addFormDataPart("sklad_id", skladId)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("user_fio", userFio)
            .addFormDataPart("action", "0")
            .addFormDataPart("system_name", version_name)
            .addFormDataPart("date", nowSec.toString())
            .addFormDataPart("prp_name", demand ?: "")
            .apply {
                if (!dateDistribution.isNullOrEmpty() && dateDistribution != "null") {
                    addFormDataPart("date_distribution", dateDistribution)
                }
            }
            .build()

        var success = false

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                Log.d("API Request", "POST $url")

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            success = true
                            withContext(Dispatchers.Main) { showToast(context, "Запись успешно добавлена на ПРОСК", 10000) }
                            return@withContext true
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("API Warning", "429 на $url — пробуем резервный…")
                            // идём на следующую итерацию
                        }
                        resp.code == 400 -> {
                            withContext(Dispatchers.Main) { showToast(context, "Некорректно введены данные для добавления на ПРОСК", 10000) }
                            return@withContext false
                        }
                        resp.code == 409 -> {
                            withContext(Dispatchers.Main) { showToast(context, "Данная ПрП уже есть на складе на ПРОСК", 10000) }
                            return@withContext false
                        }
                        else -> {
                            Log.e("API Error", "Ошибка сервера: ${resp.code} ${resp.message}")
                            if (i == urls.lastIndex) {
                                withContext(Dispatchers.Main) { showToast(context, "Ошибка сервера: ${resp.code}", 10000) }
                                return@withContext false
                            } else {

                            }
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                // сервисный режим — экран уже показан
                return@withContext false
            } catch (e: SocketTimeoutException) {
                Log.e("API Error", "Timeout на $url: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { showToast(context, "Сервер не отвечает. Попробуйте позже.", 10000) }
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("API Error", "Ошибка запроса на $url: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { showToast(context, "Ошибка при добавлении записи", 10000) }
                    return@withContext false
                }
            }
        }

        success
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
    suspend fun findDistributionDateByPrP(
        context: Context,
        barcodeValue: String
    ): SearchDataRaspredeleniyaAndSegment? = withContext(Dispatchers.IO) {

        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_task?title=$barcodeValue",
            "https://api.gkmmz.ru/api/get_task?title=$barcodeValue"
        )

        Log.d("DatabaseManager", "Finding date/segment for ПрП: $barcodeValue")

        var result: SearchDataRaspredeleniyaAndSegment? = null

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                Log.d("API Request", "GET $url")

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            val body = resp.body?.string().orEmpty()
                            Log.d("API Response", "Ответ: $body")

                            val arr = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonArray
                            val firstValid = arr.firstOrNull { el ->
                                val status = el.jsonObject["status"]?.jsonPrimitive?.content
                                status != "68"
                            }?.jsonObject

                            if (firstValid != null) {
                                val dateDistribution = firstValid["ДатаРаспределения"]?.jsonPrimitive?.content
                                val segment = firstValid["Сегмент"]?.jsonPrimitive?.content
                                val demand = firstValid["Спрос"]?.jsonPrimitive?.content
                                val status = firstValid["status"]?.jsonPrimitive?.content

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Найдена операция: Дата $dateDistribution, Сегмент $segment, Статус $status",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                result = SearchDataRaspredeleniyaAndSegment(dateDistribution, segment, demand, status)
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Не найдено операций с подходящим статусом", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("API Warning", "429 на $url — переключаемся на резервный…")
                        }
                        else -> {
                            Log.e("API Request", "Ошибка сервера: ${resp.code}")
                            if (i == urls.lastIndex) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ошибка сервера: ${resp.code}", Toast.LENGTH_LONG).show()
                                }
                            } else {

                            }
                        }
                    }
                }

                if (result != null) break
            } catch (e: ServiceModeException) {
                return@withContext null
            } catch (e: SocketTimeoutException) {
                Log.e("API Request", "Таймаут: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) {
                Log.e("API Request", "Ошибка: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Ошибка при получении данных", Toast.LENGTH_LONG).show() }
                }
            }
        }

        if (result == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Не найдены данные для ПрП = $barcodeValue", Toast.LENGTH_LONG).show()
            }
        }

        result
    }

    suspend fun getWarehouseNameById(
        context: Context,
        skladiDataId: String
    ): Pair<String?, Boolean>? = withContext(Dispatchers.IO) {

        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val urls = listOf(
            "https://api.gkmmz.ru/api/get_all_skladi",
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_skladi"
        )

        Log.d("WarehouseManager", "Поиск склада id=$skladiDataId")

        var pair: Pair<String?, Boolean>? = null

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                Log.d("API Request", "GET $url")

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            val body = resp.body?.string().orEmpty()
                            val json = JSONObject(body)
                            val obj = json.optJSONObject(skladiDataId)
                            if (obj != null) {
                                val name = obj.optString("Наименование", null)
                                val isActive = obj.optInt("is_active", 0) == 1
                                pair = name to isActive
                            } else {

                            }
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("API Warning", "429 на $url — пробуем резервный…")
                        }
                        else -> {
                            Log.e("WarehouseManager", "Ошибка сервера: ${resp.code}")
                        }
                    }
                }

                if (pair != null) {
                    withContext(Dispatchers.Main) {
                        showToast(context, "Найден склад: ${pair!!.first}", 5000)
                    }
                    break
                }
            } catch (e: ServiceModeException) {
                return@withContext null
            } catch (e: SocketTimeoutException) {
                Log.e("WarehouseManager", "Таймаут: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { showToast(context, "Сервер не отвечает. Попробуйте позже.", 5000) }
                }
            } catch (e: Exception) {
                Log.e("WarehouseManager", "Ошибка: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) { showToast(context, "Ошибка получения данных: ${e.message}", 5000) }
                }
            }
        }

        pair
    }

    suspend fun getAllWarehouses(context: Context): List<Warehouse> = withContext(Dispatchers.IO) {
        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()
        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_skladi",
            "https://api.gkmmz.ru/api/get_all_skladi"
        )
        var result: List<Warehouse>? = null
        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()
                Log.d("API Request", "GET $url")
                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            val body = resp.body?.string().orEmpty()
                            val json = JSONObject(body)
                            val warehouses = mutableListOf<Warehouse>()
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val o = json.getJSONObject(key)
                                val id    = o.optString("id", key)
                                val name  = o.optString("Наименование", "")
                                val shelf = o.optString("Полка", "")
                                val rack  = o.optString("Стеллаж", "")
                                val displayName = listOf(name, rack, shelf).filter { it.isNotBlank() }.joinToString(" ")
                                warehouses.add(Warehouse(id, name, displayName))
                            }
                            result = warehouses
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("WarehouseManager", "429 на $url — пробуем резервный…")
                        }
                        else -> {
                            Log.e("WarehouseManager", "Ошибка сервера: ${resp.code} ${resp.message}")
                        }
                    }
                }
                if (result != null) break
            } catch (e: ServiceModeException) {
                break
            } catch (e: SocketTimeoutException) {
                Log.e("WarehouseManager", "Таймаут: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Сервер не отвечает. Попробуйте позже.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("WarehouseManager", "Ошибка: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка получения данных: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        result ?: emptyList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addInventoryRecord(
        context: Context,
        barcodeValue: String,
        userId: Int,
        skladId: String,
        userFio: String,
        textField: TextView
    ): Boolean = withContext(Dispatchers.IO) {

        Log.d("DatabaseManager", "Inventory add: barcode=$barcodeValue, userId=$userId, skladId=$skladId")

        val searchData = findPrimaryDemandIdAndDate(context, barcodeValue)
        val primaryDemandId = searchData?.primarydemand_id
        val dateDistribution = searchData?.dateDistribution

        if (primaryDemandId.isNullOrEmpty()) {
            Log.e("DatabaseManager", "ПрП не найден.")
            return@withContext false
        }
        if (skladId.isBlank()) {
            Log.e("DatabaseManager", "Sklad ID пуст.")
            return@withContext false
        }

        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val nowSec = Instant.now().epochSecond
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("primarydemand_id", primaryDemandId)
            .addFormDataPart("sklad_id", skladId)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("user_fio", userFio)
            .addFormDataPart("date_distribution", dateDistribution?.takeIf { it.isNotEmpty() } ?: "")
            .addFormDataPart("system_name", version_name)
            .addFormDataPart("date", nowSec.toString())
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_skladi_inv_data",
            "https://api.gkmmz.ru/api/create_skladi_inv_data"
        )

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                Log.d("DatabaseManager", "POST $url")

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            Log.d("DatabaseManager", "ПрП успешно проинвентаризирована")
                            withContext(Dispatchers.Main) {
                                showToast(context, "ПрП успешно проинвентаризирована сотрудником $userId", 5000)
                            }
                            return@withContext true
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            Log.w("DatabaseManager", "429 на $url — пробуем резервный…")
                        }
                        resp.code == 400 -> {
                            withContext(Dispatchers.Main) {
                                showToast(context, "Данная ПрП проинвентаризирована", 5000)
                            }
                            return@withContext false
                        }
                        else -> {
                            Log.e("DatabaseManager", "Ошибка добавления: ${resp.code} ${resp.message}")
                            if (i == urls.lastIndex) {
                                withContext(Dispatchers.Main) {
                                    showToast(context, "Ошибка при добавлении записи: ${resp.code}", 5000)
                                }
                                return@withContext false
                            } else {

                            }
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                // сервисный режим
                return@withContext false
            } catch (e: SocketTimeoutException) {
                Log.e("DatabaseManager", "Таймаут: ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        showToast(context, "Попробуйте позже. Сервер не отвечает.", 5000)
                    }
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("DatabaseManager", "Исключение: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        showToast(context, "Ошибка при добавлении записи.", 5000)
                    }
                    return@withContext false
                }
            }
        }

        false
    }

    // Универсальный геттер "дней" из /api/get_project_setting
    private suspend fun fetchProjectSettingDays(context: Context, key: String): Long? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_project_setting",
            "https://api.gkmmz.ru/api/get_project_setting"
        )

        val formBody = FormBody.Builder()
            .add("key", key)
            .build()

        var result: Long? = null

        for ((i, url) in urls.withIndex()) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token", authToken)
                    .build()

                android.util.Log.d("ApiDebug", "POST $url (key=$key)")

                client.newCall(req).execute().use { resp ->
                    when {
                        resp.isSuccessful -> {
                            val body = resp.body?.string().orEmpty()
                            android.util.Log.d("ApiDebug", "Ответ сервера ($key): $body")

                            // Если вдруг пришёл массив — считаем это некорректным форматом
                            if (body.trim().startsWith("[")) {
                                android.util.Log.e("ApiError", "Ожидался объект, получен массив (key=$key)")
                            } else {
                                try {
                                    val json = JSONObject(body)
                                    val valueStr = json.optString("value", "")
                                    result = valueStr.toLongOrNull()
                                    if (result == null && valueStr.isNotEmpty()) {
                                        android.util.Log.e("ApiError", "Не удалось преобразовать 'value' в Long: '$valueStr' (key=$key)")
                                    } else {

                                    }
                                } catch (e: JSONException) {
                                    android.util.Log.e("ApiError", "Ошибка парсинга JSON (key=$key): ${e.message}", e)
                                }
                            }
                        }
                        resp.code == 429 && i < urls.lastIndex -> {
                            android.util.Log.w("ApiWarn", "429 на $url (key=$key) — пробуем резервный…")
                        }
                        else -> {
                            android.util.Log.e("ApiError", "HTTP ${resp.code} на $url (key=$key)")
                            if (i == urls.lastIndex) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ошибка при получении данных: ${resp.code}", Toast.LENGTH_LONG).show()
                                }
                            } else {

                            }
                        }
                    }
                }

                if (result != null) break
            } catch (e: ServiceModeException) {
                // сервисный режим — экран уже показан, просто выходим
                break
            } catch (e: SocketTimeoutException) {
                android.util.Log.e("ApiError", "Таймаут запроса (key=$key): ${e.message}")
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ApiError", "Ошибка запроса (key=$key): ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Не удалось получить данные: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        result
    }

    /** Обёртки под конкретные ключи **/
    suspend fun fetchDaysToAddSeria(context: Context): Long? =
        fetchProjectSettingDays(context, key = "skladi_today_plus_value_Серия")

    suspend fun fetchDaysToAddPosleProdazhnoeObsluzhivanie(context: Context): Long? =
        fetchProjectSettingDays(context, key = "skladi_today_plus_value_Послепродажное обслуживание")

    suspend fun fetchDaysToAddMezhZavod(context: Context): Long? =
        fetchProjectSettingDays(context, key = "skladi_today_plus_value_Межзавод")

    suspend fun fetchDaysToAddOKR(context: Context): Long? =
        fetchProjectSettingDays(context, key = "skladi_today_plus_value_ОКР")


    suspend fun getOperationsForPrp(context: Context, prpValue: String): List<OperationWithDemand> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as App
            val client = app.okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                .hostnameVerifier { _, _ -> true }
                .build()

            val urls = listOf(
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_task?title=${Uri.encode(prpValue)}",
                "https://api.gkmmz.ru/api/get_task?title=${Uri.encode(prpValue)}"
            )

            for (i in urls.indices) {
                val url = urls[i]
                try {
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("X-Apig-AppCode", authTokenAPI)
                        .addHeader("X-Auth-Token", authToken)
                        .build()

                    val resp = client.newCall(req).execute()
                    try {
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val arr = JSONArray(body)
                            val ops = mutableListOf<OperationWithDemand>()
                            val seen = HashSet<String>()
                            for (idx in 0 until arr.length()) {
                                val o = arr.getJSONObject(idx)
                                val operation = o.optString("Операция")
                                if (operation.isEmpty() || !seen.add(operation)) continue
                                ops.add(
                                    OperationWithDemand(
                                        operation = operation,
                                        operation2 = o.optString("Operation"),
                                        demand = o.optString("Спрос"),
                                        uchastok = o.optString("Участок"),
                                        podrazd_mdm_code = o.optString("Подразделение_mdm_code"),
                                        next_podrazd_mdm_code = o.optString("next_Подразделение_mdm_code"),
                                        zahodNomer = o.optString("ЗаходНомер"),
                                        status = o.optString("status"),
                                        nextUchastok = o.optString("next_uchastok", ""),
                                        needProsk = o.optString("НужноОтнестиНаПроск") == "1"
                                    )
                                )
                            }
                            return@withContext ops
                        }

                        if (resp.code == 429 && i < urls.lastIndex) {
                            android.util.Log.w("APIManager", "429 on $url, trying fallback…")
                            continue
                        } else if (i == urls.lastIndex) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ошибка: ${resp.code}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } finally {
                        resp.close()
                    }
                } catch (e: ServiceModeException) {
                    // экран сервисного режима уже показан — выходим
                    break
                } catch (e: SocketTimeoutException) {
                    android.util.Log.e("APIManager", "Timeout on $url: ${e.message}")
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("APIManager", "Request error: ${e.message}", e)
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Не удалось получить данные", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            emptyList()
        }


    //    private var database2: Database? = null
//    suspend fun connect2(context: Context) {
//        if (database2 != null) {
//            Log.w("DatabaseManager", "Already connected to the database")
//            showToast(context, "Уже подключен к базе данных", 5000)
//            return
//        }
//        withContext(Dispatchers.IO) {
//            try {
//                database2 = Database.connect(
//                    url = "jdbc:mysql://192.168.200.250:3306/project?useSSL=false",
//                    user = "root",
//                    password = "bitrix"
//                )
//                Log.d("DatabaseManager", "Database connection successful")
//                showToast(context, "Успешное подключение к базе данных", 5000)
//            } catch (e: Exception) {
//                Log.e("DatabaseManager", "Failed to connect to database: ${e.message}", e)
//                showToast(context, "Не удалось подключиться к базе данных", 5000)
//                database2 = null
//            }
//        }
//    }
//    fun isConnected2(): Boolean {
//        return database2 != null
//    }
    suspend fun getDeliveryLogisticsByDemand(
        prp: String,
        operation: String,
        type: String,
        context: Context
    ): List<LogisticsDeliveryItem> = withContext(Dispatchers.IO) {
        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_delivery_by_object_prp",
            "https://api.gkmmz.ru/api/get_delivery_by_object_prp"
        )

        val formBody = FormBody.Builder()
            .add("object_name", prp)
            .add("operation", operation)
            .add("type", type)
            .build()

        for (i in urls.indices) {
            val url = urls[i]
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token", authToken)
                    .build()

                val resp = client.newCall(req).execute()
                try {
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val listType = object : TypeToken<List<LogisticsDeliveryItem>>() {}.type
                        return@withContext Gson().fromJson<List<LogisticsDeliveryItem>>(body, listType) ?: emptyList()
                    }

                    if (resp.code == 429 && i < urls.lastIndex) {
                        android.util.Log.w("APIManager", "429 on $url, trying fallback…")
                        continue
                    } else if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка: ${resp.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                } finally {
                    resp.close()
                }
            } catch (e: ServiceModeException) {
                break
            } catch (e: Exception) {
                android.util.Log.e("APIManager", "Request error: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Не удалось получить данные", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        emptyList()
    }
    suspend fun getDeliveryLogisticsByDemandDoc(
        doc: String,
        type: String,
        context: Context
    ): List<LogisticsDeliveryItem> = withContext(Dispatchers.IO) {
        val app = context.applicationContext as App
        val client = app.okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_delivery_by_object_doc",
            "https://api.gkmmz.ru/api/get_delivery_by_object_doc"
        )

        val formBody = FormBody.Builder()
            .add("object_id", doc)
            .add("type", type)
            .build()

        for (i in urls.indices) {
            val url = urls[i]
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token", authToken)
                    .build()

                val resp = client.newCall(req).execute()
                try {
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val listType = object : TypeToken<List<LogisticsDeliveryItem>>() {}.type
                        return@withContext Gson().fromJson<List<LogisticsDeliveryItem>>(body, listType) ?: emptyList()
                    }

                    if (resp.code == 429 && i < urls.lastIndex) {
                        android.util.Log.w("APIManager", "429 on $url, trying fallback…")
                        continue
                    } else if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка: ${resp.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                } finally {
                    resp.close()
                }
            } catch (e: ServiceModeException) {
                break
            } catch (e: Exception) {
                android.util.Log.e("APIManager", "Request error: ${e.message}", e)
                if (i == urls.lastIndex) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Не удалось получить данные", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        emptyList()
    }

    suspend fun getAllSotrudnikiInfo(context: Context): List<SotrudnikiInfo> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as App
            val client = app.okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                .hostnameVerifier { _, _ -> true }
                .build()

            val urls = listOf(
                "https://api.gkmmz.ru/api/get_all_sotrudniki",
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_sotrudniki"
            )

            for (i in urls.indices) {
                val url = urls[i]
                try {
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("X-Apig-AppCode", authTokenAPI)
                        .addHeader("X-Auth-Token", authToken)
                        .build()

                    val resp = client.newCall(req).execute()
                    try {
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val json = JSONObject(body)
                            val list = mutableListOf<SotrudnikiInfo>()
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val o = json.getJSONObject(key)
                                val mdm = o.optString("mdmcode")
                                val fio = o.optString("Рабочий")
                                val phone = o.optString("МобильныйТелефон", "")
                                list.add(SotrudnikiInfo(mdm, fio, phone))
                            }
                            return@withContext list
                        }

                        if (resp.code == 429 && i < urls.lastIndex) {
                            Log.w("APIManager", "429 on $url, trying fallback…")
                            continue
                        } else if (i == urls.lastIndex) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ошибка: ${resp.code}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } finally {
                        resp.close()
                    }
                } catch (e: ServiceModeException) {
                    // сервисный режим — экран уже показан, возвращаем пусто
                    break
                } catch (e: SocketTimeoutException) {
                    Log.e("APIManager", "Timeout on $url: ${e.message}")
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("APIManager", "Request error: ${e.message}", e)
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Не удалось получить данные", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            emptyList()
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
    suspend fun getAllWarehousesInfo(context: Context): List<WarehouseInfo> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as App
            val client = app.okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                .hostnameVerifier { _, _ -> true }
                .build()

            val urls = listOf(
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_skladi",
                "https://api.gkmmz.ru/api/get_all_skladi"
            )

            for (i in urls.indices) {
                val url = urls[i]
                try {
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("X-Apig-AppCode", authTokenAPI)
                        .addHeader("X-Auth-Token", authToken)
                        .build()

                    val resp = client.newCall(req).execute()
                    try {
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val json = JSONObject(body)
                            val list = mutableListOf<WarehouseInfo>()
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val o = json.getJSONObject(key)
                                val id = o.optString("id")
                                val name = o.optString("Наименование")
                                val shelf = o.optString("Полка", null)
                                val rack = o.optString("Стеллаж", null)
                                val address = o.optString("Адрес", null)
                                val coordinates = o.optString("Координаты", null)
                                val purpose = o.optString("Назначение", null)
                                val description = o.optString("Описание", null)
                                val responsibleMDMCode = o.optString("ОтветственныйMDMкод", null)
                                val responsibleName = o.optString("ОтветственныйФИО", null)
                                val responsiblePosition = o.optString("ОтветственныйДолжность", null)
                                val plannerMDMCode = o.optString("ПлановикMDMкод", null)
                                val plannerName = o.optString("ПлановикФИО", null)
                                val plannerPosition = o.optString("ПлановикДолжность", null)
                                val subdivisionMDMCode = o.optString("ПодразделениеMDMкод", null)
                                val subdivision = o.optString("Подразделение", null)
                                val mdmKey = o.optString("mdm_key", null)
                                val isActive = o.optString("is_active", null)
                                val displayName = "$name $rack $shelf"
                                list.add(
                                    WarehouseInfo(
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
                                    )
                                )
                            }
                            return@withContext list
                        }

                        if (resp.code == 429 && i < urls.lastIndex) {
                            Log.w("APIManager", "429 on $url, trying fallback…")
                            continue
                        } else if (i == urls.lastIndex) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ошибка: ${resp.code}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } finally {
                        resp.close()
                    }
                } catch (e: ServiceModeException) {
                    break
                } catch (e: SocketTimeoutException) {
                    Log.e("APIManager", "Timeout on $url: ${e.message}")
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("APIManager", "Request error: ${e.message}", e)
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Не удалось получить данные", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            emptyList()
        }

    suspend fun getFilteredWarehousesInfo(context: Context): List<WarehouseInfo> {
        val warehouses = getAllWarehousesInfo(context)
        val uniqueWarehouses = mutableListOf<WarehouseInfo>()
        var isProskAdded = false
        for (warehouse in warehouses) {
            if (warehouse.isActive == "1") { // Проверка активности склада
                if (warehouse.name == "ПРОСК полуфабрикатов" && warehouse.id == "812") {
                    if (!isProskAdded) {
                        uniqueWarehouses.add(warehouse)
                        isProskAdded = true
                    }
                } else if (warehouse.name != "ПРОСК полуфабрикатов") {
                    uniqueWarehouses.add(warehouse)
                }
            }
        }
        uniqueWarehouses.forEach { warehouse ->
            //Log.d("WarehouseInfo", "Информация о складе: ID=${warehouse.id}, Наименование=${warehouse.name}")
        }
        return uniqueWarehouses
    }
    suspend fun fetchInfoPrp(context: Context, prp: String): String? =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as App
            val client = app.okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
                .hostnameVerifier { _, _ -> true }
                .build()

            val urls = listOf(
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_skladi_inv_data",
                "https://api.gkmmz.ru/api/get_skladi_inv_data"
            )

            val form = FormBody.Builder()
                .add("prp", prp)
                .build()

            for (i in urls.indices) {
                val url = urls[i]
                try {
                    val req = Request.Builder()
                        .url(url)
                        .post(form)
                        .addHeader("X-Apig-AppCode", authTokenAPI)
                        .addHeader("X-Auth-Token", authToken)
                        .build()

                    val resp = client.newCall(req).execute()
                    try {
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val arr = JSONArray(body)
                            if (arr.length() > 0) {
                                val o = arr.getJSONObject(0)
                                val userFio = o.optString("user_fio", "Неизвестно")
                                val naimenovanie = o.optString("Наименование", "Неизвестно")
                                val formattedDate = o.optString("formatted_date", "Неизвестно")
                                val result = "Сотрудник, проводивший инвентаризацию:\n$userFio\n" +
                                        "Место инвентаризации:\n$naimenovanie\n" +
                                        "Дата инвентаризации:\n$formattedDate"

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Найдены данные: $result для ПрП = $prp",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                return@withContext result
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Не найдены данные для ПрП = $prp",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                return@withContext null
                            }
                        }

                        if (resp.code == 429 && i < urls.lastIndex) {
                            Log.w("ApiManager", "429 on $url, trying fallback…")
                            continue
                        } else if (i == urls.lastIndex) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Ошибка при получении данных: ${resp.code}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } finally {
                        resp.close()
                    }
                } catch (e: ServiceModeException) {
                    // сервисный режим — выходим
                    break
                } catch (e: SocketTimeoutException) {
                    Log.e("ApiManager", "Timeout on $url: ${e.message}")
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ApiManager", "Request error: ${e.message}", e)
                    if (i == urls.lastIndex) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Не удалось получить данные", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            null
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
    val status: String,
    val nextUchastok: String,
    val needProsk: Boolean = false
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
    val fio: String,
    val phone: String = "" // Добавляем поле для телефона
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
    val demand: String?,
    val status: String?
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
object Auth {
    const val authToken: String = "MIIEmQYJKoZIhvcNAQcCoIIEijCCBIYCAQExDTALBglghkgBZQMEAgEwggLGBgkqhkiG9w0BBwGgggK3BIICs3sidG9rZW4iOnsiZXhwaXJlc19hdCI6IjIwMjUtMDMtMTFUMTE6NDc6MzIuMDQxMDAwWiIsIm1ldGhvZHMiOlsicGFzc3dvcmQiXSwiY2F0YWxvZyI6W10sInJvbGVzIjpbeyJuYW1lIjoiYXBpZ19hZG0iLCJpZCI6IjAifSx7Im5hbWUiOiI4NiwxMjAiLCJpZCI6IjgifSx7Im5hbWUiOiJvcF9maW5lX2dyYWluZWQiLCJpZCI6IjcifV0sInByb2plY3QiOnsiZG9tYWluIjp7Inhkb21haW5fdHlwZSI6IlNCQyIsIm5hbWUiOiJva2Ita3Jpc3RhbGwiLCJpZCI6ImE5NDZmN2VlMDUwNjQ3NmM4MmY5NjU0NzNhZWI5ZmQzIiwieGRvbWFpbl9pZCI6ImJiZmIxODcwLTE5ZGItNDk2My04ZjgzLTdkYjlkMGUwMWY0NSJ9LCJuYW1lIjoicnUtbW9zY293LTEiLCJpZCI6IjdiMGJlYWY5NDEzMDRjN2JiMGVjNWIwNGE0ZDBmZjFhIn0sImlzc3VlZF9hdCI6IjIwMjUtMDMtMTBUMTE6NDc6MzIuMDQxMDAwWiIsInVzZXIiOnsiZG9tYWluIjp7Inhkb21haW5fdHlwZSI6IlNCQyIsIm5hbWUiOiJva2Ita3Jpc3RhbGwiLCJpZCI6ImE5NDZmN2VlMDUwNjQ3NmM4MmY5NjU0NzNhZWI5ZmQzIiwieGRvbWFpbl9pZCI6ImJiZmIxODcwLTE5ZGItNDk2My04ZjgzLTdkYjlkMGUwMWY0NSJ9LCJuYW1lIjoibi5nYWxraW4iLCJwYXNzd29yZF9leHBpcmVzX2F0IjoiIiwiaWQiOiI4MTQ1ZmRiZWExMmE0NDQ0YjExYThiMzljNTExNGZlMiJ9fX0xggGmMIIBogIBATB9MHAxCzAJBgNVBAYTAlJVMQ8wDQYDVQQIDAZNb3Njb3cxDzANBgNVBAcMBk1vc2NvdzEbMBkGA1UECgwSU2JlckNsb3VkIENvLiwgTHRkMRIwEAYDVQQLDAlTYmVyQ2xvdWQxDjAMBgNVBAMMBXRva2VuAgkAuceJxDu+SXkwCwYJYIZIAWUDBAIBMA0GCSqGSIb3DQEBAQUABIIBALSxTwaBTIdTJAC63-Kq3Ipk-VcljqbSP0BeoRG65zVH2lX1G3IWrkByg4VhKO12nFFEcQTv5Lu2ojQmloal1vkEAuMSbXcEupzMg0EwZd5rxAuuR56LWOYBmAN7TO6-FqRAmnkQbMx6hOBt1aVnb12uYmNcXRVgtBpqziPbekEG8zmKiW-l7+DB-o368VR8Ltb-Ojz4grSG5mHKSQLLY9OLwHz3Vn88oYC4PYoDYWCgz2yY2O32Y7lv3ut51fQ93u-AZXSeUVUa65PdflOL1Y84IDvMrcZ21cqHclQZbjWQ7HuYSBGYClIVux+eEQdf5tRztqL7AWTp-OnhJpiEm-A="
    const val authTokenAPI: String = "67f7f5f5e2484c0d94bdc6121d13b0fb1c500ea7965b4b2280dc162966fc2e19"
}
data class DevicesUsers(
    val mdm_code: String,
    val device_token: String,
    val is_active: Int,
    val created_at: String
)
// data класс для хранения данных о пользователе во внутреннем хранилище мобильного приложения
data class UserData(
    val username: String,
    val userId: Int,
    val roleCheck: String,
    val mdmCode: String,
    val fio: String,
    val deviceInfo: String,
    val rolesString: String,
    val device_token: String,
    val isAuthorized: Boolean
)

data class StoredNotification(
    val id: String,
    val title: String,
    val message: String,
    val logisticsId: String?,
    val receivedDate: Date = Date(),
    var isRead: Boolean = false
)

data class ApiResponse(
    val draw: Int,          // Номер запроса
    val recordsTotal: Int,  // Всего записей
    val recordsFiltered: Int, // Количество записей после фильтрации
    val data: List<LogisticsItem>
)

data class ApiDetailResponse(
    val success: Boolean?,
    val message: String?,
    val data: LogisticsItem?
)

data class ApiResponseSegment(
    val id: String,
    val param: String,
    val value: String,
    val updated_at: String
)