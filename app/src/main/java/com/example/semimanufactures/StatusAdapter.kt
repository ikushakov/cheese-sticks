package com.example.semimanufactures

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class StatusAdapter(
    private val context: Context,
    private val statuses: Array<String>,
    private val icons: Array<Int>
) : BaseAdapter() {
    override fun getCount(): Int = statuses.size
    override fun getItem(position: Int): Any = statuses[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.status_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.status_text)
        val imageView = view.findViewById<ImageView>(R.id.status_icon)
        textView.text = statuses[position]
        imageView.setImageResource(icons[position])
        return view
    }
    private val statusUpdateLocks = mutableMapOf<Int, Boolean>()
    fun onStatusUpdateStarted(itemId: Int, holder: DetailLogisticsAdapter.DetailLogisticsViewHolder) {
        statusUpdateLocks[itemId] = true
        holder.statusProgress.visibility = View.VISIBLE
        holder.status.isEnabled = false
        holder.status.alpha = 0.5f
    }

    fun onStatusUpdateComplete(itemId: Int, holder: DetailLogisticsAdapter.DetailLogisticsViewHolder) {
        statusUpdateLocks.remove(itemId)
        holder.statusProgress.visibility = View.GONE
        holder.status.isEnabled = true
        holder.status.alpha = 1.0f
    }
}