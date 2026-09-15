package com.ryuutools.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GamingModeActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvStatus: TextView
    private lateinit var switchMaster: Switch

    private lateinit var rvBoostedApps: RecyclerView
    private lateinit var tvNoBoostedApps: TextView
    private lateinit var boostedAppsAdapter: BoostedAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaming_mode)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnGamingSettings).setOnClickListener {
            startActivity(Intent(this, GamingModeSettingsActivity::class.java))
        }

        tvStatus = findViewById(R.id.tvGamingStatus)
        switchMaster = findViewById(R.id.switchGamingMaster)

        rvBoostedApps = findViewById(R.id.rvBoostedApps)
        tvNoBoostedApps = findViewById(R.id.tvNoBoostedApps)
        rvBoostedApps.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnAddBoostedApp).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        val isActive = prefs.getBoolean("gaming_mode_active", false)
        switchMaster.isChecked = isActive
        updateStatusText(isActive)

        switchMaster.setOnCheckedChangeListener { _, checked ->
            if (checked && !ShizukuHelper.hasPermission()) {
                Toast.makeText(this, "Connect Root Mode in System Boost first.", Toast.LENGTH_LONG).show()
                switchMaster.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (checked) enableGamingMode() else disableGamingMode()
        }
    }

    override fun onResume() {
        super.onResume()
        loadBoostedApps()
    }

    private fun loadBoostedApps() {
        val packageNames = prefs.getStringSet("gaming_boosted_apps", emptySet()) ?: emptySet()
        val pm = packageManager
        val validEntries = mutableListOf<BoostedAppEntry>()
        var staleFound = false

        for (pkg in packageNames) {
            try {
                val label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                val icon = pm.getApplicationIcon(pkg)
                validEntries.add(BoostedAppEntry(pkg, label, icon))
            } catch (e: PackageManager.NameNotFoundException) {
                staleFound = true
            }
        }

        if (staleFound) {
            val cleaned = validEntries.map { it.packageName }.toSet()
            prefs.edit().putStringSet("gaming_boosted_apps", cleaned).apply()
        }

        validEntries.sortBy { it.label.lowercase() }

        tvNoBoostedApps.visibility = if (validEntries.isEmpty()) View.VISIBLE else View.GONE
        rvBoostedApps.visibility = if (validEntries.isEmpty()) View.GONE else View.VISIBLE

        boostedAppsAdapter = BoostedAppsAdapter(
            apps = validEntries,
            onLaunch = { entry -> launchBoostedApp(entry.packageName) },
            onRemove = { entry -> removeBoostedApp(entry.packageName) }
        )
        rvBoostedApps.adapter = boostedAppsAdapter
    }

    private fun removeBoostedApp(pkg: String) {
        val current = (prefs.getStringSet("gaming_boosted_apps", emptySet()) ?: emptySet()).toMutableSet()
        current.remove(pkg)
        prefs.edit().putStringSet("gaming_boosted_apps", current).apply()
        loadBoostedApps()
    }

    private fun launchBoostedApp(pkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            Toast.makeText(this, "Couldn't launch this app — it may have been uninstalled.", Toast.LENGTH_SHORT).show()
            removeBoostedApp(pkg)
            return
        }

        if (!switchMaster.isChecked && ShizukuHelper.hasPermission()) {
            switchMaster.isChecked = true
        }

        startActivity(launchIntent)
    }

    private fun updateStatusText(active: Boolean) {
        tvStatus.text = if (active) "Gaming Mode: ON" else "Gaming Mode: OFF"
    }

    private fun enableGamingMode() {
        if (prefs.getBoolean("gaming_opt_animations", true)) {
            val currentScale = ShizukuHelper.runCommand("settings get global window_animation_scale").second
            prefs.edit().putString("saved_anim_scale", currentScale.ifBlank { "1.0" }).apply()
            ShizukuHelper.runCommand("settings put global window_animation_scale 0")
            ShizukuHelper.runCommand("settings put global transition_animation_scale 0")
            ShizukuHelper.runCommand("settings put global animator_duration_scale 0")
        }

        if (prefs.getBoolean("gaming_opt_refresh", false)) {
            val maxRate = getMaxRefreshRate()
            if (maxRate > 0) {
                val currentPeak = ShizukuHelper.runCommand("settings get system peak_refresh_rate").second
                prefs.edit().putString("saved_peak_refresh", currentPeak).apply()
                ShizukuHelper.runCommand("settings put system peak_refresh_rate $maxRate")
                ShizukuHelper.runCommand("settings put system min_refresh_rate $maxRate")
            }
        }

        if (prefs.getBoolean("gaming_opt_brightness", false)) {
            val currentBrightness = ShizukuHelper.runCommand("settings get system screen_brightness").second
            val currentMode = ShizukuHelper.runCommand("settings get system screen_brightness_mode").second
            prefs.edit()
                .putString("saved_brightness", currentBrightness.ifBlank { "128" })
                .putString("saved_brightness_mode", currentMode.ifBlank { "1" })
                .apply()
            ShizukuHelper.runCommand("settings put system screen_brightness_mode 0")
            ShizukuHelper.runCommand("settings put system screen_brightness 255")
        }

        if (prefs.getBoolean("gaming_opt_battery_saver", false)) {
            val currentSaver = ShizukuHelper.runCommand("settings get global low_power").second
            prefs.edit().putString("saved_battery_saver", currentSaver.ifBlank { "0" }).apply()
            ShizukuHelper.runCommand("settings put global low_power 0")
        }

        if (prefs.getBoolean("gaming_opt_timeout", false)) {
            val currentTimeout = ShizukuHelper.runCommand("settings get system screen_off_timeout").second
            prefs.edit().putString("saved_screen_timeout", currentTimeout.ifBlank { "30000" }).apply()
            ShizukuHelper.runCommand("settings put system screen_off_timeout 1800000")
        }

        if (prefs.getBoolean("gaming_opt_rotation", false)) {
            val currentRotation = ShizukuHelper.runCommand("settings get system accelerometer_rotation").second
            prefs.edit().putString("saved_rotation", currentRotation.ifBlank { "1" }).apply()
            ShizukuHelper.runCommand("settings put system accelerometer_rotation 0")
        }

        if (prefs.getBoolean("gaming_opt_dnd", false)) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                android.app.AlertDialog.Builder(this)
                    .setTitle("One More Step")
                    .setMessage("On the next screen, find 'Ryuu Tools' in the list and turn it ON. Then come back here.")
                    .setPositiveButton("Continue") { _, _ ->
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                    .show()
            }
        }

        prefs.edit().putBoolean("gaming_mode_active", true).apply()
        updateStatusText(true)

        if (prefs.getBoolean("gaming_opt_clean_start", false)) {
            runCleanOnStart()
        } else {
            Toast.makeText(this, "Gaming Mode activated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runCleanOnStart() {
        Thread {
            val stoppedCount = try { sweepBackgroundApps(this) } catch (e: Exception) { 0 }
            runOnUiThread {
                Toast.makeText(this, "Gaming Mode activated. Closed $stoppedCount background app(s).", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun disableGamingMode() {
        val savedScale = prefs.getString("saved_anim_scale", "1.0") ?: "1.0"
        ShizukuHelper.runCommand("settings put global window_animation_scale $savedScale")
        ShizukuHelper.runCommand("settings put global transition_animation_scale $savedScale")
        ShizukuHelper.runCommand("settings put global animator_duration_scale $savedScale")

        val savedPeak = prefs.getString("saved_peak_refresh", "") ?: ""
        if (savedPeak.isNotBlank() && savedPeak != "null") {
            ShizukuHelper.runCommand("settings put system peak_refresh_rate $savedPeak")
        } else {
            ShizukuHelper.runCommand("settings delete system peak_refresh_rate")
        }
        ShizukuHelper.runCommand("settings delete system min_refresh_rate")

        val savedBrightness = prefs.getString("saved_brightness", "") ?: ""
        val savedBrightnessMode = prefs.getString("saved_brightness_mode", "") ?: ""
        if (savedBrightness.isNotBlank() && savedBrightness != "null") {
            ShizukuHelper.runCommand("settings put system screen_brightness $savedBrightness")
        }
        if (savedBrightnessMode.isNotBlank() && savedBrightnessMode != "null") {
            ShizukuHelper.runCommand("settings put system screen_brightness_mode $savedBrightnessMode")
        }

        val savedSaver = prefs.getString("saved_battery_saver", "") ?: ""
        if (savedSaver.isNotBlank() && savedSaver != "null") {
            ShizukuHelper.runCommand("settings put global low_power $savedSaver")
        }

        val savedTimeout = prefs.getString("saved_screen_timeout", "") ?: ""
        if (savedTimeout.isNotBlank() && savedTimeout != "null") {
            ShizukuHelper.runCommand("settings put system screen_off_timeout $savedTimeout")
        }

        val savedRotation = prefs.getString("saved_rotation", "") ?: ""
        if (savedRotation.isNotBlank() && savedRotation != "null") {
            ShizukuHelper.runCommand("settings put system accelerometer_rotation $savedRotation")
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        prefs.edit().putBoolean("gaming_mode_active", false).apply()
        updateStatusText(false)
        Toast.makeText(this, "Gaming Mode deactivated.", Toast.LENGTH_SHORT).show()
    }

    private fun getMaxRefreshRate(): Int {
        return try {
            val modes = windowManager.defaultDisplay.supportedModes
            modes.maxOf { it.refreshRate }.toInt()
        } catch (e: Exception) {
            0
        }
    }
}