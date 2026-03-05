package com.example.semimanufactures

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExecutorAdapter(
    private var items: List<SotrudnikiInfo>,
    var onItemClick: ((SotrudnikiInfo) -> Unit)? = null
) : RecyclerView.Adapter<ExecutorAdapter.ExecutorViewHolder>(), Filterable {

    private var filteredItems: List<SotrudnikiInfo> = items

    class ExecutorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.executor_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExecutorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_executor, parent, false)
        return ExecutorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExecutorViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.name.text = item.fio

        // Устанавливаем обработчик клика
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }

        // Можно добавить другие элементы интерфейса, если они есть в макете
    }

    override fun getItemCount(): Int = filteredItems.size

    fun updateData(newItems: List<SotrudnikiInfo>) {
        items = newItems
        filteredItems = newItems
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = if (constraint.isNullOrEmpty()) {
                    items // Если строка поиска пуста, показываем все элементы
                } else {
                    val filterPattern = constraint.toString().lowercase().trim()
                    items.filter { sotrudnik ->
                        sotrudnik.fio?.lowercase()?.contains(filterPattern) == true
                    }
                }
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<SotrudnikiInfo> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    // Дополнительные методы, если нужны

    fun getItem(position: Int): SotrudnikiInfo? {
        return if (position in 0 until filteredItems.size) {
            filteredItems[position]
        } else {
            null
        }
    }

    fun clear() {
        items = emptyList()
        filteredItems = emptyList()
        notifyDataSetChanged()
    }
}