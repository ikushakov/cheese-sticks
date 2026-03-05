package com.example.semimanufactures

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.concurrent.TimeUnit

class NotificationRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "notifications_prefs"
        private const val NOTIFICATIONS_KEY = "stored_notifications"
        private const val DAYS_TO_KEEP = 10
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val gson by lazy { Gson() }

    fun saveNotification(notification: StoredNotification) {
        val notifications = getAllNotifications().toMutableList()
        notifications.add(0, notification)
        cleanUpOldNotifications(notifications)
        saveAllNotifications(notifications)
    }

    fun getAllNotifications(): List<StoredNotification> {
        val json = sharedPrefs.getString(NOTIFICATIONS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<StoredNotification>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun markAsRead(notificationId: String) {
        val notifications = getAllNotifications().toMutableList()
        notifications.find { it.id == notificationId }?.isRead = true
        saveAllNotifications(notifications)
    }

    fun deleteNotification(notificationId: String) {
        val notifications = getAllNotifications().toMutableList()
        notifications.removeAll { it.id == notificationId }
        saveAllNotifications(notifications)
    }

    fun clearAllNotifications() {
        sharedPrefs.edit().remove(NOTIFICATIONS_KEY).apply()
    }

    private fun cleanUpOldNotifications(notifications: MutableList<StoredNotification>) {
        val currentDate = Date()
        notifications.removeAll { notification ->
            val diffInMillis = currentDate.time - notification.receivedDate.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            diffInDays > DAYS_TO_KEEP
        }
    }

    private fun saveAllNotifications(notifications: List<StoredNotification>) {
        val json = gson.toJson(notifications)
        sharedPrefs.edit().putString(NOTIFICATIONS_KEY, json).apply()
    }
}