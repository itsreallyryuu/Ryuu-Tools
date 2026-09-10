package com.ryuutools.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class VpnActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchVpn: Switch
    private lateinit var tvStatus: TextView

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startActualVpnService()
        } else {
            switchVpn.isChecked = false
            Toast.makeText(this, "VPN permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private val statsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val queries = intent.getIntExtra(RyuuVpnService.EXTRA_QUERIES, 0)
            val blocked = intent.getIntExtra(RyuuVpnService.EXTRA_BLOCKED, 0)
            findViewById<TextView>(R.id.tvQueryCount).text = "DNS queries: $queries"
            findViewById<TextView>(R.id.tvBlockedCount).text = "Blocked: $blocked"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        switchVpn = findViewById(R.id.switchVpn)
        tvStatus = findViewById(R.id.tvVpnStatus)

        switchVpn.isChecked = RyuuVpnService.isRunning
        updateStatusText(RyuuVpnService.isRunning)

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupDns)
        when (prefs.getString("vpn_upstream_dns", "1.1.1.1")) {
            "8.8.8.8" -> radioGroup.check(R.id.radioGoogle)
            "9.9.9.9" -> radioGroup.check(R.id.radioQuad9)
            else -> radioGroup.check(R.id.radioCloudflare)
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val dns = when (checkedId) {
                R.id.radioGoogle -> "8.8.8.8"
                R.id.radioQuad9 -> "9.9.9.9"
                else -> "1.1.1.1"
            }
            prefs.edit().putString("vpn_upstream_dns", dns).apply()
            if (RyuuVpnService.isRunning) {
                Toast.makeText(this, "Restart the VPN for the new DNS server to take effect.", Toast.LENGTH_SHORT).show()
            }
        }

        val switchAdBlock = findViewById<Switch>(R.id.switchAdBlock)
        switchAdBlock.isChecked = prefs.getBoolean("vpn_ad_block_enabled", true)
        switchAdBlock.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("vpn_ad_block_enabled", checked).apply()
        }

        switchVpn.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                requestVpnPermissionAndStart()
            } else {
                stopVpnService()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(RyuuVpnService.BROADCAST_STATS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statsReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(statsReceiver) } catch (e: Exception) { }
    }

    private fun requestVpnPermissionAndStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startActualVpnService()
        }
    }

    private fun startActualVpnService() {
        startService(Intent(this, RyuuVpnService::class.java))
        updateStatusText(true)
        Toast.makeText(this, "Ryuu VPN connected.", Toast.LENGTH_SHORT).show()
    }

    private fun stopVpnService() {
        val stopIntent = Intent(this, RyuuVpnService::class.java).apply { action = RyuuVpnService.ACTION_STOP }
        startService(stopIntent)
        updateStatusText(false)
        Toast.makeText(this, "Ryuu VPN disconnected.", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusText(connected: Boolean) {
        tvStatus.text = if (connected) "Connected" else "Disconnected"
    }
}