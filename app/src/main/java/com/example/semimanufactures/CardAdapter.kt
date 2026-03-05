package com.example.semimanufactures

import android.app.AlertDialog
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CardAdapter(
    private val cardItemList: List<CardItem>,
    private val activity: FeaturesOfTheFunctionalityActivity,
    private var daysMinus: Long,
    private var currentUsername: String,
    private var currentUserId: Int,
    private var currentRoleCheck: String,
    private var currentMdmCode: String,
    private var currentFio: String,
    private var currentDeviceInfo: String,
    private var currentRolesString: String,
    private var currentDeviceToken: String,
    private var currentIsAuthorized:  Boolean
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_card_new, parent, false)
            CardViewHolder(view)
        } catch (e: Exception) {
            Log.e("CardAdapter", "Error inflating view holder", e)
            throw e
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardItem = cardItemList[position]
        holder.name.text = if (cardItem.name.isNullOrEmpty() || cardItem.name == "null") {
            "Не указано"
        } else {
            cardItem.name
        }
        holder.prosk.text = if (cardItem.prosk.isNullOrEmpty() || cardItem.prosk == "null") {
            "Не указано"
        } else {
            cardItem.prosk
        }
        holder.demand.text = if (cardItem.demand.isNullOrEmpty() || cardItem.demand == "null") {
            "Не указано"
        } else {
            cardItem.demand
        }
        holder.quantity.text = if (cardItem.quantity.isNullOrEmpty() || cardItem.quantity == "null") {
            "Не указано"
        } else {
            cardItem.quantity
        }
        holder.plot.text = if (cardItem.plot.isNullOrEmpty() || cardItem.plot == "null") {
            "Не указано"
        } else {
            cardItem.plot
        }
        val dateOfDistribution = cardItem.dateOfDistribution
        Log.d("CardAdapter", "Дата распределения для '${cardItem.name}': $dateOfDistribution")
        if (dateOfDistribution.isNullOrEmpty() || dateOfDistribution == "null") {
            holder.dateOfDistribution.text = "Дата распределения пустая"
        } else {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val distributionDate = dateFormat.parse(dateOfDistribution)
                holder.dateOfDistribution.text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(distributionDate)
                val calendar = Calendar.getInstance()
                val daysMinus = 7
                calendar.add(Calendar.DAY_OF_MONTH, daysMinus)
                val thirtyDaysFromNow = calendar.time
                if (distributionDate.before(thirtyDaysFromNow)) {
                    holder.layoutForCard.setBackgroundResource(R.drawable.custom_background_style)
                } else {
                    holder.layoutForCard.setBackgroundResource(R.drawable.new_card_background_features)
                }
            } catch (e: Exception) {
                Log.e("CardAdapter", "Error parsing date: ${e.message}")
                holder.dateOfDistribution.text = "Ошибка даты"
            }
        }
        holder.toIssueButton.setOnClickListener {
            showConfirmationDialog(cardItem)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showConfirmationDialog(cardItem: CardItem) {
        if (currentUsername == "T.Test") {
            Toast.makeText(activity, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
        }
        else {
            val dialogView = activity.layoutInflater.inflate(R.layout.new_dialog_confirmation, null)
            val builder = AlertDialog.Builder(activity)
            builder.setView(dialogView)
            val yesButton = dialogView.findViewById<Button>(R.id.buttonYes)
            val noButton = dialogView.findViewById<Button>(R.id.buttonNo)
            val dialog = builder.create()
            yesButton.setOnClickListener {
                activity.lifecycleScope.launch {
                    activity.handleCardClick(cardItem)
                }
                dialog.dismiss()
            }
            noButton.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
    }
    override fun getItemCount(): Int {
        return cardItemList.size
    }
    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.text_title)
        val prosk: TextView = itemView.findViewById(R.id.text_prosk)
        val demand: TextView = itemView.findViewById(R.id.text_spros)
        val quantity: TextView = itemView.findViewById(R.id.text_kolichestvo)
        val plot: TextView = itemView.findViewById(R.id.text_uchastok)
        val dateOfDistribution: TextView = itemView.findViewById(R.id.text_date_of_distribution)
        val toIssueButton: Button = itemView.findViewById(R.id.to_issue_button)
        val layoutForCard: LinearLayout = itemView.findViewById(R.id.layout_for_card)
    }
}