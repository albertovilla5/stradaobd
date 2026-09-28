package com.torqueobd2.dash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.model.WidgetSize
import com.torqueobd2.dash.data.model.WidgetType
import com.torqueobd2.dash.ui.components.AnalogGauge
import com.torqueobd2.dash.ui.components.ControlPedalSlider
import com.torqueobd2.dash.ui.components.RealtimeChart
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite

/**
 * Pantalla que permite diseñar y visualizar un Dashboard totalmente adaptable a resolución,
 * usando GridCells.Adaptive para ajustar fluidamente la cantidad de columnas en cualquier pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDashboardScreen(
    customDashboard: CustomDashboardConfig,
    availablePids: List<ObdPid>,
    vehicleState: VehicleState,
    isSimulationMode: Boolean,
    onAddWidget: (DashboardWidget) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onToggleSize: (String) -> Unit,
    onResetDefault: () -> Unit,
    onThrottleChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val selectablePids = availablePids.filter { it.isSupported }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(10.dp)
    ) {
        // Barra superior de herramientas del diseñador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DASHBOARD PERSONALIZADO",
                    color = RacingOrange,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEditMode) "Modo Edición: cambia tamaño o elimina" else "${customDashboard.widgets.size} indicadores activos",
                    color = if (isEditMode) NeonGreen else TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { isEditMode = !isEditMode },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isEditMode) NeonGreen else TextWhite)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "Listo" else "Editar", fontSize = 11.sp, color = if (isEditMode) NeonGreen else TextWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid adaptativo que calcula dinámicamente columnas según el ancho de pantalla
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(
                customDashboard.widgets,
                key = { it.id },
                span = { widget ->
                    if (widget.size == WidgetSize.FULL_WIDTH) GridItemSpan(2) else GridItemSpan(1)
                }
            ) { widget ->
                CustomWidgetItem(
                    widget = widget,
                    vehicleState = vehicleState,
                    isEditMode = isEditMode,
                    onToggleSize = { onToggleSize(widget.id) },
                    onDelete = { onRemoveWidget(widget.id) }
                )
            }
        }

        if (isSimulationMode) {
            Spacer(modifier = Modifier.height(6.dp))
            ControlPedalSlider(onThrottleChanged = onThrottleChanged)
        }
    }

    if (showAddDialog) {
        AddWidgetDialog(
            availablePids = selectablePids,
            onDismiss = { showAddDialog = false },
            onAdd = {
                onAddWidget(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CustomWidgetItem(
    widget: DashboardWidget,
    vehicleState: VehicleState,
    isEditMode: Boolean,
    onToggleSize: () -> Unit,
    onDelete: () -> Unit
) {
    val reading = vehicleState.readings[widget.pidShortName]
    val value = reading?.value ?: when (widget.pidShortName) {
        "RPM" -> vehicleState.rpm
        "Velocidad" -> vehicleState.speedKmh
        "MAP" -> vehicleState.boostPressureBar
        "Temp Liq" -> vehicleState.coolantTempC
        "EGT" -> vehicleState.exhaustGasTemp1C
        "DPF diff.pres" -> vehicleState.dpfDifferentialPressureKpa
        "Carga" -> vehicleState.engineLoadPercent
        "Acelerador" -> vehicleState.throttlePercent
        "Consumo" -> vehicleState.fuelRateLh
        "Voltaje" -> vehicleState.batteryVoltage
        else -> 0f
    }
    val unit = reading?.units ?: when (widget.pidShortName) {
        "RPM" -> "RPM"
        "Velocidad" -> "km/h"
        "MAP" -> "bar"
        "Temp Liq", "EGT" -> "°C"
        "DPF diff.pres" -> "kPa"
        "Carga", "Acelerador" -> "%"
        "Voltaje" -> "V"
        else -> ""
    }

    val widgetColor = when (widget.colorHex) {
        "#00E5FF" -> NeonCyan
        "#00E676" -> NeonGreen
        "#FF1744" -> Color(0xFFFF1744)
        "#FFD600" -> Color(0xFFFFD600)
        else -> RacingOrange
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, Color(0xFF28303D), RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (widget.type) {
                WidgetType.ANALOG_GAUGE -> {
                    val maxVal = when (widget.pidShortName) {
                        "RPM" -> 6000f
                        "Velocidad" -> 240f
                        "MAP" -> 2.5f
                        "Temp Liq" -> 130f
                        "EGT" -> 800f
                        "DPF diff.pres" -> 30f
                        else -> 100f
                    }
                    val step = when (widget.pidShortName) {
                        "RPM" -> 1000f
                        "Velocidad" -> 40f
                        "MAP" -> 0.5f
                        "Temp Liq" -> 20f
                        else -> 20f
                    }
                    AnalogGauge(
                        value = value,
                        minValue = 0f,
                        maxValue = maxVal,
                        title = widget.title,
                        unit = unit,
                        majorTickStep = step,
                        primaryColor = widgetColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                WidgetType.DIGITAL_HUD -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(text = widget.title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (value >= 100f) value.toInt().toString() else String.format("%.1f", value),
                                color = TextWhite,
                                fontSize = if (widget.size == WidgetSize.FULL_WIDTH) 48.sp else 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = unit, color = widgetColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }

                WidgetType.HORIZONTAL_BAR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = widget.title, color = TextMuted, fontSize = 11.sp)
                            Text(text = "${if (value >= 100f) value.toInt() else String.format("%.1f", value)} $unit", color = TextWhite, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = (value / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = widgetColor,
                            trackColor = Color(0xFF222834)
                        )
                    }
                }

                WidgetType.SPARKLINE_CHART -> {
                    val points = vehicleState.sparklineHistory[widget.pidShortName] ?: emptyList()
                    RealtimeChart(
                        title = widget.title,
                        points = points,
                        currentValue = if (value >= 100f) value.toInt().toString() else String.format("%.1f", value),
                        unit = unit,
                        lineColor = widgetColor,
                        chartHeight = if (widget.size == WidgetSize.FULL_WIDTH) 95.dp else 75.dp
                    )
                }

                WidgetType.COMPACT_TILE -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Text(text = widget.title, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (value >= 100f) value.toInt() else String.format("%.1f", value)} $unit",
                            color = widgetColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        if (isEditMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xCC11141A), RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                IconButton(onClick = onToggleSize, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Cambiar Tamaño", tint = NeonCyan, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF4D4D), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWidgetDialog(
    availablePids: List<ObdPid>,
    onDismiss: () -> Unit,
    onAdd: (DashboardWidget) -> Unit
) {
    var selectedType by remember { mutableStateOf(WidgetType.ANALOG_GAUGE) }
    var selectedPid by remember { mutableStateOf(availablePids.firstOrNull()?.shortName ?: "RPM") }
    var customTitle by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FF6D00") }
    var selectedSize by remember { mutableStateOf(WidgetSize.STANDARD) }

    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var expandedPidDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Indicador al Dashboard", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Indicador") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        WidgetType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedPidDropdown,
                    onExpandedChange = { expandedPidDropdown = !expandedPidDropdown }
                ) {
                    val currentPidName = availablePids.find { it.shortName == selectedPid }?.name ?: selectedPid
                    OutlinedTextField(
                        value = "$selectedPid ($currentPidName)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sensor / Parámetro") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPidDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPidDropdown,
                        onDismissRequest = { expandedPidDropdown = false }
                    ) {
                        availablePids.forEach { pid ->
                            DropdownMenuItem(
                                text = { Text("${pid.shortName} - ${pid.name} [${pid.units}]") },
                                onClick = {
                                    selectedPid = pid.shortName
                                    if (customTitle.isBlank()) {
                                        customTitle = pid.name
                                    }
                                    expandedPidDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Título Personalizado (opcional)") },
                    placeholder = { Text(selectedPid) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedSize = WidgetSize.STANDARD },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedSize == WidgetSize.STANDARD) RacingOrange.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1x1 Estándar", fontSize = 11.sp, color = if (selectedSize == WidgetSize.STANDARD) RacingOrange else TextMuted)
                    }

                    OutlinedButton(
                        onClick = { selectedSize = WidgetSize.FULL_WIDTH },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedSize == WidgetSize.FULL_WIDTH) RacingOrange.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2x1 Ancho", fontSize = 11.sp, color = if (selectedSize == WidgetSize.FULL_WIDTH) RacingOrange else TextMuted)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (customTitle.isNotBlank()) customTitle else {
                        availablePids.find { it.shortName == selectedPid }?.name ?: selectedPid
                    }
                    onAdd(
                        DashboardWidget(
                            type = selectedType,
                            pidShortName = selectedPid,
                            title = finalTitle,
                            colorHex = selectedColor,
                            size = selectedSize
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RacingOrange)
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        },
        containerColor = Color(0xFF1D222B)
    )
}
