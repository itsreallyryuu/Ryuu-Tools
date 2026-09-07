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

class ProfileCardActivity : AppCompatActivity() {

    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_card)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etProfileName)
        val etHandle = findViewById<EditText>(R.id.etProfileHandle)
        val etBio = findViewById<EditText>(R.id.etProfileBio)
        val ivPreview = findViewById<ImageView>(R.id.ivProfilePreview)

        fun update() {
            val bmp = generateProfileCard(
                etName.text.toString().ifBlank { "Nama Fiktif" },
                etHandle.text.toString().ifBlank { "@username" },
                etBio.text.toString().ifBlank { "Bio contoh untuk desain/hiburan" }
            )
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        etName.addTextChangedListener(simpleWatcher { update() })
        etHandle.addTextChangedListener(simpleWatcher { update() })
        etBio.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "ProfileCard") }
        findViewById<Button>(R.id.btnShareProfile).setOnClickListener { ImageSaveHelper.shareBitmap(this, currentBitmap) }
    }

    private fun generateProfileCard(name: String, handle: String, bio: String): Bitmap {
        val width = 900; val height = 700
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#1A1A28"))

        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG); avatarPaint.color = Color.parseColor("#FF2D55")
        canvas.drawCircle(120f, 120f, 70f, avatarPaint)
        val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        initialPaint.color = Color.WHITE; initialPaint.textSize = 60f; initialPaint.textAlign = Paint.Align.CENTER; initialPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(name.take(1).uppercase(), 120f, 140f, initialPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        namePaint.color = Color.WHITE; namePaint.textSize = 40f; namePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(name, 220f, 105f, namePaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        handlePaint.color = Color.parseColor("#AAAAAA"); handlePaint.textSize = 28f
        canvas.drawText(handle, 220f, 145f, handlePaint)

        val bioPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bioPaint.color = Color.parseColor("#DDDDDD"); bioPaint.textSize = 26f
        canvas.drawText(bio.take(50), 60f, 240f, bioPaint)

        val statLabels = listOf("Posts", "Followers", "Following")
        val statValues = listOf("12", "345", "210")
        val statPaint = Paint(Paint.ANTI_ALIAS_FLAG); statPaint.textAlign = Paint.Align.CENTER
        for (i in statLabels.indices) {
            val x = 180f + i * 280f
            statPaint.color = Color.WHITE; statPaint.textSize = 34f; statPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(statValues[i], x, 320f, statPaint)
            statPaint.color = Color.parseColor("#999999"); statPaint.textSize = 22f; statPaint.typeface = Typeface.DEFAULT
            canvas.drawText(statLabels[i], x, 350f, statPaint)
        }

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        watermarkPaint.color = Color.argb(70, 255, 255, 255); watermarkPaint.textSize = 90f
        watermarkPaint.typeface = Typeface.DEFAULT_BOLD; watermarkPaint.textAlign = Paint.Align.CENTER
        canvas.save()
        canvas.rotate(-25f, width / 2f, height / 2f)
        canvas.restore()

        return bitmap
    }
}