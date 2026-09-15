package com.ryuutools.app

import android.content.Context

object SystemBoostPrefs {
    private const val PREFS_NAME = "ryuu_prefs"
    private const val KEY_ENABLED = "system_boost_enabled"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Kalau user belum pernah toggle manual, ikutin status Root Mode yang sebenarnya
        // — biar toggle nggak kelihatan "ON" padahal belum pernah connect apa-apa.
        if (!prefs.contains(KEY_ENABLED)) {
            return ShizukuHelper.hasPermission()
        }
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}