package com.ryuutools.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class SystemBoostActivity : BaseActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var switchEnabled: Switch
    private var isUserOverridingSwitch = false

    private val shizukuPackage = "moe.shizuku.privileged.api"

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_boost)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        tvStatus = findViewById(R.id.tvRootModeStatus)
        btnConnect = findViewById(R.id.btnConnectRootMode)
        switchEnabled = findViewById(R.id.switchSystemBoostEnabled)

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        btnConnect.setOnClickListener { handleConnectClick() }
        findViewById<Button>(R.id.btnHowTo).setOnClickListener { showGuide() }
        findViewById<Button>(R.id.btnTestConnection).setOnClickListener { testConnection() }

        switchEnabled.setOnCheckedChangeListener { _, checked ->
            isUserOverridingSwitch = true
            SystemBoostPrefs.setEnabled(this, checked)
            if (!checked) {
                androidx.work.WorkManager.getInstance(this).cancelUniqueWork("memory_sweep_work")
                Toast.makeText(this, "System Boost features are now off. Underlying Shizuku session stays open — stop it fully from the Shizuku app if needed.", Toast.LENGTH_LONG).show()
            }
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun handleConnectClick() {
        if (!ShizukuHelper.isAvailable()) {
            Toast.makeText(this, "Shizuku is not running yet. Follow the guide below.", Toast.LENGTH_LONG).show()
            showGuide()
            return
        }
        if (ShizukuHelper.hasPermission()) {
            refreshStatus()
            return
        }
        ShizukuHelper.requestPermission { granted ->
            runOnUiThread {
                if (granted) {
                    Toast.makeText(this, "Root Mode connected!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
                }
                refreshStatus()
            }
        }
    }

    private fun testConnection() {
        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(this, "Connect Root Mode first.", Toast.LENGTH_SHORT).show()
            return
        }
        val (success, output) = ShizukuHelper.runCommand("id")
        AlertDialog.Builder(this)
            .setTitle(if (success) "Test Successful" else "Test Failed")
            .setMessage(output.ifBlank { "No output returned." })
            .setPositiveButton("OK", null)
            .show()
    }

    private fun refreshStatus() {
        val available = ShizukuHelper.isAvailable()
        val granted = available && ShizukuHelper.hasPermission()
        tvStatus.text = when {
            granted -> "Root Mode: Connected"
            available -> "Root Mode: Permission Needed"
            else -> "Root Mode: Not Active"
        }
        btnConnect.text = if (granted) "Connected" else "Connect Root Mode"
        btnConnect.isEnabled = !granted

        // Cuma auto-sync kalau user belum pernah toggle manual — biar pilihan manual
        // user tidak ketiban-timpa tiap kali halaman ini di-refresh.
        if (!isUserOverridingSwitch) {
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = SystemBoostPrefs.isEnabled(this)
            switchEnabled.setOnCheckedChangeListener { _, checked ->
                isUserOverridingSwitch = true
                SystemBoostPrefs.setEnabled(this, checked)
                if (!checked) {
                    androidx.work.WorkManager.getInstance(this).cancelUniqueWork("memory_sweep_work")
                    Toast.makeText(this, "System Boost features are now off. Underlying Shizuku session stays open — stop it fully from the Shizuku app if needed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showGuide() {
        val isModern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val message = if (isModern) {
            "1. Install the 'Shizuku' app (button below)\n" +
                "2. Go to Settings > Developer Options > enable 'Wireless debugging'\n" +
                "3. Open Shizuku, tap 'Start' under 'Wireless debugging'\n" +
                "4. Come back here and tap 'Connect Root Mode'"
        } else {
            "Your Android version is older than 11, so first-time activation needs a computer:\n" +
                "1. Install the 'Shizuku' app (button below)\n" +
                "2. Connect your phone to a computer and run one ADB command (see Shizuku's own guide)\n" +
                "3. Come back here and tap 'Connect Root Mode'"
        }
        AlertDialog.Builder(this)
            .setTitle("How to Activate Root Mode")
            .setMessage(message)
            .setPositiveButton("Open Shizuku") { _, _ -> openOrInstallShizuku() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun openOrInstallShizuku() {
        val launchIntent = packageManager.getLaunchIntentForPackage(shizukuPackage)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$shizukuPackage")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$shizukuPackage")))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}