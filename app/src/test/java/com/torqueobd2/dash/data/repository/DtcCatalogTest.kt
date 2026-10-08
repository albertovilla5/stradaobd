package com.torqueobd2.dash.data.repository

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DtcCatalogTest {
    @Test fun readsBundledCatalogAndTranslatesCommonCodes() {
        val catalog = DtcCatalog.read(File("src/main/assets/dtc/generic.tsv").reader())
        assertTrue(catalog.describe("P0300").text.contains("combustión"))
        assertTrue(catalog.describe("P0133").text.contains("lenta"))
        assertTrue(catalog.describe("P0312").text.contains("12"))
        assertTrue(catalog.describe("P0099").source.contains("Inglés"))
        assertTrue(File("src/main/assets/dtc/LICENSE.txt").readText().contains("Permission is hereby granted"))
    }
    @Test fun neverAssignsGenericDefinitionToManufacturerCode() {
        val catalog = DtcCatalog.read("P1001\tWrong brand definition\nP3001\tWrong brand\nB1000\tWrong\nP3400\tGeneric\nP0099\tGeneric definition\nBAD\tBad".reader())
        listOf("P1001", "P3001", "B1000", "Z0000").forEach { assertEquals("Sin definición verificada", catalog.describe(it).source) }
        assertEquals("Generic", catalog.describe("P3400").text)
        assertEquals("Generic definition", catalog.describe("P0099").text)
    }
}
