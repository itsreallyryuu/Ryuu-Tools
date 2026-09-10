package com.ryuutools.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class HistoryEntry(val expression: String, val result: String)

class CalculatorHistoryAdapter(
    private val items: MutableList<HistoryEntry>,
    private val onClick: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<CalculatorHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val expr: TextView = view.findViewById(R.id.tvHistoryExpr)
        val result: TextView = view.findViewById(R.id.tvHistoryResult)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calc_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.expr.text = item.expression
        holder.result.text = "= ${item.result}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}