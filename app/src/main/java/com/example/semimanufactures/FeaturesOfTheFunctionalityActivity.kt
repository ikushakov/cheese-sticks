package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.DatabaseManager.fetchData
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.findCardByIdOrPrp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.semimanufactures.PM84ScannerManager

class FeaturesOfTheFunctionalityActivity : AppCompatActivity(), SupporterManager.IScanListener {
    private var tag: String = FeaturesOfTheFunctionalityActivity::class.java.simpleName
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val text_result_scan: TextView by lazy { findViewById(R.id.text_result_scan) }
    //private val button_search: ImageButton by lazy { findViewById(R.id.button_search) }
    private val button_scan: ImageButton by lazy { findViewById(R.id.button_scan) }
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardAdapter: CardAdapter
    private var cardItemList: MutableList<CardItem> = mutableListOf()
    private lateinit var data_user_info: ImageView
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private lateinit var progressBar: ProgressBar
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private var supporterManager: SupporterManager? = null
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private var deviceInfo: String = ""
    private lateinit var main_layout: LinearLayout
    //
    private val rolesList: MutableList<String> = mutableListOf()
    //
    private var pm84ScannerManager: PM84ScannerManager? = null
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //
        val sharedPreferences = getSharedPreferences ("myPrefs" , MODE_PRIVATE)
        val username = sharedPreferences.getString ("username", "") ?: ""
        val userId = sharedPreferences.getInt   ("userId", 0 ) ?: 0
        val mdmCode = sharedPreferences.getString ("mdmCode", "") ?: ""
        val fio =  sharedPreferences.getString ("fio", "") ?: ""
        val roleCheck = sharedPreferences.getString( "roleCheck","") ?: ""
        val deviceInfo = sharedPreferences.getString("deviceInfo", "") ?: ""
        val rolesString = sharedPreferences.getString("rolesString", "") ?: ""
        val isAuthorized = sharedPreferences.getBoolean("isAuthorized", false)
        if (!isAuthorized) {
            val intent = Intent(this@FeaturesOfTheFunctionalityActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        //
        setContentView(R.layout.activity_new_features_of_the_functionality)
        supportActionBar?.hide()
        progressBar = findViewById(R.id.progressBar)
        data_user_info = findViewById(R.id.data_user_info)
//        go_to_authorization = findViewById(R.id.go_to_authorization)
        go_to_add = findViewById(R.id.go_to_add)
        go_to_issue = findViewById(R.id.go_to_issue)
//        go_to_authorization.setOnClickListener {
//            startActivity(Intent(this, MainActivity::class.java))
//        }
        go_to_issue.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне выдачи и поиска", Toast.LENGTH_LONG).show()
        }
//        val intent = intent
//        username = intent.getStringExtra("username") ?: ""
//        roleCheck = intent.getStringExtra("roleCheck") ?: ""
//        userId = intent.getIntExtra("userId", 0)
//        mdmCode = intent.getStringExtra("mdmCode") ?: ""
//        fio = intent.getStringExtra("fio") ?: ""
//        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        //
        if (rolesString != null) {
            rolesList.addAll(rolesString.split(",").map { it.trim() })
        }
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        //
        if (deviceInfo == "EA630") {
            registerScannerReceiver()
            recyclerView = findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            cardAdapter = CardAdapter(cardItemList, this, 7)
            recyclerView.adapter = cardAdapter
            Log.d(tag, "$deviceInfo for EA630")
        } else if (deviceInfo == "PM84") {
            recyclerView = findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            cardAdapter = CardAdapter(cardItemList, this, 7)
            recyclerView.adapter = cardAdapter
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
                pm84ScannerManager?.startScanning(text_result_scan)
            }
        }
        else {
            supporterManager = SupporterManager(this, this)
            recyclerView = findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            cardAdapter = CardAdapter(cardItemList, this, 7)
            recyclerView.adapter = cardAdapter
            Log.d(tag, "$deviceInfo for Sunmi")
        }
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            DatabaseManager.connect(this@FeaturesOfTheFunctionalityActivity)
            if (!DatabaseManager.isConnected()) {
                Log.e(tag, "Database connection is not initialized")
                return@launch
            }
            fetchDataFromDatabase()
        }
        data_user_info.setOnClickListener {
            val intent = Intent(this@FeaturesOfTheFunctionalityActivity, SettingsActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                //
                putExtra("rolesString", rolesString)
                //
            }
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener { showPopupMenuNotification(it) }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@FeaturesOfTheFunctionalityActivity, LogisticActivity::class.java).apply {
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
        go_to_add.setOnClickListener {
            val intent = Intent(this@FeaturesOfTheFunctionalityActivity, AddActivity::class.java).apply {
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
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@FeaturesOfTheFunctionalityActivity)
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
                        Toast.makeText(this@FeaturesOfTheFunctionalityActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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
    fun handleCardClick(cardItem: CardItem) {
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
                    val fio = fio
                    val dateDistribution = card.dateOfDistribution
                    val demand = card.demand
                    Log.d(tag, "Adding to skladi_data with primaryDemandId: $primaryDemandId and userId: $userId")
                    Toast.makeText(this@FeaturesOfTheFunctionalityActivity, "Выдана ПрП с $primaryDemandId сотрудником $userId", Toast.LENGTH_LONG).show()
                    withContext(Dispatchers.IO) {
                        DatabaseManager.addToSkladiData(this@FeaturesOfTheFunctionalityActivity, primaryDemandId, userId, fio, dateDistribution, demand)
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
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_DATA)
        intentFilter.addAction(ACTION_RECEIVE_DATABYTES)
        intentFilter.addAction(ACTION_RECEIVE_DATALENGTH)
        intentFilter.addAction(ACTION_RECEIVE_DATATYPE)
        registerReceiver(mScanReceiver, intentFilter)
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
        if (deviceInfo == "EA630") {
            closeScanService()
            unregisterReceiver(mScanReceiver)
        } else if (deviceInfo == "PM84") {
            pm84ScannerManager?.unregisterScannerReceiver()
        }
        else {
            supporterManager?.recycle()
        }
        Log.v(tag, "onDestroy()")
    }
    private fun disableUI() {
        text_result_scan.isEnabled = false
        //button_search.isEnabled = false
        button_scan.isEnabled = false
        recyclerView.isEnabled = false
        data_user_info.isEnabled = false
//        go_to_authorization.isEnabled = false
        go_to_add.isEnabled = false
        go_to_issue.isEnabled = false
        go_to_send_notification.isEnabled = false
        go_to_logistic.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
}