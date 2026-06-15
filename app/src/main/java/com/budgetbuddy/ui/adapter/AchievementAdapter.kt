package com.budgetbuddy.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budgetbuddy.R
import com.budgetbuddy.data.entity.Achievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementAdapter : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {
    private var items: List<Achievement> = emptyList()

    fun update(newItems: List<Achievement>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.achievedAt))
        holder.description.text = "${item.description} (+${item.xpEarned} XP) • $date"
    }

    override fun getItemCount(): Int = items.size

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvAchievementTitle)
        val description: TextView = itemView.findViewById(R.id.tvAchievementDesc)
    }
}
