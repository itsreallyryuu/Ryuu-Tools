package com.ryuutools.app

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AppEntry(
    val packageName: String,
    val displayName: String,
    val icon: Drawable?,
    var isEnabled: Boolean,
    val isKnownSafe: Boolean
)

class AppManagerAdapter(
    private val onToggle: (AppEntry, Boolean) -> Unit
) : RecyclerView.Adapter<AppManagerAdapter.ViewHolder>() {

    private val items = mutableListOf<AppEntry>()

    fun updateItems(newItems: List<AppEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivAppIcon)
        val name: TextView = view.findViewById(R.id.tvAppName)
        val badge: TextView = view.findViewById(R.id.tvAppBadge)
        val switch: Switch = view.findViewById(R.id.switchAppEnabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_manager, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.name.text = item.displayName
        holder.badge.text = if (item.isKnownSafe) "Known Safe" else "System App"
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = item.isEnabled
        holder.switch.setOnCheckedChangeListener { _, checked -> onToggle(item, checked) }
    }

    override fun getItemCount(): Int = items.size
}