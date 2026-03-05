package com.example.semimanufactures
import android.content.BroadcastReceiver
import android.content.Context
import kotlinx.coroutines.launch
import com.example.semimanufactures.DatabaseManager.findDistributionDateByPrP
import com.example.semimanufactures.DatabaseManager.showToast
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Spinner
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
//import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

class CheckingPrPSkladSunmiActivity : ComponentActivity(), SupporterManager.IScanListener {
    private val SOFTWARE_SCANKEY = "unitech.scanservice.software_scankey"
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private lateinit var button_for_add_barcode_scan: ImageButton
    private lateinit var button_for_add_qrcode_scan: ImageButton
    private lateinit var button_for_inv_sklad: Button
    private var currentScanField: Int = 0
    private lateinit var supporterManager: SupporterManager
    private var tag: String = CheckingPrPSkladSunmiActivity::class.java.simpleName
    private lateinit var text_result_scan_sklad: TextView
    private lateinit var text_result_scan_prp: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var spinner_warehouse_names: Spinner
    private lateinit var text_info_sklad_scan: TextView
    private lateinit var go_to_logistic: ImageView
    private lateinit var text_result_add_barcode_scan_text: TextView
    private lateinit var text_result_add_qrcode_scan_text: TextView
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
    private lateinit var main_layout: ConstraintLayout
    private val rolesList: MutableList<String> = mutableListOf()
    private val mScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val bundle = intent.extras ?: return
            if (action == ACTION_RECEIVE_DATA) {
                val barcodeStr = bundle.getString("text")?.replace("\\s".toRegex(), "") ?: return
                Log.d("ScanReceiver", "Received barcode: $barcodeStr for field: $currentScanField")

                when (currentScanField) {
                    1 -> {
                        text_result_scan_prp.setText(barcodeStr.replace("\\s".toRegex(), ""))
                        Log.d("tag", "$text_result_scan_prp")
                        checkDistributionDate(barcodeStr.trim())
                    }
                    2 -> {
                        text_result_scan_sklad.setText(barcodeStr.replace("\\s".toRegex(), ""))
                        updateWarehouseInfo(barcodeStr.trim())
                    }
                    3 -> {}
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
        setContentView(R.layout.activity_new_checking_prp_sklad)
        initView()
        registerScannerReceiver()
        loadWarehouses()
        progressBar = findViewById(R.id.progressBar)
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        Log.d("tag", "${currentUserId}, ${currentUsername}, ${currentRoleCheck}, ${currentMdmCode}, ${currentFio}")
        button_for_inv_sklad.setOnClickListener {
            val barcodeValue = text_result_scan_prp.text.toString().replace("\\s".toRegex(), "")
            val qrcodeValue = text_result_scan_sklad.text.toString().replace("\\s".toRegex(), "")
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseManager.addInventoryRecord(
                    this@CheckingPrPSkladSunmiActivity,
                    barcodeValue,
                    currentUserId ?: 0,
                    qrcodeValue,
                    currentFio ?: "",
                    text_result_scan_sklad
                )
            }
        }
        spinner_warehouse_names.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedWarehouse = parent.getItemAtPosition(position) as Warehouse
                text_result_scan_sklad.text = selectedWarehouse.id
                text_info_sklad_scan.text = selectedWarehouse.name
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
        supporterManager = SupporterManager(this, this)
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
    private fun checkDistributionDate(barcodeValue: String?) {
        if (barcodeValue.isNullOrBlank() || barcodeValue.length < 4) return
        CoroutineScope(Dispatchers.Main).launch {
            val searchData = findDistributionDateByPrP(this@CheckingPrPSkladSunmiActivity, barcodeValue)
            if (searchData == null) {
                Log.e("DistributionCheck", "Нет данных для штрих-кода")
                return@launch
            }
            val distributionDate = searchData.dateDistribution
            val segment = searchData.segment
            if (distributionDate.isNullOrBlank()) {
                Log.d("DistributionCheck", "Дата распределения не найдена")
                text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
            } else {
                try {
                    val currentDate = LocalDate.now()
                    val date = LocalDate.parse(distributionDate)

                    Log.d("Сегодняшняя дата: ", "$currentDate")
                    val daysPlusSeria = fetchDaysToAddSeria(this@CheckingPrPSkladSunmiActivity) ?: 3
                    val daysPlusMezhZavod = fetchDaysToAddMezhZavod(this@CheckingPrPSkladSunmiActivity) ?: 210
                    val daysPlusOKR = fetchDaysToAddOKR(this@CheckingPrPSkladSunmiActivity) ?: 60
                    val daysPlusPosleProd = fetchDaysToAddPosleProdazhnoeObsluzhivanie(this@CheckingPrPSkladSunmiActivity) ?: 45

                    if (segment == "Серия" && date.isAfter(currentDate.plusDays(daysPlusSeria))) {
                        text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                        showToast(this@CheckingPrPSkladSunmiActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                        Log.d("DistributionCheck", "Зеленый")
                    }
                    else if (segment == "Межзавод" && date.isAfter(currentDate.plusDays(daysPlusMezhZavod))) {
                        text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                        showToast(this@CheckingPrPSkladSunmiActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                        Log.d("DistributionCheck", "Зеленый")
                    }
                    else if (segment == "ОКР" && date.isAfter(currentDate.plusDays(daysPlusOKR))) {
                        text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                        showToast(this@CheckingPrPSkladSunmiActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                        Log.d("DistributionCheck", "Зеленый")
                    }
                    else if (segment == "Послепродажное обслуживание" && date.isAfter(currentDate.plusDays(daysPlusPosleProd))) {
                        text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                        showToast(this@CheckingPrPSkladSunmiActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                        Log.d("DistributionCheck", "Зеленый")
                    }
                    else {
                        text_result_scan_prp.setBackgroundResource(R.drawable.error_prp_edit_text)
                        showToast(this@CheckingPrPSkladSunmiActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                        Log.d("DistributionCheck", "Красный")
                    }
                } catch (e: Exception) {
                    Log.e("DistributionCheck", "Ошибка при парсинге даты: ${e.message}")
                    text_result_scan_prp.setBackgroundResource(R.drawable.error_prp_edit_text)
                    Log.d("DistributionCheck", "Красный")
                }
            }

        }
    }

    private fun loadWarehouses() {
        CoroutineScope(Dispatchers.Main).launch {
            val warehouses = DatabaseManager.getAllWarehouses(this@CheckingPrPSkladSunmiActivity)
            val adapter = ArrayAdapter(this@CheckingPrPSkladSunmiActivity, android.R.layout.simple_spinner_item, warehouses)
            adapter.setDropDownViewResource(R.layout.spinner_inv_item)
            spinner_warehouse_names.adapter = adapter
        }
    }
    private fun updateWarehouseInfo(barcodeValue: String?) {
        if (barcodeValue.isNullOrBlank()) return
        CoroutineScope(Dispatchers.Main).launch {
            val warehouseName = DatabaseManager.getWarehouseNameById(this@CheckingPrPSkladSunmiActivity, barcodeValue.trim())
            if (warehouseName != null) {
                text_info_sklad_scan.text = warehouseName.toString()
                showToast(this@CheckingPrPSkladSunmiActivity, "Склад найден: $warehouseName", 5000)
            } else {
                showToast(this@CheckingPrPSkladSunmiActivity, "Склад не найден по ID: $barcodeValue", 5000)
            }
        }
    }
    @SuppressLint("MissingInflatedId")
    private fun initView() {
        button_for_add_barcode_scan = findViewById(R.id.button_for_add_barcode_scan)
        button_for_add_qrcode_scan = findViewById(R.id.button_for_add_qrcode_scan)
        text_result_scan_sklad = findViewById(R.id.text_result_scan_sklad)
        text_result_scan_prp = findViewById(R.id.text_result_scan_prp)
        text_result_add_barcode_scan_text = findViewById(R.id.text_result_add_barcode_scan_text)
        text_result_add_qrcode_scan_text = findViewById(R.id.text_result_add_qrcode_scan_text)
        text_result_scan_prp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkDistributionDate(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        button_for_add_barcode_scan.setOnClickListener {
            if (checkCameraPermission()) {
                currentScanField = 2
                callScanner(2)
            }
        }
        button_for_add_qrcode_scan.setOnClickListener {
            if (checkCameraPermission()) {
                currentScanField = 1
                callScanner(1)
            }
        }
        text_result_scan_sklad.setOnClickListener {
            currentScanField = 2
            text_result_scan_sklad.requestFocus()
            callScanner(2)
        }
        text_result_scan_prp.setOnClickListener {
            currentScanField = 1
            text_result_scan_prp.requestFocus()
            callScanner(1)
        }
        text_info_sklad_scan = findViewById(R.id.text_info_sklad_scan)
        go_to_authorization = findViewById(R.id.go_to_authorization)
        go_to_authorization.setOnClickListener {
            val intent = Intent(this@CheckingPrPSkladSunmiActivity, MainActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@CheckingPrPSkladSunmiActivity, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@CheckingPrPSkladSunmiActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@CheckingPrPSkladSunmiActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@CheckingPrPSkladSunmiActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        spinner_warehouse_names = findViewById(R.id.spinner_warehouse_names)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@CheckingPrPSkladSunmiActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        text_result_scan_sklad.addTextChangedListener(textWatcher)
        text_result_scan_prp.addTextChangedListener(textWatcher)
        button_for_inv_sklad = findViewById(R.id.button_for_inv_sklad)
        main_layout = findViewById(R.id.main_layout)
    }
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateButtonStyle()

        }
        override fun afterTextChanged(s: Editable?) {}
    }
    private fun registerScannerReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_DATA)
        registerReceiver(mScanReceiver, intentFilter)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onScannerResultChange(result: String?) {
        Log.d(tag, "Scanner result received: $result")
        val scannedText = result?.replace(Regex("[\\s\\n\\t]+"), "")?.trim() ?: ""

        if (text_result_add_qrcode_scan_text.isFocused) {
            text_result_add_qrcode_scan_text.text = scannedText
            Log.d(tag, "QR код в фокусе: ${text_result_add_qrcode_scan_text.isFocused}")
            updateWarehouseInfo(scannedText)
        } else if (text_result_scan_sklad.isFocused) {
            text_result_add_qrcode_scan_text.text = scannedText
            text_result_scan_sklad = text_result_add_qrcode_scan_text
            Log.d(tag, "Склад в фокусе: ${text_result_scan_sklad.isFocused}")
            updateWarehouseInfo(scannedText)
        } else if (text_result_scan_prp.isFocused) {
            text_result_add_barcode_scan_text.text = scannedText
            Log.d(tag, "BARCODE в фокусе: ${text_result_add_barcode_scan_text.isFocused}")
            checkDistributionDate(scannedText)
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
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        supporterManager.recycle()
    }
    private fun callScanner(currentScanField: Int) {
        Log.d(tag, "Calling scanner for field: $currentScanField")
        supporterManager.singleScan(true)
        val bundle = Bundle().apply {
            putBoolean("scan", true)
            putInt("field", currentScanField)
        }
        val mIntent = Intent().setAction(SOFTWARE_SCANKEY).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun updateButtonStyle() {
        val barcodeValue = text_result_scan_sklad.text.toString().trim().replace("\\s".toRegex(), "")
        val qrCodeValue = text_result_scan_sklad.text.toString().trim().replace("\\s".toRegex(), "")
        if (barcodeValue.isNotEmpty() && qrCodeValue.isNotEmpty()) {
            button_for_inv_sklad.setBackgroundResource(R.drawable.button_background)
        } else {
            button_for_inv_sklad.setBackgroundResource(R.drawable.button_background_add)
        }
    }
    private val REQUEST_CAMERA_PERMISSION = 1001
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
            // Разрешение предоставлено
        } else {
            Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
        }
    }
}