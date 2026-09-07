package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class IQCGeneratorActivity : AppCompatActivity() {

    private var bgColor = Color.parseColor("#0A0A12")
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iqc_generator)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etQuote = findViewById<EditText>(R.id.etQuote)
        val etAuthor = findViewById<EditText>(R.id.etAuthor)
        val ivPreview = findViewById<ImageView>(R.id.ivQuotePreview)

        fun update() {
            val bmp = generateQuoteImage(etQuote.text.toString().ifBlank { "Your quote here" }, etAuthor.text.toString())
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        val colors = listOf("#0A0A12", "#1B2A4A", "#1B4A2A", "#4A1B1B", "#3A1B4A")
        val swatchIds = listOf(R.id.swatch1, R.id.swatch2, R.id.swatch3, R.id.swatch4, R.id.swatch5)
        swatchIds.forEachIndexed { index, id ->
            findViewById<View>(id).setOnClickListener { bgColor = Color.parseColor(colors[index]); update() }
        }

        etQuote.addTextChangedListener(simpleWatcher { update() })
        etAuthor.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveQuote).setOnClickListener { ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "Quote") }
        findViewById<Button>(R.id.btnShareQuote).setOnClickListener { ImageSaveHelper.shareBitmap(this, currentBitmap) }
    }

    private fun generateQuoteImage(quote: String, author: String): Bitmap {
        val width = 800; val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = 42f
        textPaint.typeface = Typeface.create("serif", Typeface.ITALIC)

        val layout = StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, width - 120)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(1.1f, 1.1f)
            .build()

        canvas.save()
        canvas.translate(60f, (height - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()

        if (author.isNotBlank()) {
            val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            authorPaint.color = Color.parseColor("#CCCCCC")
            authorPaint.textSize = 28f
            authorPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("— $author", width - 60f, height - 80f, authorPaint)
        }
        return bitmap
    }
}