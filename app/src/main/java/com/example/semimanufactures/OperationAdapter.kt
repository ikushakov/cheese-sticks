package com.example.semimanufactures

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OperationAdapter(
    private val items: List<OperationWithDemand>,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<OperationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMain: TextView = view.findViewById(R.id.tv_main)
        val tvSub: TextView = view.findViewById(R.id.tv_sub)
        val ivIcon: ImageView = view.findViewById(R.id.iv_status_icon)

        init {
            view.setOnClickListener {
                onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val op = items[position]
        val nextOpName = if (position + 1 < items.size) items[position + 1].operation else "конечную"

        holder.tvMain.text = op.operation

        val rawDest = if (op.needProsk) "ПРОСК Полуфабрикатов" else op.nextUchastok
        val dest = rawDest.trim().takeUnless { it.isEmpty() || it.equals("null", true) } ?: ""

        if (dest.isNotEmpty()) {
            holder.tvSub.text = "на $nextOpName в $dest"
            holder.tvSub.visibility = View.VISIBLE
        } else {
            holder.tvSub.visibility = View.GONE
        }

        when (op.status) {
            "68" -> holder.ivIcon.setImageResource(R.drawable.icon_circle_grey)
            "64" -> holder.ivIcon.setImageResource(R.drawable.icon_circle_green)
            else -> holder.ivIcon.setImageResource(R.drawable.icon_circle_red)
        }
        holder.ivIcon.visibility = View.VISIBLE
    }

}
