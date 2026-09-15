package com.ryuutools.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixRainView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val fontSize = 32f
    private var columns = 0
    private lateinit var drops: IntArray

    private val paint = Paint().apply {
        color = Color.parseColor("#FF2D55")
        textSize = fontSize
        isAntiAlias = true
    }
    private val fadePaint = Paint().apply {
        color = Color.argb(40, 10, 10, 18)
    }
    private val chars = "01アイウエオカキクケコサシスセソ\$#%&*+-<>/\\".toCharArray()

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            invalidate()
            if (running) handler.postDelayed(this, 60)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        columns = (w / fontSize).toInt().coerceAtLeast(1)
        val rows = (h / fontSize).toInt().coerceAtLeast(1)
        drops = IntArray(columns) { Random.nextInt(0, rows) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::drops.isInitialized) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)

        for (i in 0 until columns) {
            val text = chars[Random.nextInt(chars.size)].toString()
            val x = i * fontSize
            val y = drops[i] * fontSize
            canvas.drawText(text, x, y, paint)

            drops[i] = if (y > height && Random.nextFloat() > 0.975f) 0 else drops[i] + 1
        }
    }

    fun startAnimating() {
        if (running) return
        running = true
        handler.post(updateRunnable)
    }

    fun stopAnimating() {
        running = false
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimating()
    }
}