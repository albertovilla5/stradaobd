package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import com.torqueobd2.dash.ui.automotive.CarIconButton as IconButton
import com.torqueobd2.dash.ui.automotive.CarTab as Tab
import com.torqueobd2.dash.ui.automotive.CarFilterChip as FilterChip
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import com.torqueobd2.dash.ui.components.ResponsiveFormDialog
import com.torqueobd2.dash.ui.components.RacingHeader
import com.torqueobd2.dash.ui.components.FittedValue

import com.torqueobd2.dash.ui.components.racingPanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.model.FuelType
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory
import com.torqueobd2.dash.data.model.VehicleProfile
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.i18n.trUnit
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.DangerRed
import com.torqueobd2.dash.ui.theme.DarkSurface
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import com.torqueobd2.dash.ui.theme.WarningAmber
import java.util.Locale

/**
 * Pantalla integral de gestión de flota de vehículos (CRUD completo con solo 1 activo)
 * y visualizador interactivo de todos los sensores disponibles detectados en la ECU conectada.
 */
@Composable
fun VehicleConfigScreen(
    activeProfile: VehicleProfile,
    savedVehicles: List<VehicleProfile>,
    allPids: List<ObdPid>,
    vehicleState: VehicleState,
    connectionState: ConnectionState,
    isScanningSensors: Boolean,
    onSaveVehicle: (VehicleProfile) -> Unit,
    onSetActiveVehicle: (String) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onScanSensors: () -> Unit,
    onTogglePid: (String, Boolean) -> Unit,
    onVisibleSensorsChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (com.torqueobd2.dash.ui.automotive.LocalCarMode.current) {
        CarVehicleConfigScreen(activeProfile, savedVehicles, allPids, vehicleState, connectionState, isScanningSensors,
            onSaveVehicle, onSetActiveVehicle, onDeleteVehicle, onScanSensors, onTogglePid, onVisibleSensorsChanged)
        return
    }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Mis Vehículos, 1: Sensores Disponibles
    var vehicleToEdit by remember { mutableStateOf<VehicleProfile?>(null) }
    var isCreatingVehicle by remember { mutableStateOf(false) }
    var vehicleToDelete by remember { mutableStateOf<VehicleProfile?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var successNotification by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSubTab) {
        if (selectedSubTab == 0) onVisibleSensorsChanged(emptySet())
    }

    // Limpiar notificación tras 4 segundos
    LaunchedEffect(successNotification) {
        if (successNotification != null) {
            kotlinx.coroutines.delay(4000)
            successNotification = null
        }
    }

    val compactHeader = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BoxWithConstraints(modifier.fillMaxSize().background(CarbonBlack)) {
        val iconOnlyAdd = maxWidth < 480.dp * uiLayoutScale()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CarbonBlack)
        ) {
            if (!compactHeader) {
                RacingHeader(tr("Tu garaje", "Your garage"), tr("Vehículos, calibración y sensores disponibles.", "Vehicles, calibration and available sensors."), modifier = Modifier.padding(16.dp), eyebrow = tr("VEHÍCULO / CONFIGURACIÓN", "VEHICLE / CONFIGURATION"))
            }
            // ENCABEZADO CON PESTAÑAS (MIS VEHÍCULOS / SENSORES DISPONIBLES)
            Row(Modifier.fillMaxWidth().background(DarkSurface), verticalAlignment = Alignment.CenterVertically) {
                TabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = selectedSubTab,
                    containerColor = DarkSurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                            color = RacingOrange,
                            height = 3.dp
                        )
                    }
                ) {
                    val supportedCount = allPids.count { it.supportChecked && it.isSupported }
                    Tab(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = if (selectedSubTab == 0) RacingOrange else TextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Garaje · ${savedVehicles.size}", "Garage · ${savedVehicles.size}"),
                                    color = if (selectedSubTab == 0) RacingOrange else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = if (selectedSubTab == 1) NeonCyan else TextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Sensores · $supportedCount/${allPids.size}", "Sensors · $supportedCount/${allPids.size}"),
                                    color = if (selectedSubTab == 1) NeonCyan else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }
                if (compactHeader && selectedSubTab == 0) {
                    Button(
                        onClick = { isCreatingVehicle = true },
                        modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = if (iconOnlyAdd) tr("Añadir Vehículo", "Add Vehicle") else null,
                            modifier = Modifier.size(18.dp), tint = Color.Black)
                        if (!iconOnlyAdd) {
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Añadir Vehículo", "Add Vehicle"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // BANNER DE NOTIFICACIÓN DE ÉXITO
            AnimatedVisibility(
                visible = successNotification != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F3B2C))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successNotification ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { successNotification = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = tr("Cerrar", "Close"), tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // CONTENIDO DE LA PESTAÑA SELECCIONADA
            Box(modifier = Modifier.weight(1f)) {
                if (selectedSubTab == 0) {
                    VehiclesListSection(
                        compactHeader = compactHeader,
                        activeProfile = activeProfile,
                        savedVehicles = savedVehicles,
                        onAddNewVehicle = { isCreatingVehicle = true },
                        onEditVehicle = { vehicleToEdit = it },
                        onSetActiveVehicle = {
                            onSetActiveVehicle(it)
                            successNotification = tr("Vehículo activo actualizado.", "Active vehicle updated.")
                        },
                        onDeleteVehicle = { vehicleToDelete = it },
                        onOpenPrivacyDialog = { showPrivacyDialog = true }
                    )
                } else {
                    SensorsListSection(
                        allPids = allPids,
                        vehicleState = vehicleState,
                        connectionState = connectionState,
                        isScanningSensors = isScanningSensors,
                        onScanSensors = onScanSensors,
                        onTogglePid = onTogglePid,
                        onVisibleSensorsChanged = onVisibleSensorsChanged
                    )
                }
            }
        }

    }

    // DIÁLOGO PARA CREAR O EDITAR VEHÍCULO
    if (isCreatingVehicle || vehicleToEdit != null) {
        val editingProfile = vehicleToEdit ?: VehicleProfile(
            name = "",
            modelYear = "2020",
            engineDisplacementLiters = 2.0f,
            fuelTankCapacityLiters = 50f,
            vehicleWeightKg = 1400f,
            fuelType = FuelType.DIESEL,
            maxRpm = 5000f,
            maxPowerHp = 150f,
            maxTorqueNm = 350f,
            volumetricEfficiencyPercent = 85f
        )

        VehicleEditDialog(
            profile = editingProfile,
            isNew = isCreatingVehicle,
            onDismiss = {
                isCreatingVehicle = false
                vehicleToEdit = null
            },
            onConfirm = { updatedVehicle, makeActive ->
                onSaveVehicle(updatedVehicle)
                if (makeActive) {
                    onSetActiveVehicle(updatedVehicle.id)
                }
                successNotification = tr(
                    "Vehículo '${updatedVehicle.name}' guardado correctamente.",
                    "Vehicle '${updatedVehicle.name}' saved successfully."
                )
                isCreatingVehicle = false
                vehicleToEdit = null
            }
        )
    }

    // DIÁLOGO DE CONFIRMACIÓN DE BORRADO
    if (vehicleToDelete != null) {
        val target = vehicleToDelete!!
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            containerColor = CardBackground,
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = tr("Eliminar Vehículo", "Delete Vehicle"),
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(
                            "¿Deseas eliminar permanentemente '${target.name}' del garaje?",
                            "Do you want to permanently delete '${target.name}' from the garage?"
                        ),
                        color = TextWhite,
                        fontSize = 13.sp
                    )
                    if (target.id == activeProfile.id) {
                        Text(
                            text = tr(
                                "Aviso: Este es el vehículo activo actualmente. Al eliminarlo, se activará automáticamente otro vehículo de tu lista.",
                                "Note: This is currently the active vehicle. Deleting it will automatically activate another vehicle from your list."
                            ),
                            color = WarningAmber,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVehicle(target.id)
                        successNotification = tr("Vehículo '${target.name}' eliminado.", "Vehicle '${target.name}' deleted.")
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(tr("Eliminar", "Delete"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text(tr("Cancelar", "Cancel"), color = TextMuted)
                }
            }
        )
    }

    // DIÁLOGO DE PRIVACIDAD
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

/**
 * Pestaña 0: Lista de vehículos guardados con CRUD completo y selector de vehículo activo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehiclesListSection(
    compactHeader: Boolean,
    activeProfile: VehicleProfile,
    savedVehicles: List<VehicleProfile>,
    onAddNewVehicle: () -> Unit,
    onEditVehicle: (VehicleProfile) -> Unit,
    onSetActiveVehicle: (String) -> Unit,
    onDeleteVehicle: (VehicleProfile) -> Unit,
    onOpenPrivacyDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = if (compactHeader) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactHeader) 8.dp else 12.dp)
    ) {
        // CABECERA CON BOTÓN DE AÑADIR
        if (!compactHeader) {
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.widthIn(max = 480.dp)) {
                        Text(
                            text = tr("GARAJE DE VEHÍCULOS", "VEHICLE GARAGE"),
                            color = RacingOrange,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "El vehículo activo define la conexión y la escala de RPM. Su ficha no genera lecturas.",
                                "The active vehicle defines connection and RPM scale. Its profile does not generate readings."
                            ),
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAddNewVehicle,
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Añadir Vehículo", "Add Vehicle"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // TARJETAS DE VEHÍCULOS GUARDADOS
        items(savedVehicles, key = { it.id }) { vehicle ->
            val isActive = vehicle.id == activeProfile.id
            VehicleCard(
                vehicle = vehicle,
                isActive = isActive,
                canDelete = savedVehicles.size > 1,
                onActivate = { onSetActiveVehicle(vehicle.id) },
                onEdit = { onEditVehicle(vehicle) },
                onDelete = { onDeleteVehicle(vehicle) }
            )
        }

        // FOOTER DE INFORMACIÓN Y PRIVACIDAD
        item {
            if (compactHeader) {
                Text(
                    tr(
                        "El vehículo activo define la conexión y la escala de RPM. Su ficha no genera lecturas.",
                        "The active vehicle defines connection and RPM scale. Its profile does not generate readings."
                    ),
                    color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .racingPanel()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Privacidad y telemetría 100% local en tu dispositivo.", "100% local privacy and telemetry on your device."),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = onOpenPrivacyDialog) {
                        Text(tr("Ver Política", "View Policy"), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta individual de vehículo en el Garaje con especificaciones y acciones CRUD.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleCard(
    vehicle: VehicleProfile,
    isActive: Boolean,
    canDelete: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) NeonCyan else Color(0xFF293341)
    val borderWidth = if (isActive) 2.dp else 1.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) Color(0xFF141E28) else CardBackground)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        val specColumns = if (maxWidth < 420.dp * uiLayoutScale()) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // FILA SUPERIOR: TÍTULO, AÑO Y BADGE ACTIVO/INACTIVO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = if (isActive) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = vehicle.name.ifBlank { tr("Vehículo Sin Nombre", "Unnamed Vehicle") },
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Año ${vehicle.modelYear} • ${vehicle.fuelType.displayName}", "Year ${vehicle.modelYear} • ${vehicle.fuelType.displayName}"),
                            color = if (isActive) NeonCyan.copy(alpha = 0.8f) else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // BADGE DE ESTADO
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.18f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("ACTIVO", "ACTIVE"),
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF202632))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tr("INACTIVO", "INACTIVE"),
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // GRID DE ESPECIFICACIONES TÉCNICAS
            val specs = listOf(
                Triple(tr("Cilindrada", "Displacement"), "${vehicle.engineDisplacementLiters} L", TextWhite),
                Triple(tr("Depósito", "Tank"), "${vehicle.fuelTankCapacityLiters.toInt()} L", TextWhite),
                Triple(tr("Peso", "Weight"), "${vehicle.vehicleWeightKg.toInt()} kg", TextWhite),
                Triple(tr("Régimen", "Redline"), "${vehicle.maxRpm.toInt()} RPM", TextWhite),
                Triple(tr("Potencia ficha", "Spec power"), "${vehicle.maxPowerHp.toInt()} ${trUnit("CV")}", RacingOrange),
                Triple(tr("Par ficha", "Spec torque"), "${vehicle.maxTorqueNm.toInt()} Nm", RacingOrange),
                Triple(tr("Eficiencia", "Efficiency"), "${vehicle.volumetricEfficiencyPercent.toInt()}% VE", TextWhite),
                Triple(tr("Combustible", "Fuel"), vehicle.fuelType.shortDisplayName, TextWhite)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                specs.chunked(specColumns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (label, value, color) ->
                            SpecBadge(label, value, color, Modifier.weight(1f))
                        }
                    }
                }
            }

            // BOTONES DE ACCIÓN (ACTIVAR / EDITAR / ELIMINAR)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("En uso por la telemetría", "In use by telemetry"), color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Activar este Vehículo", "Activate this Vehicle"), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = RacingOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("Editar", "Edit"), color = RacingOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (canDelete) DangerRed else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(label: String, value: String, color: Color = TextWhite, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1B222E))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FittedValue(text = value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Pestaña 1: Visualizador completo de todos los sensores soportados/disponibles por la ECU.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SensorsListSection(
    allPids: List<ObdPid>,
    vehicleState: VehicleState,
    connectionState: ConnectionState,
    isScanningSensors: Boolean,
    onScanSensors: () -> Unit,
    onTogglePid: (String, Boolean) -> Unit,
    onVisibleSensorsChanged: (Set<String>) -> Unit
) {
    GridMonitorScreen(
        vehicleState = vehicleState,
        availablePids = allPids,
        connectionState = connectionState,
        isScanningSensors = isScanningSensors,
        onScanSensors = onScanSensors,
        onTogglePid = onTogglePid,
        onVisibleSensorsChanged = onVisibleSensorsChanged
    )
}

/**
 * Diálogo modal para añadir un nuevo vehículo o editar los parámetros técnicos de uno existente.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleEditDialog(
    profile: VehicleProfile,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (VehicleProfile, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var year by remember { mutableStateOf(profile.modelYear) }
    var displacement by remember { mutableStateOf(profile.engineDisplacementLiters.toString()) }
    var tankCapacity by remember { mutableStateOf(profile.fuelTankCapacityLiters.toInt().toString()) }
    var weight by remember { mutableStateOf(profile.vehicleWeightKg.toInt().toString()) }
    var maxRpm by remember { mutableStateOf(profile.maxRpm.toInt().toString()) }
    var maxPowerHp by remember { mutableStateOf(profile.maxPowerHp.toInt().toString()) }
    var maxTorqueNm by remember { mutableStateOf(profile.maxTorqueNm.toInt().toString()) }
    var volumetricEfficiency by remember { mutableStateOf(profile.volumetricEfficiencyPercent.toInt().toString()) }
    var selectedFuel by remember { mutableStateOf(profile.fuelType) }
    var initCommands by remember { mutableStateOf(profile.customInitCommands) }
    var makeActive by remember { mutableStateOf(isNew) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ResponsiveFormDialog(
        title = if (isNew) tr("Añadir nuevo vehículo", "Add new vehicle") else tr("Editar: ${profile.name}", "Edit: ${profile.name}"),
        confirmLabel = tr("Guardar vehículo", "Save vehicle"),
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank()) {
                errorMessage = tr("El nombre del vehículo no puede estar vacío.", "Vehicle name cannot be empty.")
                return@ResponsiveFormDialog
            }
            val disp = displacement.toFloatOrNull()
            if (disp == null || disp <= 0) {
                errorMessage = tr("Cilindrada inválida (ej. 2.0).", "Invalid displacement (e.g. 2.0).")
                return@ResponsiveFormDialog
            }
            val tank = tankCapacity.toFloatOrNull() ?: 50f
            val w = weight.toFloatOrNull() ?: 1400f
            val rpm = maxRpm.toFloatOrNull() ?: 5000f
            val hp = maxPowerHp.toFloatOrNull() ?: 150f
            val nm = maxTorqueNm.toFloatOrNull() ?: 350f
            val ve = volumetricEfficiency.toFloatOrNull() ?: 85f

            val updated = profile.copy(
                name = name.trim(),
                modelYear = year.trim().ifBlank { "2020" },
                engineDisplacementLiters = disp,
                fuelTankCapacityLiters = tank,
                vehicleWeightKg = w,
                fuelType = selectedFuel,
                maxRpm = rpm,
                maxPowerHp = hp,
                maxTorqueNm = nm,
                volumetricEfficiencyPercent = ve,
                customInitCommands = initCommands.trim()
            )

            onConfirm(updated, makeActive)
        }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DangerRed.copy(alpha = 0.2f))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = errorMessage ?: "", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // NOMBRE Y AÑO
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text(tr("Nombre del Vehículo (Marca y Modelo)", "Vehicle Name (Make and Model)")) },
                placeholder = { Text(tr("ej. Honda Civic 2.2 i-DTEC", "e.g. Honda Civic 2.2 i-DTEC")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text(tr("Año", "Year")) },
                    placeholder = { Text(tr("ej. 2012", "e.g. 2012")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = displacement,
                    onValueChange = { displacement = it },
                    label = { Text(tr("Cilindrada (L)", "Displacement (L)")) },
                    placeholder = { Text(tr("ej. 2.2", "e.g. 2.2")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // TIPO DE COMBUSTIBLE
            Text(tr("Tipo de Combustible:", "Fuel Type:"), color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FuelType.values().forEach { fuel ->
                    FilterChip(
                        selected = selectedFuel == fuel,
                        onClick = { selectedFuel = fuel },
                        label = { Text(fuel.shortDisplayName, fontSize = 10.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RacingOrange,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // DEPÓSITO Y PESO
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tankCapacity,
                    onValueChange = { tankCapacity = it },
                    label = { Text(tr("Depósito (L)", "Tank (L)")) },
                    placeholder = { Text(tr("ej. 50", "e.g. 50")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(tr("Peso vacío (kg)", "Curb weight (kg)")) },
                    placeholder = { Text(tr("ej. 1450", "e.g. 1450")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // POTENCIA Y PAR MOTOR
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = maxPowerHp,
                    onValueChange = { maxPowerHp = it },
                    label = { Text(tr("Potencia Máx (CV)", "Max Power (HP)")) },
                    placeholder = { Text(tr("ej. 150", "e.g. 150")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxTorqueNm,
                    onValueChange = { maxTorqueNm = it },
                    label = { Text(tr("Par Máx (Nm)", "Max Torque (Nm)")) },
                    placeholder = { Text(tr("ej. 350", "e.g. 350")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // RÉGIMEN MÁX Y EFICIENCIA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = maxRpm,
                    onValueChange = { maxRpm = it },
                    label = { Text("Redline (RPM)") },
                    placeholder = { Text(tr("ej. 5000", "e.g. 5000")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = volumetricEfficiency,
                    onValueChange = { volumetricEfficiency = it },
                    label = { Text(tr("Eficiencia (%)", "Efficiency (%)")) },
                    placeholder = { Text(tr("ej. 85", "e.g. 85")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // COMANDOS INIT OBD2 (OPCIONAL)
            OutlinedTextField(
                value = initCommands,
                onValueChange = { initCommands = it },
                label = { Text(tr("Comandos Init ELM327 (Opcional)", "ELM327 Init Commands (Optional)")) },
                placeholder = { Text(tr("ej. AT SP7\\nAT SH DA10F1", "e.g. AT SP7\\nAT SH DA10F1")) },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // SWITCH PARA ACTIVAR INMEDIATAMENTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Establecer como Vehículo ACTIVO", "Set as ACTIVE Vehicle"),
                    modifier = Modifier.weight(1f),
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = makeActive,
                    onCheckedChange = { makeActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = Color(0xFF0F3B4C)
                    )
                )
            }
        }
    }
}

/**
 * Datos oficiales de contacto de desarrollador y URLs públicas HTTPS (GitHub Pages / Soporte)
 * para Google Play Console y el diálogo de Política de Privacidad.
 */
object AppPrivacyPolicy {
    const val APP_NAME = "StradaOBD"
    const val APPLICATION_ID = "com.stradaobd.dash"
    const val DEVELOPER_NAME = "Alberto Villa (albertovilla5)"
    const val DEVELOPER_GITHUB_URL = "https://github.com/albertovilla5"
    const val DEVELOPER_CONTACT_EMAIL = "22d7rrcwzm@privaterelay.appleid.com"
    const val PRIVACY_POLICY_URL = "https://albertovilla5.github.io/stradaobd/privacy-policy.html"
    const val PUBLIC_PORTAL_URL = "https://albertovilla5.github.io/stradaobd/"
    const val GITHUB_SUPPORT_URL = "https://github.com/albertovilla5/stradaobd/issues"

    fun openExternalUrlOrCopy(context: Context, url: String, label: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            tr("Abriendo $label...", "Opening $label...")
        } catch (_: Exception) {
            copyToClipboard(context, label, url)
            val msg = tr(
                "No se encontró navegador web. Enlace copiado al portapapeles: $url",
                "No web browser found. Link copied to clipboard: $url"
            )
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            msg
        }
    }

    fun openContactEmailOrCopy(context: Context): String {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$DEVELOPER_CONTACT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "$APP_NAME ($APPLICATION_ID) — Soporte / Privacidad")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            tr("Abriendo cliente de correo...", "Opening email client...")
        } catch (_: Exception) {
            copyToClipboard(context, "Email", DEVELOPER_CONTACT_EMAIL)
            val msg = tr(
                "Correo de contacto copiado al portapapeles: $DEVELOPER_CONTACT_EMAIL",
                "Contact email copied to clipboard: $DEVELOPER_CONTACT_EMAIL"
            )
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            msg
        }
    }

    private fun copyToClipboard(context: Context, label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    }
}

