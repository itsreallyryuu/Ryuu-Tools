package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch

class BratGeneratorActivity : BaseActivity() {

    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brat_generator)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etText = findViewById<EditText>(R.id.etBratText)
        val ivPreview = findViewById<ImageView>(R.id.ivBratPreview)
        val switchBlur = findViewById<Switch>(R.id.switchBlur)

        fun update() {
            val text = etText.text.toString().ifBlank { "brat" }
            val bitmap = generateBratImage(text, switchBlur.isChecked)
            currentBitmap = bitmap
            ivPreview.setImageBitmap(bitmap)
        }

        etText.addTextChangedListener(simpleWatcher { update() })
        switchBlur.setOnCheckedChangeListener { _, _ -> update() }
        update()

        findViewById<Button>(R.id.btnSaveBrat).setOnClickListener {
            ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "Brat")
        }
    }

    private fun generateBratImage(text: String, blurEnabled: Boolean): Bitmap {
        val size = 800
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background hijau lime khas cover album "Brat"
        canvas.drawColor(Color.parseColor("#ffffff"))

        val displayText = text.lowercase()
        val innerWidth = (size * 0.85f).toInt()

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

        if (blurEnabled) {
            paint.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        }

        // Auto-shrink: mulai dari font besar, kecilin sampai muat lebar & tinggi kanvas
        var textSize = 130f
        lateinit var layout: StaticLayout
        do {
            paint.textSize = textSize
            layout = StaticLayout.Builder.obtain(displayText, 0, displayText.length, paint, innerWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0.9f, 0.9f)
                .build()
            textSize -= 3f
        } while (layout.height > size * 0.7f && textSize > 20f)

        canvas.save()
        canvas.translate((size - innerWidth) / 2f, (size - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()

        return bitmap
    }
}