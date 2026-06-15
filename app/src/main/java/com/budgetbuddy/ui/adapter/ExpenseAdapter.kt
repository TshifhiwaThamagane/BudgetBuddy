package com.budgetbuddy.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budgetbuddy.R
import com.budgetbuddy.data.dao.ExpenseWithCategory
import com.budgetbuddy.ui.ReceiptViewerActivity
import com.budgetbuddy.util.CurrencyUtils
import com.google.android.material.button.MaterialButton

class ExpenseAdapter(
    private val items: MutableList<ExpenseWithCategory>,
    private val onDelete: (ExpenseWithCategory) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    fun update(newItems: List<ExpenseWithCategory>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = items[position]
        holder.amount.text = CurrencyUtils.toRand(item.amount)
        holder.date.text = item.date
        holder.category.text = item.categoryName
        holder.note.text = item.note.ifBlank { "No note" }

        if (!item.receiptUri.isNullOrBlank()) {
            holder.viewReceipt.visibility = View.VISIBLE
            holder.viewReceipt.setOnClickListener {
                val intent = Intent(holder.itemView.context, ReceiptViewerActivity::class.java).apply {
                    putExtra(ReceiptViewerActivity.EXTRA_EXPENSE_ID, item.id)
                    putExtra(ReceiptViewerActivity.EXTRA_RECEIPT_URI, item.receiptUri)
                }
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.viewReceipt.visibility = View.GONE
        }

        holder.deleteButton.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amount: TextView = itemView.findViewById(R.id.tvAmount)
        val date: TextView = itemView.findViewById(R.id.tvDate)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val note: TextView = itemView.findViewById(R.id.tvNote)
        val viewReceipt: MaterialButton = itemView.findViewById(R.id.btnViewReceipt)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.btnDeleteExpense)
    }
}
