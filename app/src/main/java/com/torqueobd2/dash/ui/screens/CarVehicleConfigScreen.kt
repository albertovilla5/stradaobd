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
import com.torqueobd2.dash.i18n.tr
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
                    val profileDesc = tr("Perfil: ${vehicle.name}", "Profile: ${vehicle.name}")
                    Surface(onClick = { selectedId = vehicle.id; section = "profile" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp).semantics { contentDescription = profileDesc },
                        color = CockpitSurface, shape = RoundedCornerShape(28.dp)) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CarIcon(Icons.Default.DirectionsCar, null, tint = CockpitAccent)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CarText(vehicle.name, color = TextWhite, fontSize = 32.sp)
                                CarText("${vehicle.modelYear} · ${vehicle.fuelType.displayName}", color = CockpitMuted, fontSize = 24.sp)
                                if (vehicle.id == activeProfile.id) CarText(tr("Vehículo activo", "Active vehicle"), color = CockpitTeal, fontSize = 24.sp)
                            }
                            CarIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CockpitMuted)
                        }
                    }
                }
            }
            CarButton({ creating = true }, Modifier.fillMaxWidth()) { CarIcon(Icons.Default.Add, null); Spacer(Modifier.width(12.dp)); CarText(tr("Nuevo vehículo", "New vehicle"), fontSize = 24.sp) }
        }
        "profile" -> CarMenu(listOf(
            CarMenuEntry(tr("Ficha del vehículo", "Vehicle specs"), "${profile.name} · ${profile.modelYear}", Icons.Default.DirectionsCar) { section = "specs" },
            CarMenuEntry(tr("Editar ficha", "Edit specs"), tr("Datos, motor y ajustes OBD", "Data, engine and OBD settings"), Icons.Default.Edit) { editing = true },
            CarMenuEntry(if (profile.id == activeProfile.id) tr("Vehículo activo", "Active vehicle") else tr("Usar este vehículo", "Use this vehicle"), "${profile.fuelType.displayName} · ${profile.engineDisplacementLiters} L", Icons.Default.CheckCircle,
                enabled = profile.id != activeProfile.id) { onSetActiveVehicle(profile.id) },
            CarMenuEntry(tr("Eliminar vehículo", "Delete vehicle"), if (savedVehicles.size > 1) profile.name else tr("Conserva al menos un perfil", "Keep at least one profile"), Icons.Default.Delete,
                enabled = savedVehicles.size > 1) { deleting = true }
        ))
        "specs" -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Surface(color = CockpitSurface, shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CarText(profile.name, color = TextWhite, fontSize = 32.sp)
                        val draft = VehicleProfileDraft.from(profile)
                        VehicleField.entries.filter { it !in listOf(VehicleField.NAME, VehicleField.OPTIONS, VehicleField.REVIEW) }.forEach { field ->
                            CarText("${field.title}: ${if (field == VehicleField.FUEL) profile.fuelType.displayName else draft.value(field).ifBlank { tr("Automático", "Automatic") }} ${field.unit}",
                                color = CockpitMuted, fontSize = 24.sp)
                        }
                        CarText(if (profile.hideUnsupportedSensors) tr("Sensores no disponibles ocultos", "Unavailable sensors hidden") else tr("Todos los sensores visibles", "All sensors visible"), color = CockpitMuted, fontSize = 24.sp)
                    }
                }
            }
            item { CarButton({ editing = true }, Modifier.fillMaxWidth()) { CarText(tr("Editar ficha", "Edit specs"), fontSize = 24.sp) } }
        }
        "sensors" -> GridMonitorScreen(vehicleState, allPids, connectionState, isScanningSensors,
            onScanSensors, onTogglePid, onVisibleSensorsChanged)
        else -> CarMenu(listOf(
            CarMenuEntry(tr("Perfil activo", "Active profile"), activeProfile.name, Icons.Default.DirectionsCar) { selectedId = activeProfile.id; section = "profile" },
            CarMenuEntry(tr("Garaje", "Garage"), if (savedVehicles.size == 1) tr("1 vehículo guardado", "1 saved vehicle") else tr("${savedVehicles.size} vehículos guardados", "${savedVehicles.size} saved vehicles"), Icons.Default.Dashboard) { section = "garage" },
            CarMenuEntry(tr("Nuevo vehículo", "New vehicle"), tr("Crear una ficha paso a paso", "Create a profile step by step"), Icons.Default.Add) { creating = true },
            CarMenuEntry(tr("Sensores de la ECU", "ECU sensors"), tr("Escanear y elegir lecturas", "Scan and choose readings"), Icons.Default.Speed) { section = "sensors" },
            CarMenuEntry(tr("Privacidad", "Privacy"), tr("Datos guardados en tu dispositivo", "Data stored on your device"), Icons.Default.Security) { privacy = true }
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
                CarText(tr("Eliminar vehículo", "Delete vehicle"), color = TextWhite, fontSize = 28.sp)
                CarText(tr("¿Eliminar la ficha de ${profile.name}?", "Delete profile for ${profile.name}?"), color = CockpitMuted, fontSize = 24.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CarOutlinedButton({ deleting = false }, Modifier.weight(1f)) { CarText(tr("Cancelar", "Cancel"), fontSize = 24.sp) }
                    CarButton({ onDeleteVehicle(profile.id); deleting = false; section = "garage" }, Modifier.weight(1f)) { CarText(tr("Eliminar", "Delete"), fontSize = 24.sp) }
                }
            }
        }
    }
    if (privacy) PrivacyPolicyDialog { privacy = false }
}
