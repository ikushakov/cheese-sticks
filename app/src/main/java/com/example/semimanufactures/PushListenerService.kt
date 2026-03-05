package com.example.semimanufactures

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import ru.rustore.sdk.pushclient.messaging.exception.RuStorePushClientException
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import java.util.UUID

class PushListenerService : RuStoreMessagingService() {
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false
    private val rolesList: MutableList<String> = mutableListOf()
    private val notificationManagerWrapper =
        NotificationManagerWrapper.getInstance(this)
    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "onNewToken token = $token")
        getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
            .putString("device_token", token)
            .apply()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val userData = readUserData()
        userData?.let {
            currentUsername = it.username
            currentUserId = it.userId
            currentRoleCheck = it.roleCheck
            currentMdmCode = it.mdmCode
            currentFio = it.fio
            currentDeviceInfo = it.deviceInfo
            currentRolesString = it.rolesString
            currentDeviceToken = it.device_token
            currentIsAuthorized = it.isAuthorized
        } ?: run {
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }

        if (!currentIsAuthorized) {
            startActivity(Intent(this, MainActivity::class.java))
            return
        }
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.clear()
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }

        val type = message.data["type"]?.trim().orEmpty()

        when {
            type.equals("web-auth", ignoreCase = true) -> handleWebAuthNotification(message)
            !message.data["logistics_id"].isNullOrBlank() -> handleLogisticNotification(message)
            !message.data["mdm_auth"].isNullOrBlank()     -> handleStandardNotification(message)
            else -> handleDefaultNotification(message) // всегда показываем
        }
    }
    private fun titleOf(msg: RemoteMessage) =
        msg.notification?.title ?: msg.data["title"] ?: "Уведомление"

    private fun bodyOf(msg: RemoteMessage, fallback: String? = null) =
        msg.notification?.body ?: msg.data["body"] ?: msg.data["message"] ?: fallback ?: "Нажмите, чтобы открыть"

    // PendingIntent (общий)
    private fun createPI(intent: Intent, requestCode: Int): PendingIntent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun handleLogisticNotification(message: RemoteMessage) {
        val logisticsId = message.data["logistics_id"] ?: run {
            startLogisticActivity()
            return
        }

        // сохраняем в логистическое хранилище
        NotificationRepository(this).saveNotification(
            StoredNotification(
                id = java.util.UUID.randomUUID().toString(),
                title = titleOf(message),
                message = bodyOf(message, "Доставка #$logisticsId"),
                logisticsId = logisticsId
            )
        )
        NotificationStorageService.startService(this)

        // показываем heads-up
        val (channelId, channelName) = getChannelInfo()
        val intent = Intent(this, DetailLogisticsActivity::class.java).apply {
            putExtra("logistics_id", logisticsId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = createPI(intent, logisticsId.hashCode())

        notificationManagerWrapper.showNotification(
            context = this,
            data = AppNotification(
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = titleOf(message),
                message = bodyOf(message, "Доставка #$logisticsId"),
                channelId = channelId,
                channelName = channelName,
                pendingIntent = pending
            )
        )
    }

    private fun handleWebAuthNotification(message: RemoteMessage) {
        val mdmAuth = message.data["mdm_auth"] ?: currentMdmCode ?: run {
            android.util.Log.e(LOG_TAG, "web-auth: mdm_auth отсутствует")
            handleDefaultNotification(message)
            return
        }

        val (channelId, channelName) = getChannelInfo()
        // Открываем MainActivity с флагами и экстрами для обработки авторизации на стороне UI
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_push", true)
            putExtra("action", "web_auth")
            putExtra("mdm_auth", mdmAuth)
            putExtra("payload", java.util.HashMap(message.data))
        }
        val pending = createPI(intent, mdmAuth.hashCode())

        notificationManagerWrapper.showNotification(
            context = this,
            data = AppNotification(
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = titleOf(message),                       // например: "Подтверждение входа"
                message = bodyOf(message, "Код подтверждения"), // fallback
                channelId = channelId,
                channelName = channelName,
                pendingIntent = pending
            )
        )

        // при необходимости сохраняйте в своё хранилище (если есть сущность WebAuthNotification)
        // NotificationRepository(this).saveNotification(...)
        // NotificationStorageService.startService(this)
    }

    private fun handleStandardNotification(message: RemoteMessage) {
        val mdm = message.data["mdm_auth"] ?: currentMdmCode

        // при необходимости — сохранить:
        // NotificationRepository(this).saveNotification(...)
        // NotificationStorageService.startService(this)

        val (channelId, channelName) = getChannelInfo()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_push", true)
            putExtra("action", "open_notification")
            if (!mdm.isNullOrBlank()) putExtra("mdm_auth", mdm)
            putExtra("payload", java.util.HashMap(message.data))
        }
        val pending = createPI(intent, (titleOf(message) + bodyOf(message)).hashCode())

        notificationManagerWrapper.showNotification(
            context = this,
            data = AppNotification(
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = titleOf(message),
                message = bodyOf(message),
                channelId = channelId,
                channelName = channelName,
                pendingIntent = pending
            )
        )
    }

    private fun handleDefaultNotification(message: RemoteMessage) {
        val (channelId, channelName) = getChannelInfo()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_push", true)
            putExtra("action", "open_notification_unknown")
            putExtra("payload", java.util.HashMap(message.data))
        }
        val pending = createPI(intent, (titleOf(message) + bodyOf(message)).hashCode())

        notificationManagerWrapper.showNotification(
            context = this,
            data = AppNotification(
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = titleOf(message),
                message = bodyOf(message),
                channelId = channelId,
                channelName = channelName,
                pendingIntent = pending
            )
        )
    }
    private fun startLogisticActivity() {
        val intent = Intent(this, LogisticActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
    private fun readUserData(): UserData? {
        return try {
            openFileInput("user_data").use {
                val json = it.bufferedReader().use { reader -> reader.readText() }
                Gson().fromJson(json, UserData::class.java)
            }
        } catch (e: Exception) {
            Log.e("FeaturesActivity", "Error reading user data", e)
            null
        }
    }
    override fun onDeletedMessages() {
        Log.d(LOG_TAG, "onDeletedMessages")
    }
    override fun onError(errors: List<RuStorePushClientException>) {
        errors.forEach { error ->
            Log.e(LOG_TAG, "Error: ${error.message}", error)
            error.printStackTrace()
        }
    }
    private fun getChannelInfo(): Pair<String, String> {
        val channelId = getString(R.string.notifications_data_push_channel_id)
        val channelName = getString(R.string.notifications_data_push_channel_name)
        return channelId to channelName
    }
    companion object {
        private const val LOG_TAG = "PushListenerService"
    }
}