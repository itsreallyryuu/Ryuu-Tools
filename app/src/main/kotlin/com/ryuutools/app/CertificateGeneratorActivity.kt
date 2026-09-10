package com.ryuutools.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class CertificateGeneratorActivity : BaseActivity() {

    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate_generator)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etCertName)
        val etReason = findViewById<EditText>(R.id.etCertReason)
        val ivPreview = findViewById<ImageView>(R.id.ivCertPreview)

        fun update() {
            val bmp = generateCertificate(
                etName.text.toString().ifBlank { "Your Name" },
                etReason.text.toString().ifBlank { "Outstanding Achievement" }
            )
            currentBitmap = bmp
            ivPreview.setImageBitmap(bmp)
        }

        etName.addTextChangedListener(simpleWatcher { update() })
        etReason.addTextChangedListener(simpleWatcher { update() })
        update()

        findViewById<Button>(R.id.btnSaveCert).setOnClickListener {
            ImageSaveHelper.saveBitmapToGallery(this, currentBitmap, "FakeCertificate")
        }
        findViewById<Button>(R.id.btnShareCert).setOnClickListener {
            ImageSaveHelper.shareBitmap(this, currentBitmap)
        }
    }

    private fun generateCertificate(name: String, reason: String): Bitmap {
        val width = 1200
        val height = 850
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cream = Color.parseColor("#FBF6E9")
        val gold = Color.parseColor("#B8860B")
        val darkGold = Color.parseColor("#8A6300")
        val navy = Color.parseColor("#1C2B4A")
        val gray = Color.parseColor("#555555")

        // Background
        canvas.drawColor(cream)

        // Subtle inner background tint panel
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        panelPaint.color = Color.parseColor("#F5EFDD")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), panelPaint)

        // Outer thick border
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        outerBorder.color = navy
        outerBorder.style = Paint.Style.STROKE
        outerBorder.strokeWidth = 14f
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, outerBorder)

        // Gold inner border
        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        goldBorder.color = gold
        goldBorder.style = Paint.Style.STROKE
        goldBorder.strokeWidth = 4f
        canvas.drawRect(44f, 44f, width - 44f, height - 44f, goldBorder)

        // Thin double line
        canvas.drawRect(52f, 52f, width - 52f, height - 52f, goldBorder.apply { strokeWidth = 1.5f })

        // Corner ornaments (all four corners)
        drawCornerOrnament(canvas, 44f, 44f, 1f, 1f, gold)
        drawCornerOrnament(canvas, width - 44f, 44f, -1f, 1f, gold)
        drawCornerOrnament(canvas, 44f, height - 44f, 1f, -1f, gold)
        drawCornerOrnament(canvas, width - 44f, height - 44f, -1f, -1f, gold)

        // Header: small label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = darkGold
        labelPaint.textSize = 24f
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.typeface = Typeface.create("serif", Typeface.NORMAL)
        labelPaint.letterSpacing = 0.3f
        canvas.drawText("R Y U U   T O O L S", width / 2f, 120f, labelPaint)

        // Main title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = navy
        titlePaint.textSize = 66f
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create("serif", Typeface.BOLD)
        canvas.drawText("CERTIFICATE", width / 2f, 205f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        subtitlePaint.color = gold
        subtitlePaint.textSize = 30f
        subtitlePaint.textAlign = Paint.Align.CENTER
        subtitlePaint.typeface = Typeface.create("serif", Typeface.ITALIC)
        subtitlePaint.letterSpacing = 0.15f
        canvas.drawText("O F   A C H I E V E M E N T", width / 2f, 245f, subtitlePaint)

        // Decorative divider under title
        drawFlourishDivider(canvas, width / 2f, 280f, gold)

        // "This is to certify that"
        val presentedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        presentedPaint.color = gray
        presentedPaint.textSize = 26f
        presentedPaint.textAlign = Paint.Align.CENTER
        presentedPaint.typeface = Typeface.create("serif", Typeface.ITALIC)
        canvas.drawText("This certificate is proudly presented to", width / 2f, 350f, presentedPaint)

        // Name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        namePaint.color = navy
        namePaint.textSize = 56f
        namePaint.textAlign = Paint.Align.CENTER
        namePaint.typeface = Typeface.create("serif", Typeface.BOLD)
        canvas.drawText(name, width / 2f, 425f, namePaint)

        // Underline beneath name
        val nameWidth = namePaint.measureText(name).coerceAtMost(700f)
        val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        underlinePaint.color = gold
        underlinePaint.strokeWidth = 2f
        canvas.drawLine(width / 2f - nameWidth / 2f - 30f, 445f, width / 2f + nameWidth / 2f + 30f, 445f, underlinePaint)

        // "for" line
        val forPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        forPaint.color = gray
        forPaint.textSize = 24f
        forPaint.textAlign = Paint.Align.CENTER
        forPaint.typeface = Typeface.create("serif", Typeface.ITALIC)
        canvas.drawText("in recognition of outstanding accomplishment as", width / 2f, 500f, forPaint)

        // Reason (big, gold)
        val reasonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        reasonPaint.color = darkGold
        reasonPaint.textSize = 40f
        reasonPaint.textAlign = Paint.Align.CENTER
        reasonPaint.typeface = Typeface.create("serif", Typeface.BOLD)
        canvas.drawText(reason, width / 2f, 560f, reasonPaint)

        // Bottom section: signature line (left) + seal (right) + date (center-left)
        val bottomY = height - 140f

        // Signature line
        val sigLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        sigLinePaint.color = navy
        sigLinePaint.strokeWidth = 2f
        canvas.drawLine(140f, bottomY, 460f, bottomY, sigLinePaint)

        val sigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        sigLabelPaint.color = gray
        sigLabelPaint.textSize = 20f
        sigLabelPaint.textAlign = Paint.Align.CENTER
        sigLabelPaint.typeface = Typeface.create("serif", Typeface.NORMAL)
        canvas.drawText("Authorized Signature", 300f, bottomY + 32f, sigLabelPaint)

        // Date line
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
        val dateValuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dateValuePaint.color = navy
        dateValuePaint.textSize = 22f
        dateValuePaint.textAlign = Paint.Align.CENTER
        dateValuePaint.typeface = Typeface.create("serif", Typeface.ITALIC)
        canvas.drawText(dateStr, 300f, bottomY - 14f, dateValuePaint)

        // Certificate ID (small, bottom center)
        val certId = "No. RT-${Random.nextInt(100000, 999999)}"
        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        idPaint.color = Color.parseColor("#999999")
        idPaint.textSize = 18f
        idPaint.textAlign = Paint.Align.CENTER
        idPaint.typeface = Typeface.create("monospace", Typeface.NORMAL)
        canvas.drawText(certId, width / 2f, height - 60f, idPaint)

        // Seal (right side, medallion + ribbon)
        drawSeal(canvas, width - 300f, bottomY - 30f, gold, darkGold, navy)

        return bitmap
    }

    /** Draws a small corner flourish. sx/sy = +1/-1 flip direction from the corner point. */
    private fun drawCornerOrnament(canvas: Canvas, x: Float, y: Float, sx: Float, sy: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        val path = Path()
        path.moveTo(x, y + sy * 60f)
        path.quadTo(x + sx * 10f, y + sy * 10f, x + sx * 60f, y)
        canvas.drawPath(path, paint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = color
        dotPaint.style = Paint.Style.FILL
        canvas.drawCircle(x + sx * 60f, y, 5f, dotPaint)
        canvas.drawCircle(x, y + sy * 60f, 5f, dotPaint)
        canvas.drawCircle(x + sx * 22f, y + sy * 22f, 4f, dotPaint)
    }

    /** Small decorative line with a diamond in the middle, used under the title. */
    private fun drawFlourishDivider(canvas: Canvas, centerX: Float, y: Float, color: Int) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.color = color
        linePaint.strokeWidth = 2f
        canvas.drawLine(centerX - 180f, y, centerX - 20f, y, linePaint)
        canvas.drawLine(centerX + 20f, y, centerX + 180f, y, linePaint)

        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        diamondPaint.color = color
        diamondPaint.style = Paint.Style.FILL
        val path = Path()
        path.moveTo(centerX, y - 10f)
        path.lineTo(centerX + 10f, y)
        path.lineTo(centerX, y + 10f)
        path.lineTo(centerX - 10f, y)
        path.close()
        canvas.drawPath(path, diamondPaint)
    }

    /** Draws a gold medallion seal with a ribbon tail and a star in the center. */
    private fun drawSeal(canvas: Canvas, cx: Float, cy: Float, gold: Int, darkGold: Int, navy: Int) {
        // Ribbon tails
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        ribbonPaint.color = navy
        val leftRibbon = Path()
        leftRibbon.moveTo(cx - 25f, cy + 40f)
        leftRibbon.lineTo(cx - 55f, cy + 110f)
        leftRibbon.lineTo(cx - 15f, cy + 90f)
        leftRibbon.close()
        canvas.drawPath(leftRibbon, ribbonPaint)

        val rightRibbon = Path()
        rightRibbon.moveTo(cx + 25f, cy + 40f)
        rightRibbon.lineTo(cx + 55f, cy + 110f)
        rightRibbon.lineTo(cx + 15f, cy + 90f)
        rightRibbon.close()
        canvas.drawPath(rightRibbon, ribbonPaint)

        // Outer medallion ring
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        outerPaint.color = darkGold
        canvas.drawCircle(cx, cy, 58f, outerPaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        innerPaint.color = gold
        canvas.drawCircle(cx, cy, 48f, innerPaint)

        // Scalloped edge dots
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = darkGold
        for (i in 0 until 16) {
            val angle = Math.toRadians((i * 22.5).toDouble())
            val dx = cx + (58f * Math.cos(angle)).toFloat()
            val dy = cy + (58f * Math.sin(angle)).toFloat()
            canvas.drawCircle(dx, dy, 4f, dotPaint)
        }

        // Center star (simple 5-point star)
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        starPaint.color = Color.parseColor("#FFF8E1")
        canvas.drawPath(buildStarPath(cx, cy, 26f, 11f), starPaint)
    }

    private fun buildStarPath(cx: Float, cy: Float, outerR: Float, innerR: Float): Path {
        val path = Path()
        val points = 5
        val angleStep = Math.PI / points
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = i * angleStep - Math.PI / 2
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }
}