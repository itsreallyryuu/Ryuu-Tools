package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CertificateGeneratorActivity : AppCompatActivity() {

    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate_generator)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etCertName)
        val etReason = findViewById<EditText>(R.id.etCertReason)
        val ivPreview = findViewById<ImageView>(R.id.ivCertPreview)

        fun update() {
            val bmp = generateCertificate(etName.text.toString().ifBlank { "Nama Kamu" }, etReason.text.toString().ifBlank { "Juara Rebahan Sedunia" })
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        etName.addTextChangedListener(simpleWatcher { update() })
        etReason.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveCert).setOnClickListener { ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "Sertifikat") }
        findViewById<Button>(R.id.btnShareCert).setOnClickListener { ImageSaveHelper.shareBitmap(this, currentBitmap) }
    }

    private fun generateCertificate(name: String, reason: String): Bitmap {
        val width = 1000; val height = 700
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#FFF8E7"))

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = Color.parseColor("#B8860B"); borderPaint.style = Paint.Style.STROKE; borderPaint.strokeWidth = 10f
        canvas.drawRect(30f, 30f, width - 30f, height - 30f, borderPaint)
        borderPaint.strokeWidth = 3f
        canvas.drawRect(50f, 50f, width - 50f, height - 50f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        headerPaint.color = Color.parseColor("#B8860B"); headerPaint.textSize = 60f; headerPaint.textAlign = Paint.Align.CENTER
        headerPaint.typeface = Typeface.create("serif", Typeface.BOLD)
        canvas.drawText("SERTIFIKAT", width / 2f, 150f, headerPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        subPaint.color = Color.parseColor("#666666"); subPaint.textSize = 26f; subPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Dengan bangga diberikan kepada", width / 2f, 260f, subPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        namePaint.color = Color.parseColor("#222222"); namePaint.textSize = 50f; namePaint.textAlign = Paint.Align.CENTER
        namePaint.typeface = Typeface.create("cursive", Typeface.BOLD)
        canvas.drawText(name, width / 2f, 340f, namePaint)

        val reasonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        reasonPaint.color = Color.parseColor("#444444"); reasonPaint.textSize = 30f; reasonPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("atas prestasinya sebagai", width / 2f, 410f, reasonPaint)

        val reasonBigPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        reasonBigPaint.color = Color.parseColor("#B8860B"); reasonBigPaint.textSize = 38f; reasonBigPaint.textAlign = Paint.Align.CENTER
        reasonBigPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(reason, width / 2f, 470f, reasonBigPaint)

        val signPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        signPaint.color = Color.parseColor("#888888"); signPaint.textSize = 22f; signPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("— Ryuu Tools —", width / 2f, height - 80f, signPaint)

        return bitmap
    }
}