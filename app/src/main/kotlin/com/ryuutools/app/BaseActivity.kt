package com.ryuutools.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Base Activity for all screens in Ryuu Tools.
 * Adds a consistent slide+fade transition whenever navigating to a new
 * screen or going back, so navigation feels less like an instant jump-cut.
 */
open class BaseActivity : AppCompatActivity() {

    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        super.startActivity(intent, options)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java, kept for back-press compatibility")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}