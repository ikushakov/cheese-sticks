package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.DatabaseManager.fetchData
import com.example.semimanufactures.DatabaseManager.findCardByIdOrPrp
import com.example.semimanufactures.service_mode.ServiceModeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import ru.rustore.sdk.pushclient.RuStorePushClient
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class FeaturesOfTheFunctionalityActivity : AppCompatActivity(), SupporterManager.IScanListener {
    private var tag: String = FeaturesOfTheFunctionalityActivity::class.java.simpleName
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val REQUEST_CAMERA_PERMISSION = 1001
    private val text_result_scan: TextView by lazy { findViewById(R.id.text_result_scan) }
    private val button_scan: ImageButton by lazy { findViewById(R.id.button_scan) }
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardAdapter: CardAdapter
    private var cardItemList: MutableList<CardItem> = mutableListOf()
    private lateinit var data_user_info: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private var supporterManager: SupporterManager? = null
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var root_layout: LinearLayout
    private val rolesList: MutableList<String> = mutableListOf()
    private var pm84ScannerManager: PM84ScannerManager? = null
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false
    private val viewModel: UpdateViewModel by viewModels()
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
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
            //Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }

        if (!currentIsAuthorized) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_new_features_of_the_functionality)
        supportActionBar?.hide()
        // для обновления мобильной версии через Rustore
        viewModel.init(this)
        lifecycleScope.launch {
            viewModel.events
                .flowWithLifecycle(lifecycle)
                .collect { event ->
                    when (event) {
                        Event.UpdateCompleted -> popupSnackBarForCompleteUpdate()
                    }
                }
        }
        // для обновления мобильной версии через Rustore
        progressBar = findViewById(R.id.progressBar)
        data_user_info = findViewById(R.id.data_user_info)
        go_to_add = findViewById(R.id.go_to_add)
        go_to_issue = findViewById(R.id.go_to_issue)

        go_to_issue.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне выдачи и поиска", Toast.LENGTH_LONG).show()
        }
        val currentDateTime = getCurrentDateTime()
        Log.d("Время сейчас", "${currentDateTime}")

        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }

        when (currentDeviceInfo) {
            "L2H-N" -> {
                supporterManager = SupporterManager(this, this)
                setupRecyclerView()
            }
            "EA630" -> {
                registerScannerReceiver()
                setupRecyclerView()
            }
            "PM84" -> {
                setupRecyclerView()
                pm84ScannerManager = PM84ScannerManager.getInstance(applicationContext)
                pm84ScannerManager?.registerScannerReceiver()
                pm84ScannerManager?.setOnScanResultListener(object : PM84ScannerManager.OnScanResultListener {
                    override fun onScanResultReceived(result: String) {
                        val skladiDataId = result.trim().takeIf { it.isNotBlank() }
                        val prp = result.trim().takeIf { it.isNotBlank() }
                        searchCard(skladiDataId, prp)
                    }
                })
                button_scan.setOnClickListener {
                    if (checkCameraPermission()) {
                        pm84ScannerManager?.startScanning(text_result_scan)
                    }
                }
            }
            else -> {
                setupRecyclerView()
            }
        }

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            fetchDataFromDatabase()
        }
        data_user_info.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@FeaturesOfTheFunctionalityActivity, NotificationActivity::class.java)
            startActivity(intent)
        }

        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this, LogisticActivity::class.java)
            startActivity(intent)
        }

        go_to_add.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
        }

        text_result_scan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString().trim()
                if (inputText.isEmpty()) {
                    fetchDataFromDatabase()
                } else if (inputText.length >= 4) {
                    searchCard(null, inputText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        // для обновления мобильной версии через Rustore
        root_layout = findViewById(R.id.root_layout)
        // для обновления мобильной версии через Rustore
        getDeviceToken()
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
    // UPDATE
    private suspend fun updateUserDevice(mdmCode: String, deviceToken: String): Boolean =
        withContext(Dispatchers.IO) {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_user_device"
            val fallbackUrl = "https://api.gkmmz.ru/api/update_user_device"

            val currentDateTime = getCurrentDateTime()
            Log.d("MainActivity", "updateUserDevice at $currentDateTime, token=$deviceToken")

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("mdm_code", mdmCode)
                .addFormDataPart("device_token", deviceToken)
                .addFormDataPart("data[is_active]", "1")           // как в вашем коде
                .addFormDataPart("data[created_at]", currentDateTime)
                .build()

            val base = Request.Builder()
                .post(body)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)

            fun build(url: String) = base.url(url).build()

            try {
                var resp: Response? = null
                var netErr: IOException? = null

                // primary
                try {
                    val req = build(primaryUrl)
                    Log.d("MainActivity", "→ POST $primaryUrl")
                    resp = client.newCall(req).execute()
                } catch (e: IOException) {
                    netErr = e
                    Log.w("MainActivity", "primary failed: ${e.message}")
                }

                // fallback по 429 ИЛИ по сетевой ошибке primary
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    try {
                        val fbReq = build(fallbackUrl)
                        Log.w("MainActivity", "fallback → $fallbackUrl (reason: ${if (netErr != null) "IO error" else "429"})")
                        resp = client.newCall(fbReq).execute()
                    } catch (e: IOException) {
                        Log.e("MainActivity", "fallback failed: ${e.message}", e)
                        return@withContext false
                    }
                }

                resp.use { r ->
                    val ok = r.isSuccessful
                    if (ok) Log.d("MainActivity", "User device updated successfully")
                    else Log.e("MainActivity", "Failed to update user device: ${r.code}")
                    ok
                }
            } catch (e: ServiceModeException) {
                Log.w("MainActivity", "Service mode active; skip update (until=${e.until})")
                false
            } catch (t: Throwable) {
                Log.e("MainActivity", "Error updating user device", t)
                false
            }
        }

    // ADD
    private suspend fun addUserDevice(mdmCode: String, deviceToken: String): Boolean =
        withContext(Dispatchers.IO) {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/add_user_device"
            val fallbackUrl = "https://api.gkmmz.ru/api/add_user_device"

            val currentDateTime = getCurrentDateTime()

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("mdm_code", mdmCode)
                .addFormDataPart("device_token", deviceToken)
                .addFormDataPart("is_active", "1")                  // у add_* без data[...]
                .addFormDataPart("created_at", currentDateTime)
                .build()

            val base = Request.Builder()
                .post(body)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)

            fun build(url: String) = base.url(url).build()

            try {
                var resp: Response? = null
                var netErr: IOException? = null

                // primary
                try {
                    val req = build(primaryUrl)
                    Log.d("MainActivity", "→ POST $primaryUrl")
                    resp = client.newCall(req).execute()
                } catch (e: IOException) {
                    netErr = e
                    Log.w("MainActivity", "primary failed: ${e.message}")
                }

                // fallback по 429 ИЛИ по сетевой ошибке primary
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    try {
                        val fbReq = build(fallbackUrl)
                        Log.w("MainActivity", "fallback → $fallbackUrl (reason: ${if (netErr != null) "IO error" else "429"})")
                        resp = client.newCall(fbReq).execute()
                    } catch (e: IOException) {
                        Log.e("MainActivity", "fallback failed: ${e.message}", e)
                        return@withContext false
                    }
                }

                resp.use { r ->
                    val ok = r.isSuccessful
                    if (ok) Log.d("MainActivity", "User device added successfully")
                    else Log.e("MainActivity", "Failed to add user device: ${r.code}")
                    ok
                }
            } catch (e: ServiceModeException) {
                Log.w("MainActivity", "Service mode active; skip add (until=${e.until})")
                false
            } catch (t: Throwable) {
                Log.e("MainActivity", "Error adding user device", t)
                false
            }
        }

    private fun getDeviceToken() {
        RuStorePushClient.getToken()
            .addOnSuccessListener { token ->
                Log.d("DeviceToken", "Токен устройства получен: $token")
                Toast.makeText(this, "Токен устройства получен", Toast.LENGTH_SHORT).show()

                // Обновляем токен в данных пользователя
                updateUserDeviceToken(token)
            }
            .addOnFailureListener { exception ->
                Log.e("DeviceToken", "Ошибка получения токена устройства", exception)
                Toast.makeText(this, "Не удалось получить токен устройства", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveUserDataToFile(userData: UserData) {
        try {
            val json = Gson().toJson(userData)
            openFileOutput("user_data", Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("DeviceToken", "Error saving user data", e)
        }
    }
    private fun updateUserDeviceToken(token: String) {
        val userData = readUserData() ?: return

        // Обновляем токен в памяти
        currentDeviceToken = token

        // Обновляем токен в сохраненных данных
        val updatedUserData = userData.copy(device_token = token)
        saveUserDataToFile(updatedUserData)

        // Отправляем токен на сервер
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isUpdated = updateUserDevice(updatedUserData.mdmCode, token)
                if (!isUpdated) {
                    val isAdded = addUserDevice(updatedUserData.mdmCode, token)
                    if (!isAdded) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FeaturesOfTheFunctionalityActivity,
                                "Не удалось отправить токен на сервер",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceToken", "Ошибка обновления токена на сервере", e)
            }
        }
    }
    // для обновления мобильной версии через Rustore
    private fun popupSnackBarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.root_layout),
            getString(R.string.downloading_completed),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.button_install)) { viewModel.completeUpdateRequested() }
            show()
        }
    }
    // для обновления мобильной версии через Rustore
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
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        cardAdapter = createCardAdapter()
        recyclerView.adapter = cardAdapter
    }

    private fun createCardAdapter(): CardAdapter {
        return CardAdapter(
            cardItemList,
            this,
            7,
            currentUsername ?: "",
            currentUserId ?: 0,
            currentRoleCheck ?: "",
            currentMdmCode ?: "",
            currentFio ?: "",
            currentDeviceInfo ?: "",
            currentRolesString ?: "",
            currentDeviceToken ?: "",
            currentIsAuthorized
        )
    }
    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
    override fun onScannerResultChange(result: String?) {
        Log.d(tag, "Scanner result received: $result")
        val scannedText = result?.replace(Regex("[\\s\\n\\t]+"), "")?.trim() ?: ""
        Log.d(tag, "Scanner scannedText received: $scannedText")
        text_result_scan.text = scannedText.replace(Regex("[\\s\\n\\t]+"), "").trim()
        if (scannedText.isNotEmpty()) {
            val skladiDataId = scannedText.takeIf { it.isNotBlank() }
            val prp = scannedText.takeIf { it.isNotBlank() }
            searchCard(skladiDataId, prp)
        } else {
            text_result_scan.text = ""
        }
    }
    override fun onScannerServiceConnected() {
        Log.d(tag, "Scanner service connected")
    }
    override fun onScannerServiceDisconnected() {
        Log.d(tag, "Scanner service disconnected")
    }
    override fun onScannerInitFail() {
        Log.e(tag, "Scanner initialization failed")
    }

    fun handleCardClick(cardItem: CardItem) {
        if (currentUsername == "T.Test") {
            Toast.makeText(this, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
            return
        }
        if (currentUserId == 0 || currentFio.isNullOrEmpty()) {
            Toast.makeText(this, "Вернитесь в настройки, затем попробуйте снова", Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            val prp = cardItem.prp
            Log.d(tag, "Processing card with prp: $prp")
            val cards = withContext(Dispatchers.IO) {
                findCardByIdOrPrp(this@FeaturesOfTheFunctionalityActivity, prp)
            }
            if (cards.isNotEmpty()) {
                val card = cards[0]
                try {
                    val primaryDemandId = card.primarydemand_id
                    val fio = currentFio
                    val dateDistribution = card.dateOfDistribution
                    val demand = card.demand
                    Log.d(tag, "Adding to skladi_data with primaryDemandId: $primaryDemandId and userId: $currentUserId")
                    Toast.makeText(this@FeaturesOfTheFunctionalityActivity, "Выдана ПрП с $primaryDemandId сотрудником $currentUserId", Toast.LENGTH_LONG).show()
                    withContext(Dispatchers.IO) {
                        DatabaseManager.addToSkladiData(this@FeaturesOfTheFunctionalityActivity, primaryDemandId, currentUserId ?: 0,
                            fio ?: "", dateDistribution, demand)
                    }
                    fetchDataFromDatabase()
                } catch (e: Exception) {
                    Log.e(tag, "Error during add/delete operation: ${e.message}", e)
                    Toast.makeText(this@FeaturesOfTheFunctionalityActivity, "Произошла ошибка при выдаче", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e(tag, "Card not found for prp: $prp")
                Toast.makeText(this@FeaturesOfTheFunctionalityActivity, "Карточка не найдена", Toast.LENGTH_LONG).show()
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun fetchDataFromDatabase() {
        lifecycleScope.launch {
            try {
                val data = fetchData(this@FeaturesOfTheFunctionalityActivity)
                updateRecyclerView(data)
                Log.d(tag, "Data fetched successfully: ${data.size} items")
            } catch (e: Exception) {
                Log.e(tag, "Error fetching data: ${e.message}", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecyclerView(data: List<CardItem>) {
        try {
            cardItemList.clear()
            cardItemList.addAll(data)
            cardAdapter.notifyDataSetChanged()
            Log.d(tag, "RecyclerView updated with new data")
        } catch (e: Exception) {
            Log.e(tag, "Error updating RecyclerView: ${e.message}", e)
        } finally {
            progressBar.visibility = View.GONE
        }
    }
    fun searchCard(skladiDataId: String?, prp: String?) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val cardItems = if (skladiDataId.isNullOrBlank() && prp.isNullOrBlank()) {
                    fetchData(this@FeaturesOfTheFunctionalityActivity)
                } else {
                    findCardByIdOrPrp(this@FeaturesOfTheFunctionalityActivity, prp)
                }
                updateRecyclerView(cardItems)
            } catch (e: Exception) {
                Log.e(tag, "Ошибка при поиске карточки: ${e.message}", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    private fun registerScannerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_RECEIVE_DATA)
            addAction(ACTION_RECEIVE_DATABYTES)
            addAction(ACTION_RECEIVE_DATALENGTH)
            addAction(ACTION_RECEIVE_DATATYPE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
            registerReceiver(mScanReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            // Для версий ниже Android 14
            registerReceiver(mScanReceiver, intentFilter)
        }
    }
    private val mScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(tag, "onReceive()")
            val action = intent.action
            val bundle = intent.extras ?: return

            when (action) {
                ACTION_RECEIVE_DATA -> {
                    Log.v(tag, "ACTION_RECEIVE_DATA")
                    val barcodeStr = bundle.getString("text")
                    Log.v(tag, "barcode data: $barcodeStr")
                    progressBar.visibility = View.GONE
                    text_result_scan.text = ""
                    text_result_scan.text = barcodeStr?.replace(" ", "")?.trim() ?: ""
                    val skladiDataId = text_result_scan.text.toString().trim().takeIf { it.isNotBlank() }
                    val prp = text_result_scan.text.toString().trim().takeIf { it.isNotBlank() }
                    searchCard(skladiDataId, prp)
                }
            }
        }
    }
    private fun closeScanService() {
        val bundle = Bundle().apply {
            putBoolean("close", true)
        }
        val mIntent = Intent().setAction(CLOSE_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            when (currentDeviceInfo) {
                "EA630" -> {
                    closeScanService()
                    unregisterReceiver(mScanReceiver)
                }
                "PM84" -> {
                    pm84ScannerManager?.unregisterScannerReceiver()
                }
                "L2H-N" -> {
                    supporterManager?.recycle()
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.e(tag, "Receiver not registered or already unregistered", e)
        } catch (e: Exception) {
            Log.e(tag, "Error in onDestroy", e)
        }
        Log.v(tag, "onDestroy()")
    }
    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            false
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pm84ScannerManager?.startScanning(text_result_scan)
        } else {
            Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        checkBackgroundPermission()
    }
    private fun checkBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationSnackbar()
            }
        }
    }

    private fun showBatteryOptimizationSnackbar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Для корректной работы уведомлений отключите оптимизацию батареи",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Настроить") {
            (application as? App)?.openBatteryOptimizationSettings(this)
        }.show()
    }
}