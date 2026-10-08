package com.torqueobd2.dash.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.ui.automotive.*
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.theme.*

@Composable
fun CarVehicleConfigScreen(activeProfile: VehicleProfile, savedVehicles: List<VehicleProfile>, allPids: List<ObdPid>,
    vehicleState: VehicleState, connectionState: ConnectionState, isScanningSensors: Boolean,
    onSaveVehicle: (VehicleProfile) -> Unit, onSetActiveVehicle: (String) -> Unit,
    onDeleteVehicle: (String) -> Unit, onScanSensors: () -> Unit, onTogglePid: (String, Boolean) -> Unit,
    onVisibleSensorsChanged: (Set<String>) -> Unit) {
    var section by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf(activeProfile.id) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    val profile = savedVehicles.find { it.id == selectedId } ?: activeProfile
    BackHandler(enabled = section != "home") { section = "home" }
    LaunchedEffect(section) { if (section != "sensors") onVisibleSensorsChanged(emptySet()) }
    when (section) {
        "garage" -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(savedVehicles, key = { it.id }) { vehicle ->
                    Surface(onClick = { selectedId = vehicle.id; section = "profile" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp).semantics { contentDescription = "Perfil: ${vehicle.name}" },
                        color = CockpitSurface, shape = RoundedCornerShape(28.dp)) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CarIcon(Icons.Default.DirectionsCar, null, tint = CockpitAccent)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CarText(vehicle.name, color = TextWhite, fontSize = 32.sp)
                                CarText("${vehicle.modelYear} · ${vehicle.fuelType.displayName}", color = CockpitMuted, fontSize = 24.sp)
                                if (vehicle.id == activeProfile.id) CarText("Vehículo activo", color = CockpitTeal, fontSize = 24.sp)
                            }
                            CarIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CockpitMuted)
                        }
                    }
                }
            }
            CarButton({ creating = true }, Modifier.fillMaxWidth()) { CarIcon(Icons.Default.Add, null); Spacer(Modifier.width(12.dp)); CarText("Nuevo vehículo", fontSize = 24.sp) }
        }
        "profile" -> CarMenu(listOf(
            CarMenuEntry("Ficha del vehículo", "${profile.name} · ${profile.modelYear}", Icons.Default.DirectionsCar) { section = "specs" },
            CarMenuEntry("Editar ficha", "Datos, motor y ajustes OBD", Icons.Default.Edit) { editing = true },
            CarMenuEntry(if (profile.id == activeProfile.id) "Vehículo activo" else "Usar este vehículo", "${profile.fuelType.displayName} · ${profile.engineDisplacementLiters} L", Icons.Default.CheckCircle,
                enabled = profile.id != activeProfile.id) { onSetActiveVehicle(profile.id) },
            CarMenuEntry("Eliminar vehículo", if (savedVehicles.size > 1) profile.name else "Conserva al menos un perfil", Icons.Default.Delete,
                enabled = savedVehicles.size > 1) { deleting = true }
        ))
        "specs" -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Surface(color = CockpitSurface, shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CarText(profile.name, color = TextWhite, fontSize = 32.sp)
                        val draft = VehicleProfileDraft.from(profile)
                        VehicleField.entries.filter { it !in listOf(VehicleField.NAME, VehicleField.OPTIONS, VehicleField.REVIEW) }.forEach { field ->
                            CarText("${field.title}: ${if (field == VehicleField.FUEL) profile.fuelType.displayName else draft.value(field).ifBlank { "Automático" }} ${field.unit}",
                                color = CockpitMuted, fontSize = 24.sp)
                        }
                        CarText(if (profile.hideUnsupportedSensors) "Sensores no disponibles ocultos" else "Todos los sensores visibles", color = CockpitMuted, fontSize = 24.sp)
                    }
                }
            }
            item { CarButton({ editing = true }, Modifier.fillMaxWidth()) { CarText("Editar ficha", fontSize = 24.sp) } }
        }
        "sensors" -> GridMonitorScreen(vehicleState, allPids, connectionState, isScanningSensors,
            onScanSensors, onTogglePid, onVisibleSensorsChanged)
        else -> CarMenu(listOf(
            CarMenuEntry("Perfil activo", activeProfile.name, Icons.Default.DirectionsCar) { selectedId = activeProfile.id; section = "profile" },
            CarMenuEntry("Garaje", if (savedVehicles.size == 1) "1 vehículo guardado" else "${savedVehicles.size} vehículos guardados", Icons.Default.Dashboard) { section = "garage" },
            CarMenuEntry("Nuevo vehículo", "Crear una ficha paso a paso", Icons.Default.Add) { creating = true },
            CarMenuEntry("Sensores de la ECU", "Escanear y elegir lecturas", Icons.Default.Speed) { section = "sensors" },
            CarMenuEntry("Privacidad", "Datos guardados en tu dispositivo", Icons.Default.Security) { privacy = true }
        ))
    }
    if (creating || editing) ResponsiveDialog(onDismissRequest = { creating = false; editing = false }) {
        val dialogHeight = maxHeight
        Surface(Modifier.fillMaxWidth().height(dialogHeight), color = CockpitBackground, shape = RoundedCornerShape(28.dp)) {
            VehicleProfileWizard(if (creating) null else profile, onCancel = { creating = false; editing = false }, onSave = {
                onSaveVehicle(it)
                if (creating) onSetActiveVehicle(it.id)
                selectedId = it.id; section = "profile"; creating = false; editing = false
            })
        }
    }
    if (deleting) ResponsiveDialog(onDismissRequest = { deleting = false }) {
        Surface(color = CockpitSurface, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CarText("Eliminar vehículo", color = TextWhite, fontSize = 28.sp)
                CarText("¿Eliminar la ficha de ${profile.name}?", color = CockpitMuted, fontSize = 24.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CarOutlinedButton({ deleting = false }, Modifier.weight(1f)) { CarText("Cancelar", fontSize = 24.sp) }
                    CarButton({ onDeleteVehicle(profile.id); deleting = false; section = "garage" }, Modifier.weight(1f)) { CarText("Eliminar", fontSize = 24.sp) }
                }
            }
        }
    }
    if (privacy) PrivacyPolicyDialog { privacy = false }
}
