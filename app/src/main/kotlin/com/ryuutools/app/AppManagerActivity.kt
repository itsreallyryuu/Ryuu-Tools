package com.ryuutools.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppManagerActivity : BaseActivity() {

    private val neverTouch = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.gms",
        "com.android.vending",
        "com.android.providers.settings",
        "com.android.phone",
        "com.android.settings"
    )

    // Starter list — commonly known safe removals, not exhaustive. Always double check before disabling.
    private val knownSafeList = setOf(
        "com.facebook.appmanager",
        "com.facebook.services",
        "com.facebook.system",
        "com.samsung.android.bixby.agent",
        "com.samsung.android.app.spage",
        "com.samsung.android.themestore",
        "com.miui.miservice",
        "com.miui.bugreport",
        "com.heytap.market"
    )

    private lateinit var adapter: AppManagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnAutostart).setOnClickListener { openAutostartSettings() }

        val rv = findViewById<RecyclerView>(R.id.rvApps)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AppManagerAdapter { app, enable -> toggleApp(app, enable) }
        rv.adapter = adapter

        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(this, "Connect Root Mode in System Boost first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadApps()
    }

    private fun loadApps() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val excluded = getExcludedPackages(this) + neverTouch + packageName

        Thread {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val systemApps = apps.filter { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && !excluded.contains(app.packageName)
            }.sortedByDescending { knownSafeList.contains(it.packageName) }

            val entries = systemApps.map { app ->
                AppEntry(
                    packageName = app.packageName,
                    displayName = try { pm.getApplicationLabel(app).toString() } catch (e: Exception) { app.packageName },
                    icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                    isEnabled = app.enabled,
                    isKnownSafe = knownSafeList.contains(app.packageName)
                )
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                adapter.updateItems(entries)
            }
        }.start()
    }

    private fun toggleApp(app: AppEntry, enable: Boolean) {
        val command = if (enable) "pm enable ${app.packageName}" else "pm disable-user --user 0 ${app.packageName}"
        Thread {
            val (success, output) = ShizukuHelper.runCommand(command)
            runOnUiThread {
                if (success) {
                    app.isEnabled = enable
                    Toast.makeText(this, "${app.displayName} ${if (enable) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed: $output", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun openAutostartSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = when {
            manufacturer.contains("xiaomi") -> Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            manufacturer.contains("oppo") -> Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            manufacturer.contains("vivo") -> Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            manufacturer.contains("huawei") -> Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            else -> null
        }
        try {
            if (intent != null) startActivity(intent) else openAppSettingsFallback()
        } catch (e: Exception) {
            openAppSettingsFallback()
        }
    }

    private fun openAppSettingsFallback() {
        Toast.makeText(this, "Autostart settings not found for this device.", Toast.LENGTH_LONG).show()
        try { startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) } catch (e: Exception) { }
    }
}