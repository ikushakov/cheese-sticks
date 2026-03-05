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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
//import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventarisationFirsovActivity : ComponentActivity() {
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var text_result_scan_prp: EditText
    private lateinit var button_for_add_scan: ImageButton
    private var tag: String = InventarisationFirsovActivity::class.java.simpleName
    private val SCANNER_INIT = "unitech.scanservice.init"
    private val SCAN2KEY_SETTING = "unitech.scanservice.scan2key_setting"
    private val START_SCANSERVICE = "unitech.scanservice.start"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val SOFTWARE_SCANKEY = "unitech.scanservice.software_scankey"
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
    private val REQUEST_CAMERA_PERMISSION = 1001
    private lateinit var supporterManager: SupporterManager
    private lateinit var main_layout: ConstraintLayout
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
    private lateinit var text_info_prp_user: TextView
    private lateinit var text_info_prp_place: TextView
    private lateinit var text_info_prp_date: TextView
    private var pm84ScannerManager: PM84ScannerManager? = null
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
        setContentView(R.layout.activity_new_inventarization_firsov)
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val addIntent = Intent(this@InventarisationFirsovActivity, AddActivity::class.java)
            startActivity(addIntent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@InventarisationFirsovActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@InventarisationFirsovActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@InventarisationFirsovActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@InventarisationFirsovActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        text_result_scan_prp = findViewById(R.id.text_result_scan_prp)
        button_for_add_scan = findViewById(R.id.button_for_add_scan)
        text_info_prp_user = findViewById(R.id.text_info_prp_user)
        text_info_prp_place = findViewById(R.id.text_info_prp_place)
        text_info_prp_date = findViewById(R.id.text_info_prp_date)
        text_result_scan_prp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Этот метод вызывается до изменения текста
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Этот метод вызывается во время изменения текста
            }

            override fun afterTextChanged(s: Editable?) {
                // Этот метод вызывается после изменения текста
                val inputText = s.toString().trim()
                if (inputText.length > 5) {
                    fetchDataFromDatabase(inputText)
                }
            }
        })
        initView()
        registerScannerReceiver()
        main_layout = findViewById(R.id.main_layout)
//        CoroutineScope(Dispatchers.Main).launch {
//            Log.d("MainActivity", "Запуск получения версии...")
//            val versionMobile = fetchMobileVersion(this@InventarisationFirsovActivity)
//            Log.d("MainActivity", "Версия получена: $versionMobile")
//
//            if (versionMobile == null) {
//                Log.e("MainActivity", "Не удалось получить версию")
//                //disableUI()
//            } else {
//                Log.d("MainActivity", "Версия приложения: $versionMobile")
//                if (versionMobile.toInt() != myGlobalVariable) {
//                    Log.e("MainActivity", "Версии не совпадают. Доступ к функционалу отключен.")
//                    disableUI()
//                    main_layout.setOnClickListener {
//                        Toast.makeText(this@InventarisationFirsovActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//        }
        if (currentDeviceInfo == "PM84") {
            pm84ScannerManager = PM84ScannerManager.getInstance(applicationContext)
            pm84ScannerManager?.registerScannerReceiver()
            pm84ScannerManager?.setOnScanResultListener(object : PM84ScannerManager.OnScanResultListener {
                override fun onScanResultReceived(result: String) {
                    handleScanResult(result.trim())
                }
            })
            button_for_add_scan.setOnClickListener {
                if (checkCameraPermission()) {
                    pm84ScannerManager?.startScanning(text_result_scan_prp)
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
            button_for_add_scan.setOnClickListener {
                supporterManager?.singleScan(true)
            }
        }
    }
    private fun handleScanResult(result: String?) {
        result?.let {
            text_result_scan_prp.setText(it)
            if (it.length > 5) {
                fetchDataFromDatabase(it)
            }
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
//    @SuppressLint("MissingInflatedId")
//    private fun showPopupMenuNotification(view: View) {
//        val popupView = layoutInflater.inflate(R.layout.custom_menu_notification, null)
//        val popupWindow = PopupWindow(popupView, 550, 450)
//        popupView.findViewById<LinearLayout>(R.id.item_write_sms).setOnClickListener {
//            val intent = Intent(this, CreateNotificationActivity::class.java)
//            startActivity(intent)
//            popupWindow.dismiss()
//        }
//        popupView.findViewById<LinearLayout>(R.id.item_incoming_sms).setOnClickListener {
//            Toast.makeText(this, "Входящие нажаты", Toast.LENGTH_SHORT).show()
//            popupWindow.dismiss()
//        }
//        popupView.findViewById<LinearLayout>(R.id.item_sent_sms).setOnClickListener {
//            Toast.makeText(this, "Отправленные нажаты", Toast.LENGTH_SHORT).show()
//            popupWindow.dismiss()
//        }
//        popupWindow.isFocusable = true
//        popupWindow.showAsDropDown(view)
//    }

    private fun initView(){
        button_for_add_scan.setOnClickListener {
            if (checkCameraPermission()) {
                callScanner()
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
        closeScanService()
        unregisterReceiver(mScanReceiver)
    }

    private fun callScanner() {
        Log.v(tag, "callScanner()")
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
                    val barcodeStr = bundle.getString("text")
                    Log.v(tag, "barcode data: $barcodeStr")
                    text_result_scan_prp.setText(barcodeStr?.replace(" ", "")?.trim() ?: "")
                    val prpValue = text_result_scan_prp.text.toString().trim()
                    if (prpValue.isNotBlank()) {
                        fetchDataFromDatabase(prpValue)
                    }
                }
            }
        }
    }

    private fun fetchDataFromDatabase(prp: String) {
        lifecycleScope.launch {
            val result = DatabaseManager.fetchInfoPrp(this@InventarisationFirsovActivity, prp)
            if (result != null) {
                // Разделение строки на три части
                val lines = result.split("\n")
                if (lines.size >= 6) {
                    val userFio = lines[1]
                    val naimenovanie = lines[3]
                    val formattedDate = lines[5]

                    // Вывод информации в отдельные поля
                    text_info_prp_user.text = userFio
                    text_info_prp_place.text = naimenovanie
                    text_info_prp_date.text = formattedDate
                } else {
                    text_info_prp_user.text = "Ошибка инвентаризации"
                    text_info_prp_place.text = "Ошибка инвентаризации"
                    text_info_prp_date.text = "Ошибка инвентаризации"
                }
            } else {
                text_info_prp_user.text = "Данная ПрП не проинвентаризирована"
                text_info_prp_place.text = "Данная ПрП не проинвентаризирована"
                text_info_prp_date.text = "Данная ПрП не проинвентаризирована"
            }
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
            pm84ScannerManager?.startScanning(text_result_scan_prp)
        } else {
            Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun disableUI() {
//        text_result_scan_prp.isEnabled = false
//        button_for_add_scan.isEnabled = false
//        go_to_add.isEnabled = false
//        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
//    }
}