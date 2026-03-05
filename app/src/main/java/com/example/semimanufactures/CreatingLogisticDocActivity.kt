package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
//import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.getDeliveryLogisticsByDemandDoc
import com.example.semimanufactures.service_mode.ServiceModeException
import com.google.gson.Gson
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class CreatingLogisticDocActivity : ComponentActivity() {
    private var tag: String = CreatingLogisticActivity::class.java.simpleName
    private val SCANNER_INIT = "unitech.scanservice.init"
    private val SCAN2KEY_SETTING = "unitech.scanservice.scan2key_setting"
    private val START_SCANSERVICE = "unitech.scanservice.start"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val SOFTWARE_SCANKEY = "unitech.scanservice.software_scankey"
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var doc_for_creating: EditText
    private lateinit var create_new_logistic_button: Button
    private lateinit var scan_button: ImageButton
    private var pm84ScannerManager: PM84ScannerManager? = null
    private var supporterManager: SupporterManager? = null
    private var typeDoc: String = "doc"
    @SuppressLint("MissingInflatedId")
    private var lastFetchTime = 0L
    private val fetchDelay = 300L
    private lateinit var main_layout: ConstraintLayout
    private val REQUEST_CAMERA_PERMISSION = 1001
    //
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false
    //
    private val rolesList: MutableList<String> = mutableListOf()
    @RequiresApi(Build.VERSION_CODES.O)
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
        setContentView(R.layout.activity_creating_logistic_doc)
        scan_button = findViewById(R.id.scan_button)
        progressBar = findViewById(R.id.progressBar)
        doc_for_creating = findViewById(R.id.doc_for_creating)
        create_new_logistic_button = findViewById(R.id.create_new_logistic_button)
        initView()
        registerScannerReceiver()
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        Log.d(tag, "User id: $currentUserId, Username: $currentUsername, Role: $currentRoleCheck, MDM Code: $currentMdmCode")
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@CreatingLogisticDocActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@CreatingLogisticDocActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@CreatingLogisticDocActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@CreatingLogisticDocActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@CreatingLogisticDocActivity, AddActivity::class.java)
            startActivity(intent)
        }
        doc_for_creating.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val docValue = s.toString()
                if (docValue.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFetchTime > fetchDelay) {
                        lastFetchTime = currentTime
                        Log.d(tag, "PRP value changed: $docValue")
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        create_new_logistic_button.setOnClickListener {
            if (currentUsername == "T.Test") {
                Toast.makeText(this, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
            }
            else {
                progressBar.visibility = View.VISIBLE
                val docValue = doc_for_creating.text.toString().trim()
                Log.d(tag, "Button clicked. Document value retrieved: $docValue")
                if (docValue.isNotEmpty()) {
                    Log.d(tag, "Document value is not empty. Proceeding to fetch logistics.")
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val logisticsList = getDeliveryLogisticsByDemandDoc(docValue, "doc", this@CreatingLogisticDocActivity)
                            if (logisticsList.isNotEmpty()) {
                                Log.d(tag, "Logistics found for document: $docValue. Number of logistics: ${logisticsList.size}")
                                val logisticsId = logisticsList[0].id
                                Log.d(tag, "Extracted ID: $logisticsId")
                                val intentDoc = Intent(this@CreatingLogisticDocActivity, DetailLogisticsActivity::class.java)
                                startActivity(intentDoc)
                                progressBar.visibility = View.GONE
                            } else {
                                Log.d(tag, "No logistics found for document: $docValue")
                                Toast.makeText(this@CreatingLogisticDocActivity, "Существующей заявки не найдено. Создание новой заявки...", Toast.LENGTH_LONG).show()
                                createLogisticEntry(currentMdmCode ?: "", docValue)
                                progressBar.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error fetching logistics: ${e.message}")
                            Toast.makeText(this@CreatingLogisticDocActivity, "Ошибка при получении данных", Toast.LENGTH_LONG).show()
                            progressBar.visibility = View.GONE
                        }
                    }
                } else {
                    Log.d(tag, "Document value is empty.")
                    Toast.makeText(this@CreatingLogisticDocActivity, "Введите значение документа", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
        if (currentDeviceInfo == "PM84") {
            pm84ScannerManager = PM84ScannerManager.getInstance(applicationContext)
            pm84ScannerManager?.registerScannerReceiver()
            pm84ScannerManager?.setOnScanResultListener(object : PM84ScannerManager.OnScanResultListener {
                override fun onScanResultReceived(result: String) {
                    handleScanResult(result.trim())
                }
            })
            scan_button.setOnClickListener {
                if (checkCameraPermission()) {
                    pm84ScannerManager?.startScanning(doc_for_creating)
                }
            }
        } else if (currentDeviceInfo == "L2H-N") {
            supporterManager = SupporterManager(this, object : SupporterManager.IScanListener {
                override fun onScannerResultChange(result: String?) {
                    handleScanResult(result?.trim())
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
            })
            scan_button.setOnClickListener {
                supporterManager?.singleScan(true)
            }
        }
        main_layout = findViewById(R.id.main_layout)

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
    private fun handleScanResult(result: String?) {
        result?.let {
            doc_for_creating.setText(it)
        }
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
            pm84ScannerManager?.startScanning(doc_for_creating)
        } else {
            Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
        }
    }
    private fun createLogisticEntry(mdmCode: String, docValue: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            // primary → fallback
            val primaryUrl  = "https://api.gkmmz.ru/api/create_logistic_doc"
            val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_logistic_doc"

            val body = FormBody.Builder()
                .add("created_by", mdmCode)
                .add("type", "doc")
                .add("planned_date", "1")
                .add("box_id", docValue)
                .add("created_by_name", currentFio ?: "")
                .add("mdm_code", currentMdmCode ?: "")
                .add("version_name", version_name)
                .build()

            val baseReq = Request.Builder()
                .post(body)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token",  authToken)

            fun exec(url: String): Response? = try {
                client.newCall(baseReq.url(url).build()).execute()
            } catch (e: IOException) {
                Log.e("CreateLogisticEntry", "IO error for $url: ${e.message}")
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("CreateLogisticEntry", "fallback → $fallbackUrl (reason: ${if (resp == null) "IO error" else "429"})")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreatingLogisticDocActivity, "Все серверы недоступны", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    val responseBody = r.body?.string()
                    if (r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CreatingLogisticDocActivity, "Заявка успешно создана", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@CreatingLogisticDocActivity, NewLogisticDocActivity::class.java).apply {
                                putExtra("mdmCode", mdmCode)
                                putExtra("planned_date", "1")
                                putExtra("responseBody", responseBody)
                                putExtra("box_id", docValue)
                                putExtra("type", typeDoc)
                            }
                            startActivity(intent)
                        }
                    } else {
                        Log.e("CreateLogisticEntry", "Ошибка: ${r.code} - ${r.message}; body=$responseBody")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CreatingLogisticDocActivity, "Ошибка при создании заявки: ${r.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                // Экран техработ уже показан перехватчиком
            } catch (t: Throwable) {
                Log.e("CreateLogisticEntry", "Unexpected error", t)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreatingLogisticDocActivity, "Ошибка: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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


    private fun initView() {
        scan_button.setOnClickListener {
            callScanner()
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
            // Для Android 14 и выше
            registerReceiver(mScanReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            // Для версий ниже Android 14
            registerReceiver(mScanReceiver, intentFilter)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        if (currentDeviceInfo == "EA630") {
            closeScanService()
            unregisterReceiver(mScanReceiver)
        } else if (currentDeviceInfo == "PM84") {
            pm84ScannerManager?.unregisterScannerReceiver()
        } else if (currentDeviceInfo == "L2H-N") {
            supporterManager?.recycle()
        }
    }
    private fun callScanner() {
        Log.v(tag, "callScanner()")
        progressBar.visibility = View.VISIBLE
        startScanService()
        setScan2Key()
        setInit()
        val bundle = Bundle()
        bundle.putBoolean("scan", true)
        val mIntent = Intent().setAction(SOFTWARE_SCANKEY).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun setScan2Key() {
        val bundle = Bundle()
        bundle.putBoolean("scan2key", false)
        val mIntent = Intent().setAction(SCAN2KEY_SETTING).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun setInit() {
        val bundle1 = Bundle()
        bundle1.putBoolean("enable", true)
        val mIntent1 = Intent().setAction(SCANNER_INIT).putExtras(bundle1)
        sendBroadcast(mIntent1)
    }
    private fun startScanService() {
        val bundle = Bundle()
        bundle.putBoolean("close", true)
        val mIntent = Intent().setAction(START_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun closeScanService() {
        val bundle = Bundle()
        bundle.putBoolean("close", true)
        val mIntent = Intent().setAction(CLOSE_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private val mScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(tag, "onReceive()")
            val action = intent.action
            val bundle = intent.extras ?: return
            when (action) {
                ACTION_RECEIVE_DATA -> {
                    Log.v(tag, "ACTION_RECEIVE_DATA")
                    val barcodeStr = bundle.getString("text")?.trim()
                    Log.v(tag, "barcode data: $barcodeStr")
                    val cleanedBarcodeStr = barcodeStr?.replace("\\s+".toRegex(), "")
                    doc_for_creating.setText(cleanedBarcodeStr)
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

}