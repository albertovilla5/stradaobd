package com.torqueobd2.dash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.torqueobd2.dash.data.model.FuelType
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.DangerRed
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import com.torqueobd2.dash.ui.theme.WarningAmber

/**
 * Pantalla de configuración del vehículo (cilindrada, depósito, peso, tipo de combustible)
 * y detección de sensores soportados por la centralita del coche.
 */
@Composable
fun VehicleConfigScreen(
    currentProfile: VehicleProfile,
    allPids: List<ObdPid>,
    isScanningSensors: Boolean,
    onSaveProfile: (VehicleProfile) -> Unit,
    onScanSensors: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var year by remember(currentProfile) { mutableStateOf(currentProfile.modelYear) }
    var displacement by remember(currentProfile) { mutableStateOf(currentProfile.engineDisplacementLiters.toString()) }
    var tankCapacity by remember(currentProfile) { mutableStateOf(currentProfile.fuelTankCapacityLiters.toString()) }
    var weight by remember(currentProfile) { mutableStateOf(currentProfile.vehicleWeightKg.toInt().toString()) }
    var maxRpm by remember(currentProfile) { mutableStateOf(currentProfile.maxRpm.toInt().toString()) }
    var volumetricEfficiency by remember(currentProfile) { mutableStateOf(currentProfile.volumetricEfficiencyPercent.toInt().toString()) }
    var selectedFuel by remember(currentProfile) { mutableStateOf(currentProfile.fuelType) }
    var hideUnsupported by remember(currentProfile) { mutableStateOf(currentProfile.hideUnsupportedSensors) }

    val supportedCount = allPids.count { it.isSupported }
    val unsupportedCount = allPids.size - supportedCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "PERFIL Y CONFIGURACIÓN DEL VEHÍCULO",
            color = RacingOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Parámetros del coche necesarios para el cálculo de consumo exacto, potencia estimada, autonomía y filtrado de sensores.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SECCIÓN 1: DETECCIÓN DE SENSORES SOPORTADOS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .border(1.dp, Color(0xFF28303D), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filtro de Sensores Disponibles",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mostrar únicamente los sensores soportados por el coche",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = hideUnsupported,
                        onCheckedChange = {
                            hideUnsupported = it
                            onSaveProfile(
                                currentProfile.copy(hideUnsupportedSensors = it)
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RacingOrange,
                            checkedTrackColor = Color(0xFF5A2A04)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$supportedCount Soportados",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (unsupportedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DangerRed.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$unsupportedCount No disponibles",
                                    color = DangerRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onScanSensors,
                        enabled = !isScanningSensors,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B38)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isScanningSensors) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escaneando...", fontSize = 11.sp, color = NeonCyan)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escanear ECU", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN 2: PRESETS RÁPIDOS
        Text(
            text = "Plantillas Rápidas de Coche:",
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    name = "Honda Civic 2.2 i-DTEC"
                    displacement = "2.2"
                    tankCapacity = "50"
                    weight = "1450"
                    maxRpm = "5000"
                    selectedFuel = FuelType.DIESEL
                    volumetricEfficiency = "85"
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Honda 2.2 i-DTEC", fontSize = 11.sp, color = NeonCyan)
            }
            OutlinedButton(
                onClick = {
                    name = "Volkswagen 2.0 TDI"
                    displacement = "2.0"
                    tankCapacity = "55"
                    weight = "1480"
                    maxRpm = "5000"
                    selectedFuel = FuelType.DIESEL
                    volumetricEfficiency = "85"
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("VAG 2.0 TDI", fontSize = 11.sp, color = TextWhite)
            }
            OutlinedButton(
                onClick = {
                    name = "Gasolina 1.6 Turbo"
                    displacement = "1.6"
                    tankCapacity = "48"
                    weight = "1280"
                    maxRpm = "6500"
                    selectedFuel = FuelType.GASOLINE
                    volumetricEfficiency = "88"
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("1.6 Turbo", fontSize = 11.sp, color = RacingOrange)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN 3: CAMPOS DEL VEHÍCULO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .border(1.dp, Color(0xFF28303D), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre / Modelo del Vehículo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = displacement,
                        onValueChange = { displacement = it },
                        label = { Text("Cilindrada (Litros)") },
                        placeholder = { Text("ej. 2.2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tankCapacity,
                        onValueChange = { tankCapacity = it },
                        label = { Text("Depósito (Litros)") },
                        placeholder = { Text("ej. 50") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Peso en vacío (kg)") },
                        placeholder = { Text("ej. 1450") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxRpm,
                        onValueChange = { maxRpm = it },
                        label = { Text("Régimen Máx (RPM)") },
                        placeholder = { Text("ej. 5000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = volumetricEfficiency,
                    onValueChange = { volumetricEfficiency = it },
                    label = { Text("Eficiencia Volumétrica (%)") },
                    placeholder = { Text("ej. 85") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Tipo de Combustible:",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FuelType.values().forEach { fuel ->
                        FilterChip(
                            selected = selectedFuel == fuel,
                            onClick = { selectedFuel = fuel },
                            label = { Text(fuel.displayName, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RacingOrange,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val profile = VehicleProfile(
                            name = name.ifBlank { "Vehículo" },
                            modelYear = year,
                            engineDisplacementLiters = displacement.toFloatOrNull() ?: 2.2f,
                            fuelTankCapacityLiters = tankCapacity.toFloatOrNull() ?: 50f,
                            vehicleWeightKg = weight.toFloatOrNull() ?: 1450f,
                            fuelType = selectedFuel,
                            maxRpm = maxRpm.toFloatOrNull() ?: 5000f,
                            volumetricEfficiencyPercent = volumetricEfficiency.toFloatOrNull() ?: 85f,
                            hideUnsupportedSensors = hideUnsupported
                        )
                        onSaveProfile(profile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Perfil del Vehículo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
