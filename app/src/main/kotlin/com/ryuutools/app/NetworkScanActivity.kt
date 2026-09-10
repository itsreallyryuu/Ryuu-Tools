package com.ryuutools.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NetworkScanActivity : BaseActivity() {

    private val foundDevices = CopyOnWriteArrayList<ScannedDevice>()
    private lateinit var adapter: DeviceScanAdapter
    private lateinit var prefs: SharedPreferences

    private var filterActive = false
    private var sortActive = false

    // Common ports checked per found device. Kept short so the scan stays fast.
    private val commonPorts = listOf(21, 22, 23, 80, 139, 443, 445, 3389, 5000, 8080, 8443, 9100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_scan)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvDevices)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = DeviceScanAdapter(foundDevices.toMutableList())
        rv.adapter = adapter

        val localIp = getLocalIpAddress()
        findViewById<TextView>(R.id.tvLocalIpInfo).text = "Your IP: ${localIp ?: "Unknown"}"

        loadNetworkInfo()
        loadLastScanInfo()

        findViewById<Button>(R.id.btnStartScan).setOnClickListener {
            if (!NetworkUtils.isOnWifi(this)) {
                NetworkUtils.showWifiRequiredWarning(this)
            } else if (localIp != null) {
                startScan(localIp)
            } else {
                findViewById<TextView>(R.id.tvScanStatus).text =
                    "Could not detect your local network. Make sure you're connected to WiFi."
            }
        }

        findViewById<Button>(R.id.btnToggleFilter).setOnClickListener {
            filterActive = !filterActive
            adapter.setFilterOnlyWithPorts(filterActive)
            (it as Button).text = if (filterActive) "Filter: Open Ports Only" else "Show All"
        }

        findViewById<Button>(R.id.btnToggleSort).setOnClickListener {
            sortActive = !sortActive
            adapter.setSortBySpeed(sortActive)
            (it as Button).text = if (sortActive) "Sort: Fastest First" else "Sort: Default"
        }

        findViewById<Button>(R.id.btnExportResults).setOnClickListener {
            exportResults()
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

    /** Shows gateway, subnet mask, and DNS servers using the official ConnectivityManager APIs. */
    private fun loadNetworkInfo() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val linkProperties = activeNetwork?.let { cm.getLinkProperties(it) }

            if (linkProperties == null) {
                findViewById<TextView>(R.id.tvGatewayInfo).text = "Gateway: Unknown"
                findViewById<TextView>(R.id.tvSubnetInfo).text = "Subnet Mask: Unknown"
                findViewById<TextView>(R.id.tvDnsInfo).text = "DNS: Unknown"
                return
            }

            val gateway = linkProperties.routes
                .firstOrNull { it.isDefaultRoute && it.gateway != null }
                ?.gateway?.hostAddress ?: "Unknown"
            findViewById<TextView>(R.id.tvGatewayInfo).text = "Gateway: $gateway"

            val prefixLength = linkProperties.linkAddresses
                .firstOrNull { !it.address.isLoopbackAddress && it.address.hostAddress?.contains(":") == false }
                ?.prefixLength
            val subnetMask = if (prefixLength != null) prefixLengthToSubnetMask(prefixLength) else "Unknown"
            findViewById<TextView>(R.id.tvSubnetInfo).text = "Subnet Mask: $subnetMask"

            val dnsServers = linkProperties.dnsServers.joinToString(", ") { it.hostAddress ?: "" }
            findViewById<TextView>(R.id.tvDnsInfo).text =
                "DNS: ${if (dnsServers.isBlank()) "Unknown" else dnsServers}"
        } catch (e: Exception) {
            Log.e("NetworkScan", "Network info error: ${e.message}")
        }
    }

    private fun prefixLengthToSubnetMask(prefixLength: Int): String {
        val mask = -0x1 shl (32 - prefixLength)
        return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
    }

    private fun loadLastScanInfo() {
        val lastTime = prefs.getLong("last_network_scan_time", -1L)
        val lastCount = prefs.getInt("last_network_scan_count", -1)
        val tvLastScan = findViewById<TextView>(R.id.tvLastScanInfo)

        if (lastTime > 0) {
            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(lastTime))
            tvLastScan.text = "Last scan: $dateStr — $lastCount device(s) found"
        } else {
            tvLastScan.text = "No previous scan found"
        }
    }

    private fun saveLastScanInfo(count: Int) {
        prefs.edit()
            .putLong("last_network_scan_time", System.currentTimeMillis())
            .putInt("last_network_scan_count", count)
            .apply()
        loadLastScanInfo()
    }

    private fun startScan(localIp: String) {
        val btnStart = findViewById<Button>(R.id.btnStartScan)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = findViewById<TextView>(R.id.tvScanStatus)
        val filterSortRow = findViewById<View>(R.id.filterSortRow)
        val btnExport = findViewById<Button>(R.id.btnExportResults)

        foundDevices.clear()
        adapter.getAllDevices().let { /* no-op, adapter rebuilt below */ }
        rebuildAdapter()

        btnStart.isEnabled = false
        filterSortRow.visibility = View.GONE
        btnExport.visibility = View.GONE
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
                    val startTime = SystemClock.elapsedRealtime()
                    val address = InetAddress.getByName(host)
                    val reachable = host != localIp && address.isReachable(400)
                    val responseTime = SystemClock.elapsedRealtime() - startTime

                    if (reachable) {
                        val hostname = try {
                            address.canonicalHostName ?: host
                        } catch (e: Exception) {
                            host
                        }

                        val openPorts = scanPorts(host)
                        val deviceType = guessDeviceType(openPorts)

                        val device = ScannedDevice(
                            ip = host,
                            hostname = hostname,
                            responseTimeMs = responseTime,
                            openPorts = openPorts,
                            deviceTypeGuess = deviceType
                        )
                        foundDevices.add(device)

                        runOnUiThread {
                            rebuildAdapter()
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
                saveLastScanInfo(foundDevices.size)

                if (foundDevices.isNotEmpty()) {
                    filterSortRow.visibility = View.VISIBLE
                    btnExport.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    /** Quick TCP connect check on common ports. Short timeout per port to keep the scan fast. */
    private fun scanPorts(ip: String): List<Int> {
        val open = mutableListOf<Int>()
        for (port in commonPorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 150)
                    open.add(port)
                }
            } catch (e: Exception) {
                // Port closed or filtered, skip
            }
        }
        return open
    }

    /** Rough, honest guess only — based on which common ports respond. Not a real fingerprint. */
    private fun guessDeviceType(ports: List<Int>): String {
        return when {
            ports.contains(3389) -> "Likely Windows (RDP open)"
            ports.contains(445) || ports.contains(139) -> "Likely Windows / file share (SMB)"
            ports.contains(9100) -> "Likely a network printer"
            ports.contains(22) -> "Likely Linux / SSH-enabled device"
            ports.contains(21) -> "Likely FTP server"
            ports.contains(80) || ports.contains(443) || ports.contains(8080) || ports.contains(8443) -> "Likely router / web-managed device"
            else -> "Unknown device type"
        }
    }

    private fun rebuildAdapter() {
        val rv = findViewById<RecyclerView>(R.id.rvDevices)
        adapter = DeviceScanAdapter(foundDevices.toMutableList())
        adapter.setFilterOnlyWithPorts(filterActive)
        adapter.setSortBySpeed(sortActive)
        rv.adapter = adapter
    }

    private fun exportResults() {
        try {
            val devices = adapter.getAllDevices()
            if (devices.isEmpty()) {
                Toast.makeText(this, "No scan results to export", Toast.LENGTH_SHORT).show()
                return
            }

            val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val content = buildString {
                appendLine("Ryuu Tools — Network Scan Results")
                appendLine("Generated: ${SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.US).format(Date())}")
                appendLine("Devices found: ${devices.size}")
                appendLine("--------------------------------------")
                devices.forEach { device ->
                    appendLine("IP: ${device.ip}")
                    appendLine("Hostname: ${device.hostname}")
                    appendLine("Response time: ${device.responseTimeMs} ms")
                    appendLine("Device type guess: ${device.deviceTypeGuess}")
                    appendLine(
                        "Open ports: ${if (device.openPorts.isEmpty()) "none found" else device.openPorts.joinToString(", ")}"
                    )
                    appendLine("--------------------------------------")
                }
            }

            val file = File(getExternalFilesDir(null), "RyuuTools_NetworkScan_$dateStr.txt")
            file.writeText(content)

            val uri = FileProvider.getUriForFile(this, "com.ryuutools.app.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export Scan Results"))
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}