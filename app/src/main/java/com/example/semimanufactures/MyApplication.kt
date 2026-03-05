package com.example.semimanufactures

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import com.example.semimanufactures.lazy_loading_photo_delete_cache.SmartImageLoader
import com.example.semimanufactures.service_mode.ServiceModeInterceptor
import com.example.semimanufactures.service_mode.ServiceModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import ru.rustore.sdk.pushclient.RuStorePushClient
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class App : Application() {


    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var serviceModeManager: ServiceModeManager
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "Application onCreate()")
        
        // Инициализация Maptiler SDK - временно закомментировано
        // Раскомментируйте после настройки правильного репозитория Maptiler
        // try {
        //     com.maptiler.sdk.MTConfig.apiKey = "hLhaPLSIrELIBZAk08wF"
        //     Log.d(LOG_TAG, "Maptiler SDK initialized")
        // } catch (e: Exception) {
        //     Log.e(LOG_TAG, "Error initializing Maptiler SDK", e)
        // }
        
        initPushes()
        ContextProvider.initialize(this)
        serviceModeManager = ServiceModeManager(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onActivityResumed(activity: Activity) {
                serviceModeManager.setCurrentActivity(activity)
                serviceModeManager.ensureShownIfNeeded()
            }
            override fun onActivityPaused(activity: Activity) {
                serviceModeManager.setCurrentActivity(null)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(ServiceModeInterceptor(serviceModeManager))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager()) // если нужно
            .hostnameVerifier { _, _ -> true }
            .build()

        setupCacheCleanup()
    }

    private fun setupCacheCleanup() {
        // Очистка старых файлов при запуске
        CoroutineScope(Dispatchers.IO).launch {
            SmartImageLoader.getInstance(this@App).cleanupOldFiles(14) // Оставляем файлы моложе 7 дней
        }

        // Регулярная очистка каждые 24 часа
        setupPeriodicCacheCleanup()
    }

    private fun setupPeriodicCacheCleanup() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, CacheCleanupReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Запускаем каждые 24 часа
        val interval = 24 * 60 * 60 * 1000L // 24 часа
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + interval,
            interval,
            pendingIntent
        )
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
    private fun initPushes() {
        Log.d(LOG_TAG, "initPushes()")
        createNotificationPushChannel()
        RuStorePushClient.init(
            application = this,
            projectId = "tj1814XJcpfo399RIfMCrEADi0WZ3dR5",
            logger = PushLogger(tag = "PushExampleLogger")
        )
        Log.d(LOG_TAG, "RuStorePushClient.init() called")
        requestPushToken()
    }
    private fun requestPushToken() {
        RuStorePushClient.getToken()
            .addOnSuccessListener { result ->
                Log.d(LOG_TAG, "getToken onSuccess token = $result")
                saveToken(result)
            }
            .addOnFailureListener { throwable ->
                Log.e(LOG_TAG, "getToken onFailure", throwable)
                savePushError(throwable)
            }
    }
    private fun saveToken(token: String) {
        getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
            .putString("device_token", token)
            .apply()
    }
    private fun savePushError(throwable: Throwable) {
        getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
            .putString("last_push_error", throwable.toString())
            .apply()
    }
    fun checkBackgroundPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            if (!isIgnoringBatteryOptimizations && shouldShowBackgroundPermissionDialog()) {
                showBackgroundPermissionDialog(context)
            }
        }
    }
    fun shouldShowBackgroundPermissionDialog(): Boolean {
        val prefs = getSharedPreferences("myPrefs", MODE_PRIVATE)
        return !prefs.getBoolean("bg_permission_shown", false)
    }
    fun showBackgroundPermissionDialog(context: Context) {
        val dialog = AlertDialog.Builder(context).apply {
            setTitle("Разрешение фоновой работы")
            setMessage("Для работы push-уведомлений необходимо разрешить работу в фоне.")
            setPositiveButton("Разрешить") { _, _ ->
                openBatteryOptimizationSettings(context)
            }
            setNegativeButton("Позже", null)
            setCancelable(false)
        }.create()
        dialog.show()
        getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
            .putBoolean("bg_permission_shown", true)
            .apply()
    }
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:$packageName")
                    }
                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.parse("package:$packageName")
                    }
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error opening battery settings", e)
        }
    }
    private fun createNotificationPushChannel() {
        NotificationManagerWrapper.getInstance(this).createNotificationChannel(
            channelId = getString(R.string.notifications_data_push_channel_id),
            channelName = getString(R.string.notifications_data_push_channel_name),
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            description = "Входящие оповещения"
        )
    }
    companion object {
        private const val LOG_TAG = "App"
    }
}

class CacheCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            SmartImageLoader.getInstance(context).cleanupOldFiles(14)
        }
    }
}

object ContextProvider {
    lateinit var applicationContext: Context
        private set

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}