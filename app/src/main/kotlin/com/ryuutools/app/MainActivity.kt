package com.ryuutools.app

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.view.View
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : BaseActivity() {

    private var batteryReceiver: BroadcastReceiver? = null
    private lateinit var drawerLayout: DrawerLayout

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Whether granted or denied, refresh the status line
        setupNetworkStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)

        setupVideoBanner()
        setupDeviceInfo()
        setupNetworkStatus()
        setupToolsGrid()
        setupHeaderMenu()
        setupDrawerMenu()
    }

    private fun setupHeaderMenu() {
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupDrawerMenu() {
        findViewById<View>(R.id.menuSettings).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.menuDonate).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            openUrl("https://saweria.co/itsmeryuu")
        }

        findViewById<View>(R.id.menuShare).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out Ryuu Tools — a handy all-in-one Android toolkit! 🚀")
                startActivity(Intent.createChooser(shareIntent, "Share Ryuu Tools"))
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to share right now.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.menuFeedback).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            openUrl("https://github.com/itsreallyryuu/Ryuu-Tools/issues/new")
        }

        findViewById<View>(R.id.menuAbout).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AboutActivity::class.java))
        }

        findViewById<View>(R.id.menuSystemBoost).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SystemBoostActivity::class.java))
        }
    }

    private fun setupVideoBanner() {
        val container = findViewById<FrameLayout>(R.id.videoContainer)
        val videoView = findViewById<VideoView>(R.id.videoBanner)

        container.clipToOutline = true
        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 18f * resources.displayMetrics.density)
            }
        }

        try {
            val uri = Uri.parse("android.resource://$packageName/${R.raw.dashboard_loop}")
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f)

                val videoWidth = mp.videoWidth
                val videoHeight = mp.videoHeight
                container.post {
                    val viewWidth = container.width
                    val viewHeight = container.height
                    if (videoWidth > 0 && videoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                        val videoRatio = videoWidth.toFloat() / videoHeight
                        val viewRatio = viewWidth.toFloat() / viewHeight
                        val lp = videoView.layoutParams
                        if (videoRatio > viewRatio) {
                            lp.height = viewHeight
                            lp.width = (viewHeight * videoRatio).toInt()
                        } else {
                            lp.width = viewWidth
                            lp.height = (viewWidth / videoRatio).toInt()
                        }
                        videoView.layoutParams = lp
                    }
                }
                videoView.start()
            }

            videoView.setOnErrorListener { _, _, _ -> true }
        } catch (e: Exception) {
            // Aman, video cuma tidak tampil kalau bermasalah
        }
    }

    private fun setupDeviceInfo() {
        val tvDeviceName = findViewById<TextView>(R.id.tvDeviceName)
        val tvCpuInfo = findViewById<TextView>(R.id.tvCpuInfo)
        val tvRamInfo = findViewById<TextView>(R.id.tvRamInfo)
        val tvStorageInfo = findViewById<TextView>(R.id.tvStorageInfo)
        val tvBattery = findViewById<TextView>(R.id.tvBatteryInfo)

        tvDeviceName.text = "Device: ${Build.MANUFACTURER} ${Build.MODEL}"

        val cores = Runtime.getRuntime().availableProcessors()
        tvCpuInfo.text = "CPU: ${getChipsetName()} ($cores cores)"

        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        tvRamInfo.text = "RAM: %.1f GB".format(totalRamGb)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalGb = statFs.totalBytes / (1024.0 * 1024.0 * 1024.0)
        val freeGb = statFs.availableBytes / (1024.0 * 1024.0 * 1024.0)
        tvStorageInfo.text = "Storage: %.1f GB free / %.1f GB".format(freeGb, totalGb)

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val percent = (level * 100) / scale
                    tvBattery.text = "Battery: $percent%"
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    /**
     * Shows Online/Offline, and if online, the WiFi SSID (when location
     * permission is granted) or the mobile carrier name.
     */
    private fun setupNetworkStatus() {
        val tvNetwork = findViewById<TextView>(R.id.tvNetworkStatus)
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            tvNetwork.text = "Network: Offline"
            return
        }

        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    tvNetwork.text = "Network: Online (WiFi)"
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    val ssid = getWifiSSID()
                    tvNetwork.text = if (ssid != null) "Network: Online (WiFi - $ssid)" else "Network: Online (WiFi)"
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val carrier = getCarrierName()
                tvNetwork.text = if (carrier != null) "Network: Online ($carrier)" else "Network: Online (Mobile Data)"
            }
            else -> {
                tvNetwork.text = "Network: Online"
            }
        }
    }

    private fun getWifiSSID(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var ssid = wifiManager.connectionInfo?.ssid
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length - 1)
            }
            if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") null else ssid
        } catch (e: Exception) {
            null
        }
    }

    private fun getCarrierName(): String? {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val name = tm.networkOperatorName
            if (name.isNullOrBlank()) null else name
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Android doesn't officially expose chipset/SoC names. On Android 12+ we can
     * read Build.SOC_MODEL directly. On older versions we fall back to a small
     * lookup table based on Build.HARDWARE/Build.BOARD, and if that also fails,
     * we're honest and just show the CPU ABI instead of guessing.
     */
    private fun getChipsetName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            if (!socModel.isNullOrBlank() && !socModel.equals("unknown", ignoreCase = true)) {
                return socModel
            }
        }

        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()

        val knownChipPrefixes = listOf(
            "qcom" to "Qualcomm Snapdragon",
            "exynos" to "Samsung Exynos",
            "universal" to "Samsung Exynos",
            "mt6" to "MediaTek",
            "mt8" to "MediaTek",
            "kirin" to "HiSilicon Kirin",
            "sm8" to "Qualcomm Snapdragon",
            "sm6" to "Qualcomm Snapdragon",
            "sm4" to "Qualcomm Snapdragon"
        )

        for ((key, name) in knownChipPrefixes) {
            if (hardware.contains(key) || board.contains(key)) {
                return name
            }
        }

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
        return "Unknown chipset ($abi)"
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // Aman kalau tidak ada app yang bisa buka link
        }
    }

    private fun setupToolsGrid() {
        val rv = findViewById<RecyclerView>(R.id.rvTools)
        rv.layoutManager = GridLayoutManager(this, 3)

        val tools = listOf(
            ToolItem("Device Temperature", R.drawable.ic_thermal, true),
            ToolItem("TikTok Downloader", R.drawable.ic_tiktok, true),
            ToolItem("Network Scan", R.drawable.ic_network, true),
            ToolItem("IP Checker", R.drawable.ic_ip, true),
            ToolItem("Speed Test", R.drawable.ic_speed, true),
            ToolItem("Quote Generator", R.drawable.ic_quote, true),
            ToolItem("Windows Quotes", R.drawable.ic_windows, true),
            ToolItem("Fake Certificate", R.drawable.ic_certificate, true),
            ToolItem("IQC Generator", R.drawable.ic_chat, true),
            ToolItem("Tanya Ustad", R.drawable.ic_ustad, true),
            ToolItem("Gaming Mode", R.drawable.ic_game, true),
            ToolItem("Memory Sweep", R.drawable.ic_broom, true),
            ToolItem("Live Stats", R.drawable.ic_chart, true),
            ToolItem("App Manager", R.drawable.ic_apps, true),
            ToolItem("Terminal", R.drawable.ic_terminal, true),
            ToolItem("VPN", R.drawable.ic_vpn, true),
            ToolItem("Calculator", R.drawable.ic_calculator, true)
        )

        rv.adapter = ToolsAdapter(tools) { item ->
            when (item.title) {
                "TikTok Downloader" -> startActivity(Intent(this, TikTokDownloaderActivity::class.java))
                "Device Temperature" -> startActivity(Intent(this, ThermalMonitorActivity::class.java))
                "IP Checker" -> startActivity(Intent(this, IPCheckerActivity::class.java))
                "Speed Test" -> startActivity(Intent(this, SpeedTestActivity::class.java))
                "Network Scan" -> startActivity(Intent(this, NetworkScanActivity::class.java))
                "Quote Generator" -> startActivity(Intent(this, IQCGeneratorActivity::class.java))
                "Windows Quotes" -> startActivity(Intent(this, WindowsDialogActivity::class.java))
                "Fake Certificate" -> startActivity(Intent(this, CertificateGeneratorActivity::class.java))
                "IQC Generator" -> startActivity(Intent(this, WhatsAppChatActivity::class.java))
                "Tanya Ustad" -> startActivity(Intent(this, TanyaUstadActivity::class.java))
                "Gaming Mode" -> startActivity(Intent(this, GamingModeActivity::class.java))
                "Memory Sweep" -> startActivity(Intent(this, MemorySweepActivity::class.java))
                "Live Stats" -> startActivity(Intent(this, LiveStatsActivity::class.java))
                "App Manager" -> startActivity(Intent(this, AppManagerActivity::class.java))
                "Terminal" -> startActivity(Intent(this, TerminalActivity::class.java))
                "VPN" -> startActivity(Intent(this, VpnActivity::class.java))
                "Calculator" -> startActivity(Intent(this, CalculatorActivity::class.java))
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSystemBoostDot()
        setupNetworkStatus()
    }

    private fun updateSystemBoostDot() {
        val dot = findViewById<View>(R.id.dotSystemBoostStatus)
        val connected = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()
        val color = if (connected) android.graphics.Color.parseColor("#4CD964") else android.graphics.Color.parseColor("#FF2D55")
        dot.background.setTint(color)
    }
}