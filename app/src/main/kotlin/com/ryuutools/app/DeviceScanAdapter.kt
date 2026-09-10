package com.ryuutools.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ScannedDevice(
    val ip: String,
    val hostname: String,
    val responseTimeMs: Long,
    val openPorts: List<Int>,
    val deviceTypeGuess: String
)

class DeviceScanAdapter(private val devices: MutableList<ScannedDevice>) :
    RecyclerView.Adapter<DeviceScanAdapter.DeviceViewHolder>() {

    private var displayList: List<ScannedDevice> = devices.toList()
    private var onlyShowWithPorts = false
    private var sortBySpeed = false

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIp: TextView = view.findViewById(R.id.tvDeviceIp)
        val tvResponseTime: TextView = view.findViewById(R.id.tvResponseTime)
        val tvHostname: TextView = view.findViewById(R.id.tvHostname)
        val tvDeviceType: TextView = view.findViewById(R.id.tvDeviceType)
        val tvOpenPorts: TextView = view.findViewById(R.id.tvOpenPorts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scanned_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = displayList[position]
        holder.tvIp.text = device.ip
        holder.tvResponseTime.text = "${device.responseTimeMs} ms"
        holder.tvHostname.text = if (device.hostname.isNotBlank() && device.hostname != device.ip) {
            "Hostname: ${device.hostname}"
        } else {
            "Hostname: not available"
        }
        holder.tvDeviceType.text = "Type guess: ${device.deviceTypeGuess}"
        holder.tvOpenPorts.text = if (device.openPorts.isEmpty()) {
            "Open ports: none found"
        } else {
            "Open ports: ${device.openPorts.joinToString(", ")}"
        }
    }

    override fun getItemCount(): Int = displayList.size

    fun refresh() {
        applyFilterAndSort()
    }

    fun setFilterOnlyWithPorts(enabled: Boolean) {
        onlyShowWithPorts = enabled
        applyFilterAndSort()
    }

    fun setSortBySpeed(enabled: Boolean) {
        sortBySpeed = enabled
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var list = devices.toList()
        if (onlyShowWithPorts) {
            list = list.filter { it.openPorts.isNotEmpty() }
        }
        if (sortBySpeed) {
            list = list.sortedBy { it.responseTimeMs }
        }
        displayList = list
        notifyDataSetChanged()
    }

    fun getAllDevices(): List<ScannedDevice> = devices.toList()
}