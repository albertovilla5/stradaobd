package com.torqueobd2.dash.data.parser

import kotlin.math.pow

/**
 * Evaluador de ecuaciones matemáticas compatible con Torque Pro.
 * Soporta variables de bytes OBD2 (A, B, C... Z, AA, AB... etc.),
 * funciones INT16(X:Y), INT24(X:Y:Z), INT32(A:B:C:D), SIGNED(X),
 * y operadores aritméticos y lógicos (+, -, *, /, %, &, |, ^).
 */
object EquationEvaluator {

    /**
     * Evalúa una ecuación de Torque Pro a partir de un mapa de variables (bytes A..Z, AA.. etc).
     */
    fun evaluate(equation: String, variables: Map<String, Double>): Double {
        require(equation.isNotBlank()) { "Ecuación vacía" }
        val byteNames = Regex("[A-Za-z][A-Za-z0-9_]*").findAll(equation)
            .map { it.value.uppercase() }.filter { it !in setOf("INT16", "INT24", "INT32", "SIGNED") }.toSet()
        require(byteNames.isNotEmpty()) { "La ecuación no utiliza bytes de la ECU" }
        require(byteNames.all { it in variables }) { "Respuesta incompleta o función no admitida" }
        val preprocessed = preprocessFunctions(equation, variables)
        val result = Parser(tokenize(preprocessed), variables).parse()
        require(result.isFinite()) { "Resultado no finito" }
        return result
    }

    /**
     * Mapea un array de bytes crudos (hexadecimales convertidos a enteros 0..255)
     * a las variables de Torque Pro:
     * Índice 0 -> "A"
     * Índice 1 -> "B"
     * ...
     * Índice 25 -> "Z"
     * Índice 26 -> "AA"
     * Índice 27 -> "AB", etc.
     */
    fun mapBytesToVariables(bytes: IntArray): Map<String, Double> {
        val vars = mutableMapOf<String, Double>()
        for (i in bytes.indices) {
            val varName = indexToVariableName(i)
            vars[varName] = (bytes[i] and 0xFF).toDouble()
        }
        return vars
    }

    /**
     * Convierte un índice 0-indexed a nombre de variable Torque Pro:
     * 0 -> A, 25 -> Z, 26 -> AA, 27 -> AB, etc.
     */
    fun indexToVariableName(index: Int): String {
        var num = index
        val sb = StringBuilder()
        while (true) {
            val rem = num % 26
            sb.append(('A'.code + rem).toChar())
            num = num / 26 - 1
            if (num < 0) break
        }
        return sb.reverse().toString()
    }

    /**
     * Convierte nombre de variable ("A", "Z", "AA", "AB") a índice 0-indexed.
     */
    fun variableNameToIndex(name: String): Int {
        var res = 0
        val upper = name.uppercase()
        for (ch in upper) {
            if (ch in 'A'..'Z') {
                res = res * 26 + (ch.code - 'A'.code + 1)
            }
        }
        return res - 1
    }

    /**
     * Preprocesa llamadas a funciones de Torque Pro como INT16(V:W), INT32(AM:AN:AO:AP), SIGNED(X).
     */
    private fun preprocessFunctions(eq: String, vars: Map<String, Double>): String {
        var result = eq.replace(" ", "")

        // INT16(X:Y) -> ((X * 256) + Y)
        val int16Regex = Regex("""INT16\s*\(\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)\s*\)""", RegexOption.IGNORE_CASE)
        result = int16Regex.replace(result) { mr ->
            val v1 = mr.groupValues[1]
            val v2 = mr.groupValues[2]
            "(($v1*256)+$v2)"
        }

        // INT24(X:Y:Z) -> ((X * 65536) + (Y * 256) + Z)
        val int24Regex = Regex("""INT24\s*\(\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)\s*\)""", RegexOption.IGNORE_CASE)
        result = int24Regex.replace(result) { mr ->
            val v1 = mr.groupValues[1]
            val v2 = mr.groupValues[2]
            val v3 = mr.groupValues[3]
            "(($v1*65536)+($v2*256)+$v3)"
        }

        // INT32(W:X:Y:Z) -> ((W * 16777216) + (X * 65536) + (Y * 256) + Z)
        val int32Regex = Regex("""INT32\s*\(\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)\s*\)""", RegexOption.IGNORE_CASE)
        result = int32Regex.replace(result) { mr ->
            val v1 = mr.groupValues[1]
            val v2 = mr.groupValues[2]
            val v3 = mr.groupValues[3]
            val v4 = mr.groupValues[4]
            "(($v1*16777216)+($v2*65536)+($v3*256)+$v4)"
        }

        // SIGNED(X)
        val signedRegex = Regex("""SIGNED\s*\(\s*([A-Za-z0-9_]+)\s*\)""", RegexOption.IGNORE_CASE)
        result = signedRegex.replace(result) { mr ->
            val v = mr.groupValues[1]
            val valNum = vars[v.uppercase()] ?: error("Falta el byte $v")
            val signedVal = if (valNum > 127.0) valNum - 256.0 else valNum
            signedVal.toString()
        }

        return result
    }

    private fun tokenize(input: String): List<String> {
        val pattern = Regex("[A-Za-z][A-Za-z0-9_]*|(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)|[()+*/%&|^\\-]")
        val tokens = mutableListOf<String>()
        var end = 0
        for (match in pattern.findAll(input)) {
            require(input.substring(end, match.range.first).isBlank()) { "Símbolo desconocido" }
            tokens.add(match.value.uppercase())
            end = match.range.last + 1
        }
        require(input.substring(end).isBlank()) { "Símbolo desconocido" }
        return tokens
    }

    /** Analizador estricto: una fórmula incompleta o un byte ausente nunca equivale a cero. */
    private class Parser(private val tokens: List<String>, private val variables: Map<String, Double>) {
        private var cursor = 0
        private fun peek(): String? = tokens.getOrNull(cursor)
        private fun take(): String {
            require(cursor < tokens.size) { "Falta un operando" }
            return tokens[cursor++]
        }

        fun parse(): Double {
            val value = expression(1)
            require(cursor == tokens.size) { "Ecuación incompleta o función no admitida" }
            return value
        }

        private fun precedence(op: String?): Int = when (op) {
            "|" -> 1
            "^" -> 2
            "&" -> 3
            "+", "-" -> 4
            "*", "/", "%" -> 5
            else -> 0
        }

        private fun expression(minPrecedence: Int): Double {
            var a = operand()
            while (precedence(peek()) >= minPrecedence) {
                val op = take()
                val b = expression(precedence(op) + 1)
                a = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> { require(b != 0.0) { "División por cero" }; a / b }
                    "%" -> { require(b != 0.0) { "División por cero" }; a % b }
                    "&" -> (a.toLong() and b.toLong()).toDouble()
                    "|" -> (a.toLong() or b.toLong()).toDouble()
                    "^" -> (a.toLong() xor b.toLong()).toDouble()
                    else -> error("Operador desconocido")
                }
            }
            return a
        }

        private fun operand(): Double = when (val token = take()) {
            "-" -> -operand()
            "+" -> operand()
            "(" -> expression(1).also { require(take() == ")") { "Falta cerrar paréntesis" } }
            else -> token.toDoubleOrNull() ?: variables[token] ?: error("Falta el byte $token")
        }
    }
}
