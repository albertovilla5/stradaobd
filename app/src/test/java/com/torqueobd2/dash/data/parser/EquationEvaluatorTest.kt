package com.torqueobd2.dash.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class EquationEvaluatorTest {

    @Test
    fun testVariableNameToIndex() {
        assertEquals(0, EquationEvaluator.variableNameToIndex("A"))
        assertEquals(1, EquationEvaluator.variableNameToIndex("B"))
        assertEquals(25, EquationEvaluator.variableNameToIndex("Z"))
        assertEquals(26, EquationEvaluator.variableNameToIndex("AA"))
        assertEquals(27, EquationEvaluator.variableNameToIndex("AB"))
    }

    @Test
    fun testIndexToVariableName() {
        assertEquals("A", EquationEvaluator.indexToVariableName(0))
        assertEquals("B", EquationEvaluator.indexToVariableName(1))
        assertEquals("Z", EquationEvaluator.indexToVariableName(25))
        assertEquals("AA", EquationEvaluator.indexToVariableName(26))
        assertEquals("AB", EquationEvaluator.indexToVariableName(27))
    }

    @Test
    fun testRpmEquation() {
        // ((A*256)+B)/4 para A=15 (0x0F), B=160 (0xA0) -> (15*256 + 160)/4 = 4000 / 4 = 1000 RPM
        val vars = mapOf("A" to 15.0, "B" to 160.0)
        val result = EquationEvaluator.evaluate("((A*256)+B)/4", vars)
        assertEquals(1000.0, result, 0.001)
    }

    @Test
    fun testCoolantTempEquation() {
        // A - 40 para A=130 -> 90°C
        val vars = mapOf("A" to 130.0)
        val result = EquationEvaluator.evaluate("A-40", vars)
        assertEquals(90.0, result, 0.001)
    }

    @Test
    fun testInt16Function() {
        // INT16(V:W)/10 para V=1, W=100 -> (1*256 + 100) / 10 = 35.6
        val vars = mapOf("V" to 1.0, "W" to 100.0)
        val result = EquationEvaluator.evaluate("INT16(V:W)/10", vars)
        assertEquals(35.6, result, 0.001)
    }

    @Test
    fun testBitwiseAndAdditionEquation() {
        // (AM & 127)+(AN & 207)+AO+(AP & 254)
        val vars = mapOf(
            "AM" to 129.0, // 129 & 127 = 1
            "AN" to 207.0, // 207 & 207 = 207
            "AO" to 5.0,   // 5
            "AP" to 255.0  // 255 & 254 = 254
        )
        // 1 + 207 + 5 + 254 = 467
        val result = EquationEvaluator.evaluate("(AM & 127)+(AN & 207)+AO+(AP & 254)", vars)
        assertEquals(467.0, result, 0.001)
    }

    @Test
    fun testEgtEquation() {
        // O * 4 para O = 75 -> 300°C
        val vars = mapOf("O" to 75.0)
        val result = EquationEvaluator.evaluate("O*4", vars)
        assertEquals(300.0, result, 0.001)
    }

    @Test
    fun testOilLevelEquation() {
        // AS / 2 para AS = 196 -> 98 mm
        val vars = mapOf("AS" to 196.0)
        val result = EquationEvaluator.evaluate("AS/2", vars)
        assertEquals(98.0, result, 0.001)
    }
}
