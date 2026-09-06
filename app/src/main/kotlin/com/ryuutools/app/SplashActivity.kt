package com.ryuutools.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    private var currentStep = 0
    private val totalSteps = 10
    private val stepIntervalMillis = 1300L // total ±13 detik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)
        val savedVersionCode = prefs.getInt("last_seen_version_code", -1)
        val currentVersionCode = getAppVersionCode()

        // Sudah pernah lihat splash untuk versi app ini -> langsung ke Dashboard, skip splash
        if (savedVersionCode == currentVersionCode) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Install baru ATAU baru update -> tampilkan splash penuh
        setContentView(R.layout.activity_splash)
        prefs.edit().putInt("last_seen_version_code", currentVersionCode).apply()

        startNeonBackgroundAnimation(findViewById(R.id.splashRoot))
        setupCardClipping()
        setupSplashVideo()
        startFakeLoading()
    }

    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            -1
        }
    }

    private fun startNeonBackgroundAnimation(root: RelativeLayout) {
        val colorFrom = android.graphics.Color.parseColor("#FF2D55")
        val colorTo = android.graphics.Color.parseColor("#7A0C2E")

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        animator.duration = 1200
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val animatedColor = animation.animatedValue as Int
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    android.graphics.Color.parseColor("#0A0A12"),
                    animatedColor
                )
            )
            root.background = gradient
        }
        animator.start()
    }

    private fun setupCardClipping() {
        val card = findViewById<View>(R.id.splashCard)
        card.clipToOutline = true
        card.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 16f * resources.displayMetrics.density)
            }
        }
    }

    private fun setupSplashVideo() {
        val container = findViewById<FrameLayout>(R.id.videoContainerSplash)
        val videoView = findViewById<VideoView>(R.id.videoSplash)

        try {
            val uri = Uri.parse("android.resource://$packageName/${R.raw.splash_loop}")
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

    private fun startFakeLoading() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarSplash)
        val tvPercent = findViewById<TextView>(R.id.tvPercent)
        val tvStepStart = findViewById<TextView>(R.id.tvStepStart)
        val tvStepConfig = findViewById<TextView>(R.id.tvStepConfig)
        val tvStepTools = findViewById<TextView>(R.id.tvStepTools)
        val tvStepReady = findViewById<TextView>(R.id.tvStepReady)

        val activeColor = ContextCompat.getColor(this, R.color.neon_cyan)
        val dimColor = ContextCompat.getColor(this, android.R.color.darker_gray)

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return

                currentStep++
                progressBar.progress = currentStep
                tvPercent.text = "$currentStep / $totalSteps"

                when {
                    currentStep <= 3 -> tvStepStart.setTextColor(activeColor)
                    currentStep <= 6 -> {
                        tvStepStart.setTextColor(dimColor)
                        tvStepConfig.setTextColor(activeColor)
                    }
                    currentStep <= 9 -> {
                        tvStepConfig.setTextColor(dimColor)
                        tvStepTools.setTextColor(activeColor)
                    }
                    else -> {
                        tvStepTools.setTextColor(dimColor)
                        tvStepReady.setTextColor(activeColor)
                    }
                }

                if (currentStep < totalSteps) {
                    handler.postDelayed(this, stepIntervalMillis)
                } else {
                    handler.postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                            finish()
                        }
                    }, 500L)
                }
            }
        }
        handler.postDelayed(runnable, stepIntervalMillis)
    }
}