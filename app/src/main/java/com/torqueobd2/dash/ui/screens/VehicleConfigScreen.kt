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
import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
                RacingHeader("Tu garaje", "Vehículos, calibración y sensores disponibles.", modifier = Modifier.padding(16.dp), eyebrow = "VEHÍCULO / CONFIGURACIÓN")
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
                                    text = "Garaje · ${savedVehicles.size}",
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
                                    text = "Sensores · $supportedCount/${allPids.size}",
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
                        Icon(Icons.Default.Add, contentDescription = if (iconOnlyAdd) "Añadir Vehículo" else null,
                            modifier = Modifier.size(18.dp), tint = Color.Black)
                        if (!iconOnlyAdd) {
                            Spacer(Modifier.width(4.dp))
                            Text("Añadir Vehículo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextMuted, modifier = Modifier.size(14.dp))
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
                            successNotification = "Vehículo activo actualizado."
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
                successNotification = "Vehículo '${updatedVehicle.name}' guardado correctamente."
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
                    text = "Eliminar Vehículo",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas eliminar permanentemente '${target.name}' del garaje?",
                        color = TextWhite,
                        fontSize = 13.sp
                    )
                    if (target.id == activeProfile.id) {
                        Text(
                            text = "Aviso: Este es el vehículo activo actualmente. Al eliminarlo, se activará automáticamente otro vehículo de tu lista.",
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
                        successNotification = "Vehículo '${target.name}' eliminado."
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("Cancelar", color = TextMuted)
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
                            text = "GARAJE DE VEHÍCULOS",
                            color = RacingOrange,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "El vehículo activo define la conexión y la escala de RPM. Su ficha no genera lecturas.",
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
                        Text("Añadir Vehículo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    "El vehículo activo define la conexión y la escala de RPM. Su ficha no genera lecturas.",
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
                            text = "Privacidad y telemetría 100% local en tu dispositivo.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = onOpenPrivacyDialog) {
                        Text("Ver Política", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            text = vehicle.name.ifBlank { "Vehículo Sin Nombre" },
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Año ${vehicle.modelYear} • ${vehicle.fuelType.displayName}",
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
                                text = "ACTIVO",
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
                            text = "INACTIVO",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // GRID DE ESPECIFICACIONES TÉCNICAS
            val specs = listOf(
                Triple("Cilindrada", "${vehicle.engineDisplacementLiters} L", TextWhite),
                Triple("Depósito", "${vehicle.fuelTankCapacityLiters.toInt()} L", TextWhite),
                Triple("Peso", "${vehicle.vehicleWeightKg.toInt()} kg", TextWhite),
                Triple("Régimen", "${vehicle.maxRpm.toInt()} RPM", TextWhite),
                Triple("Potencia ficha", "${vehicle.maxPowerHp.toInt()} CV", RacingOrange),
                Triple("Par ficha", "${vehicle.maxTorqueNm.toInt()} Nm", RacingOrange),
                Triple("Eficiencia", "${vehicle.volumetricEfficiencyPercent.toInt()}% VE", TextWhite),
                Triple("Combustible", vehicle.fuelType.displayName.split(" ").first(), TextWhite)
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
                        Text("En uso por la telemetría", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Activar este Vehículo", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = RacingOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = RacingOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        title = if (isNew) "Añadir nuevo vehículo" else "Editar: ${profile.name}",
        confirmLabel = "Guardar vehículo",
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank()) {
                errorMessage = "El nombre del vehículo no puede estar vacío."
                return@ResponsiveFormDialog
            }
            val disp = displacement.toFloatOrNull()
            if (disp == null || disp <= 0) {
                errorMessage = "Cilindrada inválida (ej. 2.0)."
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
                label = { Text("Nombre del Vehículo (Marca y Modelo)") },
                placeholder = { Text("ej. Honda Civic 2.2 i-DTEC") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Año") },
                    placeholder = { Text("ej. 2012") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = displacement,
                    onValueChange = { displacement = it },
                    label = { Text("Cilindrada (L)") },
                    placeholder = { Text("ej. 2.2") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // TIPO DE COMBUSTIBLE
            Text("Tipo de Combustible:", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FuelType.values().forEach { fuel ->
                    FilterChip(
                        selected = selectedFuel == fuel,
                        onClick = { selectedFuel = fuel },
                        label = { Text(fuel.displayName.split(" ").first(), fontSize = 10.sp, maxLines = 1) },
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
                    label = { Text("Depósito (L)") },
                    placeholder = { Text("ej. 50") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso vacío (kg)") },
                    placeholder = { Text("ej. 1450") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // POTENCIA Y PAR MOTOR
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = maxPowerHp,
                    onValueChange = { maxPowerHp = it },
                    label = { Text("Potencia Máx (CV)") },
                    placeholder = { Text("ej. 150") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxTorqueNm,
                    onValueChange = { maxTorqueNm = it },
                    label = { Text("Par Máx (Nm)") },
                    placeholder = { Text("ej. 350") },
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
                    placeholder = { Text("ej. 5000") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = volumetricEfficiency,
                    onValueChange = { volumetricEfficiency = it },
                    label = { Text("Eficiencia (%)") },
                    placeholder = { Text("ej. 85") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // COMANDOS INIT OBD2 (OPCIONAL)
            OutlinedTextField(
                value = initCommands,
                onValueChange = { initCommands = it },
                label = { Text("Comandos Init ELM327 (Opcional)") },
                placeholder = { Text("ej. AT SP7\\nAT SH DA10F1") },
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
                    text = "Establecer como Vehículo ACTIVO",
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
 * Diálogo de Política de Privacidad y Permisos (Requisito Google Play Store).
 */
@Composable
internal fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = RacingOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Política de Privacidad y Permisos", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    text = "1. Uso de Permisos Bluetooth y Ubicación:",
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Android requiere permisos de dispositivos cercanos y localización para detectar y emparejar adaptadores ELM327 Bluetooth (SPP) y Wi-Fi. La aplicación NUNCA rastrea ni almacena tu ubicación geográfica.",
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = "2. Almacenamiento y Telemetría Vehicular:",
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Todos los parámetros OBD2 (velocidad, RPM, presión DPF, temperaturas) y perfiles del vehículo se procesan y almacenan únicamente en la memoria local privada de tu teléfono. No se envían a ningún servidor en la nube ni se comparten con terceros.",
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Text(
                    text = "3. Sin Anuncios ni Rastreadores:",
                    color = RacingOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Esta aplicación no contiene kits de desarrollo de software (SDK) de publicidad ni análisis de comportamiento que recopilen telemetría del usuario.",
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = RacingOrange, fontWeight = FontWeight.Bold)
            }
        }
    )
}
