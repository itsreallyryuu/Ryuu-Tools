package com.ryuutools.app

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var batteryReceiver: BroadcastReceiver? = null
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)

        setupVideoBanner()
        setupDeviceInfo()
        setupSocialButtons()
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

    findViewById<View>(R.id.menuShare).setOnClickListener {
        drawerLayout.closeDrawer(GravityCompat.START)
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Check out Ryuu Tools — a handy all-in-one Android toolkit! 🚀"
            )
            startActivity(Intent.createChooser(shareIntent, "Share Ryuu Tools"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share right now.", Toast.LENGTH_SHORT).show()
        }
    }

    findViewById<View>(R.id.menuFeedback).setOnClickListener {
    drawerLayout.closeDrawer(GravityCompat.START)
    try {
        startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://github.com/itsreallyryuu/Ryuu-Tools/issues/new")))
    } catch (e: Exception) {
        Toast.makeText(this, "Unable to open link.", Toast.LENGTH_SHORT).show()
    }
}

    findViewById<View>(R.id.menuAbout).setOnClickListener {
        drawerLayout.closeDrawer(GravityCompat.START)
        startActivity(Intent(this, AboutActivity::class.java))
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
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
        tvCpuInfo.text = "CPU: $abi ($cores cores)"

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

    private fun setupSocialButtons() {
        findViewById<Button>(R.id.btnWhatsapp).setOnClickListener {
            openUrl("https://whatsapp.com/channel/0029VbC4xiq3wtbC3VsYt127")
        }
        findViewById<Button>(R.id.btnTiktok).setOnClickListener {
            openUrl("https://vm.tiktok.com/ZS9S8ftJeT1dt-imXTA/")
        }
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
        ToolItem("Thermal Monitor", R.drawable.ic_thermal, true),
        ToolItem("TikTok Downloader", R.drawable.ic_tiktok, true),
        ToolItem("Network Scan", R.drawable.ic_network, true),
        ToolItem("IP Checker", R.drawable.ic_ip, true),
        ToolItem("Speed Test", R.drawable.ic_speed, true),
        ToolItem("Quote Generator", R.drawable.ic_quote, true),
        ToolItem("Windows Quotes", R.drawable.ic_windows, true),
        ToolItem("Sertifikat Tolol", R.drawable.ic_certificate, true),
        ToolItem("Profile Card", R.drawable.ic_profile, true)
    )

    rv.adapter = ToolsAdapter(tools) { item ->
        when (item.title) {
            "TikTok Downloader" -> startActivity(Intent(this, TikTokDownloaderActivity::class.java))
            "Thermal Monitor" -> startActivity(Intent(this, ThermalMonitorActivity::class.java))
            "IP Checker" -> startActivity(Intent(this, IPCheckerActivity::class.java))
            "Speed Test" -> startActivity(Intent(this, SpeedTestActivity::class.java))
            "Network Scan" -> startActivity(Intent(this, NetworkScanActivity::class.java))
            "Quote Generator" -> startActivity(Intent(this, IQCGeneratorActivity::class.java))
            "Windows Quotes" -> startActivity(Intent(this, WindowsDialogActivity::class.java))
            "Sertifikat Tolol" -> startActivity(Intent(this, CertificateGeneratorActivity::class.java))
            "Profile Card" -> startActivity(Intent(this, ProfileCardActivity::class.java))
        }
    }
}

private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= 33) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501
            )
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
}