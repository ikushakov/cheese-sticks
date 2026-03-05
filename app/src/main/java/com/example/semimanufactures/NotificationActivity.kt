package com.example.semimanufactures

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson


class NotificationActivity : AppCompatActivity() {

    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false

    private lateinit var go_to_logistic: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView

    private lateinit var repository: NotificationRepository
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            Log.d("UserData", "Логин: ${it.username}")
            Log.d("UserData", "User ID: ${it.userId}")
            Log.d("UserData", "Роль: ${it.roleCheck}")
            Log.d("UserData", "MdmdCode: ${it.mdmCode}")
            Log.d("UserData", "ФИО: ${it.fio}")
            Log.d("UserData", "Название устройства: ${it.deviceInfo}")
            Log.d("UserData", "Список ролей: ${it.rolesString}")
            Log.d("UserData", "Токен устройства: ${it.device_token}")
            Log.d("isAuthorized", "Авторизован? ${it.isAuthorized}")
        } ?: run {
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }

        if (!currentIsAuthorized) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_notifications)
        supportActionBar?.hide()
        repository = NotificationRepository(this)
        setupRecyclerView()

        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@NotificationActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@NotificationActivity, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@NotificationActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@NotificationActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне с уведомлениями", Toast.LENGTH_LONG).show()
        }
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

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.notificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val notifications = repository.getAllNotifications()
        adapter = NotificationAdapter(notifications) { notification ->
            // Handle click - mark as read and maybe open details
            repository.markAsRead(notification.id)
            // You could also open the logistics detail here
        }

        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to activity
        adapter.updateNotifications(repository.getAllNotifications())
    }
}