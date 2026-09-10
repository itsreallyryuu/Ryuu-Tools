package com.ryuutools.app

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BoostedAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class BoostedAppsAdapter(
    private val apps: List<BoostedAppEntry>,
    private val onLaunch: (BoostedAppEntry) -> Unit,
    private val onRemove: (BoostedAppEntry) -> Unit
) : RecyclerView.Adapter<BoostedAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivBoostedAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvBoostedAppName)
        val btnRemove: TextView = view.findViewById(R.id.btnRemoveBoostedApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_boosted_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.label
        holder.ivIcon.setImageDrawable(app.icon)
        holder.itemView.setOnClickListener { onLaunch(app) }
        holder.btnRemove.setOnClickListener { onRemove(app) }
    }

    override fun getItemCount(): Int = apps.size
}