package com.ryuutools.app

import kotlin.math.*

class ExpressionEvaluator(private val useDegrees: Boolean) {

    private lateinit var tokens: List<String>
    private var pos = 0

    fun evaluate(expression: String): Double {
        val cleaned = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
        tokens = tokenize(cleaned)
        pos = 0
        val result = parseExpression()
        if (pos != tokens.size) throw IllegalArgumentException("Unexpected token: ${tokens.getOrNull(pos)}")
        return result
    }

    private fun tokenize(expr: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    list.add(expr.substring(start, i))
                }
                c.isLetter() -> {
                    val start = i
                    while (i < expr.length && expr[i].isLetter()) i++
                    list.add(expr.substring(start, i))
                }
                else -> { list.add(c.toString()); i++ }
            }
        }
        return list
    }

    private fun peek(): String? = tokens.getOrNull(pos)

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (peek() == "+" || peek() == "-") {
            val op = tokens[pos]; pos++
            val rhs = parseTerm()
            value = if (op == "+") value + rhs else value - rhs
        }
        return value
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (peek() == "*" || peek() == "/" || peek() == "%") {
            val op = tokens[pos]; pos++
            val rhs = parseFactor()
            value = when (op) { "*" -> value * rhs; "/" -> value / rhs; else -> value % rhs }
        }
        return value
    }

    private fun parseFactor(): Double {
        var value = parseUnary()
        while (peek() == "^") {
            pos++
            val rhs = parseUnary()
            value = value.pow(rhs)
        }
        return value
    }

    private fun parseUnary(): Double {
        if (peek() == "-") { pos++; return -parseUnary() }
        if (peek() == "+") { pos++; return parseUnary() }
        return parsePostfix()
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (peek() == "!") { pos++; value = factorial(value) }
        return value
    }

    private fun parsePrimary(): Double {
        val tok = peek() ?: throw IllegalArgumentException("Unexpected end of expression")

        if (tok == "(") {
            pos++
            val value = parseExpression()
            if (peek() != ")") throw IllegalArgumentException("Expected ')'")
            pos++
            return value
        }

        if (tok.toDoubleOrNull() != null) { pos++; return tok.toDouble() }

        if (tok[0].isLetter()) {
            pos++
            if (peek() == "(") {
                pos++
                val arg = parseExpression()
                if (peek() != ")") throw IllegalArgumentException("Expected ')'")
                pos++
                return applyFunction(tok, arg)
            } else {
                throw IllegalArgumentException("Unknown identifier: $tok")
            }
        }

        throw IllegalArgumentException("Unexpected token: $tok")
    }

    private fun applyFunction(name: String, arg: Double): Double {
        val angle = if (useDegrees) Math.toRadians(arg) else arg
        return when (name) {
            "sin" -> sin(angle)
            "cos" -> cos(angle)
            "tan" -> tan(angle)
            "log" -> log10(arg)
            "ln" -> ln(arg)
            "sqrt" -> sqrt(arg)
            "abs" -> abs(arg)
            else -> throw IllegalArgumentException("Unknown function: $name")
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n)) throw IllegalArgumentException("Factorial requires a non-negative integer")
        var result = 1.0
        var i = 2
        while (i <= n.toInt()) { result *= i; i++ }
        return result
    }
}