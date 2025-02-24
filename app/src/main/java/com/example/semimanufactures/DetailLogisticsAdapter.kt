package com.example.semimanufactures

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailLogisticsAdapter(private val items: MutableList<LogisticsItem>,
                             private val mdmCode: String,
                             private val userId: String,
                             private val username: String,
                             private val roleCheck: String,
                             private val deviceInfo: String,
                             private val fio: String,
                             private val onStatusClick: (LogisticsItem) -> Unit) : RecyclerView.Adapter<DetailLogisticsAdapter.DetailLogisticsViewHolder>() {
    class DetailLogisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val id: TextView = itemView.findViewById(R.id.id)
        val send_from_title: TextView = itemView.findViewById(R.id.send_from_title)
        val send_to_title: TextView = itemView.findViewById(R.id.send_to_title)
        val type: TextView = itemView.findViewById(R.id.type)
        val comment: TextView = itemView.findViewById(R.id.comment)
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
        val save_photo_logistic: Button = itemView.findViewById(R.id.save_photo_logistic)
        val planned_date: TextView = itemView.findViewById(R.id.planned_date)
        val loader_gruzchik: ImageView = itemView.findViewById(R.id.loader_gruzchik)
        val loader_pogruzchik: ImageView = itemView.findViewById(R.id.loader_pogruzchik)
        val layout_address_sklada_otkuda: LinearLayout = itemView.findViewById(R.id.layout_address_sklada_otkuda)
        val layout_address_sklada_kuda: LinearLayout = itemView.findViewById(R.id.layout_address_sklada_kuda)
        val layout_comment: LinearLayout = itemView.findViewById(R.id.layout_comment)
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
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailLogisticsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail_logistics, parent, false)
        return DetailLogisticsViewHolder(view)
    }
    override fun onBindViewHolder(holder: DetailLogisticsViewHolder, position: Int) {
        val item = items[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        var plannedDateMillis: Long = 0
        try {
            plannedDateMillis = dateFormat.parse(item.planned_date)?.time ?: 0
        } catch (e: ParseException) {
            Log.e("DetailLogisticsAdapter", "Ошибка при парсинге даты", e)
        }
        val nowMillis = System.currentTimeMillis()
        holder.runnable?.let { holder.handler.removeCallbacks(it) }
        if (plannedDateMillis > nowMillis) {
            var timeDiffMillis = plannedDateMillis - nowMillis
            holder.runnable = object : Runnable {
                override fun run() {
                    if (timeDiffMillis <= 0) {
                        holder.handler.removeCallbacks(this)
                        holder.id.text = "Доставка №${item.id}\nот ${item.created_at}\n${item.creator}"
                        holder.id.setTextColor(Color.BLACK)
                        return
                    }
                    val formattedTime = formatTime(timeDiffMillis)
                    val fullText = "Доставка №${item.id} $formattedTime\nот ${item.created_at}\n${item.creator}"
                    val spannableString = SpannableString(fullText)
                    val timerStartIndex = fullText.indexOf(formattedTime)
                    val timerEndIndex = timerStartIndex + formattedTime.length
                    spannableString.setSpan(
                        ForegroundColorSpan(Color.RED),
                        timerStartIndex,
                        timerEndIndex,
                        0
                    )
                    holder.id.text = spannableString
                    timeDiffMillis -= 1000
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
        holder.type.text = item.type
        holder.comment.text = item.comment
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
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        try {
            val date: Date? = inputFormat.parse(item.planned_date)
            val formattedDate: String = outputFormat.format(date)
            holder.planned_date.text = formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (item.sklad_from_address?.isEmpty() ?: true || item.sklad_from_address == "null") {
            holder.layout_address_sklada_otkuda.visibility = View.GONE
        } else {
            holder.layout_address_sklada_otkuda.visibility = View.VISIBLE
        }
        if (item.sklad_to_address?.isEmpty() ?: true || item.sklad_to_address == "null") {
            holder.layout_address_sklada_kuda.visibility = View.GONE
        } else {
            holder.layout_address_sklada_kuda.visibility = View.VISIBLE
        }
        if (item.comment?.isEmpty() ?: true || item.comment == "null") {
            holder.layout_comment.visibility = View.GONE
        } else {
            holder.layout_comment.visibility = View.VISIBLE
        }
        if (item.sklad_from_resp_name?.isEmpty() ?: true || item.sklad_from_resp_name == "null") {
            holder.layout_responsible_otkuda.visibility = View.GONE
        } else {
            holder.layout_responsible_otkuda.visibility = View.VISIBLE
        }
        if (item.sklad_to_resp_name?.isEmpty() ?: true || item.sklad_to_resp_name == "null") {
            holder.layout_responsible_kuda.visibility = View.GONE
        } else {
            holder.layout_responsible_kuda.visibility = View.VISIBLE
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
        if (item.getObjects().isNotEmpty()) {
            var logisticsObject = item.getObjects()[0]
            var demandText = logisticsObject.demand ?: ""
            var operationText = logisticsObject.operation ?: ""
            var zahodText = logisticsObject.zahod ?: ""
            val document = logisticsObject.docNumber ?: ""
            holder.demand.text = "$demandText $operationText $zahodText $document"
        }
        holder.status.text = when (item.status) {
            "1" -> "Принята"
            "-1" -> "Аннулирована"
            "2" -> "Выполняется"
            "3" -> "На подтверждении"
            "4" -> "Выполнена"
            else -> "Неизвестно"
        }
        holder.status.setOnClickListener {
            onStatusClick(item)
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
            intent.putExtra("mdmCode", mdmCode)
            intent.putExtra("userId", userId)
            intent.putExtra("username", username)
            intent.putExtra("roleCheck", roleCheck)
            intent.putExtra("deviceInfo", deviceInfo)
            intent.putExtra("fio", fio)
            intent.putExtra("type", item.type)
            val rolesString = (context as DetailLogisticsActivity).rolesList.joinToString(separator = ",") { it }
            intent.putExtra("rolesString", rolesString)
            context.startActivity(intent)
        }
        if (item.docs != null && item.docs!!.isNotEmpty()) {
            val docsAdapter = DocsAdapter(item.docs!!.values.toList())
            holder.docsRecyclerView.adapter = docsAdapter
            holder.docsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            holder.docsRecyclerView.addItemDecoration(HorizontalSpaceItemDecoration(1))
            holder.docsRecyclerView.visibility = View.VISIBLE
        } else {
            holder.docsRecyclerView.visibility = View.GONE
        }
        holder.take_photo_button.setOnClickListener {
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
        holder.save_photo_logistic.setOnClickListener {
            val uris = ArrayList<Uri>()
            uploadPhotosToServer(uris, item.id, holder.itemView.context)
        }
    }
    override fun onViewDetachedFromWindow(holder: DetailLogisticsViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.runnable?.let { holder.handler.removeCallbacks(it) }
    }
    private fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    private fun openCamera(context: Context, logisticsId: String) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(context.packageManager) != null) {
                val photoFile: File? = createImageFile(context)
                photoFile?.also {
                    savePhotoFile(it, logisticsId)
                    val photoURI: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        it
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    (context as Activity).startActivityForResult(intent, REQUEST_CAMERA)
                }
            }
        }
    }
    private fun openGallery(context: Context, logisticsId: String) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), MY_READ_EXTERNAL_REQUEST)
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            (context as Activity).startActivityForResult(intent, REQUEST_GALLERY)
            saveLogisticsId(logisticsId)
        }
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
    private fun uploadPhotosToServer(uris: List<Uri>, logisticsId: String, context: Context) {
        if (logisticsId.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val files = ArrayList<MultipartBody.Part>()
                for (uri in uris) {
                    val file = File(uri.path)
                    val requestBody = RequestBody.create("image/*".toMediaType(), file)
                    val part = MultipartBody.Part.createFormData("files[]", file.name, requestBody)
                    files.add(part)
                }
                val createdById = RequestBody.create("text/plain".toMediaType(), mdmCode)
                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                for (part in files) {
                    builder.addPart(part)
                }
                builder.addFormDataPart("created_by", mdmCode)
                val request = Request.Builder()
                    .url("http://192.168.200.250/api/upload_logistic_files/$logisticsId")
                    .post(builder.build())
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    showToast(context, "Фото успешно загружены", 7000)
                    Log.d("Upload", "Фото успешно загружены")
                } else {
                    showToast(context, "Ошибка загрузки фотографий", 7000)
                    Log.e("Upload", "Ошибка загрузки фотографий")
                }
            } catch (e: Exception) {
                Log.e("Upload", "Ошибка загрузки фотографий", e)
            }
        }
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
    private val MY_READ_EXTERNAL_REQUEST = 1
    private val REQUEST_CAMERA_PERMISSION = 2
    override fun getItemCount() = items.size
}