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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddMezhZavod
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddOKR
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddPosleProdazhnoeObsluzhivanie
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddSeria
import com.example.semimanufactures.DatabaseManager.findDistributionDateByPrP
import com.example.semimanufactures.DatabaseManager.getWarehouseNameById
import com.example.semimanufactures.DatabaseManager.showToast
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AddActivity : ComponentActivity() {
    private var tag: String = AddActivity::class.java.simpleName
    private val SCANNER_INIT = "unitech.scanservice.init"
    private val SCAN2KEY_SETTING = "unitech.scanservice.scan2key_setting"
    private val START_SCANSERVICE = "unitech.scanservice.start"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val SOFTWARE_SCANKEY = "unitech.scanservice.software_scankey"
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
    private lateinit var button_for_add_barcode_scan: ImageButton
    private lateinit var button_for_add_qrcode_scan: ImageButton
    private lateinit var text_result_add_barcode_scan: TextView
    private lateinit var text_result_add_qrcode_scan: TextView
    private lateinit var button_for_add_on_sklad: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var text_result_add_barcode_scan_text: TextView
    private lateinit var text_result_add_qrcode_scan_text: TextView
    private lateinit var go_to_logistic: ImageView
    private lateinit var button_to_inv: Button
    private lateinit var button_check: Button
    private var currentScanField: Int = 0
    private var isBarcodeStyled: Boolean = false
    private var isQRCodeActive: Boolean = false
    private lateinit var supporterManager: SupporterManager
    private lateinit var main_layout: ConstraintLayout
    private val REQUEST_CAMERA_PERMISSION = 1001
    private val rolesList: MutableList<String> = mutableListOf()
    private var pm84ScannerManager: PM84ScannerManager? = null
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
    private val mScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val bundle = intent.extras ?: return
            when (action) {
                ACTION_RECEIVE_DATA -> {
                    val barcodeStr = bundle.getString("text")?.replace("\\s".toRegex(), "")
                    Log.d("ScanReceiver", "Received barcode: $barcodeStr for field: $currentScanField")
                    progressBar.visibility = View.GONE
                    runOnUiThread {
                        when (currentScanField) {
                            1 -> {
                                text_result_add_barcode_scan.setText(barcodeStr)
                                text_result_add_barcode_scan_text.text = barcodeStr
                                checkDistributionDate(barcodeStr?.trim())
                            }
                            2 -> {
                                text_result_add_qrcode_scan.setText(barcodeStr)
                                text_result_add_qrcode_scan_text.text = barcodeStr
                                isBarcodeStyled = false
                            }
                            3 -> {}
                        }
                    }
                }
            }
        }
    }
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
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }

        if (!currentIsAuthorized) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_new_add)
        initView()
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        Log.d(tag, "User id: $currentUserId, Username: $currentUsername, Role: $currentRoleCheck, MDM Code: $currentMdmCode")
        if (currentDeviceInfo == "EA630") {
            registerScannerReceiver()
            progressBar = findViewById(R.id.progressBar)
        } else if (currentDeviceInfo == "L2H-N") {
            supporterManager = SupporterManager(this, object : SupporterManager.IScanListener {
                override fun onScannerResultChange(result: String?) {
                    Log.d(tag, "Scanner result received: $result")
                    if (text_result_add_qrcode_scan.isFocused) {
                        val scannedQrcodeText = result?.replace(Regex("[\\s\\n\\t]+"), "")?.trim() ?: ""
                        text_result_add_qrcode_scan_text.text =
                            scannedQrcodeText.replace(Regex("[\\s\\n\\t]+"), "").trim()
                        Log.d(tag, "1 qr код: ${text_result_add_qrcode_scan}")
                    } else {
                        val scannedBarcodeText = result?.replace(Regex("[\\s\\n\\t]+"), "")?.trim() ?: ""
                        text_result_add_barcode_scan_text.text =
                            scannedBarcodeText.replace(Regex("[\\s\\n\\t]+"), "").trim() ?: ""
                        Log.d(tag, "2 qr код: ${text_result_add_barcode_scan}")
                        checkDistributionDate(text_result_add_barcode_scan_text.text.toString())
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
            })
        }
        if (currentDeviceInfo == "PM84") {
            pm84ScannerManager = PM84ScannerManager.getInstance(applicationContext)
            pm84ScannerManager?.registerScannerReceiver()
            pm84ScannerManager?.setOnScanResultListener(object : PM84ScannerManager.OnScanResultListener {
                override fun onScanResultReceived(result: String) {
                    val scannedText = result.trim().takeIf { it.isNotBlank() }
                    handleScanResult(scannedText)
                }
            })
            button_for_add_barcode_scan.setOnClickListener {
                if (checkCameraPermission()) {
                    pm84ScannerManager?.startScanning(text_result_add_barcode_scan)
                }
            }
            button_for_add_qrcode_scan.setOnClickListener {
                if (checkCameraPermission()) {
                    pm84ScannerManager?.startScanning(text_result_add_qrcode_scan)
                }
            }
        }
        setupButtonListeners()
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@AddActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне для добавления ПрП на склад", Toast.LENGTH_LONG).show()
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@AddActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@AddActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@AddActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        button_to_inv = findViewById(R.id.button_to_inv)
        button_to_inv.setOnClickListener {
            if (currentDeviceInfo == "L2H-N") {
                val intent = Intent(this, CheckingPrPSkladSunmiActivity::class.java)
                startActivity(intent)
            }
            else {
                val intent = Intent(this, CheckingPrPSkladActivity::class.java)
                startActivity(intent)
            }
        }
        button_check = findViewById(R.id.button_check)
        button_check.setOnClickListener {
            val intent = Intent(this, InventarisationFirsovActivity::class.java)
            startActivity(intent)
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleScanResult(result: String?) {
        if (result.isNullOrEmpty()) {
            Toast.makeText(this, "Сканирование не удалось", Toast.LENGTH_SHORT).show()
            return
        }
        when (currentScanField) {
            1 -> {
                text_result_add_barcode_scan.text = result
                if (result.length > 6) {  // Only check if length > 6
                    checkDistributionDate(result)
                }
            }
            2 -> {
                text_result_add_qrcode_scan.text = result
            }
            else -> Log.e(tag, "Неизвестное поле для сканирования")
        }
    }

    private val textWatcherQrCode = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val qrcodeValue = s.toString().trim()
            if (qrcodeValue.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val warehouseStatus = getWarehouseNameById(this@AddActivity, qrcodeValue)
                    if (warehouseStatus != null) {
                        if (!warehouseStatus.second) {
                            showToast(this@AddActivity, "Склад с ID $qrcodeValue не активен", 5000)
                            text_result_add_qrcode_scan.error = "Склад не активен"
                        } else {
                            text_result_add_qrcode_scan.error = null
                        }
                    } else {
                        text_result_add_qrcode_scan.error = "Склад не найден"
                    }
                }
            } else {
                text_result_add_qrcode_scan.error = null
            }
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initView() {
        button_for_add_barcode_scan = findViewById(R.id.button_for_add_barcode_scan)
        button_for_add_qrcode_scan = findViewById(R.id.button_for_add_qrcode_scan)
        text_result_add_barcode_scan = findViewById(R.id.text_result_add_barcode_scan)
        text_result_add_qrcode_scan = findViewById(R.id.text_result_add_qrcode_scan)
        button_for_add_on_sklad = findViewById(R.id.button_for_add_on_sklad)
        progressBar = findViewById(R.id.progressBar)
        text_result_add_barcode_scan_text = findViewById(R.id.text_result_add_barcode_scan_text)
        text_result_add_qrcode_scan_text = findViewById(R.id.text_result_add_qrcode_scan_text)
        text_result_add_barcode_scan.addTextChangedListener(textWatcher)
        text_result_add_qrcode_scan.addTextChangedListener(textWatcher)
        text_result_add_qrcode_scan.addTextChangedListener(textWatcherQrCode)
        text_result_add_barcode_scan.setOnClickListener {
            currentScanField = 1
            isBarcodeStyled = true
            isQRCodeActive = false
        }
        text_result_add_qrcode_scan.setOnClickListener {
            currentScanField = 2
            isQRCodeActive = true
            isBarcodeStyled = false
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtonListeners() {
        button_for_add_barcode_scan.setOnClickListener {
            if (checkCameraPermission()) {
                if (currentDeviceInfo == "EA630") {
                    currentScanField = 1
                    isBarcodeStyled = true
                    isQRCodeActive = false
                    callScannerEA630()
                } else {
                    callScannerSunmi(1)
                }
            }
        }
        button_for_add_qrcode_scan.setOnClickListener {
            if (checkCameraPermission()) {
                if (currentDeviceInfo == "EA630") {
                    currentScanField = 2
                    isQRCodeActive = true
                    isBarcodeStyled = false
                    callScannerEA630()
                } else {
                    callScannerSunmi(2)
                }
            }
        }
        button_for_add_on_sklad.setOnClickListener {
            // Создаем корутину сразу для всего обработчика
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    if (currentUsername == "T.Test") {
                        showToast(this@AddActivity, "У вас недостаточно прав для совершения данной операции", 7000)
                        return@launch
                    }

                    // Проверяем, есть ли ошибка в штрих-коде
                    val background = text_result_add_barcode_scan.background
                    val hasErrorStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        background?.constantState?.equals(ContextCompat.getDrawable(this@AddActivity, R.drawable.error_prp_edit_text)?.constantState) ?: false
                    } else {
                        val errorBackground = ContextCompat.getDrawable(this@AddActivity, R.drawable.error_prp_edit_text)
                        background?.constantState?.equals(errorBackground?.constantState) ?: false
                    }

                    if (hasErrorStyle) {
                        showToast(this@AddActivity, "Дата распределения и сегмент ПрП не соответствуют условиям добавления на ПРОСК", 7000)
                        return@launch
                    }

                    val qrcodeValue = if (currentDeviceInfo == "EA630") {
                        text_result_add_qrcode_scan.text.toString().replace("\\s".toRegex(), "")
                    } else {
                        text_result_add_qrcode_scan_text.text.toString()
                    }

                    if (qrcodeValue.isBlank()) {
                        showToast(this@AddActivity, "Поле id склада не может быть пустым", 5000)
                        return@launch
                    }

                    // Основная логика
                    val warehouseStatus = getWarehouseNameById(this@AddActivity, qrcodeValue)
                    when {
                        warehouseStatus == null -> {
                            showToast(this@AddActivity, "Склад с ID $qrcodeValue не найден", 5000)
                        }
                        !warehouseStatus.second -> {
                            showToast(this@AddActivity, "Склад с ID $qrcodeValue не активен", 5000)
                        }
                        else -> {
                            progressBar.visibility = View.VISIBLE
                            button_for_add_on_sklad.isEnabled = false

                            val warehouseName = warehouseStatus.first
                            val barcodeValue = if (currentDeviceInfo == "EA630") {
                                text_result_add_barcode_scan.text.toString().replace("\\s".toRegex(), "")
                            } else {
                                text_result_add_barcode_scan_text.text.toString()
                            }

                            // Выполняем операцию добавления в IO-диспетчере
                            withContext(Dispatchers.IO) {
                                DatabaseManager.addToSkladiDataPrP(
                                    this@AddActivity,
                                    barcodeValue,
                                    currentUserId ?: 0,
                                    currentFio ?: "",
                                    qrcodeValue,
                                    text_result_add_barcode_scan
                                )
                            }

                            //showToast(this@AddActivity, "Добавлен на склад $warehouseName c id $qrcodeValue", 7000)
                            progressBar.visibility = View.GONE
                            button_for_add_on_sklad.isEnabled = true
                        }
                    }

                } catch (e: Exception) {
                    Log.e("AddActivity", "Ошибка при добавлении на склад", e)
                    showToast(this@AddActivity, "Произошла ошибка: ${e.message}", 5000)
                    progressBar.visibility = View.GONE
                    button_for_add_on_sklad.isEnabled = true
                }
            }
        }

    }
    private fun callScannerEA630() {
        progressBar.visibility = View.VISIBLE
        startScanService()
        setScan2Key()
        setInit()
        val bundle = Bundle().apply {
            putBoolean("scan", true)
            putInt("field", currentScanField)
        }
        val mIntent = Intent().setAction(SOFTWARE_SCANKEY).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun callScannerSunmi(currentScanField: Int) {
        Log.d(tag, "Calling scanner for field: $currentScanField")
        supporterManager.singleScan(true)
        val bundle = Bundle().apply {
            putBoolean("scan", true)
            putInt("field", currentScanField)
        }
        val mIntent = Intent().setAction(SOFTWARE_SCANKEY).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val text = s.toString().trim()
            if (text.length > 6) {  // Only apply if length > 6
                checkDistributionDate(text)
            } else {
                // Reset styling if text is too short
                text_result_add_barcode_scan.setBackgroundResource(R.drawable.error_prp_edit_text)
                text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDistributionDate(barcodeValue: String?) {
        if (barcodeValue.isNullOrBlank() || barcodeValue.length < 6) return
        CoroutineScope(Dispatchers.Main).launch {
            val searchData = findDistributionDateByPrP(this@AddActivity, barcodeValue)
            if (searchData == null) {
                Log.e("DistributionCheck", "Нет данных для штрих-кода")
                return@launch
            }
            val distributionDate = searchData.dateDistribution
            val segment = searchData.segment
            if (isBarcodeStyled) {
                if (distributionDate.isNullOrBlank()) {
                    Log.e("DistributionCheck", "Дата распределения не найдена")
                    text_result_add_barcode_scan.setBackgroundResource(R.drawable.successful_prp_edit_text)
                } else {
                    try {
                        val currentDate = LocalDate.now()
                        val date = LocalDate.parse(distributionDate)
                        val daysPlusSeria = fetchDaysToAddSeria(this@AddActivity) ?: 3
                        val daysPlusMezhZavod = fetchDaysToAddMezhZavod(this@AddActivity) ?: 210
                        val daysPlusOKR = fetchDaysToAddOKR(this@AddActivity) ?: 60
                        val daysPlusPosleProd = fetchDaysToAddPosleProdazhnoeObsluzhivanie(this@AddActivity) ?: 45
                        if (segment == "Серия" && date.isAfter(currentDate.plusDays(daysPlusSeria))) {    text_result_add_barcode_scan.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@AddActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
                        }
                        else if (segment == "Межзавод" && date.isAfter(currentDate.plusDays(daysPlusMezhZavod))) {    text_result_add_barcode_scan.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@AddActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
                        }
                        else if (segment == "ОКР" && date.isAfter(currentDate.plusDays(daysPlusOKR))) { text_result_add_barcode_scan.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@AddActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
                        }
                        else if (segment == "Послепродажное обслуживание" && date.isAfter(currentDate.plusDays(daysPlusPosleProd))) { text_result_add_barcode_scan.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@AddActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
                        }
                        else { text_result_add_barcode_scan.setBackgroundResource(R.drawable.error_prp_edit_text)
                            showToast(this@AddActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
                        }
                    } catch (e: Exception) {
                        Log.e("DistributionCheck", "Ошибка при парсинге даты: ${e.message}")
                        text_result_add_barcode_scan.setBackgroundResource(R.drawable.error_prp_edit_text)
                        text_result_add_barcode_scan.setTextColor(resources.getColor(R.color.black))
                    }
                }
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
            registerReceiver(mScanReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(mScanReceiver, intentFilter)
        }
    }
    private fun startScanService() {
        val bundle = Bundle().apply {
            putBoolean("close", true)
        }
        val mIntent = Intent().setAction(START_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun setScan2Key() {
        val bundle = Bundle()
        bundle.putBoolean("scan2key", false)
        val mIntent = Intent().setAction(SCAN2KEY_SETTING).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun setInit() {
        val bundle = Bundle()
        bundle.putBoolean("enable", true)
        val mIntent1 = Intent().setAction(SCANNER_INIT).putExtras(bundle)
        sendBroadcast(mIntent1)
    }
    private fun closeScanService() {
        val bundle = Bundle().apply {
            putBoolean("close", true)
        }
        val mIntent = Intent().setAction(CLOSE_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
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
        } else {
            Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        if (currentDeviceInfo == "EA630") {
            closeScanService()
            unregisterReceiver(mScanReceiver)
        } else if (currentDeviceInfo == "L2H-N") {
            supporterManager.recycle()
        } else if (currentDeviceInfo == "PM84") {
            pm84ScannerManager?.unregisterScannerReceiver()
        }
        Log.v(tag, "onDestroy()")
    }
}