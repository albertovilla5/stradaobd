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
        if (equation.isBlank()) return 0.0
        val preprocessed = preprocessFunctions(equation, variables)
        val tokens = tokenize(preprocessed)
        val rpn = toRpn(tokens)
        return evalRpn(rpn, variables)
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
            val valNum = vars[v.uppercase()] ?: 0.0
            val signedVal = if (valNum > 127.0) valNum - 256.0 else valNum
            signedVal.toString()
        }

        return result
    }

    // Tokenizador básico
    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c in "()+-*/%&|^" -> {
                    // Soporte para signos unarios '-' o '+' al inicio o tras otro operador
                    if ((c == '-' || c == '+') && (tokens.isEmpty() || tokens.last() in "(+-*/%&|^")) {
                        // Unario: agregar '0' antes del signo para evaluación estándar
                        tokens.add("0")
                    }
                    tokens.add(c.toString())
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                        i++
                    }
                    tokens.add(input.substring(start, i))
                }
                c.isLetter() -> {
                    val start = i
                    while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) {
                        i++
                    }
                    tokens.add(input.substring(start, i).uppercase())
                }
                else -> i++
            }
        }
        return tokens
    }

    // Shunting-Yard algorithm para RPN (Reverse Polish Notation)
    private fun toRpn(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()

        fun precedence(op: String): Int = when (op) {
            "|", "^" -> 1
            "&" -> 2
            "+", "-" -> 3
            "*", "/", "%" -> 4
            else -> 0
        }

        for (token in tokens) {
            when {
                token.first().isDigit() || token.first().isLetter() -> output.add(token)
                token == "(" -> stack.addLast(token)
                token == ")" -> {
                    while (stack.isNotEmpty() && stack.last() != "(") {
                        output.add(stack.removeLast())
                    }
                    if (stack.isNotEmpty() && stack.last() == "(") {
                        stack.removeLast()
                    }
                }
                else -> {
                    while (stack.isNotEmpty() && stack.last() != "(" && precedence(stack.last()) >= precedence(token)) {
                        output.add(stack.removeLast())
                    }
                    stack.addLast(token)
                }
            }
        }
        while (stack.isNotEmpty()) {
            output.add(stack.removeLast())
        }
        return output
    }

    // Evaluación de RPN
    private fun evalRpn(rpn: List<String>, variables: Map<String, Double>): Double {
        val stack = ArrayDeque<Double>()

        for (token in rpn) {
            when (token) {
                "+" -> {
                    val b = stack.removeLastOrNull() ?: 0.0
                    val a = stack.removeLastOrNull() ?: 0.0
                    stack.addLast(a + b)
                }
                "-" -> {
                    val b = stack.removeLastOrNull() ?: 0.0
                    val a = stack.removeLastOrNull() ?: 0.0
                    stack.addLast(a - b)
                }
                "*" -> {
                    val b = stack.removeLastOrNull() ?: 0.0
                    val a = stack.removeLastOrNull() ?: 0.0
                    stack.addLast(a * b)
                }
                "/" -> {
                    val b = stack.removeLastOrNull() ?: 1.0
                    val a = stack.removeLastOrNull() ?: 0.0
                    stack.addLast(if (b == 0.0) 0.0 else a / b)
                }
                "%" -> {
                    val b = stack.removeLastOrNull() ?: 1.0
                    val a = stack.removeLastOrNull() ?: 0.0
                    stack.addLast(if (b == 0.0) 0.0 else a % b)
                }
                "&" -> {
                    val b = (stack.removeLastOrNull() ?: 0.0).toLong()
                    val a = (stack.removeLastOrNull() ?: 0.0).toLong()
                    stack.addLast((a and b).toDouble())
                }
                "|" -> {
                    val b = (stack.removeLastOrNull() ?: 0.0).toLong()
                    val a = (stack.removeLastOrNull() ?: 0.0).toLong()
                    stack.addLast((a or b).toDouble())
                }
                "^" -> {
                    val b = (stack.removeLastOrNull() ?: 0.0).toLong()
                    val a = (stack.removeLastOrNull() ?: 0.0).toLong()
                    stack.addLast((a xor b).toDouble())
                }
                else -> {
                    val num = token.toDoubleOrNull()
                    if (num != null) {
                        stack.addLast(num)
                    } else {
                        // Es una variable OBD (A, B, C, AA, etc.)
                        val v = variables[token.uppercase()] ?: 0.0
                        stack.addLast(v)
                    }
                }
            }
        }
        return stack.lastOrNull() ?: 0.0
    }
}
