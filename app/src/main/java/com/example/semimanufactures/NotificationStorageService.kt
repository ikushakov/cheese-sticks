package com.example.semimanufactures

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class NotificationStorageService : Service() {

    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Perform cleanup
        repository.getAllNotifications() // This will trigger cleanUpOldNotifications
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, NotificationStorageService::class.java)
            context.startService(intent)
        }
    }
}