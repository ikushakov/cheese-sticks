package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogisticsAdapter(private var items: List<LogisticsItem>,
                       private val mdmCode: String,
                       private val userId: Int,
                       private val username: String,
                       private val roleCheck: String,
                       private val deviceInfo: String,
                       private val fio: String) : RecyclerView.Adapter<LogisticsAdapter.LogisticsViewHolder>() {
    class LogisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId = itemView.findViewById<TextView>(R.id.tvId)
        val tvType = itemView.findViewById<TextView>(R.id.tvType)
        val tvPlannedDate = itemView.findViewById<TextView>(R.id.tvPlannedDate)
        val tvSendFromTitle = itemView.findViewById<TextView>(R.id.tvSendFromTitle)
        val tvSendToTitle = itemView.findViewById<TextView>(R.id.tvSendToTitle)
        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
        val tvObject = itemView.findViewById<TextView>(R.id.tvObject)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogisticsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_logistics, parent, false)
        return LogisticsViewHolder(view)
    }
    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: LogisticsViewHolder, position: Int) {
        val item = items[position]
        holder.tvId.text = "Доставка №"+item.id+"\nот "+item.created_at + "\n" + item.creator
        holder.tvType.text = item.type
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
        holder.tvObject.text = if (item.spros.isNullOrEmpty() || item.spros == "null") {
            if (item.object_id != "null" || item.object_id != "") {
                item.object_id
            } else {
                "Не указано"
            }
        } else {
            item.spros
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
            Log.d("LogisticsAdapter", "logistics_id: $item.id, mdmCode: $mdmCode, userId: $userId, username: $username, roleCheck: $roleCheck, deviceInfo: $deviceInfo, fio: $fio")
            val intent = Intent(context, DetailLogisticsActivity::class.java).apply {
                putExtra("logistics_id", item.id)
                putExtra("mdmCode", mdmCode)
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("deviceInfo", deviceInfo)
                putExtra("fio", fio)
                putExtra("type", item.type)
                val rolesString = (context as LogisticActivity).rolesList.joinToString(separator = ",") { it }
                putExtra("rolesString", rolesString)
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