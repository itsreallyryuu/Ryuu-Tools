package com.ryuutools.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NetworkScanActivity : AppCompatActivity() {

    private val foundDevices = CopyOnWriteArrayList<String>()
    private lateinit var adapter: DeviceScanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_scan)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvDevices)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = DeviceScanAdapter(foundDevices)
        rv.adapter = adapter

        val localIp = getLocalIpAddress()
        findViewById<TextView>(R.id.tvLocalIpInfo).text = "Your IP: ${localIp ?: "Unknown"}"

        findViewById<Button>(R.id.btnStartScan).setOnClickListener {
            if (localIp != null) {
                startScan(localIp)
            } else {
                findViewById<TextView>(R.id.tvScanStatus).text =
                    "Could not detect your local network. Make sure you're connected to WiFi."
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkScan", "Local IP error: ${e.message}")
        }
        return null
    }

    private fun startScan(localIp: String) {
        val btnStart = findViewById<Button>(R.id.btnStartScan)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = findViewById<TextView>(R.id.tvScanStatus)

        foundDevices.clear()
        adapter.notifyDataSetChanged()

        btnStart.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        tvStatus.text = "Scanning network..."

        val subnet = localIp.substringBeforeLast(".")
        val completedCount = AtomicInteger(0)
        val totalHosts = 254
        val latch = CountDownLatch(totalHosts)
        val executor = Executors.newFixedThreadPool(40)

        for (i in 1..totalHosts) {
            executor.execute {
                try {
                    val host = "$subnet.$i"
                    val address = InetAddress.getByName(host)
                    if (host != localIp && address.isReachable(400)) {
                        foundDevices.add(host)
                        runOnUiThread {
                            adapter.notifyItemInserted(foundDevices.size - 1)
                        }
                    }
                } catch (e: Exception) {
                    // Host tidak aktif / timeout, dilewati
                } finally {
                    val done = completedCount.incrementAndGet()
                    runOnUiThread {
                        progressBar.progress = (done * 100) / totalHosts
                        tvStatus.text = "Scanning... $done / $totalHosts"
                    }
                    latch.countDown()
                }
            }
        }

        Thread {
            latch.await()
            executor.shutdown()
            runOnUiThread {
                progressBar.visibility = View.GONE
                btnStart.isEnabled = true
                tvStatus.text = "Scan complete — ${foundDevices.size} device(s) found"
            }
        }.start()
    }
}