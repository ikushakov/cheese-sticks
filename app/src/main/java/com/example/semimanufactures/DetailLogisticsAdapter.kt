package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.DatabaseManager.getAllSotrudnikiInfo
import com.example.semimanufactures.service_mode.ServiceModeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DetailLogisticsAdapter(private val items: MutableList<LogisticsItem>,
                             private var currentUsername: String,
                             private var currentUserId: Int,
                             private var currentRoleCheck: String,
                             private var currentMdmCode: String,
                             private var currentFio: String,
                             private var currentDeviceInfo: String,
                             private var currentRolesString: String,
                             private var currentDeviceToken: String,
                             private var currentIsAuthorized:  Boolean,
                             private val onStatusClick: (LogisticsItem) -> Unit) : RecyclerView.Adapter<DetailLogisticsAdapter.DetailLogisticsViewHolder>() {
    val statusLoadingStates = mutableMapOf<String, Boolean>()
    class DetailLogisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val id: TextView = itemView.findViewById(R.id.id)
        val send_from_title: TextView = itemView.findViewById(R.id.send_from_title)
        val send_to_title: TextView = itemView.findViewById(R.id.send_to_title)
        val type: TextView = itemView.findViewById(R.id.type)
        val comment: EditText = itemView.findViewById(R.id.comment)
        val send_comment: TextView = itemView.findViewById(R.id.send_comment)
        val receive_comment: TextView = itemView.findViewById(R.id.receive_comment)
        val sender_phone: TextView = itemView.findViewById(R.id.sender_phone)
        val receiver_phone: TextView = itemView.findViewById(R.id.receiver_phone)
        val status: TextView = itemView.findViewById(R.id.status)
        val sender_name: TextView = itemView.findViewById(R.id.sender_name)
        val receiver_name: TextView = itemView.findViewById(R.id.receiver_name)
        val sklad_to_address: TextView = itemView.findViewById(R.id.sklad_to_address)
        val sklad_from_address: TextView = itemView.findViewById(R.id.sklad_from_address)
        val sklad_to_resp_name: TextView = itemView.findViewById(R.id.sklad_to_resp_name)
        val sklad_from_resp_name: TextView = itemView.findViewById(R.id.sklad_from_resp_name)
        val demand: TextView = itemView.findViewById(R.id.object_logistic)
        val docsRecyclerView: RecyclerView = itemView.findViewById(R.id.docs_recycler_view)
        val take_photo_button: Button = itemView.findViewById(R.id.take_photo_button)
        val planned_date: TextView = itemView.findViewById(R.id.planned_date)
        val loader_gruzchik: ImageView = itemView.findViewById(R.id.loader_gruzchik)
        val loader_pogruzchik: ImageView = itemView.findViewById(R.id.loader_pogruzchik)
        val layout_address_sklada_otkuda: LinearLayout = itemView.findViewById(R.id.layout_address_sklada_otkuda)
        val layout_address_sklada_kuda: LinearLayout = itemView.findViewById(R.id.layout_address_sklada_kuda)
        val layout_responsible_otkuda: LinearLayout = itemView.findViewById(R.id.layout_responsible_otkuda)
        val layout_responsible_kuda: LinearLayout = itemView.findViewById(R.id.layout_responsible_kuda)
        val layout_comment_sender: LinearLayout = itemView.findViewById(R.id.layout_comment_sender)
        val layout_comment_receiver: LinearLayout = itemView.findViewById(R.id.layout_comment_receiver)
        val layout_gruzchik_pogruzchik: LinearLayout = itemView.findViewById(R.id.layout_gruzchik_pogruzchik)
        val layout_phone_sender: LinearLayout = itemView.findViewById(R.id.layout_phone_sender)
        val layout_phone_receiver: LinearLayout = itemView.findViewById(R.id.layout_phone_receiver)
        val layout_sender: LinearLayout = itemView.findViewById(R.id.layout_sender)
        val layout_receiver: LinearLayout = itemView.findViewById(R.id.layout_receiver)
        var handler: Handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        val layout_kuda: LinearLayout = itemView.findViewById(R.id.layout_kuda)
        val layout_otkuda: LinearLayout = itemView.findViewById(R.id.layout_otkuda)
        val icon_sender_phone: ImageView = itemView.findViewById(R.id.icon_sender_phone)
        val icon_receiver_phone: ImageView = itemView.findViewById(R.id.icon_receiver_phone)
        val executor: TextView = itemView.findViewById(R.id.executor)
        val statusProgress: ProgressBar = itemView.findViewById(R.id.status_progress)
        val openMapButton: Button = itemView.findViewById(R.id.open_map_button)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailLogisticsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail_logistics, parent, false)
        return DetailLogisticsViewHolder(view)
    }
    override fun onBindViewHolder(holder: DetailLogisticsViewHolder, position: Int) {
        val item = items[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        var createdAtMillis: Long = 0
        try {
            createdAtMillis = dateFormat.parse(item.created_at)?.time ?: 0
        } catch (e: ParseException) {
            Log.e("DetailLogisticsAdapter", "Ошибка при парсинге даты", e)
        }
        holder.runnable?.let { holder.handler.removeCallbacks(it) }
        if (createdAtMillis > 0 && item.status !in listOf("-1", "4")) {
            val startTimeMillis = createdAtMillis
            holder.runnable = object : Runnable {
                override fun run() {
                    val pos = holder.bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) {
                        holder.handler.removeCallbacks(this)
                        return
                    }
                    val currentItem = items[pos]
                    val nowMillis = System.currentTimeMillis()
                    var elapsedMillis = nowMillis - startTimeMillis
                    if (elapsedMillis < 0) elapsedMillis = 0
                    val formattedTime = formatTimeAtWork(elapsedMillis)
                    val fullText = "Доставка №${currentItem.id} $formattedTime\nот ${currentItem.created_at}\n${currentItem.creator}"
                    val spannableString = SpannableString(fullText)
                    val timeStart = fullText.indexOf(formattedTime)
                    if (timeStart >= 0) {
                        spannableString.setSpan(
                            ForegroundColorSpan(Color.RED),
                            timeStart,
                            timeStart + formattedTime.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    holder.id.text = spannableString
                    holder.handler.postDelayed(this, 1000)
                }
            }
            holder.handler.post(holder.runnable!!)
        } else {
            holder.id.text = "Доставка №${item.id}\nот ${item.created_at}\n${item.creator}"
            holder.id.setTextColor(Color.BLACK)
        }
        holder.send_from_title.text = item.send_from_title
        if (item.send_from_title?.isEmpty() ?: true || item.send_from_title == "null") {
            holder.layout_kuda.visibility = View.GONE
        } else {
            holder.layout_kuda.visibility = View.VISIBLE
        }
        holder.send_to_title.text = item.send_to_title
        if (item.send_to_title?.isEmpty() ?: true || item.send_to_title == "null") {
            holder.layout_otkuda.visibility = View.GONE
        } else {
            holder.layout_otkuda.visibility = View.VISIBLE
        }
        holder.type.text = when (item.type) {
            "prp" -> "ПрП"
            "stanok" -> "Оборудование"
            "other" -> "Прочее"
            "doc" -> "Документ"
            else -> "Неизвестный тип"
        }
        holder.comment.setText(item.comment)
        holder.send_comment.text = item.send_comment
        holder.receive_comment.text = item.receive_comment
        holder.sender_phone.text = item.sender_phone
        holder.receiver_phone.text = item.receiver_phone
        holder.sender_name.text = item.sender_name
        holder.receiver_name.text = item.receiver_name
        holder.sklad_to_address.text = item.sklad_to_address
        holder.sklad_from_address.text = item.sklad_from_address
        holder.sklad_to_resp_name.text = item.sklad_to_resp_name
        holder.sklad_from_resp_name.text = item.sklad_from_resp_name
        holder.executor.text = item.executor_name
        val inputFormat1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        try {
            val date: Date? = try {
                inputFormat1.parse(item.planned_date)
            } catch (e: ParseException) {
                inputFormat2.parse(item.planned_date)
            }
            val formattedDate: String = outputFormat.format(date ?: Date())
            holder.planned_date.text = formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
            holder.planned_date.text = item.planned_date
        }
        Log.d("!!!", "роль пользователя: $currentRoleCheck")
        if (item.sklad_from_address?.isEmpty() ?: true || item.sklad_from_address == "null" || currentRoleCheck == "65") {
            holder.layout_address_sklada_otkuda.visibility = View.GONE
            Log.d("!!!", "роль пользователя: $currentRoleCheck")
        } else {
            holder.layout_address_sklada_otkuda.visibility = View.VISIBLE
            Log.d("!!!", "роль пользователя: $currentRoleCheck")
        }
        if (item.sklad_to_address?.isEmpty() ?: true || item.sklad_to_address == "null" || currentRoleCheck == "65") {
            holder.layout_address_sklada_kuda.visibility = View.GONE
        } else {
            holder.layout_address_sklada_kuda.visibility = View.VISIBLE
        }
        if (item.sklad_from_resp_name?.isEmpty() ?: true || item.sklad_from_resp_name == "null" || currentRoleCheck == "65") {
            holder.layout_responsible_otkuda.visibility = View.GONE
        } else {
            holder.layout_responsible_otkuda.visibility = View.VISIBLE
        }
        if (item.sklad_to_resp_name?.isEmpty() ?: true || item.sklad_to_resp_name == "null" || currentRoleCheck == "65") {
            holder.layout_responsible_kuda.visibility = View.GONE
        } else {
            holder.layout_responsible_kuda.visibility = View.VISIBLE
        }
        if (item.sender_phone?.isEmpty() ?: true || item.sender_phone == "null" || item.sender_phone == "+7") {
            holder.layout_phone_sender.visibility = View.GONE
        } else {
            holder.layout_phone_sender.visibility = View.VISIBLE
        }
        if (item.receiver_phone?.isEmpty() ?: true || item.receiver_phone == "null" || item.receiver_phone == "+7") {
            holder.layout_phone_receiver.visibility = View.GONE
        } else {
            holder.layout_phone_receiver.visibility = View.VISIBLE
        }
        if (item.send_comment?.isEmpty() ?: true || item.send_comment == "null"|| item.send_comment == "Значение") {
            holder.layout_comment_sender.visibility = View.GONE
        } else {
            holder.layout_comment_sender.visibility = View.VISIBLE
        }
        if (item.receive_comment?.isEmpty() ?: true || item.receive_comment == "null") {
            holder.layout_comment_receiver.visibility = View.GONE
        } else {
            holder.layout_comment_receiver.visibility = View.VISIBLE
        }
        if (item.loader?.isEmpty() ?: true || item.loader == "null") {
            holder.layout_gruzchik_pogruzchik.visibility = View.GONE
        } else {
            holder.layout_gruzchik_pogruzchik.visibility = View.VISIBLE
        }
        if (item.sender_name?.isEmpty() ?: true || item.sender_name == "null") {
            holder.layout_sender.visibility = View.GONE
        } else {
            holder.layout_sender.visibility = View.VISIBLE
        }
        if (item.receiver_name?.isEmpty() ?: true || item.receiver_name == "null") {
            holder.layout_receiver.visibility = View.GONE
        } else {
            holder.layout_receiver.visibility = View.VISIBLE
        }
        if (item.loader == "Грузчик"){
            holder.loader_gruzchik.setImageResource(R.drawable.remember_me_svg)
        }
        else if (item.loader == "Погрузчик") {
            holder.loader_pogruzchik.setImageResource(R.drawable.remember_me_svg)
        }
        else if (item.loader == "Грузчик,Погрузчик") {
            holder.loader_gruzchik.setImageResource(R.drawable.remember_me_svg)
            holder.loader_pogruzchik.setImageResource(R.drawable.remember_me_svg)
        }
        holder.status.text = item.status
//        if (item.getObjects().isNotEmpty()) {
//            val logisticsObject = item.getObjects()[0]
//            val demandText = logisticsObject.demand ?: ""
//            val operationText = logisticsObject.operation ?: ""
//            val zahodText = logisticsObject.zahod ?: ""
//            val document = logisticsObject.docNumber ?: ""
//            val stanokText = logisticsObject.stanok ?: ""
//            holder.demand.text = item.object_name ?: "Нет информации об объекте"
//        }

        val objectName = item.dnObjectNames ?: item.object_name ?: "Нет информации об объекте"
        holder.demand.text = objectName


        holder.status.text = when (item.status) {
            "1" -> "Принята"
            "-1" -> "Аннулирована"
            "2" -> "Выполняется"
            "3" -> "На подтверждении"
            "4" -> "Выполнена"
            else -> "Неизвестно"
        }
        if (currentUsername == "T.Test") {
            Toast.makeText(holder.itemView.context, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
        }
        else {
            val isLoading = statusLoadingStates[item.id] ?: false

            holder.statusProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            holder.status.isEnabled = !isLoading
            holder.status.alpha = if (isLoading) 0.5f else 1.0f

            holder.status.setOnClickListener {
                if (!isLoading) {
                    statusLoadingStates[item.id] = true
                    notifyItemChanged(position)

                    onStatusClick(item)
                }
            }
        }

        val textColor = Color.WHITE
        when (item.status) {
            0.toString() -> {
                holder.status.text = "Зарегистрирована"
                holder.status.setBackgroundResource(R.drawable.custom_zaregistrirovana_vtext_view_background)
                holder.status.setTextColor(Color.WHITE)
            }
            1.toString() -> {
                holder.status.text = "Принята"
                holder.status.setBackgroundResource(R.drawable.custom_prinyata_text_view_background)
                holder.status.setTextColor(Color.WHITE)
            }
            (-1).toString() -> {
                holder.status.text = "Аннулирована"
                holder.status.setBackgroundResource(R.drawable.custom_annulirovanna_text_view_background)
                holder.status.setTextColor(Color.WHITE)
            }
            2.toString() -> {
                holder.status.text = "Выполняется"
                holder.status.setBackgroundResource(R.drawable.custom_vipolnyaetsya_text_view_background)
                holder.status.setTextColor(Color.WHITE)
            }
            3.toString() -> {
                holder.status.text = "На подтверждении"
                holder.status.setBackgroundResource(R.drawable.custom_napodtverzhdenii_text_view_background)
                holder.status.setTextColor(Color.WHITE)
            }
            4.toString() -> {
                holder.status.text = "Выполнена"
                holder.status.setBackgroundResource(R.drawable.custom_vipolnena_text_view_background)
                holder.status.setTextColor(Color.WHITE)
            }
            else -> {
                holder.status.text = "Неизвестно"
                holder.status.setTextColor(Color.WHITE)
            }
        }
        holder.status.setTextColor(textColor)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailLogisticsActivity::class.java)
            intent.putExtra("logistics_id", item.id)
            intent.putExtra("mdmCode", currentMdmCode)
            intent.putExtra("userId", currentUserId)
            intent.putExtra("username", currentUsername)
            intent.putExtra("roleCheck", currentRoleCheck)
            intent.putExtra("deviceInfo", currentDeviceInfo)
            intent.putExtra("fio", currentFio)
            intent.putExtra("type", item.type)
            intent.putExtra("device_token", currentDeviceToken)
            intent.putExtra("rolesString", currentRolesString)
            intent.putExtra("isAuthorized", currentIsAuthorized)
            context.startActivity(intent)
        }

        // ИСПРАВЛЕННЫЙ КОД ДЛЯ docsRecyclerView:
        // Получаем только документы с фото (с использованием нового метода getPhotoDocs())
        val photoDocs = item.getPhotoDocs()
        if (photoDocs.isNotEmpty()) {
            // Получаем токены из Auth класса
            val authTokenAPI = Auth.authTokenAPI
            val authToken = Auth.authToken

            // Передаем токены в DocsAdapter
            val docsAdapter = DocsAdapter(
                docs = photoDocs,
                authTokenAPI = authTokenAPI,
                authToken = authToken
            )
            holder.docsRecyclerView.adapter = docsAdapter
            holder.docsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            holder.docsRecyclerView.addItemDecoration(HorizontalSpaceItemDecoration(1))
            holder.docsRecyclerView.visibility = View.VISIBLE
        } else {
            holder.docsRecyclerView.visibility = View.GONE
        }

        // Или если еще не добавили метод getPhotoDocs(), используйте фильтрацию:
        /*
        val photoDocs = item.docs?.filter {
            !it.md5Name.isNullOrBlank() || !it.fileUrl.isNullOrBlank()
        } ?: emptyList()

        if (photoDocs.isNotEmpty()) {
            val docsAdapter = DocsAdapter(photoDocs)
            holder.docsRecyclerView.adapter = docsAdapter
            holder.docsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            holder.docsRecyclerView.addItemDecoration(HorizontalSpaceItemDecoration(1))
            holder.docsRecyclerView.visibility = View.VISIBLE
        } else {
            holder.docsRecyclerView.visibility = View.GONE
        }
        */

        if (currentUsername == "T.Test") {
            Toast.makeText(holder.itemView.context, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
        }
        else {
            holder.comment.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    item.comment = s.toString().trim()
                }
                override fun afterTextChanged(s: Editable?) {
                    holder.itemView.setOnFocusChangeListener { v, hasFocus ->
                        if (!hasFocus && s.toString().trim() != item.comment) {
                            showSaveDialog(holder.itemView.context, item.id, s.toString().trim())
                        }
                    }
                }
            })
            holder.comment.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            holder.comment.imeOptions = EditorInfo.IME_ACTION_DONE
            holder.comment.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val commentText = holder.comment.text.toString().trim()
                    val logisticsId = item.id
                    if (currentUsername == "T.Test") {
                        Toast.makeText(holder.itemView.context, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
                    } else {
                        updateLogisticComment(logisticsId, commentText, holder.itemView.context)
                    }
                    val imm = holder.itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(holder.comment.windowToken, 0)
                    return@setOnEditorActionListener true
                }
                false
            }
        }
        holder.executor.setOnClickListener {
            if (currentUsername == "T.Test" ||
                !(currentRoleCheck == "2" || currentRoleCheck == "64" ||
                        currentRolesString.contains("2") || currentRolesString.contains("64"))) {
                Toast.makeText(holder.itemView.context,
                    "У вас недостаточно прав для изменения исполнителя",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            showExecutorSelectionDialog(holder.itemView.context, item.id)
        }
        holder.take_photo_button.setOnClickListener {
            if (currentUsername == "T.Test") {
                Toast.makeText(holder.itemView.context, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
            }
            else {
                val dialogView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.dialog_custom, null)
                val builder = AlertDialog.Builder(holder.itemView.context)
                builder.setView(dialogView)
                val buttonCamera = dialogView.findViewById<Button>(R.id.button_camera)
                val buttonGallery = dialogView.findViewById<Button>(R.id.button_gallery)
                val alertDialog = builder.create()
                buttonCamera.setOnClickListener {
                    openCamera(holder.itemView.context, item.id)
                    alertDialog.dismiss()
                }
                buttonGallery.setOnClickListener {
                    openGallery(holder.itemView.context, item.id)
                    alertDialog.dismiss()
                }
                alertDialog.show()
            }
        }
        holder.icon_sender_phone.setOnClickListener {
            item.sender_phone?.let { phoneNumber ->
                makePhoneCall(holder.itemView.context, phoneNumber)
            }
        }

        holder.icon_receiver_phone.setOnClickListener {
            item.receiver_phone?.let { phoneNumber ->
                makePhoneCall(holder.itemView.context, phoneNumber)
            }
        }

        holder.openMapButton.setOnClickListener {
            if (currentUsername == "T.Test" || !(currentRoleCheck == "2" || currentRolesString.contains("2"))) {
                Toast.makeText(holder.itemView.context,
                    "У вас недостаточно прав", Toast.LENGTH_LONG).show()
            } else {
                // Открываем карту внутри приложения
                WebMapActivity.start(holder.itemView.context)
            }
        }
    }

    fun resetLoadingState(itemId: String) {
        statusLoadingStates.remove(itemId)
        val position = items.indexOfFirst { it.id == itemId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }
    private fun makePhoneCall(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            context.startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
        }
    }
    @SuppressLint("MissingInflatedId")
    private fun showSaveDialog(context: Context, logisticsId: String, commentText: String) {
        if (currentUsername == "T.Test") {
            Toast.makeText(context, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
        }
        else {
            val builder = AlertDialog.Builder(context)
            val customLayout = LayoutInflater.from(context).inflate(R.layout.update_comment_dialog, null)
            val titleTextView: TextView = customLayout.findViewById(R.id.title)
            titleTextView.text = "Сохранение изменений"
            val messageTextView: TextView = customLayout.findViewById(R.id.message)
            messageTextView.text = "Вы хотите сохранить изменения для поля Примечание?"
            val yesButton: Button = customLayout.findViewById(R.id.yes_button)
            yesButton.setOnClickListener {
                updateLogisticComment(logisticsId, commentText, context)
                (context as Dialog).dismiss()
            }
            val noButton: Button = customLayout.findViewById(R.id.no_button)
            noButton.setOnClickListener {
                (context as Dialog).dismiss()
            }
            builder.setView(customLayout)
            builder.setCancelable(false)
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }
    override fun onViewDetachedFromWindow(holder: DetailLogisticsViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.runnable?.let { holder.handler.removeCallbacks(it) }
    }
    private fun openCamera(context: Context, logisticsId: String) {
        if (!isCameraAvailable(context)) {
            Toast.makeText(context, "Устройство не имеет камеры", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
            return
        }

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(context.packageManager) == null) {
            Toast.makeText(context, "Камера не доступна", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = try {
            createImageFile(context)
        } catch (e: IOException) {
            Toast.makeText(context, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                it
            )
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            (context as Activity).startActivityForResult(cameraIntent, REQUEST_CAMERA)
            savePhotoFile(it, logisticsId)
        }
    }
    private fun isCameraAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    private fun openGallery(context: Context, logisticsId: String) {
        if (checkGalleryPermission(context)) {
            startGalleryIntent(context, logisticsId)
        } else {
            requestGalleryPermission(context)
        }
    }

    private fun checkGalleryPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestGalleryPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                REQUEST_CODE_GALLERY_PERMISSION
            )
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_GALLERY_PERMISSION
            )
        }
    }
    fun notifyGalleryPermissionGranted(context: Context, logisticsId: String) {
        openGallery(context, logisticsId)
    }
    private fun startGalleryIntent(context: Context, logisticsId: String) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        (context as Activity).startActivityForResult(intent, REQUEST_GALLERY)
        saveLogisticsId(logisticsId)
    }
    private fun createImageFile(context: Context): File? {
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return storageDir?.let {
            File.createTempFile(
                "JPEG_${System.currentTimeMillis()}_",
                ".jpg",
                it
            )
        }
    }
    private fun savePhotoFile(photoFile: File?, logisticsId: String) {
        photoFile?.let { file ->
            currentPhotoFile = file
            currentLogisticsId = logisticsId
        }
    }
    private fun saveLogisticsId(logisticsId: String) {
        currentLogisticsId = logisticsId
    }
    private fun formatTimeAtWork(timeMillis: Long): String {
        val seconds = (timeMillis / 1000) % 60
        val minutes = (timeMillis / (1000 * 60)) % 60
        val hours = (timeMillis / (1000 * 60 * 60))

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            else -> String.format("%02d", seconds)
        }
    }
    private fun updateLogisticComment(logisticsId: String, comment: String, context: Context) {
        if (logisticsId.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val client = (context.applicationContext as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://api.gkmmz.ru/api/update_logistic_comment/$logisticsId"
            val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_logistic_comment/$logisticsId"

            fun buildBody() = FormBody.Builder()
                .add("comment", comment)
                .add("mdm_code", currentMdmCode ?: "")
                .add("version_name", version_name)
                .build()

            fun exec(url: String): Response? = try {
                val req = Request.Builder()
                    .url(url)
                    .post(buildBody())
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token",  authToken)
                    .build()
                client.newCall(req).execute()
            } catch (e: IOException) {
                Log.e("UpdateComment", "IO error $url: ${e.message}")
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("UpdateComment", "fallback → $fallbackUrl")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) { showToast(context, "Сервер недоступен", 5000) }
                    return@launch
                }

                resp.use { r ->
                    if (r.isSuccessful) {
                        withContext(Dispatchers.Main) { showToast(context, "Комментарий успешно обновлён", 5000) }
                    } else {
                        withContext(Dispatchers.Main) { showToast(context, "Ошибка обновления: ${r.code}", 5000) }
                        Log.e("UpdateComment", "HTTP ${r.code} - ${r.message}")
                    }
                }
            } catch (_: ServiceModeException) {
            } catch (t: Throwable) {
                Log.e("UpdateComment", "Unexpected", t)
                withContext(Dispatchers.Main) { showToast(context, "Произошла ошибка", 5000) }
            }
        }
    }
    private fun updateLogisticExecutor(logisticsId: String, executorMdmCode: String, context: Context) {
        if (logisticsId.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val client = (context.applicationContext as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://api.gkmmz.ru/api/update_logistic_executor/$logisticsId"
            val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_logistic_executor/$logisticsId"

            fun buildBody() = FormBody.Builder()
                .add("executor", executorMdmCode)
                .add("mdm_code", currentMdmCode ?: "")
                .add("version_name", version_name)
                .build()

            fun exec(url: String): Response? = try {
                val req = Request.Builder()
                    .url(url)
                    .post(buildBody())
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token",  authToken)
                    .build()
                client.newCall(req).execute()
            } catch (e: IOException) {
                Log.e("UpdateExecutor", "IO error $url: ${e.message}")
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("UpdateExecutor", "fallback → $fallbackUrl")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) { showToast(context, "Сервер недоступен", 5000) }
                    return@launch
                }

                resp.use { r ->
                    if (r.isSuccessful) {
                        withContext(Dispatchers.Main) { showToast(context, "Исполнитель успешно обновлён", 5000) }
                    } else {
                        withContext(Dispatchers.Main) { showToast(context, "Ошибка обновления: ${r.code}", 5000) }
                        Log.e("UpdateExecutor", "HTTP ${r.code} - ${r.message}")
                    }
                }
            } catch (_: ServiceModeException) {
            } catch (t: Throwable) {
                Log.e("UpdateExecutor", "Unexpected", t)
                withContext(Dispatchers.Main) { showToast(context, "Произошла ошибка", 5000) }
            }
        }
    }
    private fun showExecutorSelectionDialog(context: Context, logisticsId: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_executor_selection, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Выберите исполнителя")
            .setNegativeButton("Отмена", null)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        val requiredExecutors = listOf(
            "Домнин Анатолий Александрович",
            "Ильичев Александр Сергеевич",
            "Кузнецов Роман Алексеевич",
            "Маган Андрей Михайлович",
            "Метелкин Андрей Иванович",
            "Попов Роман Дмитриевич",
            "Потапов Иван Артемович",
            "Соломенный Сергей Александрович",
            "Усманов Ростислав Сергеевич"
        )

        val adapter = ExecutorAdapter(emptyList()) { selectedExecutor ->
            updateLogisticExecutor(logisticsId, selectedExecutor.mdmcode, context)
            dialog.dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allEmployees = getAllSotrudnikiInfo(context)

                val filteredExecutors = allEmployees.filter { employee ->
                    requiredExecutors.any { it == employee.fio }
                }

                withContext(Dispatchers.Main) {
                    adapter.updateData(filteredExecutors)
                    progressBar.visibility = View.GONE

                    if (filteredExecutors.isEmpty()) {
                        Toast.makeText(context, "Исполнители не найдены", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }
    suspend fun showToast(context: Context, message: String, duration: Long) {
        withContext(Dispatchers.Main) {
            val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            toast.show()
            Handler(Looper.getMainLooper()).postDelayed({
                toast.cancel()
            }, duration)
        }
    }
    companion object {
        const val REQUEST_CAMERA = 1
        const val REQUEST_GALLERY = 2
        var currentPhotoFile: File? = null
        var currentLogisticsId: String? = null
    }
    private val REQUEST_CAMERA_PERMISSION = 2
    override fun getItemCount() = items.size
    private val REQUEST_CODE_GALLERY_PERMISSION = 1003
    private val REQUEST_CALL_PHONE = 1004
}

class HorizontalSpaceItemDecoration(private val spaceSize: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != parent.adapter?.itemCount?.minus(1)) {
            outRect.right = spaceSize
        }
    }
}