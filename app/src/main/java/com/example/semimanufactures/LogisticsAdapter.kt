package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogisticsAdapter(private var items: List<LogisticsItem>,
                       private var currentUsername: String,
                       private var currentUserId: Int,
                       private var currentRoleCheck: String,
                       private var currentMdmCode: String,
                       private var currentFio: String,
                       private var currentDeviceInfo: String,
                       private var currentRolesString: String,
                       private var currentDeviceToken: String,
                       private var currentIsAuthorized:  Boolean) : RecyclerView.Adapter<LogisticsAdapter.LogisticsViewHolder>() {
    class LogisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId = itemView.findViewById<TextView>(R.id.tvId)
        val tvType = itemView.findViewById<TextView>(R.id.tvType)
        val tvPlannedDate = itemView.findViewById<TextView>(R.id.tvPlannedDate)
        val tvSendFromTitle = itemView.findViewById<TextView>(R.id.tvSendFromTitle)
        val tvSendToTitle = itemView.findViewById<TextView>(R.id.tvSendToTitle)
        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
        val tvObject = itemView.findViewById<TextView>(R.id.tvObject)
        val ivExecutorIcon = itemView.findViewById<ImageView>(R.id.ivExecutorIcon)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogisticsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_logistics, parent, false)
        return LogisticsViewHolder(view)
    }
    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: LogisticsViewHolder, position: Int) {
        val item = items[position]
        holder.tvId.text = "Доставка №"+item.id+"\nот "+item.created_at + "\n" + item.creator
        holder.tvType.text = when (item.type) {
            "prp" -> "ПрП"
            "stanok" -> "Оборудование"
            "other" -> "Прочее"
            "doc" -> "Документ"
            else -> "Неизвестный тип"
        }
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        try {
            val date: Date? = inputFormat.parse(item.planned_date)
            val formattedDate: String = outputFormat.format(date)
            holder.tvPlannedDate.text = formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
        }
        holder.tvSendFromTitle.text = if (item.send_from_title.isNullOrEmpty() || item.send_from_title == "null") {
            "Не указано"
        } else {
            item.send_from_title
        }
        holder.tvSendToTitle.text = if (item.send_to_title.isNullOrEmpty() || item.send_to_title == "null") {
            "Не указано"
        } else {
            item.send_to_title
        }

        val objectName = item.dnObjectNames ?: item.object_name ?: "Нет информации об объекте"
        holder.tvObject.text = objectName

        if (!item.executor.isNullOrEmpty() && item.executor != "null") {
            holder.ivExecutorIcon.visibility = View.VISIBLE
        } else {
            holder.ivExecutorIcon.visibility = View.GONE
        }
        val textColor = Color.WHITE
        when (item.status) {
            0.toString() -> {
                holder.tvStatus.text = "Зарегистрирована"
                holder.tvStatus.setBackgroundResource(R.drawable.custom_zaregistrirovana_vtext_view_background)
            }
            1.toString() -> {
                holder.tvStatus.text = "Принята"
                holder.tvStatus.setBackgroundResource(R.drawable.custom_prinyata_text_view_background)
            }
            (-1).toString() -> {
                holder.tvStatus.text = "Аннулирована"
                holder.tvStatus.setBackgroundResource(R.drawable.custom_annulirovanna_text_view_background)
            }
            2.toString() -> {
                holder.tvStatus.text = "Выполняется"
                holder.tvStatus.setBackgroundResource(R.drawable.custom_vipolnyaetsya_text_view_background)
            }
            3.toString() -> {
                holder.tvStatus.text = "На подтверждении"
                holder.tvStatus.setBackgroundResource(R.drawable.custom_napodtverzhdenii_text_view_background)
            }
            4.toString() -> {
                holder.tvStatus.text = "Выполнена"
                holder.tvStatus.setBackgroundResource(R.drawable.custom_vipolnena_text_view_background)
            }
            else -> {
                holder.tvStatus.text = "Неизвестно"
            }
        }
        holder.tvStatus.setTextColor(textColor)
        holder.tvId.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailLogisticsActivity::class.java).apply {
                putExtra("logistics_id", item.id)
                putExtra("mdmCode", currentMdmCode)
                putExtra("userId", currentUserId)
                putExtra("username", currentUsername)
                putExtra("roleCheck", currentRoleCheck)
                putExtra("deviceInfo", currentDeviceInfo)
                putExtra("fio", currentFio)
                putExtra("type", item.type)
                putExtra("device_token", currentDeviceToken)
                putExtra("rolesString", currentRolesString)
                putExtra("isAuthorized", currentIsAuthorized)
            }
            context.startActivity(intent)
        }
    }
    override fun getItemCount() = items.size
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<LogisticsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}