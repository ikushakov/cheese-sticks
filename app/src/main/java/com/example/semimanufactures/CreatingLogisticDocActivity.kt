package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.getDeliveryLogisticsByDemandDoc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

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
    private var deviceInfo: String = ""
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private lateinit var progressBar: ProgressBar
    private lateinit var doc_for_creating: EditText
    private lateinit var create_new_logistic_button: Button
    private lateinit var scan_button: ImageButton
    private var typeDoc: String = "doc"
    @SuppressLint("MissingInflatedId")
    private var lastFetchTime = 0L
    private val fetchDelay = 300L
    private lateinit var main_layout: ConstraintLayout
    private val rolesList: MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creating_logistic_doc)
        scan_button = findViewById(R.id.scan_button)
        progressBar = findViewById(R.id.progressBar)
        doc_for_creating = findViewById(R.id.doc_for_creating)
        create_new_logistic_button = findViewById(R.id.create_new_logistic_button)
        initView()
        registerScannerReceiver()
        val intent = intent
        username = intent.getStringExtra("username") ?: ""
        val password = intent.getStringExtra("password") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        userId = intent.getIntExtra("userId", 0)
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@CreatingLogisticDocActivity, LogisticActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            showPopupMenuNotification(it)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@CreatingLogisticDocActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@CreatingLogisticDocActivity, SettingsActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@CreatingLogisticDocActivity, AddActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
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
                            val intentDoc = Intent(this@CreatingLogisticDocActivity, DetailLogisticsActivity::class.java).apply {
                                putExtra("logistics_id", logisticsId.toString())
                                putExtra("mdmCode", mdmCode)
                                putExtra("userId", userId)
                                putExtra("username", username)
                                putExtra("roleCheck", roleCheck)
                                putExtra("fio", fio)
                                putExtra("deviceInfo", deviceInfo)
                                putExtra("type", typeDoc)
                                putExtra("rolesString", rolesString)
                            }
                            startActivity(intentDoc)
                            progressBar.visibility = View.GONE
                        } else {
                            Log.d(tag, "No logistics found for document: $docValue")
                            Toast.makeText(this@CreatingLogisticDocActivity, "Существующей заявки не найдено. Создание новой заявки...", Toast.LENGTH_LONG).show()
                            createLogisticEntry(mdmCode, docValue)
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
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@CreatingLogisticDocActivity)
            Log.d("MainActivity", "Версия получена: $versionMobile")
            if (versionMobile == null) {
                Log.e("MainActivity", "Не удалось получить версию")
                //disableUI()
            } else {
                Log.d("MainActivity", "Версия приложения: $versionMobile")
                if (versionMobile.toInt() != myGlobalVariable) {
                    Log.e("MainActivity", "Версии не совпадают. Доступ к функционалу отключен.")
                    disableUI()
                    main_layout.setOnClickListener {
                        Toast.makeText(this@CreatingLogisticDocActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    private fun createLogisticEntry(mdmCode: String, docValue: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val url = "http://192.168.200.250/api/create_logistic/"
        val formBody = FormBody.Builder()
            .add("created_by", mdmCode)
            .add("type", "doc")
            .add("planned_date", "1")
            .add("box_id", docValue)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    if (e is SocketTimeoutException) {
                        Toast.makeText(this@CreatingLogisticDocActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@CreatingLogisticDocActivity, "Ошибка при создании заявки: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        Toast.makeText(this@CreatingLogisticDocActivity, "Заявка успешно создана", Toast.LENGTH_SHORT).show()
                        val rolesString = intent.getStringExtra("rolesString") ?: ""
                        rolesList.addAll(rolesString.split(",").map { it.trim() })
                        rolesList.forEach { role ->
                            Log.d("Список ролей", "Роль: $role")
                        }
                        val intent = Intent(this@CreatingLogisticDocActivity, NewLogisticDocActivity::class.java).apply {
                            putExtra("mdmCode", mdmCode)
                            putExtra("planned_date", "1")
                            putExtra("responseBody", responseBody)
                            putExtra("box_id", docValue)
                            putExtra("userId", userId)
                            putExtra("username", username)
                            putExtra("roleCheck", roleCheck)
                            putExtra("fio", fio)
                            putExtra("type", typeDoc)
                            putExtra("deviceInfo", deviceInfo)
                            putExtra("rolesString", rolesString)
                        }
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@CreatingLogisticDocActivity, "Ошибка при создании заявки: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
    @SuppressLint("MissingInflatedId")
    private fun showPopupMenuNotification(view: View) {
        val popupView = layoutInflater.inflate(R.layout.custom_menu_notification, null)
        val popupWindow = PopupWindow(popupView, 500, 450)
        popupView.findViewById<LinearLayout>(R.id.item_write_sms).setOnClickListener {
            val rolesString = intent.getStringExtra("rolesString") ?: ""
            rolesList.addAll(rolesString.split(",").map { it.trim() })
            rolesList.forEach { role ->
                Log.d("Список ролей", "Роль: $role")
            }
            val intent = Intent(this, CreateNotificationActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.item_incoming_sms).setOnClickListener {
            Toast.makeText(this, "Входящие нажаты", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.item_sent_sms).setOnClickListener {
            Toast.makeText(this, "Отправленные нажаты", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(view)
    }
    private fun initView() {
        scan_button.setOnClickListener {
            callScanner()
        }
    }
    private fun registerScannerReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_DATA)
        intentFilter.addAction(ACTION_RECEIVE_DATABYTES)
        intentFilter.addAction(ACTION_RECEIVE_DATALENGTH)
        intentFilter.addAction(ACTION_RECEIVE_DATATYPE)
        registerReceiver(mScanReceiver, intentFilter)
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        closeScanService()
        unregisterReceiver(mScanReceiver)
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
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        scan_button.isEnabled = false
        doc_for_creating.isEnabled = false
        create_new_logistic_button.isEnabled = false
        go_to_add.isEnabled = false
        go_to_issue.isEnabled = false
        data_user_info.isEnabled = false
        go_to_send_notification.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
}