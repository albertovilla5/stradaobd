package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.ui.components.DashboardLayoutPolicy
import org.junit.Assert.*
import org.junit.Test

class DashboardEditorTest {
    @Test fun duplicatesTabIncludingHiddenSlotsAndMakesIndependentIdentities() {
        val widget = DashboardWidget(pidId = "real-sensor", title = "Presión", decimals = 2, scaleMin = -10f, scaleMax = 30f)
        val original = CustomDashboardConfig(widgets = emptyList(), slots = mapOf(0 to widget, 23 to widget.copy(id = "hidden")),
            density = DashboardDensity.COMPACT, gridLayout = DashboardGridLayout.GRID_1X2)
        val tabs = CustomDashboardTabs(listOf(original)).duplicate(original.id)
        val copy = tabs.selectedDashboard
        assertEquals(2, tabs.dashboards.size)
        assertNotEquals(original.id, copy.id)
        assertEquals(setOf(0,23), copy.slots.keys)
        assertEquals(original.density, copy.density)
        assertEquals("real-sensor", copy.slots[0]!!.pidId)
        assertEquals(2, copy.slots[0]!!.decimals)
        assertNotEquals(widget.id, copy.slots[0]!!.id)
        assertEquals(widget, tabs.dashboards.first().slots[0])
    }
    @Test fun deletionSelectsAdjacentTabAndPreservesLastTab() {
        val tabs = CustomDashboardTabs().add("Segundo", DashboardGridLayout.GRID_3X6).add("Tercero", DashboardGridLayout.GRID_4X6)
        val remaining = tabs.delete(tabs.selectedId)
        assertEquals(2, remaining.dashboards.size)
        assertEquals("Segundo", remaining.selectedDashboard.name)
        val one = remaining.delete(remaining.selectedId)
        assertEquals(one, one.delete(one.selectedId))
        assertEquals(tabs, tabs.delete("missing"))
    }
    @Test fun movingSwapsWithoutOverwritingAndMovingToEmptyKeepsHole() {
        val first = DashboardWidget(pidId = "a"); val second = DashboardWidget(pidId = "b")
        val original = CustomDashboardConfig(widgets = emptyList(), slots = mapOf(0 to first, 1 to second, 23 to DashboardWidget()))
        val swapped = original.transferSlot(0,1)
        assertEquals(first, swapped.slots[1]); assertEquals(second, swapped.slots[0])
        assertEquals(original.slots[23], swapped.slots[23])
        val moved = original.transferSlot(0,3)
        assertNull(moved.slots[0]); assertEquals(first, moved.slots[3]); assertEquals(second, moved.slots[1])
    }
    @Test fun copyingNeverOverwritesAndUsesNewWidgetIdentity() {
        val original = CustomDashboardConfig()
        assertEquals(original, original.transferSlot(0,1,true))
        val config = original.withSlot(3,null)
        val copied = config.transferSlot(0,3,true)
        assertEquals(config.getWidgetForSlot(0), copied.getWidgetForSlot(0))
        assertNotEquals(copied.getWidgetForSlot(0)!!.id, copied.getWidgetForSlot(3)!!.id)
        assertEquals(copied.getWidgetForSlot(0)!!.pidId, copied.getWidgetForSlot(3)!!.pidId)
        assertEquals(config, config.transferSlot(0,4,true))
        assertEquals(config, config.transferSlot(3,2,true))
    }
    @Test fun importedTabsAreAppendedWithNewIdentitiesAndSelection() {
        val original = CustomDashboardTabs()
        val imported = CustomDashboardConfig(gridLayout = DashboardGridLayout.GRID_4X6, density = DashboardDensity.COMPACT)
        val result = original.appendImported(listOf(imported))
        assertEquals(original.dashboards.first(), result.dashboards.first())
        assertEquals(2,result.dashboards.size)
        assertNotEquals(imported.id,result.selectedId)
        assertEquals(imported.gridLayout,result.selectedDashboard.gridLayout)
        assertEquals(original,original.appendImported(emptyList()))
    }
    @Test fun preciseFormattingPreservesLabelsAndNeverInventsValues() {
        val reading = ObdReading("a","a","a",14.56789f,"V",0f,20f,decodedValue = 14.56789)
        assertEquals("14.568", DashboardWidget(decimals = 3).displayValue(reading))
        assertEquals("15", DashboardWidget(decimals = 0).displayValue(reading))
        assertEquals("—",DashboardWidget(decimals = 2).displayValue(null))
        assertEquals("—",DashboardWidget(decimals = 2).displayValue(reading.copy(value = Float.NaN)))
        assertEquals("Activo",DashboardWidget(decimals = 4).displayValue(reading.copy(value=1f,decodedValue=1.0,valueLabels=mapOf(1 to "Activo"))))
    }
    @Test fun scaleOnlyAffectsPresentationAndInvalidBoundsFallbackToDefinition() {
        val widget = DashboardWidget(scaleMin = -40f,scaleMax=150f)
        assertTrue(widget.hasCustomScale); assertEquals(-40f to 150f,widget.scale(null))
        listOf(widget.copy(scaleMin=150f),widget.copy(scaleMin=Float.NaN),widget.copy(scaleMax=null),widget.copy(scaleMax=Float.POSITIVE_INFINITY),
            widget.copy(scaleMin=-Float.MAX_VALUE,scaleMax=Float.MAX_VALUE)).forEach {
            assertFalse(it.hasCustomScale);assertEquals(0f to 100f,it.scale(null))
        }
    }
    @Test fun everyGridHasUniqueIdAndExpectedSlotCount() {
        assertEquals(19,DashboardGridLayout.entries.size)
        assertEquals(19,DashboardGridLayout.entries.map { it.id }.distinct().size)
        DashboardGridLayout.entries.filterNot { it.isSplit }.forEach {
            assertEquals(it.id.substringBefore('x').toInt() * it.preferredColumns,it.slotCount)
        }
    }
    @Test fun compactFits24InFullRadioAndNormalRemainsReadable() {
        val compact = DashboardLayoutPolicy.measure(DashboardGridLayout.GRID_4X6,666f,1f,DashboardDensity.COMPACT)
        assertEquals(6,compact.columns);assertEquals(4,compact.rows);assertTrue(compact.minimumHeight <= 292f)
        val normal = DashboardLayoutPolicy.measure(DashboardGridLayout.GRID_4X6,666f,1f)
        assertTrue(normal.minimumHeight > compact.minimumHeight)
        val largeText = DashboardLayoutPolicy.measure(DashboardGridLayout.GRID_4X6,368f,2f,DashboardDensity.COMPACT)
        assertEquals(3,largeText.columns);assertEquals(8,largeText.rows);assertTrue(largeText.minimumHeight >= 8*110)
    }
    @Test fun bulkAddingFillsOnlyVisibleEmptySlotsAndKeepsHiddenAssignments() {
        val config = CustomDashboardConfig(gridLayout = DashboardGridLayout.GRID_2X3,
            slots = mapOf(23 to DashboardWidget(title="Oculto")))
        val additions = (1..4).map { DashboardWidget(pidId = "sensor-$it") }
        val filled = config.fillEmptySlots(additions)
        assertEquals(config.getWidgetForSlot(0),filled.getWidgetForSlot(0))
        assertEquals(additions[0],filled.getWidgetForSlot(4))
        assertEquals(additions[1],filled.getWidgetForSlot(5))
        assertEquals(config.slots[23],filled.slots[23])
        assertEquals(filled,filled.fillEmptySlots(additions))
    }
    @Test fun resizingEveryGridKeepsAllSlotsAccessibleAndHasPositiveGeometry() {
        for (layout in DashboardGridLayout.entries) for (width in listOf(220f,320f,400f,666f,1000f))
            for (font in listOf(1f,1.5f,2f)) for (density in DashboardDensity.entries) {
                val measure = DashboardLayoutPolicy.measure(layout,width,font,density)
                assertTrue(measure.columns in 1..layout.preferredColumns)
                assertTrue(measure.minimumHeight.isFinite() && measure.minimumHeight>0)
                assertTrue(measure.rows * measure.columns >= layout.slotCount || layout.isSplit)
            }
    }
    @Test fun carChoicesCoverEveryRectangleAndNeverExceedFourColumnsOrTwoRows() {
        val choices = DashboardGridLayout.carLayouts
        assertEquals((1..2).flatMap { row -> (1..4).map { column -> column to row } }.toSet(),
            choices.filterNot { it.isSplit }.map { it.preferredColumns to it.preferredRows }.toSet())
        assertTrue(choices.all { it.carSupported && it.preferredColumns in 1..4 && it.preferredRows in 1..2 && it.slotCount <= 8 })
    }
    @Test fun carGridKeepsSelectedColumnsRowsAndSlotsAcrossResolutionsAndFontSizes() {
        for (layout in DashboardGridLayout.carLayouts) for ((width,height) in listOf(
            320f to 140f, 512f to 220f, 640f to 320f, 864f to 420f, 1200f to 600f)) {
            val grid = DashboardLayoutPolicy.carGrid(layout,width,height)
            assertEquals(layout.preferredColumns,grid.columns)
            assertEquals(layout.preferredRows,grid.rows)
            assertEquals((0 until layout.slotCount).toList(),grid.slots)
            assertEquals(layout.isSplit,grid.split)
            for (font in listOf(1f,1.5f,2f)) {
                val measure=DashboardLayoutPolicy.measure(layout,width,font,carMode=true)
                assertEquals(grid.columns,measure.columns);assertEquals(grid.rows,measure.rows)
            }
        }
    }
    @Test fun legacyDenseLayoutsHaveBoundedCarPresentationWithoutErasingMobileSlots() {
        val original=CustomDashboardConfig(gridLayout=DashboardGridLayout.GRID_4X6,
            widgets=emptyList(),slots=(0..23).associateWith { DashboardWidget(title="Sensor $it") })
        val car=original.forCar()
        assertEquals(DashboardGridLayout.GRID_2X4,car.gridLayout)
        assertEquals(8,car.getSlotList().size)
        assertEquals(original.resolvedSlots(),car.resolvedSlots())
        assertEquals(DashboardGridLayout.GRID_4X6,original.gridLayout)
        assertEquals(24,original.getSlotList().size)
    }
    @Test fun explicitCarLayoutSurvivesChangesToMobileGridAndDuplicates() {
        val config=CustomDashboardConfig(gridLayout=DashboardGridLayout.GRID_4X6,carGridLayout=DashboardGridLayout.GRID_2X3)
        assertEquals(DashboardGridLayout.GRID_2X3,config.forCar().gridLayout)
        assertEquals(DashboardGridLayout.GRID_2X3,config.copy(gridLayout=DashboardGridLayout.GRID_1X1).forCar().gridLayout)
        assertEquals(config.carGridLayout,config.duplicateAs("Otro").carGridLayout)
        assertEquals(config.forCar().gridLayout,config.forCar().forCar().gridLayout)
    }
}
