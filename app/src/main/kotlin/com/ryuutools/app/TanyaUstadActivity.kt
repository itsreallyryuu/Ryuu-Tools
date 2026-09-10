package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class TanyaUstadActivity : BaseActivity() {

    // ===== POSISI DIKUNCI PERMANEN, TIDAK ADA UI KALIBRASI LAGI =====
    private val boxTopPct = 19
    private val boxBottomPct = 48
    private val boxLeftPct = 9
    private val boxRightPct = 91

    private var currentBitmap: Bitmap? = null
    private lateinit var etQuestion: EditText
    private lateinit var ivPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tanya_ustad)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        etQuestion = findViewById(R.id.etQuestion)
        ivPreview = findViewById(R.id.ivUstadPreview)

        etQuestion.addTextChangedListener(simpleWatcher { refreshPreview() })
        refreshPreview()

        findViewById<Button>(R.id.btnSaveUstad).setOnClickListener {
            ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "TanyaUstad")
        }
    }

    private fun refreshPreview() {
        val bmp = generateImage(etQuestion.text.toString().ifBlank { "Pertanyaanmu di sini" })
        currentBitmap = bmp
        ivPreview.setImageBitmap(bmp)
    }

    private fun generateImage(question: String): Bitmap {
        val width = 800
        val photoSource = BitmapFactory.decodeResource(resources, R.drawable.ustad_default)
        val height = (width * (photoSource.height.toFloat() / photoSource.width.toFloat())).toInt()
        val baseBitmap = Bitmap.createScaledBitmap(photoSource, width, height, true)
        val bitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        val boxTop = height * (boxTopPct / 100f)
        val boxBottom = height * (boxBottomPct / 100f)
        val boxLeft = width * (boxLeftPct / 100f)
        val boxRight = width * (boxRightPct / 100f)
        val headerHeight = 60f
        val boxRect = RectF(boxLeft, boxTop, boxRight, maxOf(boxBottom, boxTop + headerHeight + 40f))

        val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG); coverPaint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(boxRect, coverPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG); headerPaint.color = Color.parseColor("#222222")
        canvas.drawRect(boxRect.left, boxRect.top, boxRect.right, boxRect.top + headerHeight, headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        headerTextPaint.color = Color.WHITE; headerTextPaint.textSize = 28f; headerTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Soalan", boxRect.centerX(), boxRect.top + 40f, headerTextPaint)

        val textAreaHeight = boxRect.height() - headerHeight
        val innerWidth = (boxRect.width() - 80f).toInt().coerceAtLeast(50)
        val questionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        questionPaint.color = Color.parseColor("#111111")
        questionPaint.typeface = Typeface.DEFAULT_BOLD
        var textSize = 40f
        lateinit var layout: StaticLayout
        do {
            questionPaint.textSize = textSize
            layout = StaticLayout.Builder.obtain(question, 0, question.length, questionPaint, innerWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(1.15f, 1.15f)
                .build()
            textSize -= 2f
        } while ((layout.height > textAreaHeight - 30f || layout.width > innerWidth) && textSize > 12f)

        canvas.save()
        canvas.translate(boxRect.left + 40f, boxRect.top + headerHeight + (textAreaHeight - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()

        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        wmPaint.color = Color.argb(160, 255, 255, 255); wmPaint.textSize = 22f

        return ImageUtils.roundCorners(bitmap, width * 0.05f)
    }
}