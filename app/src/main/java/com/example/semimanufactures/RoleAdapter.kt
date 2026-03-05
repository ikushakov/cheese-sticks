package com.example.semimanufactures

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoleAdapter(private val roles: List<String>) : RecyclerView.Adapter<RoleAdapter.RoleViewHolder>() {

    class RoleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roleTextView: TextView = itemView.findViewById(R.id.role_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.role_item, parent, false) // Создайте role_item.xml
        return RoleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.roleTextView.text = roles[position]
    }

    override fun getItemCount(): Int {
        return roles.size
    }
}
