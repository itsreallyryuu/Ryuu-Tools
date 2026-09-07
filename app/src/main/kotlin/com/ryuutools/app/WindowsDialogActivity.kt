package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class WindowsDialogActivity : AppCompatActivity() {

    private var dialogType = "info"
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_windows_dialog)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etTitle = findViewById<EditText>(R.id.etDialogTitle)
        val etMessage = findViewById<EditText>(R.id.etDialogMessage)
        val ivPreview = findViewById<ImageView>(R.id.ivDialogPreview)

        fun update() {
            val bmp = generateDialogImage(etTitle.text.toString().ifBlank { "Windows" }, etMessage.text.toString().ifBlank { "Something went wrong." })
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        findViewById<Button>(R.id.btnTypeInfo).setOnClickListener { dialogType = "info"; update() }
        findViewById<Button>(R.id.btnTypeWarning).setOnClickListener { dialogType = "warning"; update() }
        findViewById<Button>(R.id.btnTypeError).setOnClickListener { dialogType = "error"; update() }

        etTitle.addTextChangedListener(simpleWatcher { update() })
        etMessage.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveDialog).setOnClickListener { ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "WindowsQuote") }
        findViewById<Button>(R.id.btnShareDialog).setOnClickListener { ImageSaveHelper.shareBitmap(this, currentBitmap) }
    }

    private fun generateDialogImage(title: String, message: String): Bitmap {
        val width = 900; val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#ECECEC"))

        val accentColor = when (dialogType) {
            "warning" -> Color.parseColor("#E8A400")
            "error" -> Color.parseColor("#D93025")
            else -> Color.parseColor("#0067C0")
        }
        val symbol = when (dialogType) { "warning" -> "!"; "error" -> "X"; else -> "i" }

        val titleBarPaint = Paint(Paint.ANTI_ALIAS_FLAG); titleBarPaint.color = accentColor
        canvas.drawRect(0f, 0f, width.toFloat(), 70f, titleBarPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = Color.WHITE; titlePaint.textSize = 32f; titlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(title, 24f, 46f, titlePaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG); iconPaint.color = accentColor
        canvas.drawCircle(110f, 220f, 55f, iconPaint)
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        symbolPaint.color = Color.WHITE; symbolPaint.textSize = 60f; symbolPaint.textAlign = Paint.Align.CENTER; symbolPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(symbol, 110f, 240f, symbolPaint)

        val msgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        msgPaint.color = Color.parseColor("#222222"); msgPaint.textSize = 30f
        val layout = StaticLayout.Builder.obtain(message, 0, message.length, msgPaint, width - 280)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(1.2f, 1.2f).build()
        canvas.save(); canvas.translate(200f, 150f); layout.draw(canvas); canvas.restore()

        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG); btnPaint.color = Color.parseColor("#DDDDDD")
        val btnRect = RectF(width - 180f, height - 90f, width - 40f, height - 40f)
        canvas.drawRoundRect(btnRect, 6f, 6f, btnPaint)
        val btnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        btnBorderPaint.color = Color.parseColor("#999999"); btnBorderPaint.style = Paint.Style.STROKE; btnBorderPaint.strokeWidth = 2f
        canvas.drawRoundRect(btnRect, 6f, 6f, btnBorderPaint)
        val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        btnTextPaint.color = Color.parseColor("#222222"); btnTextPaint.textSize = 28f; btnTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("OK", btnRect.centerX(), btnRect.centerY() + 10f, btnTextPaint)

        return bitmap
    }
}