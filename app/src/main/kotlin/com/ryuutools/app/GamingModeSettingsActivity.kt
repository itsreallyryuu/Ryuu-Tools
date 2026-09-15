package com.ryuutools.app

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch

class GamingModeSettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaming_mode_settings)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        bindSwitch(R.id.switchAnimations, "gaming_opt_animations", true)
        bindSwitch(R.id.switchDnd, "gaming_opt_dnd", false)
        bindSwitch(R.id.switchRefreshRate, "gaming_opt_refresh", false)
        bindSwitch(R.id.switchMaxBrightness, "gaming_opt_brightness", false)
        bindSwitch(R.id.switchDisableBatterySaver, "gaming_opt_battery_saver", false)
        bindSwitch(R.id.switchScreenTimeout, "gaming_opt_timeout", false)
        bindSwitch(R.id.switchLockRotation, "gaming_opt_rotation", false)
        bindSwitch(R.id.switchCleanOnStart, "gaming_opt_clean_start", false)
    }

    private fun bindSwitch(id: Int, prefKey: String, default: Boolean) {
        val switch = findViewById<Switch>(id)
        switch.isChecked = prefs.getBoolean(prefKey, default)
        switch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(prefKey, checked).apply()
        }
    }
}