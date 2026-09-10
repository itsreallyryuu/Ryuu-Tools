package com.ryuutools.app

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WhatsAppChatActivity : BaseActivity() {

    private var currentBitmap: Bitmap? = null
    private val operators = listOf("Telkomsel", "Indosat", "XL", "Axis", "Tri", "Smartfren", "by.U")
    private lateinit var prefs: SharedPreferences

    private lateinit var seekStatusH: SeekBar
    private lateinit var seekBubbleLeft: SeekBar
    private lateinit var seekBubbleRight: SeekBar
    private lateinit var seekBubbleTop: SeekBar
    private lateinit var seekBubbleBottom: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp_chat)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val spinnerOperator = findViewById<Spinner>(R.id.spinnerOperator)
        val adapter = ArrayAdapter(this, R.layout.spinner_item_dark, operators)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        spinnerOperator.adapter = adapter

        val etBattery = findViewById<EditText>(R.id.etBattery)
        val etTime = findViewById<EditText>(R.id.etChatTime)
        val etMessage = findViewById<EditText>(R.id.etChatMessage)
        val ivPreview = findViewById<ImageView>(R.id.ivChatPreview)

        seekStatusH = findViewById(R.id.seekStatusH)
        seekBubbleLeft = findViewById(R.id.seekBubbleLeft)
        seekBubbleRight = findViewById(R.id.seekBubbleRight)
        seekBubbleTop = findViewById(R.id.seekBubbleTop)
        seekBubbleBottom = findViewById(R.id.seekBubbleBottom)

        val tvStatusHLabel = findViewById<TextView>(R.id.tvStatusHLabel)
        val tvBubbleLeftLabel = findViewById<TextView>(R.id.tvBubbleLeftLabel)
        val tvBubbleRightLabel = findViewById<TextView>(R.id.tvBubbleRightLabel)
        val tvBubbleTopLabel = findViewById<TextView>(R.id.tvBubbleTopLabel)
        val tvBubbleBottomLabel = findViewById<TextView>(R.id.tvBubbleBottomLabel)

        // ===== NILAI DEFAULT SUDAH DIKUNCI SESUAI HASIL KALIBRASI KAMU =====
        seekStatusH.progress = prefs.getInt("wa_statush_pct", 4)
        seekBubbleLeft.progress = prefs.getInt("wa_bubble_left_pct", 3)
        seekBubbleRight.progress = prefs.getInt("wa_bubble_right_pct", 70)
        seekBubbleTop.progress = prefs.getInt("wa_bubble_top_pct", 46)
        seekBubbleBottom.progress = prefs.getInt("wa_bubble_bottom_pct", 54)

        fun updateLabels() {
            tvStatusHLabel.text = "Tinggi Status Bar: ${seekStatusH.progress}%"
            tvBubbleLeftLabel.text = "Bubble Kiri: ${seekBubbleLeft.progress}%"
            tvBubbleRightLabel.text = "Bubble Kanan: ${seekBubbleRight.progress}%"
            tvBubbleTopLabel.text = "Bubble Atas: ${seekBubbleTop.progress}%"
            tvBubbleBottomLabel.text = "Bubble Bawah: ${seekBubbleBottom.progress}%"
        }
        updateLabels()

        fun update() {
            val bmp = generateChatImage(
                operators[spinnerOperator.selectedItemPosition],
                etBattery.text.toString().ifBlank { "71" },
                etTime.text.toString().ifBlank { "13.54" },
                etMessage.text.toString().ifBlank { "hallo bro" }
            )
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLabels()
                prefs.edit()
                    .putInt("wa_statush_pct", seekStatusH.progress)
                    .putInt("wa_bubble_left_pct", seekBubbleLeft.progress)
                    .putInt("wa_bubble_right_pct", seekBubbleRight.progress)
                    .putInt("wa_bubble_top_pct", seekBubbleTop.progress)
                    .putInt("wa_bubble_bottom_pct", seekBubbleBottom.progress)
                    .apply()
                update()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        seekStatusH.setOnSeekBarChangeListener(seekListener)
        seekBubbleLeft.setOnSeekBarChangeListener(seekListener)
        seekBubbleRight.setOnSeekBarChangeListener(seekListener)
        seekBubbleTop.setOnSeekBarChangeListener(seekListener)
        seekBubbleBottom.setOnSeekBarChangeListener(seekListener)

        spinnerOperator.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = update()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        etBattery.addTextChangedListener(simpleWatcher { update() })
        etTime.addTextChangedListener(simpleWatcher { update() })
        etMessage.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveChat).setOnClickListener {
            ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "IQC")
        }
    }

    private fun generateChatImage(operator: String, battery: String, time: String, message: String): Bitmap {
        val rawBg = BitmapFactory.decodeResource(resources, R.drawable.wa_chat_bg)

        // ===== FIX UTAMA: standarisasi lebar ke 800px, biar font & elemen selalu proporsional
        // walau resolusi asli foto kamu beda-beda =====
        val width = 800
        val height = (width * (rawBg.height.toFloat() / rawBg.width.toFloat())).toInt()
        val scaledBg = Bitmap.createScaledBitmap(rawBg, width, height, true)
        val bitmap = scaledBg.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        val statusBarHeight = height * (seekStatusH.progress / 100f)
        val statusBg = Paint(Paint.ANTI_ALIAS_FLAG); statusBg.color = Color.BLACK
        canvas.drawRect(0f, 0f, width.toFloat(), statusBarHeight, statusBg)

        val statusText = Paint(Paint.ANTI_ALIAS_FLAG)
        statusText.color = Color.WHITE
        statusText.textSize = statusBarHeight * 0.5f
        canvas.drawText(operator, width * 0.03f, statusBarHeight * 0.68f, statusText)
        statusText.textAlign = Paint.Align.CENTER
        canvas.drawText(time, width * 0.5f, statusBarHeight * 0.68f, statusText)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG); barPaint.color = Color.WHITE
        val barBaseX = width * 0.78f
        val barBaseY = statusBarHeight * 0.75f
        for (i in 0..3) {
            val barHeight = statusBarHeight * (0.25f + i * 0.12f)
            canvas.drawRect(barBaseX + i * (width * 0.018f), barBaseY - barHeight, barBaseX + i * (width * 0.018f) + width * 0.012f, barBaseY, barPaint)
        }

        val battOutline = Paint(Paint.ANTI_ALIAS_FLAG)
        battOutline.color = Color.WHITE; battOutline.style = Paint.Style.STROKE; battOutline.strokeWidth = 4f
        val battRect = RectF(width * 0.90f, statusBarHeight * 0.30f, width * 0.965f, statusBarHeight * 0.70f)
        canvas.drawRoundRect(battRect, 4f, 4f, battOutline)
        canvas.drawRect(battRect.right, statusBarHeight * 0.42f, battRect.right + width * 0.008f, statusBarHeight * 0.58f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

        val battLevel = (battery.toIntOrNull() ?: 71).coerceIn(0, 100)
        val battFill = Paint(Paint.ANTI_ALIAS_FLAG)
        battFill.color = if (battLevel <= 20) Color.parseColor("#FF3B30") else Color.parseColor("#4CD964")
        val fillWidth = (battRect.width() - 6f) * (battLevel / 100f)
        canvas.drawRect(battRect.left + 3f, battRect.top + 3f, battRect.left + 3f + fillWidth, battRect.bottom - 3f, battFill)

        val bubbleLeft = width * (seekBubbleLeft.progress / 100f)
        val bubbleRight = width * (seekBubbleRight.progress / 100f)
        val bubbleTop = height * (seekBubbleTop.progress / 100f)
        val bubbleBottom = height * (seekBubbleBottom.progress / 100f)
        val bubbleRect = RectF(bubbleLeft, bubbleTop, bubbleRight, maxOf(bubbleBottom, bubbleTop + 60f))

        val msgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        msgPaint.color = Color.WHITE
        var textSize = 34f
        val innerWidth = (bubbleRect.width() - 50f).toInt().coerceAtLeast(50)
        val innerHeight = (bubbleRect.height() - 50f).coerceAtLeast(30f)
        lateinit var layout: StaticLayout
        do {
            msgPaint.textSize = textSize
            layout = StaticLayout.Builder.obtain(message.uppercase(), 0, message.length, msgPaint, innerWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.15f, 1.15f)
                .build()
            textSize -= 1f
        } while ((layout.width > innerWidth || layout.height > innerHeight) && textSize > 14f)

        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG); bubblePaint.color = Color.parseColor("#202C33")
        canvas.drawRoundRect(bubbleRect, 24f, 24f, bubblePaint)

        canvas.save()
        canvas.translate(bubbleRect.left + 25f, bubbleRect.top + 25f)
        layout.draw(canvas)
        canvas.restore()

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        timePaint.color = Color.parseColor("#AAAAAA")
        timePaint.textSize = 20f
        timePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(time, bubbleRect.right - 20f, bubbleRect.bottom - 15f, timePaint)

        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        wmPaint.color = Color.argb(160, 255, 255, 255)
        wmPaint.textSize = 22f

        return ImageUtils.roundCorners(bitmap, width * 0.05f)
    }
}