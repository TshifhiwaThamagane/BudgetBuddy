package com.budgetbuddy.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.budgetbuddy.R
import com.budgetbuddy.gamification.Badge

class BadgeAdapter : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {
    private var badges: List<Badge> = emptyList()
    private var unlockedIds: Set<String> = emptySet()

    fun update(allBadges: List<Badge>, unlocked: Set<String>) {
        badges = allBadges
        unlockedIds = unlocked
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        val isUnlocked = unlockedIds.contains(badge.id)
        holder.emoji.text = badge.iconEmoji
        holder.name.text = badge.name
        holder.description.text = badge.description
        holder.status.text = if (isUnlocked) {
            holder.itemView.context.getString(R.string.badge_unlocked)
        } else {
            holder.itemView.context.getString(R.string.badge_locked)
        }
        holder.status.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isUnlocked) R.color.green_600 else R.color.text_light
            )
        )
        holder.itemView.alpha = if (isUnlocked) 1.0f else 0.5f
    }

    override fun getItemCount(): Int = badges.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emoji: TextView = itemView.findViewById(R.id.tvBadgeEmoji)
        val name: TextView = itemView.findViewById(R.id.tvBadgeName)
        val description: TextView = itemView.findViewById(R.id.tvBadgeDescription)
        val status: TextView = itemView.findViewById(R.id.tvBadgeStatus)
    }
}