/**
 * Diálogo de Política de Privacidad, Seguridad Vial y Contacto (Requisito Google Play Store).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var linkFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = RacingOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    tr("Política de Privacidad y Contacto", "Privacy Policy & Contact"),
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr("1. Uso de Permisos Bluetooth y Ubicación:", "1. Bluetooth and Location Permissions:"),
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "Android requiere permisos de dispositivos cercanos y localización (solo en Android 6–11) para detectar y emparejar adaptadores ELM327 Bluetooth (SPP) y Wi-Fi. En Android 12+ se declara neverForLocation. La aplicación NUNCA rastrea, lee ni almacena tu ubicación geográfica.",
                        "Android requires nearby devices and location permissions (Android 6–11 only) to discover and pair Bluetooth (SPP) and Wi-Fi ELM327 adapters. On Android 12+, neverForLocation is declared. The app NEVER tracks, reads or stores your geographic location."
                    ),
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = tr("2. Almacenamiento, Copias de Seguridad y Telemetría:", "2. Storage, Backups and Vehicle Telemetry:"),
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "Todos los parámetros OBD2 y grabaciones de telemetría se procesan y almacenan en la memoria privada de tu dispositivo sin enviarse a servidores de terceros. La configuración del usuario (perfiles de vehículos, pantallas a medida, sensores, avisos e idioma) puede respaldarse mediante la copia automática cifrada de Android (Google Drive del usuario) o exportarse manualmente en un archivo JSON, excluyendo siempre direcciones MAC Bluetooth, IP Wi-Fi y sesiones CSV.",
                        "All OBD2 parameters and telemetry recordings are processed and stored in your device's private storage and are never sent to third-party servers. User configuration (vehicle profiles, custom dashboards, sensors, alerts and language) may be backed up via Android's encrypted Auto Backup (to the user's Google Drive) or manually exported as a JSON file, always excluding Bluetooth MAC, Wi-Fi IP addresses and CSV recordings."
                    ),
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = tr("3. Sin Anuncios ni Rastreadores:", "3. No Ads or Trackers:"),
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "Esta aplicación no contiene kits de desarrollo de software (SDK) de publicidad ni análisis de comportamiento que recopilen telemetría del usuario, ni utiliza el identificador publicitario de Android (AD_ID).",
                        "This app contains no advertising or behavioral analytics SDKs that collect user telemetry, and does not use the Android Advertising ID (AD_ID)."
                    ),
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = tr("4. Seguridad Vial y Responsabilidad de Uso:", "4. Road Safety and Usage Disclaimer:"),
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "El conductor es el único responsable de mantener la atención en la carretera. No manipules la aplicación ni edites pantallas con el vehículo en movimiento. El borrado de averías (Modo 04, con contacto encendido y motor parado) y el envío de comandos AT/OBD se realizan bajo la exclusiva responsabilidad del usuario.",
                        "The driver is solely responsible for maintaining full attention on the road. Do not manipulate the app or edit dashboards while the vehicle is moving. Clearing fault codes (Mode 04, ignition ON and engine OFF) and sending custom AT/OBD commands are performed at the user's sole responsibility."
                    ),
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = tr("5. Contacto del Desarrollador y URL Pública HTTPS:", "5. Developer Contact & Public HTTPS URL:"),
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, Color(0xFF293341), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tr(
                            "Desarrollador: ${AppPrivacyPolicy.DEVELOPER_NAME}",
                            "Developer: ${AppPrivacyPolicy.DEVELOPER_NAME}"
                        ),
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tr(
                            "Correo: ${AppPrivacyPolicy.DEVELOPER_CONTACT_EMAIL}",
                            "Email: ${AppPrivacyPolicy.DEVELOPER_CONTACT_EMAIL}"
                        ),
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                    Text(
                        text = tr(
                            "Soporte GitHub: ${AppPrivacyPolicy.GITHUB_SUPPORT_URL}",
                            "GitHub Support: ${AppPrivacyPolicy.GITHUB_SUPPORT_URL}"
                        ),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = tr(
                            "Política Web (HTTPS): ${AppPrivacyPolicy.PRIVACY_POLICY_URL}",
                            "Web Policy (HTTPS): ${AppPrivacyPolicy.PRIVACY_POLICY_URL}"
                        ),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            linkFeedback = AppPrivacyPolicy.openExternalUrlOrCopy(
                                context,
                                AppPrivacyPolicy.PRIVACY_POLICY_URL,
                                tr("Política de Privacidad Web", "Web Privacy Policy")
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Política Web (HTTPS)", "Web Policy (HTTPS)"), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            linkFeedback = AppPrivacyPolicy.openExternalUrlOrCopy(
                                context,
                                AppPrivacyPolicy.GITHUB_SUPPORT_URL,
                                tr("Soporte en GitHub", "GitHub Support")
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp), tint = RacingOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Soporte GitHub", "GitHub Support"), color = RacingOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            linkFeedback = AppPrivacyPolicy.openContactEmailOrCopy(context)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Contacto Email", "Email Contact"), color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (linkFeedback != null) {
                    Text(
                        text = linkFeedback ?: "",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cerrar", "Close"), color = RacingOrange, fontWeight = FontWeight.Bold)
            }
        }
    )
}
