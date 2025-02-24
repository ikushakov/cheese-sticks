package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddMezhZavod
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddOKR
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddPosleProdazhnoeObsluzhivanie
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddSeria
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.findDistributionDateByPrP
import com.example.semimanufactures.DatabaseManager.getWarehouseNameById
import com.example.semimanufactures.DatabaseManager.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var userFio: String = ""
    private lateinit var progressBar: ProgressBar
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var text_result_add_barcode_scan_text: TextView
    private lateinit var text_result_add_qrcode_scan_text: TextView
    private lateinit var go_to_logistic: ImageView
    private lateinit var button_to_inv: Button
    private lateinit var button_check: Button
    private var deviceInfo: String = ""
    private var currentScanField: Int = 0
    private var isBarcodeStyled: Boolean = false
    private var isQRCodeActive: Boolean = false
    private lateinit var supporterManager: SupporterManager
    private lateinit var main_layout: ConstraintLayout
    private val rolesList: MutableList<String> = mutableListOf()
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
        setContentView(R.layout.activity_new_add)
        initView()
        val intent = intent
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        userFio = intent.getStringExtra("fio") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        Log.d(tag, "User id: $userId, Username: $username, Role: $roleCheck, MDM Code: $mdmCode")
        if (deviceInfo == "EA630") {
            registerScannerReceiver()
            progressBar = findViewById(R.id.progressBar)
        } else {
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
        setupButtonListeners()
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@AddActivity, SettingsActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", userFio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
//        go_to_authorization = findViewById(R.id.go_to_authorization)
//        go_to_authorization.setOnClickListener {
//            val intent = Intent(this@AddActivity, MainActivity::class.java)
//            startActivity(intent)
//        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне для добавления ПрП на склад", Toast.LENGTH_LONG).show()
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@AddActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", userFio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@AddActivity, LogisticActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", userFio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            showPopupMenuNotification(it)
        }
        button_to_inv = findViewById(R.id.button_to_inv)
        button_to_inv.setOnClickListener {
            if (deviceInfo == "EA630"){
                val intent = Intent(this, CheckingPrPSkladActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("username", username)
                    putExtra("roleCheck", roleCheck)
                    putExtra("mdmCode", mdmCode)
                    putExtra("fio", userFio)
                    putExtra("deviceInfo", deviceInfo)
                    putExtra("rolesString", rolesString)
                }
                startActivity(intent)
            } else if (deviceInfo == "L2H-N") {
                val intent = Intent(this, CheckingPrPSkladSunmiActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("username", username)
                    putExtra("roleCheck", roleCheck)
                    putExtra("mdmCode", mdmCode)
                    putExtra("fio", userFio)
                    putExtra("deviceInfo", deviceInfo)
                    putExtra("rolesString", rolesString)
                }
                startActivity(intent)
            }
            else {
                Toast.makeText(this, "Предусмотрено только для устройств Unitech EA630 \uD83D\uDE22", Toast.LENGTH_LONG).show()
            }
        }
        button_check = findViewById(R.id.button_check)
        button_check.setOnClickListener {
            if (deviceInfo == "EA630"){
                val intent = Intent(this, InventarisationFirsovActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("username", username)
                    putExtra("roleCheck", roleCheck)
                    putExtra("mdmCode", mdmCode)
                    putExtra("fio", userFio)
                    putExtra("deviceInfo", deviceInfo)
                    putExtra("rolesString", rolesString)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Предусмотрено только для устройств Unitech EA630 \uD83D\uDE22", Toast.LENGTH_LONG).show()
            }
        }
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("AddActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@AddActivity)
            Log.d("AddActivity", "Версия получена: $versionMobile")

            if (versionMobile == null) {
                Log.e("AddActivity", "Не удалось получить версию")
                //disableUI()
            } else {
                Log.d("AddActivity", "Версия приложения: $versionMobile")
                // Сравнение версий и отключение UI, если они не совпадают
                if (versionMobile.toInt() != myGlobalVariable) {
                    Log.e("AddActivity", "Версии не совпадают. Доступ к функционалу отключен.")
                    disableUI()
                    // Устанавливаем обработчик нажатия на Layout
                    main_layout.setOnClickListener {
                        // Код, который будет выполняться при нажатии на Layout
                        Toast.makeText(this@AddActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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
                putExtra("fio", userFio)
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
            if (deviceInfo == "EA630") {
                currentScanField = 1
                isBarcodeStyled = true
                isQRCodeActive = false
                callScannerEA630()
            } else {
                callScannerSunmi(1)
            }
        }
        button_for_add_qrcode_scan.setOnClickListener {
            if (deviceInfo == "EA630") {
                currentScanField = 2
                isQRCodeActive = true
                isBarcodeStyled = false
                callScannerEA630()
            } else {
                callScannerSunmi(2)
            }
        }
        button_for_add_on_sklad.setOnClickListener {
            val barcodeValue = if (deviceInfo == "EA630") {
                text_result_add_barcode_scan.text.toString().replace("\\s".toRegex(), "")
            } else {
                text_result_add_barcode_scan_text.text.toString()
            }
            val qrcodeValue = if (deviceInfo == "EA630") {
                text_result_add_qrcode_scan.text.toString().replace("\\s".toRegex(), "")
            } else {
                text_result_add_qrcode_scan_text.text.toString()
            }
            if (qrcodeValue.isBlank()) {
                CoroutineScope(Dispatchers.Main).launch {
                    showToast(this@AddActivity, "Поле id склада не может быть пустым", 5000)
                }
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            button_for_add_on_sklad.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                val warehouseName = getWarehouseNameById(this@AddActivity, qrcodeValue)
                if (warehouseName != null) {
                    showToast(this@AddActivity, "Склад: $warehouseName", 7000)
                    DatabaseManager.addToSkladiDataPrP(
                        this@AddActivity,
                        barcodeValue,
                        userId,
                        qrcodeValue,
                        userFio,
                        text_result_add_barcode_scan
                    )
                    showToast(this@AddActivity, "Добавлен на склад $warehouseName c id $qrcodeValue", 7000)
                } else {
                    showToast(this@AddActivity, "Склад с ID $qrcodeValue не найден", 7000)
                }
                progressBar.visibility = View.GONE
                button_for_add_on_sklad.isEnabled = true
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
            if (isBarcodeStyled) {
                checkDistributionDate(s.toString().trim())
            }
        }
        override fun afterTextChanged(s: Editable?) {}
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDistributionDate(barcodeValue: String?) {
        if (barcodeValue.isNullOrBlank() || barcodeValue.length < 4) return
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
                        val daysPlusMezhZavod = fetchDaysToAddMezhZavod(this@AddActivity) ?: 90
                        val daysPlusOKR = fetchDaysToAddOKR(this@AddActivity) ?: 45
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
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_DATA)
        intentFilter.addAction(ACTION_RECEIVE_DATABYTES)
        intentFilter.addAction(ACTION_RECEIVE_DATALENGTH)
        intentFilter.addAction(ACTION_RECEIVE_DATATYPE)
        registerReceiver(mScanReceiver, intentFilter)
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
    private fun showPopupMenu(view: View) {
        val popupWindow = PopupWindow(this)
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.custom_popup_menu_layout, null)
        val userIdTextView = layout.findViewById<TextView>(R.id.user_id_text)
        userIdTextView.text = "User ID: $userId"
        val usernameTextView = layout.findViewById<TextView>(R.id.username_text)
        usernameTextView.text = "Username: $username"
        val roleCheckTextView = layout.findViewById<TextView>(R.id.role_check_text)
        roleCheckTextView.text = "Role: $roleCheck"
        popupWindow.contentView = layout
        popupWindow.width = 550
        popupWindow.height = 350
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.showAsDropDown(view)
    }
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        text_result_add_barcode_scan.isEnabled = false
        button_for_add_barcode_scan.isEnabled = false
        text_result_add_qrcode_scan.isEnabled = false
        button_for_add_qrcode_scan.isEnabled = false
//        go_to_authorization.isEnabled = false
        button_for_add_on_sklad.isEnabled = false
        go_to_issue.isEnabled = false
        button_to_inv.isEnabled = false
        button_check.isEnabled = false
        go_to_send_notification.isEnabled = false
        data_user_info.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        if (deviceInfo == "EA630") {
            closeScanService()
            unregisterReceiver(mScanReceiver)
        } else {
            supporterManager.recycle()
        }
    }
}