package com.budgetbuddy.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budgetbuddy.R
import com.budgetbuddy.data.ChallengeRepository
import com.budgetbuddy.data.entity.Challenge
import com.google.android.material.button.MaterialButton

class ChallengeAdapter(
    private val onStartChallenge: (ChallengeRepository.ChallengeTemplate) -> Unit
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    sealed class ChallengeItem {
        data class Active(val challenge: Challenge) : ChallengeItem()
        data class Template(val template: ChallengeRepository.ChallengeTemplate) : ChallengeItem()
    }

    private var items: List<ChallengeItem> = emptyList()

    fun update(newItems: List<ChallengeItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChallengeItem.Active -> bindActive(holder, item.challenge)
            is ChallengeItem.Template -> bindTemplate(holder, item.template)
        }
    }

    private fun bindActive(holder: ChallengeViewHolder, challenge: Challenge) {
        holder.title.text = challenge.title
        holder.description.text = challenge.description
        holder.status.text = if (challenge.isCompleted) {
            holder.itemView.context.getString(R.string.challenge_completed)
        } else {
            holder.itemView.context.getString(R.string.challenge_active)
        }
        holder.startButton.visibility = View.GONE
        holder.progressBar.visibility = View.VISIBLE

        val progress = if (challenge.targetAmount > 0) {
            ((challenge.currentProgress / challenge.targetAmount) * 100).toInt().coerceIn(0, 100)
        } else {
            if (challenge.isCompleted) 100 else 0
        }
        holder.progressBar.progress = progress
        holder.progressText.text = "$progress%"
        holder.rewardPoints.text = "+${challenge.rewardPoints} XP"
    }

    private fun bindTemplate(holder: ChallengeViewHolder, template: ChallengeRepository.ChallengeTemplate) {
        holder.title.text = template.title
        holder.description.text = template.description
        holder.status.text = "Available"
        holder.progressBar.visibility = View.GONE
        holder.progressText.visibility = View.GONE
        holder.rewardPoints.text = "+${template.rewardPoints} XP"
        holder.startButton.visibility = View.VISIBLE
        holder.startButton.setOnClickListener { onStartChallenge(template) }
    }

    override fun getItemCount(): Int = items.size

    class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvChallengeTitle)
        val description: TextView = itemView.findViewById(R.id.tvChallengeDesc)
        val status: TextView = itemView.findViewById(R.id.tvChallengeStatus)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressChallenge)
        val progressText: TextView = itemView.findViewById(R.id.tvChallengeProgress)
        val rewardPoints: TextView = itemView.findViewById(R.id.tvRewardPoints)
        val startButton: MaterialButton = itemView.findViewById(R.id.btnStartChallenge)
    }
}
