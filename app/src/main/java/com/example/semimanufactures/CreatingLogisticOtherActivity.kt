package com.example.semimanufactures

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class CreatingLogisticOtherActivity : ComponentActivity() {

    // user
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized: Boolean = false

    // ui
    private lateinit var go_to_logistic: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var whatObjectTv: TextView
    private lateinit var whenGoTv: TextView
    private lateinit var skladFromTv: TextView
    private lateinit var skladToTv: TextView
    private lateinit var otpravitelActv: AutoCompleteTextView
    private lateinit var priemshchikActv: AutoCompleteTextView
    private lateinit var commentSenderEt: EditText
    private lateinit var commentReceiverEt: EditText
    private lateinit var primechanieEt: EditText
    private lateinit var gruzchikIv: ImageView
    private lateinit var pogruzchikIv: ImageView
    private lateinit var saveBtn: Button

    // state
    private var allWarehouses: List<WarehouseInfoOther> = emptyList()
    private var selectedFrom: WarehouseInfoOther? = null
    private var selectedTo: WarehouseInfoOther? = null
    private var plannedDate: String? = null
    private var useGruzchik: Boolean = false
    private var usePogruzchik: Boolean = false
    private var isLoadingUnloadingChecked: Boolean = false
    private lateinit var checkboxLoadingUnloading: ImageView
    private lateinit var layoutLoadingUnloading: LinearLayout
    private lateinit var layoutSkladFrom: LinearLayout

    private val tag = "CreatingLogOther"

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
        } ?: Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()

        if (!currentIsAuthorized) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_new_logistic_other)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        bindViews()
        wireUi()
        loadWarehouses()

        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            startActivity(Intent(this, LogisticActivity::class.java))
        }
    }

    // ===== UI =====

    private fun bindViews() {
        progressBar       = findViewById(R.id.progressBar)
        whatObjectTv      = findViewById(R.id.what_object)
        whenGoTv          = findViewById(R.id.when_go)
        skladFromTv       = findViewById(R.id.sklad_otpravki)
        skladToTv         = findViewById(R.id.sklad_pribitiya)
        otpravitelActv    = findViewById(R.id.otpravitel)
        priemshchikActv   = findViewById(R.id.priemshchik)
        commentSenderEt   = findViewById(R.id.comment_otpravitelyu)
        commentReceiverEt = findViewById(R.id.comment_priemshchika)
        primechanieEt     = findViewById(R.id.primechanie)
        gruzchikIv        = findViewById(R.id.gruzchik)
        pogruzchikIv      = findViewById(R.id.pogruzchik)
        saveBtn           = findViewById(R.id.button_save_new_logistic)
        checkboxLoadingUnloading = findViewById(R.id.checkbox_loading_unloading)
        layoutLoadingUnloading = findViewById(R.id.layout_loading_unloading)
        layoutSkladFrom = findViewById(R.id.layout_sklad_from)
    }

    private fun wireUi() {
        // кастомный ввод объекта
        whatObjectTv.setOnClickListener { showWhatObjectDialog() }

        // дата/время
        whenGoTv.setOnClickListener { pickDateTime() }

        // Обработчик чекпоинта Погрузка/Разгрузка
        layoutLoadingUnloading.setOnClickListener {
            isLoadingUnloadingChecked = !isLoadingUnloadingChecked
            if (isLoadingUnloadingChecked) {
                checkboxLoadingUnloading.setImageResource(R.drawable.remember_me_svg)
                layoutSkladFrom.visibility = View.GONE
                selectedFrom = null
                skladFromTv.text = ""
                otpravitelActv.setText("")
            } else {
                checkboxLoadingUnloading.setImageResource(R.drawable.no_remember_me_svg)
                layoutSkladFrom.visibility = View.VISIBLE
            }
        }
        
        checkboxLoadingUnloading.setOnClickListener {
            layoutLoadingUnloading.performClick()
        }
        
        // выбор складов через кастомный диалог
        skladFromTv.setOnClickListener {
            if (isLoadingUnloadingChecked) return@setOnClickListener
            if (allWarehouses.isEmpty()) {
                Toast.makeText(this, "Список складов ещё загружается…", Toast.LENGTH_SHORT).show()
            } else {
                showWarehousePicker(
                    title = "Место отправления",
                    items = allWarehouses
                ) { wh ->
                    selectedFrom = wh
                    skladFromTv.text = wh.name
                    otpravitelActv.setText(wh.responsibleName.orEmpty())
                }
            }
        }
        skladToTv.setOnClickListener {
            if (allWarehouses.isEmpty()) {
                Toast.makeText(this, "Список складов ещё загружается…", Toast.LENGTH_SHORT).show()
            } else {
                showWarehousePicker(
                    title = "Место прибытия",
                    items = allWarehouses
                ) { wh ->
                    selectedTo = wh
                    skladToTv.text = wh.name
                    priemshchikActv.setText(wh.responsibleName.orEmpty())
                }
            }
        }

        // запрет ручного редактирования отправителя/получателя
        listOf(otpravitelActv, priemshchikActv).forEach { v ->
            v.isFocusable = false
            v.isFocusableInTouchMode = false
            v.isClickable = false
            v.isCursorVisible = false
            v.keyListener = null
            v.setOnLongClickListener { true }
        }

        // переключатели грузчика/погрузчика
        gruzchikIv.setOnClickListener {
            useGruzchik = !useGruzchik
            updateToggle(gruzchikIv, useGruzchik)
        }
        pogruzchikIv.setOnClickListener {
            usePogruzchik = !usePogruzchik
            updateToggle(pogruzchikIv, usePogruzchik)
        }

        saveBtn.setOnClickListener { onCreateLogistics() }
    }

    private fun updateToggle(view: ImageView, active: Boolean) {
        view.setImageResource(
            if (active) R.drawable.remember_me_svg else R.drawable.no_remember_me_svg
        )
        view.alpha = if (active) 1f else 0.35f
    }

    // ===== Custom dialogs =====

    private fun showWhatObjectDialog() {
        val ctx = ContextThemeWrapper(this, R.style.AppAlertDialogTheme)
        val root = LayoutInflater.from(ctx).inflate(R.layout.dialog_input_object, null)
        val et = root.findViewById<EditText>(R.id.et_object)
        val btnOk = root.findViewById<Button>(R.id.btn_ok)
        val btnCancel = root.findViewById<Button>(R.id.btn_cancel)
        et.setText(whatObjectTv.text?.toString() ?: "")

        val dlg = AlertDialog.Builder(ctx)
            .setView(root)
            .create()

        btnOk.setOnClickListener {
            whatObjectTv.text = et.text.toString().trim()
            dlg.dismiss()
        }
        btnCancel.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    private fun showWarehousePicker(
        title: String,
        items: List<WarehouseInfoOther>,
        onPick: (WarehouseInfoOther) -> Unit
    ) {
        val ctx = ContextThemeWrapper(this, R.style.AppAlertDialogTheme)
        val root = LayoutInflater.from(ctx).inflate(R.layout.dialog_choose_warehouse, null)
        val tvTitle = root.findViewById<TextView>(R.id.tv_title)
        val search = root.findViewById<EditText>(R.id.et_search)
        val listView = root.findViewById<ListView>(R.id.list_view)
        val btnClose = root.findViewById<ImageView>(R.id.btn_close)

        tvTitle.text = title

        var current = items
        fun setAdapter(src: List<WarehouseInfoOther>) {
            listView.adapter = ArrayAdapter(ctx, R.layout.item_warehouse_row, R.id.tv_row, src.map { it.display })
        }
        setAdapter(items)

        val dlg = AlertDialog.Builder(ctx)
            .setView(root)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            onPick(current[position])
            dlg.dismiss()
        }
        btnClose.setOnClickListener { dlg.dismiss() }
        search.addTextChangedListener { s ->
            val q = s?.toString().orEmpty()
            current = items.filter { it.display.contains(q, true) || it.id.contains(q, true) }
            setAdapter(current)
        }

        dlg.show()
    }


    // ===== Networking =====

    private fun okHttp(): OkHttpClient = (application as App).okHttpClient

    private fun loadWarehouses() {
        progressBar.visibility = View.VISIBLE
        saveBtn.isEnabled = false

        lifecycleScope.launchWhenStarted {
            val list = withContext(Dispatchers.IO) { fetchWarehousesDetailed(this@CreatingLogisticOtherActivity) }
            allWarehouses = list

            progressBar.visibility = View.GONE
            saveBtn.isEnabled = true

            if (allWarehouses.isEmpty()) {
                Snackbar.make(findViewById(R.id.main_layout), "Не удалось загрузить список складов", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun fetchWarehousesDetailed(context: Context): List<WarehouseInfoOther> =
        withContext(Dispatchers.IO) {
            val urls = listOf(
                "https://api.gkmmz.ru/api/get_all_skladi",
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_skladi"
            )

            val out = mutableListOf<WarehouseInfoOther>()
            val client = okHttp()
            var success = false

            for ((i, url) in urls.withIndex()) {
                var resp: Response? = null
                try {
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("X-Auth-Token", authToken)
                        .addHeader("X-Apig-AppCode", authTokenAPI)
                        .build()

                    resp = client.newCall(req).execute()
                    val code = resp.code
                    if (code in 200..299) {
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val o = json.getJSONObject(key)
                            val id = o.optString("id", key)
                            val name = o.optString("Наименование", "")
                            val mdmKey = o.optString("mdm_key", "")
                            val respFio = o.optString("ОтветственныйФИО", "")
                            val respMdm = o.optString("ОтветственныйMDMкод", "")
                            val shelf = o.optString("Полка", "")
                            val rack = o.optString("Стеллаж", "")
                            out += WarehouseInfoOther(
                                id = id,
                                name = name,
                                mdmKey = mdmKey,
                                responsibleName = respFio,
                                responsibleMDMCode = respMdm,
                                shelf = shelf,
                                rack = rack
                            )
                        }
                        success = true
                        break
                    } else if (code == 429 && i < urls.lastIndex) {
                        continue
                    } else {
                        Log.e(tag, "get_all_skladi: HTTP $code")
                        break
                    }
                } catch (t: Throwable) {
                    Log.e(tag, "get_all_skladi error: ${t.localizedMessage}", t)
                    break
                } finally {
                    resp?.close()
                }
            }

            if (!success) out.clear()
            out
        }

    private fun onCreateLogistics() {
        val boxId      = whatObjectTv.text?.toString()?.trim().orEmpty()
        val planned    = plannedDate.orEmpty()
        val from       = selectedFrom
        val to         = selectedTo
        
        // Если чекпоинт выбран, передаем "Погрузка/Разгрузка"
        val fromTitle  = if (isLoadingUnloadingChecked) "Погрузка/Разгрузка" else from?.name.orEmpty()
        val fromId     = if (isLoadingUnloadingChecked) "Погрузка/Разгрузка" else from?.id.orEmpty()
        val senderMdm  = if (isLoadingUnloadingChecked) "" else from?.responsibleMDMCode.orEmpty()
        
        val toTitle    = to?.name.orEmpty()
        val toId       = to?.id.orEmpty()
        val receiverMdm= to?.responsibleMDMCode.orEmpty()

        val loader = listOfNotNull(
            if (useGruzchik) "gruzchik" else null,
            if (usePogruzchik) "pogruzchik" else null
        ).joinToString(",")

        // обязательные поля
        when {
            boxId.isBlank() -> { toast("Укажите объект доставки"); whatObjectTv.performClick(); return }
            planned.isBlank() -> { toast("Укажите планируемые дату и время"); whenGoTv.performClick(); return }
            !isLoadingUnloadingChecked && from == null -> { toast("Выберите место отправления"); skladFromTv.performClick(); return }
            to == null -> { toast("Выберите место прибытия"); skladToTv.performClick(); return }
        }

        val sendComment = commentSenderEt.text?.toString()?.trim().orEmpty()
        val recvComment = commentReceiverEt.text?.toString()?.trim().orEmpty()
        val comment     = primechanieEt.text?.toString()?.trim().orEmpty()

        progressBar.visibility = View.VISIBLE
        saveBtn.isEnabled = false

        lifecycleScope.launchWhenStarted {
            val result = withContext(Dispatchers.IO) {
                createLogisticOtherOnServer(
                    createdByMdm  = currentMdmCode.orEmpty(),
                    createdByName = currentFio.orEmpty(),
                    boxId         = boxId,
                    plannedDate   = planned,
                    fromId        = fromId,
                    fromTitle     = fromTitle,
                    toId          = toId,
                    toTitle       = toTitle,
                    senderMdm     = senderMdm,
                    receiverMdm   = receiverMdm,
                    sendComment   = sendComment,
                    receiverComment = recvComment,
                    comment       = comment,
                    loader        = loader
                )
            }

            progressBar.visibility = View.GONE
            saveBtn.isEnabled = true

            if (result.first) {
                val logisticsId = result.second
                if (logisticsId != null) {
                    startActivity(
                        Intent(this@CreatingLogisticOtherActivity, DetailLogisticsActivity::class.java).apply {
                            putExtra("logistics_id", logisticsId)
                            putExtra("type", "other")
                        }
                    )
                    finish()
                } else {
                    toast("Заявка создана, но ID не получен от сервера")
                }
            } else {
                toast("Не удалось создать заявку. Повторите попытку.")
            }
        }
    }

    /**
     * @return Pair(created, id?)
     */
    private fun createLogisticOtherOnServer(
        createdByMdm: String,
        createdByName: String,
        boxId: String,
        plannedDate: String,
        fromId: String,
        fromTitle: String,
        toId: String,
        toTitle: String,
        senderMdm: String,
        receiverMdm: String,
        sendComment: String,
        receiverComment: String,
        comment: String,
        loader: String
    ): Pair<Boolean, String?> {

        val urls = listOf(
            "https://api.gkmmz.ru/api/create_logistic_other",
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_logistic_other"
        )

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("created_by", createdByMdm)
            .addFormDataPart("type", "other")
            .addFormDataPart("box_id", boxId)
            .addFormDataPart("version_name", version_name)
            .addFormDataPart("mdm_code", createdByMdm)
            .addFormDataPart("planned_date", plannedDate)
            .addFormDataPart("send_from_title", fromTitle)
            .addFormDataPart("send_to_title", toTitle)
            .addFormDataPart("send_from", fromId)
            .addFormDataPart("send_to", toId)
            .addFormDataPart("created_by_name", createdByName)
            .addFormDataPart("send_comment", sendComment)
            .addFormDataPart("receive_comment", receiverComment)
            .addFormDataPart("comment", comment)
            .addFormDataPart("sender_mdm", senderMdm)
            .addFormDataPart("receiver_mdm", receiverMdm)
            .apply { if (loader.isNotBlank()) addFormDataPart("loader", loader) }
            .build()

        val client = okHttp()
        var created = false
        var newId: String? = null

        for ((i, url) in urls.withIndex()) {
            var resp: Response? = null
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(form)
                    .addHeader("X-Auth-Token", authToken)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .build()

                resp = client.newCall(req).execute()
                val code = resp.code
                val body = resp.body?.string().orEmpty().trim()
                Log.d(tag, "create_logistic_other: HTTP $code\n$body")

                if (code in 200..299) {
                    created = true
                    newId = tryExtractId(body)
                    break
                } else if (code == 429 && i < urls.lastIndex) {
                    continue
                } else {
                    created = false
                    break
                }
            } catch (t: Throwable) {
                Log.e(tag, "create_logistic_other error: ${t.localizedMessage}", t)
                created = false
                break
            } finally {
                resp?.close()
            }
        }
        return created to newId
    }

    private fun tryExtractId(raw: String): String? {
        try {
            val json = JSONObject(raw)
            if (json.has("data")) {
                val data = json.get("data")
                if (data is JSONObject) {
                    if (data.has("id")) return data.optString("id").ifBlank { null }
                    if (data.has("logistics_id")) return data.optString("logistics_id").ifBlank { null }
                }
            }
            if (json.has("id")) return json.optString("id").ifBlank { null }
            if (json.has("logistics_id")) return json.optString("logistics_id").ifBlank { null }
        } catch (_: Exception) { /* not json */ }
        return raw.toLongOrNull()?.toString()
    }

    // ===== Pickers with color =====

    private fun pickDateTime() {
        val cal = Calendar.getInstance()

        val dpCtx = ContextThemeWrapper(this, R.style.AppDatePickerDialogTheme)
        val dpd = DatePickerDialog(
            dpCtx,
            { _, y, m, d ->
                val tpCtx = ContextThemeWrapper(this, R.style.AppTimePickerDialogTheme)
                val tpd = TimePickerDialog(
                    tpCtx,
                    { _, hh, mm ->
                        val c = Calendar.getInstance().apply {
                            set(Calendar.YEAR, y)
                            set(Calendar.MONTH, m)
                            set(Calendar.DAY_OF_MONTH, d)
                            set(Calendar.HOUR_OF_DAY, hh)
                            set(Calendar.MINUTE, mm)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        plannedDate = fmt.format(c.time)
                        whenGoTv.text = plannedDate
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                )

                // Подкрашиваем кнопки и (по возможности) шапку TimePicker
                tpd.setOnShowListener {
                    val blue = Color.parseColor("#001D64")
                    val white = Color.WHITE
                    tpd.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(blue)
                    tpd.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(blue)

                    try {
                        val root = tpd.window?.decorView
                        if (root is ViewGroup) {
                            // кандидаты на id хедера у разных прошивок
                            val names = listOf("time_header", "timePickerHeader", "time_picker_header")
                            val res = Resources.getSystem()
                            var header: View? = null
                            for (n in names) {
                                val id = res.getIdentifier(n, "id", "android")
                                if (id != 0) {
                                    header = root.findViewById(id)
                                    if (header != null) break
                                }
                            }
                            header?.let { h ->
                                h.setBackgroundColor(blue)
                                tintAllTexts(h, white)
                            }
                        }
                    } catch (_: Throwable) { /* тихо игнорим */ }
                }
                tpd.show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        // Подкрашиваем кнопки и шапку DatePicker (день недели и число)
        dpd.setOnShowListener {
            val blue = Color.parseColor("#001D64")
            val white = Color.WHITE
            dpd.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(blue)
            dpd.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(blue)

            try {
                val res = Resources.getSystem()
                val dp = dpd.datePicker

                // 1) пробуем найти контейнер хедера и покрасить всё внутри
                val headerContainerNames = listOf(
                    "date_picker_header",        // AOSP
                    "date_picker_header_view",   // некоторые прошивки
                    "day_picker_selector_layout" // старые прошивки
                )
                var header: View? = null
                for (n in headerContainerNames) {
                    val id = res.getIdentifier(n, "id", "android")
                    if (id != 0) {
                        header = dp.findViewById(id)
                        if (header != null) break
                    }
                }
                header?.let { h ->
                    h.setBackgroundColor(blue)
                    tintAllTexts(h, white) // делаем текст в шапке белым
                }

                // 2) на всякий случай — прямые айди для текста даты/года
                val textIds = listOf("date_picker_header_year", "date_picker_header_date")
                for (n in textIds) {
                    val id = res.getIdentifier(n, "id", "android")
                    (dp.findViewById<TextView>(id))?.setTextColor(white)
                }
            } catch (_: Throwable) { /* ок, пропускаем */ }
        }
        dpd.show()
    }

    /** Рекурсивно красит все TextView внутри view. */
    private fun tintAllTexts(view: View, color: Int) {
        when (view) {
            is TextView -> view.setTextColor(color)
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    tintAllTexts(view.getChildAt(i), color)
                }
            }
        }
    }


    // ===== Utils =====

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun readUserData(): UserData? {
        return try {
            openFileInput("user_data").use {
                val json = it.bufferedReader().use { r -> r.readText() }
                Gson().fromJson(json, UserData::class.java)
            }
        } catch (e: Exception) {
            Log.e(tag, "readUserData error", e)
            null
        }
    }

    // (оставил на случай если в проекте используется)
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
}

data class WarehouseInfoOther(
    val id: String,
    val name: String,
    val mdmKey: String?,
    val responsibleName: String?,
    val responsibleMDMCode: String?,
    val shelf: String? = null,
    val rack: String? = null
) {
    val display: String get() = listOfNotNull(name, rack, shelf).filter { it.isNotBlank() }.joinToString(" • ")
}