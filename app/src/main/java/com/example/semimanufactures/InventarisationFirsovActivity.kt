package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventarisationFirsovActivity : ComponentActivity() {
    private lateinit var go_to_add: ImageView
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var text_result_scan_prp: EditText
    private lateinit var button_for_add_scan: ImageButton
    private lateinit var text_info_prp: TextView
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
    private var deviceInfo: String = ""
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private lateinit var main_layout: ConstraintLayout
    private val rolesList: MutableList<String> = mutableListOf()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_inventarization_firsov)
        val intent = intent
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val addIntent = Intent(this@InventarisationFirsovActivity, AddActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(addIntent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@InventarisationFirsovActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
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
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@InventarisationFirsovActivity, LogisticActivity::class.java).apply {
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
            val intent = Intent(this@InventarisationFirsovActivity, SettingsActivity::class.java).apply {
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
//        go_to_authorization = findViewById(R.id.go_to_authorization)
//        go_to_authorization.setOnClickListener {
//            val intent = Intent(this@InventarisationFirsovActivity, MainActivity::class.java)
//            startActivity(intent)
//        }
        text_result_scan_prp = findViewById(R.id.text_result_scan_prp)
        button_for_add_scan = findViewById(R.id.button_for_add_scan)
        text_info_prp = findViewById(R.id.text_info_prp)
        initView()
        registerScannerReceiver()
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@InventarisationFirsovActivity)
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
                        Toast.makeText(this@InventarisationFirsovActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
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

    private fun initView(){
        button_for_add_scan.setOnClickListener {
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
                text_info_prp.text = result
            } else {
                text_info_prp.text = "Данная ПрП не проинвентаризирована"
            }
        }
    }
    private fun disableUI() {
        text_result_scan_prp.isEnabled = false
        button_for_add_scan.isEnabled = false
        text_info_prp.isEnabled = false
        go_to_add.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
}