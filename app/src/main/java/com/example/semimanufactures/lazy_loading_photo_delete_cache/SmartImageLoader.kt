package com.example.semimanufactures.lazy_loading_photo_delete_cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SmartImageLoader(private val context: Context) {
    private val memoryCache = LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt())
    private val diskCache = DiskCache(context)
    private val loadingStates = mutableMapOf<String, Boolean>()
    private val loadingCallbacks = mutableMapOf<String, MutableList<(Bitmap?) -> Unit>>()

    // Клиент с настройками как в оригинальном коде
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(ApiHeadersInterceptor(authTokenAPI, authToken))
            .addInterceptor(Fallback429UrlSwapInterceptor()) // Универсальный интерцептор
            .build()
    }

    // Дополнительный клиент для загрузки с кастомными заголовками
    private val customHeadersClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(Fallback429UrlSwapInterceptor()) // Универсальный интерцептор
            .build()
    }

    private val loadCompletionCallbacks = mutableMapOf<String, (Boolean) -> Unit>()

    fun setLoadCompletionListener(url: String, callback: (Boolean) -> Unit) {
        loadCompletionCallbacks[url] = callback
    }

    companion object {
        @Volatile
        private var INSTANCE: SmartImageLoader? = null

        fun getInstance(context: Context): SmartImageLoader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartImageLoader(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Метод для загрузки изображения с кастомными заголовками авторизации
    fun loadImageWithCustomAuth(
        url: String,
        imageView: ImageView,
        authTokenAPI: String,
        authToken: String,
        placeholderRes: Int = R.drawable.photo_placeholder,
        errorRes: Int = R.drawable.error,
        size: Int = 300
    ) {
        imageView.setImageResource(placeholderRes)
        imageView.tag = url

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = getBitmapWithCustomAuth(url, size, authTokenAPI, authToken)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(errorRes)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmartImageLoader", "Error loading image with custom auth: ${e.message}")
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        imageView.setImageResource(errorRes)
                    }
                }
            }
        }
    }

    // Метод для загрузки с прогрессом и кастомными заголовками
    fun loadImageWithProgressAndCustomAuth(
        url: String,
        imageView: ImageView,
        progressBar: ProgressBar,
        authTokenAPI: String,
        authToken: String,
        placeholderRes: Int = R.drawable.photo_placeholder,
        errorRes: Int = R.drawable.error,
        size: Int = 300,
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        imageView.setImageResource(placeholderRes)
        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.INVISIBLE
        imageView.tag = url

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = getBitmapWithCustomAuth(url, size, authTokenAPI, authToken)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        progressBar.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                            onSuccess?.invoke()
                        } else {
                            imageView.setImageResource(errorRes)
                            onError?.invoke()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmartImageLoader", "Error loading image with custom auth: ${e.message}")
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        progressBar.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        imageView.setImageResource(errorRes)
                        onError?.invoke()
                    }
                }
            }
        }
    }

    // Приватный метод для получения битмапа с кастомными заголовками
    private suspend fun getBitmapWithCustomAuth(
        url: String,
        size: Int,
        authTokenAPI: String,
        authToken: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Проверяем в памяти
        memoryCache[url]?.let { return@withContext it }

        // 2. Проверяем на диске
        diskCache.get(url)?.let { bytes ->
            val bitmap = decodeSampledBitmapFromBytes(bytes, size, size)
            bitmap?.let { memoryCache.put(url, it) }
            return@withContext bitmap
        }

        // 3. Загружаем из сети с кастомными заголовками
        return@withContext downloadAndCacheBitmapWithCustomAuth(url, size, authTokenAPI, authToken)
    }

    private suspend fun downloadAndCacheBitmapWithCustomAuth(
        url: String,
        size: Int,
        authTokenAPI: String,
        authToken: String
    ): Bitmap? {
        if (loadingStates[url] == true) {
            return waitForLoad(url)
        }

        loadingStates[url] = true

        return try {
            Log.d("SmartImageLoader", "Downloading with custom auth: $url")
            Log.d("SmartImageLoader", "Auth headers: X-Apig-AppCode=$authTokenAPI, X-Auth-Token=$authToken")

            val request = Request.Builder()
                .url(url)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)
                .build()

            val response = customHeadersClient.newCall(request).execute()

            Log.d("SmartImageLoader", "Response code: ${response.code}")
            Log.d("SmartImageLoader", "Response headers: ${response.headers}")

            if (response.isSuccessful) {
                response.body?.bytes()?.let { bytes ->
                    Log.d("SmartImageLoader", "Successfully downloaded ${bytes.size} bytes")
                    val bitmap = decodeSampledBitmapFromBytes(bytes, size, size)

                    bitmap?.let {
                        memoryCache.put(url, it)
                        diskCache.put(url, bytes)
                        Log.d("SmartImageLoader", "Bitmap decoded: ${it.width}x${it.height}")
                    }

                    notifyCallbacks(url, bitmap)
                    loadCompletionCallbacks[url]?.invoke(true)
                    bitmap
                } ?: run {
                    Log.w("SmartImageLoader", "Response body is null")
                    notifyCallbacks(url, null)
                    loadCompletionCallbacks[url]?.invoke(false)
                    null
                }
            } else {
                val errorBody = response.body?.string()
                Log.w("SmartImageLoader", "Failed to load image: HTTP ${response.code}")
                Log.w("SmartImageLoader", "Error response: $errorBody")
                notifyCallbacks(url, null)
                loadCompletionCallbacks[url]?.invoke(false)
                null
            }
        } catch (e: Exception) {
            Log.e("SmartImageLoader", "Error loading image with custom auth: ${e.message}", e)
            notifyCallbacks(url, null)
            loadCompletionCallbacks[url]?.invoke(false)
            null
        } finally {
            loadingStates.remove(url)
            loadingCallbacks.remove(url)
            loadCompletionCallbacks.remove(url)
        }
    }

    // Оригинальные методы (остаются без изменений)
    fun loadImage(
        url: String,
        imageView: ImageView,
        placeholderRes: Int = R.drawable.photo_placeholder,
        errorRes: Int = R.drawable.error,
        size: Int = 300
    ) {
        imageView.setImageResource(placeholderRes)
        imageView.tag = url

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = getBitmap(url, size)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(errorRes)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        imageView.setImageResource(errorRes)
                    }
                }
            }
        }
    }

    fun loadImageWithProgress(
        url: String,
        imageView: ImageView,
        progressBar: ProgressBar,
        placeholderRes: Int = R.drawable.photo_placeholder,
        errorRes: Int = R.drawable.error
    ) {
        imageView.setImageResource(placeholderRes)
        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.INVISIBLE
        imageView.tag = url

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = getBitmap(url, 300)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        progressBar.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(errorRes)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        progressBar.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        imageView.setImageResource(errorRes)
                    }
                }
            }
        }
    }

    private suspend fun getBitmap(url: String, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Проверяем в памяти
        memoryCache[url]?.let { return@withContext it }

        // 2. Проверяем на диске
        diskCache.get(url)?.let { bytes ->
            val bitmap = decodeSampledBitmapFromBytes(bytes, size, size)
            bitmap?.let { memoryCache.put(url, it) }
            return@withContext bitmap
        }

        // 3. Загружаем из сети с нашим клиентом
        return@withContext downloadAndCacheBitmap(url, size)
    }

    private suspend fun downloadAndCacheBitmap(url: String, size: Int): Bitmap? {
        if (loadingStates[url] == true) {
            return waitForLoad(url)
        }

        loadingStates[url] = true

        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.bytes()?.let { bytes ->
                    val bitmap = decodeSampledBitmapFromBytes(bytes, size, size)

                    bitmap?.let {
                        memoryCache.put(url, it)
                        diskCache.put(url, bytes)
                    }

                    notifyCallbacks(url, bitmap)
                    // Уведомляем о успешной загрузке
                    loadCompletionCallbacks[url]?.invoke(true)
                    bitmap
                }
            } else {
                notifyCallbacks(url, null)
                // Уведомляем о неудачной загрузке
                loadCompletionCallbacks[url]?.invoke(false)
                null
            }
        } catch (e: Exception) {
            Log.e("SmartImageLoader", "Error loading image: ${e.message}")
            notifyCallbacks(url, null)
            // Уведомляем о неудачной загрузке
            loadCompletionCallbacks[url]?.invoke(false)
            null
        } finally {
            loadingStates.remove(url)
            loadingCallbacks.remove(url)
            loadCompletionCallbacks.remove(url)
        }
    }

    // SSL методы как в оригинальном коде
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

    // Остальные вспомогательные методы без изменений...
    private suspend fun waitForLoad(url: String): Bitmap? = suspendCancellableCoroutine { continuation ->
        val callbacks = loadingCallbacks.getOrPut(url) { mutableListOf() }
        callbacks.add { bitmap ->
            continuation.resume(bitmap, null)
        }
    }

    private fun notifyCallbacks(url: String, bitmap: Bitmap?) {
        loadingCallbacks[url]?.forEach { callback ->
            try {
                callback(bitmap)
            } catch (e: Exception) {
                Log.e("SmartImageLoader", "Error in callback", e)
            }
        }
    }

    private fun decodeSampledBitmapFromBytes(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.e("SmartImageLoader", "Error decoding bitmap", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (width, height) = options.run { outWidth to outHeight }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun isImageCached(url: String): Boolean {
        return memoryCache[url] != null || diskCache.get(url) != null
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    fun clearDiskCache() {
        diskCache.clear()
    }

    fun getCacheSize(): Long {
        return diskCache.getSize()
    }

    fun cleanupOldFiles(maxAgeDays: Int = 14) {
        diskCache.cleanupOldFiles(maxAgeDays)
    }

    fun getCachedImageBytes(url: String): ByteArray? {
        // 1. Проверяем на диске
        return diskCache.get(url)
    }
}

/**
 * Универсальный интерцептор для обработки 429 ошибок
 * Автоматически переключается на fallback URL при получении 429
 */
private class Fallback429UrlSwapInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        // Если получили 429 - пробуем fallback URL
        if (response.code == 429) {
            response.close()

            val originalUrl = originalRequest.url.toString()
            val fallbackUrl = getFallbackUrl(originalUrl)

            if (fallbackUrl != null) {
                Log.w("Fallback429", "429 on $originalUrl → trying $fallbackUrl")
                val fallbackRequest = originalRequest.newBuilder()
                    .url(fallbackUrl)
                    .build()
                response = chain.proceed(fallbackRequest)
            }
        }

        return response
    }

    private fun getFallbackUrl(originalUrl: String): String? {
        return when {
            originalUrl.contains("api.gkmmz.ru") -> {
                originalUrl.replace(
                    "https://api.gkmmz.ru",
                    "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru"
                )
            }
            originalUrl.contains("09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru") -> {
                originalUrl.replace(
                    "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru",
                    "https://api.gkmmz.ru"
                )
            }
            else -> null
        }
    }

}

private class ApiHeadersInterceptor(
    private val appCode: String,
    private val authToken: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req: Request = chain.request().newBuilder()
            .addHeader("X-Apig-AppCode", appCode)
            .addHeader("X-Auth-Token", authToken)
            .build()
        return chain.proceed(req)
    }
}