package com.ryuutools.app

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CalculatorActivity : BaseActivity() {

    private var expression = ""
    private var useDegrees = true
    private var memoryValue = 0.0
    private val historyList = mutableListOf<HistoryEntry>()
    private lateinit var historyAdapter: CalculatorHistoryAdapter
    private lateinit var prefs: SharedPreferences

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var btnToggleDegRad: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        rvHistory = findViewById(R.id.rvHistory)
        btnToggleDegRad = findViewById(R.id.btnToggleDegRad)

        rvHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = CalculatorHistoryAdapter(historyList) { entry ->
            expression = entry.result
            rvHistory.visibility = View.GONE
            updateDisplay()
        }
        rvHistory.adapter = historyAdapter
        loadHistory()

        btnToggleDegRad.setOnClickListener {
            useDegrees = !useDegrees
            btnToggleDegRad.text = if (useDegrees) "DEG" else "RAD"
            updateDisplay()
        }

        findViewById<TextView>(R.id.btnToggleHistory).setOnClickListener {
            rvHistory.visibility = if (rvHistory.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        setupDigitButtons()
        setupOperatorButtons()
        setupFunctionButtons()
        setupMemoryButtons()
        setupControlButtons()

        updateDisplay()
    }

    private fun setupDigitButtons() {
        val digitIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9", R.id.btnDot to "."
        )
        digitIds.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener { expression += value; updateDisplay() }
        }
    }

    private fun setupOperatorButtons() {
        val opIds = mapOf(
            R.id.btnPlus to "+", R.id.btnMinus to "−", R.id.btnMultiply to "×",
            R.id.btnDivide to "÷", R.id.btnPercent to "%",
            R.id.btnParenOpen to "(", R.id.btnParenClose to ")"
        )
        opIds.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener { expression += value; updateDisplay() }
        }
    }

    private fun setupFunctionButtons() {
        findViewById<Button>(R.id.btnSin).setOnClickListener { expression += "sin("; updateDisplay() }
        findViewById<Button>(R.id.btnCos).setOnClickListener { expression += "cos("; updateDisplay() }
        findViewById<Button>(R.id.btnTan).setOnClickListener { expression += "tan("; updateDisplay() }
        findViewById<Button>(R.id.btnSqrt).setOnClickListener { expression += "sqrt("; updateDisplay() }
        findViewById<Button>(R.id.btnLog).setOnClickListener { expression += "log("; updateDisplay() }
        findViewById<Button>(R.id.btnLn).setOnClickListener { expression += "ln("; updateDisplay() }
        findViewById<Button>(R.id.btnFactorial).setOnClickListener { expression += "!"; updateDisplay() }
        findViewById<Button>(R.id.btnPow).setOnClickListener { expression += "^"; updateDisplay() }
        findViewById<Button>(R.id.btnPi).setOnClickListener { expression += "π"; updateDisplay() }
        findViewById<Button>(R.id.btnE).setOnClickListener { expression += "e"; updateDisplay() }
    }

    private fun setupMemoryButtons() {
        findViewById<Button>(R.id.btnMC).setOnClickListener { memoryValue = 0.0 }
        findViewById<Button>(R.id.btnMS).setOnClickListener { evaluateSafely(expression)?.let { memoryValue = it } }
        findViewById<Button>(R.id.btnMPlus).setOnClickListener { evaluateSafely(expression)?.let { memoryValue += it } }
        findViewById<Button>(R.id.btnMMinus).setOnClickListener { evaluateSafely(expression)?.let { memoryValue -= it } }
        findViewById<Button>(R.id.btnMR).setOnClickListener { expression += formatNumber(memoryValue); updateDisplay() }
    }

    private fun setupControlButtons() {
        findViewById<Button>(R.id.btnAC).setOnClickListener { expression = ""; updateDisplay() }
        findViewById<Button>(R.id.btnDel).setOnClickListener {
            if (expression.isNotEmpty()) { expression = expression.dropLast(1); updateDisplay() }
        }
        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            val result = evaluateSafely(expression)
            if (result != null) {
                val resultStr = formatNumber(result)
                addToHistory(expression, resultStr)
                expression = resultStr
                updateDisplay()
            }
        }
    }

    private fun updateDisplay() {
        tvExpression.text = expression
        val result = evaluateSafely(expression)
        tvResult.text = if (result != null) formatNumber(result) else if (expression.isEmpty()) "0" else "..."
    }

    private fun evaluateSafely(expr: String): Double? {
        if (expr.isBlank()) return null
        return try { ExpressionEvaluator(useDegrees).evaluate(expr) } catch (e: Exception) { null }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.8f".format(value).trimEnd('0').trimEnd('.')
        }
    }

    private fun addToHistory(expr: String, result: String) {
        historyList.add(0, HistoryEntry(expr, result))
        if (historyList.size > 50) historyList.removeAt(historyList.size - 1)
        historyAdapter.notifyDataSetChanged()
        saveHistory()
    }

    private fun saveHistory() {
        val serialized = historyList.joinToString("|||") { "${it.expression}:::${it.result}" }
        prefs.edit().putString("calc_history", serialized).apply()
    }

    private fun loadHistory() {
        val serialized = prefs.getString("calc_history", "") ?: ""
        if (serialized.isBlank()) return
        serialized.split("|||").forEach { entry ->
            val parts = entry.split(":::")
            if (parts.size == 2) historyList.add(HistoryEntry(parts[0], parts[1]))
        }
        historyAdapter.notifyDataSetChanged()
    }
}