package com.example.semimanufactures

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationManagerWrapper private constructor(
    private val notificationManager: NotificationManagerCompat
) {
    fun createNotificationChannel(
        channelId: String,
        channelName: String,
        importance: Int = NotificationManagerCompat.IMPORTANCE_HIGH, // было DEFAULT
        description: String? = null
    ) {
        val builder = NotificationChannelCompat.Builder(channelId, importance)
            .setName(channelName)
            .setVibrationEnabled(true)
            .setLightsEnabled(true)
            .setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                null // без AudioAttributesCompat
            )

        if (!description.isNullOrBlank()) {
            builder.setDescription(description)
        }

        notificationManager.createNotificationChannel(builder.build())
    }

    fun showNotification(context: Context, data: AppNotification) {
        val channelId = data.channelId ?: return

        // гарантируем наличие канала (HIGH)
        if (notificationManager.getNotificationChannel(channelId) == null) {
            createNotificationChannel(
                channelId = channelId,
                channelName = data.channelName ?: "Уведомления",
                importance = NotificationManagerCompat.IMPORTANCE_HIGH,
                description = "Входящие уведомления"
            )
        }

        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.png_512)              // монохромный статус-икон
            .setContentTitle(data.title)
            .setContentText(data.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(data.message))
            .setAutoCancel(true)
            .setContentIntent(data.pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)     // важно для heads-up
            .setPriority(NotificationCompat.PRIORITY_HIGH)        // для < Android 8
            .setDefaults(NotificationCompat.DEFAULT_ALL)          // звук/вибро/свет
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        notificationManager.notify(data.id, n)
    }

    companion object {
        private var instance: NotificationManagerWrapper? = null
        fun getInstance(context: Context): NotificationManagerWrapper {
            return instance ?: NotificationManagerWrapper(
                notificationManager = NotificationManagerCompat.from(context)
            ).also { instance = it }
        }
    }
}