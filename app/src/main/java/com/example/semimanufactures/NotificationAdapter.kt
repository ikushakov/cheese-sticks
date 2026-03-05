package com.example.semimanufactures

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<StoredNotification>,
    private val onItemClick: (StoredNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val date: TextView = view.findViewById(R.id.notificationDate)
        val statusIcon: ImageView = view.findViewById(R.id.notificationStatusIcon)
        val cardView: CardView = view.findViewById(R.id.notificationCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.date.text = dateFormat.format(notification.receivedDate)

        val iconRes = if (notification.isRead) {
            R.drawable.new_icon_notification_grey
        } else {
            R.drawable.icon_notification_svg_blue
        }
        holder.statusIcon.setImageResource(iconRes)

        holder.cardView.alpha = if (notification.isRead) 0.7f else 1.0f

        holder.cardView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newNotifications: List<StoredNotification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}