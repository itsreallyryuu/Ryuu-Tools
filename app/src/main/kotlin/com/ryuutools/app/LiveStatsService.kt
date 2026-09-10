package com.ryuutools.app

import android.app.ActivityManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class LiveStatsService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private var lastIdle = 0L
    private var lastTotal = 0L
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastNetTimestamp = 0L
    private var currentTempC = 0f

    private var lastFrameCount = -1L
    private var lastFrameTimestamp = 0L
    private var lastFpsPackage: String? = null

    private var batteryReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(4001, buildNotification())
        showOverlay()
        registerBatteryReceiver()
        isRunning = true
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun buildNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, NotificationHelper.LIVE_STATS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Live Stats is running")
            .setContentText("Tap the overlay's X button to stop")
            .setOngoing(true)
            .build()
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (tempTenths >= 0) currentTempC = tempTenths / 10f
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_live_stats, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 100

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        overlayView?.findViewById<TextView>(R.id.overlayCloseButton)?.setOnClickListener { stopSelf() }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            updateStats()
            handler.postDelayed(this, 1500)
        }
    }

    private fun updateStats() {
        val shizukuReady = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()

        val cpu = if (shizukuReady) getCpuUsagePercentViaShell() else -1
        val fps = if (shizukuReady) getForegroundAppFps() else -1
        val (down, up) = getNetworkSpeedKbps()
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val availGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)

        overlayView?.findViewById<TextView>(R.id.tvOverlayCpu)?.text =
            if (cpu >= 0) "CPU: $cpu%" else if (shizukuReady) "CPU: N/A" else "CPU: connect Root Mode"
        overlayView?.findViewById<TextView>(R.id.tvOverlayFps)?.text =
            if (fps >= 0) "FPS: $fps" else "FPS: N/A"
        overlayView?.findViewById<TextView>(R.id.tvOverlayRam)?.text = "RAM: %.1f GB free".format(availGb)
        overlayView?.findViewById<TextView>(R.id.tvOverlayTemp)?.text = "Temp: %.1f°C".format(currentTempC)
        overlayView?.findViewById<TextView>(R.id.tvOverlayNet)?.text = "Net: ↓$down ↑$up KB/s"
    }

    /**
     * Direct /proc/stat reads from regular app code are blocked by SELinux on many
     * modern devices (confirmed on the dev's device). Reading it via an ADB shell
     * command through Shizuku bypasses that restriction, since the shell user has
     * broader read access than a regular app. Requires Root Mode/Shizuku connected;
     * falls back to N/A otherwise (handled in updateStats()).
     */
    private fun getCpuUsagePercentViaShell(): Int {
        return try {
            val (success, output) = ShizukuHelper.runCommand("cat /proc/stat")
            if (!success || output.isBlank()) return -1

            val firstLine = output.lineSequence().firstOrNull { it.trim().startsWith("cpu ") } ?: return -1
            val toks = firstLine.trim().split(Regex("\\s+"))
            if (toks.size < 8) return -1

            val idle = toks[4].toLong()
            val total = toks.drop(1).take(7).sumOf { it.toLong() }

            val diffIdle = idle - lastIdle
            val diffTotal = total - lastTotal
            lastIdle = idle
            lastTotal = total

            if (lastTotal == total && diffTotal == 0L) return -1 // first read, no baseline yet
            if (diffTotal <= 0) -1 else (((diffTotal - diffIdle) * 100) / diffTotal).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Approximate FPS of the current foreground app via dumpsys, through Shizuku.
     * This is an estimate (frames rendered / time since last read via gfxinfo),
     * not a precise per-frame measurement — Android has no public API for that
     * from an overlay service. Requires Root Mode/Shizuku connected.
     */
    private fun getForegroundAppFps(): Int {
        return try {
            val foregroundPackage = getForegroundPackage() ?: return -1

            // Reset counters if the foreground app changed, otherwise the diff is meaningless
            if (foregroundPackage != lastFpsPackage) {
                lastFpsPackage = foregroundPackage
                lastFrameCount = -1
                ShizukuHelper.runCommand("dumpsys gfxinfo $foregroundPackage reset")
            }

            val (success, output) = ShizukuHelper.runCommand("dumpsys gfxinfo $foregroundPackage")
            if (!success || output.isBlank()) return -1

            val totalFramesLine = output.lineSequence()
                .firstOrNull { it.trim().startsWith("Total frames rendered:") }
                ?: return -1

            val frameCount = totalFramesLine.substringAfter(":").trim().toLongOrNull() ?: return -1
            val now = System.currentTimeMillis()

            if (lastFrameCount < 0) {
                lastFrameCount = frameCount
                lastFrameTimestamp = now
                return -1
            }

            val elapsedSec = (now - lastFrameTimestamp) / 1000.0
            val frameDiff = frameCount - lastFrameCount
            lastFrameCount = frameCount
            lastFrameTimestamp = now

            if (elapsedSec <= 0 || frameDiff < 0) return -1
            (frameDiff / elapsedSec).toInt().coerceIn(0, 240)
        } catch (e: Exception) {
            -1
        }
    }

    private fun getForegroundPackage(): String? {
        return try {
            val (success, output) = ShizukuHelper.runCommand("dumpsys window windows")
            if (!success) return null
            val line = output.lineSequence().firstOrNull {
                it.contains("mCurrentFocus") || it.contains("mFocusedApp")
            } ?: return null
            Regex("([a-zA-Z0-9_.]+)/[a-zA-Z0-9_.]+").find(line)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun getNetworkSpeedKbps(): Pair<Int, Int> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = System.currentTimeMillis()
        if (lastNetTimestamp == 0L) {
            lastRxBytes = rx; lastTxBytes = tx; lastNetTimestamp = now
            return Pair(0, 0)
        }
        val elapsedSec = (now - lastNetTimestamp) / 1000.0
        val downKbps = if (elapsedSec > 0) ((rx - lastRxBytes) / 1024.0 / elapsedSec).toInt() else 0
        val upKbps = if (elapsedSec > 0) ((tx - lastTxBytes) / 1024.0 / elapsedSec).toInt() else 0
        lastRxBytes = rx; lastTxBytes = tx; lastNetTimestamp = now
        return Pair(downKbps, upKbps)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        try { overlayView?.let { windowManager?.removeView(it) } } catch (e: Exception) { }
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}