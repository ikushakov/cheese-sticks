package com.example.semimanufactures

import android.app.PendingIntent

data class AppNotification(
    val id: Int,
    val title: String?,
    val message: String?,
    val channelId: String?,
    val channelName: String?,
    val pendingIntent: PendingIntent? = null
)