package com.ryuutools.app

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class InstallableAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class InstalledAppsAdapter(
    private val allApps: List<InstallableAppInfo>,
    private val isAddedCheck: (String) -> Boolean,
    private val onToggle: (InstallableAppInfo) -> Unit
) : RecyclerView.Adapter<InstalledAppsAdapter.AppViewHolder>() {

    private var filteredApps: List<InstallableAppInfo> = allApps

    class AppViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val tvStatus: TextView = view.findViewById(R.id.tvAppAddedStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_installable_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.tvName.text = app.label
        holder.ivIcon.setImageDrawable(app.icon)

        val added = isAddedCheck(app.packageName)
        holder.tvStatus.text = if (added) "Added ✓" else "Add"
        holder.tvStatus.setTextColor(
            if (added) android.graphics.Color.parseColor("#4CD964")
            else android.graphics.Color.parseColor("#FF2D55")
        )

        holder.itemView.setOnClickListener {
            onToggle(app)
            notifyItemChanged(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = filteredApps.size

    fun filter(query: String) {
        filteredApps = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}