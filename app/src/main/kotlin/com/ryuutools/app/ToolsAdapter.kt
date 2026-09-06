package com.ryuutools.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ToolsAdapter(
    private val items: List<ToolItem>,
    private val onClick: (ToolItem) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.ToolViewHolder>() {

    class ToolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivToolIcon)
        val title: TextView = view.findViewById(R.id.tvToolTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.title.text = item.title

        holder.itemView.setOnClickListener {
            if (item.isReady) {
                onClick(item)
            } else {
                Toast.makeText(holder.itemView.context, "${item.title} — Coming Soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = items.size
}