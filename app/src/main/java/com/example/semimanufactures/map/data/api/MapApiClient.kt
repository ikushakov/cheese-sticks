package com.example.semimanufactures.map.data.api

import com.example.semimanufactures.Auth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * API клиент для работы с картой на основе OkHttp
 */
object MapApiClient {
    
    private const val BASE_URL = "https://api.gkmmz.ru/v1/"
    
    /**
     * Moshi для парсинга JSON
     */
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * OkHttpClient с необходимыми headers и отключенной проверкой SSL (по запросу)
     */
    private val okHttpClient: OkHttpClient = getUnsafeOkHttpClient()

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            // Install the all-trusting trust manager
            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .addHeader("X-Apig-AppCode", Auth.authTokenAPI)
                        .addHeader("X-Auth-Token", Auth.authToken)
                    
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    
    /**
     * Выполнить GET запрос (возвращает JSON строку)
     */
    suspend fun get(
        endpoint: String,
        responseClass: Class<*>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (endpoint.startsWith("http")) endpoint else BASE_URL + endpoint
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Выполнить POST запрос (возвращает JSON строку)
     */
    suspend fun post(
        endpoint: String,
        body: Any?,
        responseClass: Class<*>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (endpoint.startsWith("http")) endpoint else BASE_URL + endpoint
            val requestBuilder = Request.Builder()
                .url(url)
            
            if (body != null) {
                val adapter = moshi.adapter(body.javaClass)
                val jsonBody = adapter.toJson(body)
                val requestBody = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    jsonBody
                )
                requestBuilder.post(requestBody)
            } else {
                requestBuilder.post(okhttp3.RequestBody.create(null, ""))
            }
            
            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null && responseBody.isNotEmpty()) {
                    Result.success(responseBody)
                } else {
                    Result.success("Success")
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Выполнить PUT запрос (возвращает JSON строку)
     */
    suspend fun put(
        endpoint: String,
        body: Any?,
        responseClass: Class<*>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (endpoint.startsWith("http")) endpoint else BASE_URL + endpoint
            val requestBuilder = Request.Builder()
                .url(url)
            
            if (body != null) {
                val adapter = moshi.adapter(body.javaClass)
                val jsonBody = adapter.toJson(body)
                val requestBody = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    jsonBody
                )
                requestBuilder.put(requestBody)
            } else {
                requestBuilder.put(okhttp3.RequestBody.create(null, ""))
            }
            
            val request = requestBuilder.build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null && responseBody.isNotEmpty()) {
                    Result.success(responseBody)
                } else {
                    Result.success("Success")
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Выполнить DELETE запрос (возвращает JSON строку)
     */
    suspend fun delete(
        endpoint: String,
        responseClass: Class<*>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (endpoint.startsWith("http")) endpoint else BASE_URL + endpoint
            val request = Request.Builder()
                .url(url)
                .delete()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null && responseBody.isNotEmpty()) {
                    Result.success(responseBody)
                } else {
                    Result.success("Success")
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

