package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
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

class IQCGeneratorActivity : BaseActivity() {

    private var bgColorIndex = 0
    private var currentBitmap: Bitmap? = null

    private val gradientPairs = listOf(
        "#0A0A12" to "#1A1A2E",
        "#1B2A4A" to "#0D1626",
        "#1B4A2A" to "#0D2614",
        "#4A1B1B" to "#260D0D",
        "#3A1B4A" to "#1D0D26"
    )

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

        val swatchIds = listOf(R.id.swatch1, R.id.swatch2, R.id.swatch3, R.id.swatch4, R.id.swatch5)
        swatchIds.forEachIndexed { index, id ->
            findViewById<View>(id).setOnClickListener { bgColorIndex = index; update() }
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

        val (startHex, endHex) = gradientPairs[bgColorIndex]
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor(startHex), Color.parseColor(endHex),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val quoteMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        quoteMarkPaint.color = Color.argb(40, 255, 255, 255)
        quoteMarkPaint.textSize = 220f
        quoteMarkPaint.typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)
        canvas.drawText("\u201C", 40f, 260f, quoteMarkPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = 42f
        textPaint.typeface = Typeface.create("serif", Typeface.ITALIC)

        val layout = StaticLayout.Builder.obtain(quote, 0, quote.length, textPaint, width - 140)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(1.15f, 1.15f)
            .build()

        canvas.save()
        canvas.translate(70f, (height - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()

        if (author.isNotBlank()) {
            val lineY = height - 130f
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            linePaint.color = Color.argb(150, 255, 255, 255)
            linePaint.strokeWidth = 2f
            canvas.drawLine(width - 220f, lineY, width - 60f, lineY, linePaint)

            val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            authorPaint.color = Color.parseColor("#DDDDDD")
            authorPaint.textSize = 26f
            authorPaint.letterSpacing = 0.05f
            authorPaint.textAlign = Paint.Align.RIGHT
            authorPaint.typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            canvas.drawText(author.uppercase(), width - 60f, lineY + 40f, authorPaint)
        }

        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        wmPaint.color = Color.argb(100, 255, 255, 255)
        wmPaint.textSize = 20f

        return ImageUtils.roundCorners(bitmap, 30f)
    }
}