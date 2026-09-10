package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class WindowsDialogActivity : BaseActivity() {

    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_windows_dialog)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etTitle = findViewById<EditText>(R.id.etDialogTitle)
        val etMessage = findViewById<EditText>(R.id.etDialogMessage)
        val ivPreview = findViewById<ImageView>(R.id.ivDialogPreview)

        fun update() {
            val bmp = generateDialogImage(etTitle.text.toString().ifBlank { "Windows Media Player" }, etMessage.text.toString().ifBlank { "lau sape mpruy" })
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        etTitle.addTextChangedListener(simpleWatcher { update() })
        etMessage.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveDialog).setOnClickListener { ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "WindowsQuote") }
        findViewById<Button>(R.id.btnShareDialog).setOnClickListener { ImageSaveHelper.shareBitmap(this, currentBitmap) }
    }

    private fun generateDialogImage(title: String, message: String): Bitmap {
        val width = 900; val height = 650
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Title bar
        val titleBar = Paint(Paint.ANTI_ALIAS_FLAG); titleBar.color = Color.parseColor("#3A6EA5")
        canvas.drawRect(0f, 0f, width.toFloat(), 55f, titleBar)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = Color.WHITE; titlePaint.textSize = 26f; titlePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(title, 20f, 36f, titlePaint)

        // window buttons
        val btnColors = listOf("#FDBE3B" to "_", "#3AB449" to "▢", "#E64C3C" to "X")
        for (i in btnColors.indices) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG); p.color = Color.parseColor(btnColors[i].first)
            val x = width - 130f + i * 42f
            canvas.drawRect(x, 15f, x + 30f, 40f, p)
        }

        // Menu bar
        val menuBg = Paint(Paint.ANTI_ALIAS_FLAG); menuBg.color = Color.parseColor("#ECECEC")
        canvas.drawRect(0f, 55f, width.toFloat(), 90f, menuBg)
        val menuText = Paint(Paint.ANTI_ALIAS_FLAG); menuText.color = Color.parseColor("#333333"); menuText.textSize = 22f
        canvas.drawText("File     View     Play     Tools     Help", 16f, 78f, menuText)

        // Body message
        val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        msgPaint.color = Color.parseColor("#111111"); msgPaint.textSize = 44f; msgPaint.typeface = Typeface.DEFAULT_BOLD
        val lines = message.split(" ")
        var y = 220f
        var line = ""
        for (word in lines) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (msgPaint.measureText(test) > width * 0.55f) {
                canvas.drawText(line, 60f, y, msgPaint); y += 55f; line = word
            } else line = test
        }
        canvas.drawText(line, 60f, y, msgPaint)

        // Stick figure (shrug pose)
        val stickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        stickPaint.color = Color.parseColor("#BBBBBB"); stickPaint.style = Paint.Style.STROKE; stickPaint.strokeWidth = 8f
        val cx = width - 180f; val cy = 260f
        canvas.drawCircle(cx, cy - 60f, 35f, stickPaint) // head
        canvas.drawLine(cx, cy - 25f, cx, cy + 60f, stickPaint) // body
        canvas.drawLine(cx, cy - 5f, cx - 50f, cy - 40f, stickPaint) // left arm up
        canvas.drawLine(cx, cy - 5f, cx + 50f, cy - 40f, stickPaint) // right arm up
        canvas.drawLine(cx, cy + 60f, cx - 30f, cy + 130f, stickPaint) // left leg
        canvas.drawLine(cx, cy + 60f, cx + 30f, cy + 130f, stickPaint) // right leg

        // Bottom control bar
        val controlBg = Paint(Paint.ANTI_ALIAS_FLAG); controlBg.color = Color.parseColor("#2C4A6E")
        canvas.drawRect(0f, height - 90f, width.toFloat(), height.toFloat(), controlBg)

        val playPaint = Paint(Paint.ANTI_ALIAS_FLAG); playPaint.color = Color.WHITE
        val path = android.graphics.Path()
        path.moveTo(40f, height - 65f); path.lineTo(40f, height - 25f); path.lineTo(70f, height - 45f); path.close()
        canvas.drawPath(path, playPaint)

        val seekPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        seekPaint.color = Color.parseColor("#5A7A9E"); seekPaint.strokeWidth = 6f
        canvas.drawLine(110f, height - 45f, width - 150f, height - 45f, seekPaint)
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG); markerPaint.color = Color.WHITE
        canvas.drawCircle(width * 0.4f, height - 45f, 10f, markerPaint)

        return bitmap
    }
}