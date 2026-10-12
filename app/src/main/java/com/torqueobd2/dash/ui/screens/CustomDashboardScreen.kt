package com.torqueobd2.dash.ui.screens

import com.torqueobd2.dash.ui.automotive.uiLayoutScale
import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarIcon as Icon
import com.torqueobd2.dash.ui.automotive.CarButton as Button
import com.torqueobd2.dash.ui.automotive.CarOutlinedButton as OutlinedButton
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import com.torqueobd2.dash.ui.automotive.CarIconButton as IconButton
import com.torqueobd2.dash.ui.automotive.CarFilterChip as FilterChip
import com.torqueobd2.dash.ui.automotive.LocalCarMode
import com.torqueobd2.dash.ui.automotive.CarMenu
import com.torqueobd2.dash.ui.automotive.CarInstrument
import com.torqueobd2.dash.ui.automotive.CarMenuEntry
import androidx.activity.compose.BackHandler
import com.torqueobd2.dash.ui.components.DashboardLayoutPolicy
import com.torqueobd2.dash.ui.components.racingPanel
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.components.FittedValue

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.torqueobd2.dash.ui.automotive.CarDropdownMenuItem as DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.torqueobd2.dash.ui.theme.WarningAmber
import com.torqueobd2.dash.ui.theme.CockpitBackground
import com.torqueobd2.dash.ui.theme.CockpitMuted
import com.torqueobd2.dash.ui.theme.CockpitAccent
import com.torqueobd2.dash.ui.theme.CockpitElevated
import com.torqueobd2.dash.ui.theme.CockpitSurface
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.SensorTelemetryStatus
import com.torqueobd2.dash.data.model.resolvePid
import com.torqueobd2.dash.data.model.currentReading
import com.torqueobd2.dash.data.model.telemetryStatus
import com.torqueobd2.dash.data.model.CustomDashboardTabs
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardGridLayout
import com.torqueobd2.dash.data.model.DashboardPreset
import com.torqueobd2.dash.data.model.DashboardPresets
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory
import com.torqueobd2.dash.data.model.VehicleState
import com.torqueobd2.dash.data.model.telemetryText
import com.torqueobd2.dash.data.model.DashboardDensity
import com.torqueobd2.dash.data.model.WidgetType
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.i18n.trUnit
import com.torqueobd2.dash.ui.components.AnalogGauge
import com.torqueobd2.dash.ui.components.ControlPedalSlider
import com.torqueobd2.dash.ui.components.RealtimeChart
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.DarkSurface
import com.torqueobd2.dash.ui.theme.BorderDark
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Pantalla de Dashboard totalmente a medida y adaptada a la pantalla:
 * - La rejilla del coche conserva las columnas y filas elegidas; el móvil mantiene su scroll.
 * - Diálogo de añadir widgets táctil y robusto con distribución horizontal adaptada a pantallas de coche.
 * - Selección intuitiva de sensores con filtrado por categorías y buscador instantáneo.
 * - Soporte para plantillas (presets) y vaciado rápido.
 */
@Composable
fun CustomDashboardScreen(
    connectionState: ConnectionState,
    storedDashboards: CustomDashboardTabs,
    availablePids: List<ObdPid>,
    vehicleState: VehicleState,
    isSimulationMode: Boolean,
    onSelectTab: (String) -> Unit,
    onCreateTab: (String, DashboardGridLayout) -> Unit,
    onRenameTab: (String, String) -> Unit,
    onMoveTab: (String, Int) -> Unit,
    onResetDefault: () -> Unit,
    onApplyPreset: (DashboardPreset) -> Unit,
    onClearDashboard: () -> Unit,
    onSelectGridLayout: (DashboardGridLayout) -> Unit,
    onSetWidgetInSlot: (Int, DashboardWidget?) -> Unit,
    onClearSlot: (Int) -> Unit,
    onThrottleChanged: (Float?) -> Unit,
    onDuplicateTab: (String) -> Unit,
    onDeleteTab: (String) -> Unit,
    onTransferWidget: (Int, Int, Boolean) -> Unit,
    onDensityChanged: (DashboardDensity) -> Unit,
    onImportDesigns: (List<CustomDashboardConfig>) -> Unit,
    onAddSensors: (List<String>) -> Unit,
    fullScreen: Boolean,
    onFullScreenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onEditModeChanged: (Boolean) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val car = LocalCarMode.current
    val customDashboards = if (car) storedDashboards.copy(dashboards = storedDashboards.dashboards.map { it.forCar() }) else storedDashboards
    val customDashboard = customDashboards.selectedDashboard
    var showCarTools by rememberSaveable { mutableStateOf(false) }
    var showCarPanels by rememberSaveable { mutableStateOf(false) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showCreateTab by remember { mutableStateOf(false) }
    var renameTabId by remember { mutableStateOf<String?>(null) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var targetSlotForAdd by remember { mutableStateOf<Int?>(null) }
    val pidsList = availablePids.ifEmpty { DefaultPids.getStandardPids() + DefaultPids.getHondaIdtecPids() }
    val fontScale = uiLayoutScale()
    val latestOnSelectTab by rememberUpdatedState(onSelectTab)
    var selectedSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var transferSource by remember { mutableStateOf<Int?>(null) }
    var duplicateWidget by remember { mutableStateOf(false) }
    var deleteTabId by remember { mutableStateOf<String?>(null) }
    var showDesignFiles by remember { mutableStateOf(false) }
    var showBulkSensors by remember { mutableStateOf(false) }
    val isAnyModalOrEditing = isEditMode || showCarPanels || showAddDialog || showPresetsDialog ||
        showGridDialog || showCreateTab || renameTabId != null || showOrderDialog ||
        deleteTabId != null || showDesignFiles || showBulkSensors || showCarTools
    val currentOnEditModeChanged by rememberUpdatedState(onEditModeChanged)
    LaunchedEffect(isAnyModalOrEditing) {
        currentOnEditModeChanged(isAnyModalOrEditing)
    }
    LaunchedEffect(customDashboard.id, customDashboard.gridLayout) {
        showAddDialog = false; targetSlotForAdd = null; transferSource = null; selectedSlot = null
    }
    val finishEditing = { isEditMode = false; showCarTools = false; selectedSlot = null }
    BackHandler(enabled = isEditMode) { finishEditing() }

    val editorActions = buildList {
        selectedSlot?.let { slot ->
            val widget = customDashboard.getWidgetForSlot(slot)
            if (widget == null) {
                add(DashboardEditorAction(Icons.Default.Add, tr("Añadir aquí", "Add here"), tr("Ranura ${slot + 1}", "Slot ${slot + 1}")) {
                    targetSlotForAdd = slot; showAddDialog = true
                })
            } else {
                add(DashboardEditorAction(Icons.Default.Edit, tr("Editar indicador", "Edit gauge")) {
                    targetSlotForAdd = slot; showAddDialog = true
                })
                add(DashboardEditorAction(Icons.Default.SwapVert, tr("Mover / intercambiar", "Move / swap")) {
                    transferSource = slot; duplicateWidget = false
                })
                add(DashboardEditorAction(Icons.Default.Add, tr("Duplicar indicador", "Duplicate gauge")) {
                    transferSource = slot; duplicateWidget = true
                })
                add(DashboardEditorAction(Icons.Default.Delete, tr("Vaciar ranura", "Clear slot")) {
                    onClearSlot(slot); selectedSlot = null
                })
            }
        }
        add(DashboardEditorAction(Icons.Default.Add, tr("Añadir indicador", "Add gauge")) {
            targetSlotForAdd = (0 until customDashboard.gridLayout.slotCount).firstOrNull {
                customDashboard.getWidgetForSlot(it) == null
            }
            selectedSlot = targetSlotForAdd
            if (targetSlotForAdd == null) showGridDialog = true else showAddDialog = true
        })
        add(DashboardEditorAction(Icons.Default.Add, tr("Añadir varios indicadores", "Add multiple gauges")) {
            if (customDashboard.getSlotList().all { it != null }) showGridDialog = true else showBulkSensors = true
        })
        add(DashboardEditorAction(Icons.Default.DashboardCustomize, tr("Disposición de rejilla", "Grid layout"), (if (car) customDashboard.gridLayout.carBadge else customDashboard.gridLayout.badge)) {
            showGridDialog = true
        })
        if (!car) add(DashboardEditorAction(Icons.Default.Tune,
            if (customDashboard.density == DashboardDensity.COMPACT) tr("Presentación normal", "Normal density") else tr("Presentación compacta", "Compact density")) {
            onDensityChanged(if (customDashboard.density == DashboardDensity.COMPACT) DashboardDensity.COMFORTABLE else DashboardDensity.COMPACT)
        })
        add(DashboardEditorAction(Icons.Default.Style, tr("Plantillas", "Templates")) { showPresetsDialog = true })
        add(DashboardEditorAction(Icons.Default.Add, tr("Nueva pestaña", "New tab")) { showCreateTab = true })
        add(DashboardEditorAction(Icons.Default.DriveFileRenameOutline, tr("Renombrar pestaña", "Rename tab")) { renameTabId = customDashboard.id })
        add(DashboardEditorAction(Icons.Default.SwapVert, tr("Ordenar pestañas", "Reorder tabs")) { showOrderDialog = true })
        add(DashboardEditorAction(Icons.Default.Add, tr("Duplicar pestaña", "Duplicate tab")) { onDuplicateTab(customDashboard.id) })
        add(DashboardEditorAction(Icons.Default.Delete, tr("Eliminar pestaña", "Delete tab"),
            detail = if (customDashboards.dashboards.size == 1) tr("Conserva al menos una", "Keep at least one") else null,
            enabled = customDashboards.dashboards.size > 1) { deleteTabId = customDashboard.id })
        add(DashboardEditorAction(Icons.Default.Style, tr("Importar / exportar diseños", "Import / export designs")) { showDesignFiles = true })
        add(DashboardEditorAction(if (fullScreen) Icons.Default.Close else Icons.Default.OpenInFull,
            if (fullScreen) tr("Salir de pantalla completa", "Exit full screen") else tr("Pantalla completa", "Full screen")) {
            finishEditing(); onFullScreenChanged(!fullScreen)
        })
    }

    BoxWithConstraints(modifier.fillMaxSize().background(if (car) CockpitBackground else CarbonBlack).padding(if (car) 16.dp else 8.dp)) {
        val sidePanel = maxWidth > maxHeight
        val panelWidth = minOf(260.dp * fontScale, maxWidth * 0.44f)
        val panelHeight = minOf(240.dp * fontScale, maxHeight * 0.52f)
        val panelColumns = if (sidePanel || maxWidth < 320.dp * fontScale) 1 else 2
        Column(Modifier.fillMaxSize()) {
            if (car && fullScreen && isSimulationMode) Text(tr("SIMULACIÓN · Datos de prueba", "SIMULATION · Test data"),
                color = NeonCyan, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
            // Las pestañas solo necesitan una fila cuando hay varios cuadros.
            key(customDashboards.dashboards.map { it.id }) {
                val pagerState = rememberPagerState(initialPage = customDashboards.selectedIndex,
                    pageCount = { customDashboards.dashboards.size })
                val scope = rememberCoroutineScope()
                val tabScroll = androidx.compose.foundation.lazy.rememberLazyListState()
                val displayedPage = pagerState.currentPage
                val showTabs = !car && !fullScreen && customDashboards.dashboards.size > 1
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { index ->
                        latestOnSelectTab(customDashboards.dashboards[index].id)
                    }
                }
                LaunchedEffect(displayedPage, showTabs) {
                    if (showTabs) tabScroll.animateScrollToItem(displayedPage)
                }
                if (showTabs) {
                    LazyRow(Modifier.fillMaxWidth().padding(bottom = 6.dp), state = tabScroll,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(customDashboards.dashboards, key = { it.id }) { dashboard ->
                            val active = customDashboards.dashboards[displayedPage].id == dashboard.id
                            Box(Modifier.heightIn(min = if (car) 76.dp else 40.dp).widthIn(max = 160.dp * fontScale)
                                .clip(RoundedCornerShape(if (car) 24.dp else 8.dp))
                                .background(if (car) { if (active) CockpitElevated else Color.Transparent } else if (active) RacingOrange.copy(alpha = 0.14f) else DarkSurface)
                                .then(if (car) Modifier else Modifier.border(1.dp, if (active) RacingOrange else BorderDark, RoundedCornerShape(8.dp)))
                                .selectable(selected = active, role = Role.Tab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch { pagerState.animateScrollToPage(customDashboards.dashboards.indexOf(dashboard)) }
                                }.padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(dashboard.localizedName, color = if (active) (if (car) CockpitAccent else RacingOrange) else TextMuted, fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                @Composable
                fun DashboardCanvas(canvasModifier: Modifier) {
                    val editDesc = tr("Editar dashboard", "Edit dashboard")
                    Box(canvasModifier) {
                        HorizontalPager(state = pagerState, key = { customDashboards.dashboards[it].id },
                            modifier = Modifier.fillMaxSize(), pageSpacing = 8.dp) { page ->
                            val config = customDashboards.dashboards[page]
                            DashboardGridViewport(config, isEditMode, { isEditMode = true },
                                if (customDashboards.dashboards.size > 1) ({ showCarPanels = true }) else null,
                                isFullScreen = fullScreen,
                                onToggleFullScreen = { finishEditing(); onFullScreenChanged(!fullScreen) },
                                Modifier.fillMaxSize()) { indices, columns, split ->
                                CustomDashboardGridContainer(config, pidsList, vehicleState, connectionState, isEditMode,
                                    selectedSlot = selectedSlot.takeIf { config.id == customDashboard.id },
                                    visibleSlots = indices, carColumns = columns, carSplit = split,
                                    onTapSlot = { slot ->
                                        if (isEditMode && !pagerState.isScrollInProgress && page == pagerState.settledPage) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedSlot = slot
                                            if (car) showCarTools = true
                                        }
                                    })
                            }
                        }
                        if (!isEditMode && !car && !fullScreen) {
                            Surface(onClick = { isEditMode = true },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(if (car) 76.dp else 36.dp)
                                    .semantics { contentDescription = editDesc },
                                color = DarkSurface.copy(alpha = 0.9f), shape = CircleShape,
                                border = BorderStroke(1.dp, RacingOrange.copy(alpha = 0.65f)), shadowElevation = 2.dp) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, null, tint = RacingOrange, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (fullScreen && isSimulationMode) {
                                Text(tr("SIMULACIÓN", "SIMULATION"), Modifier.align(Alignment.TopStart)
                                    .background(DarkSurface.copy(alpha = 0.9f), RoundedCornerShape(6.dp)).padding(6.dp),
                                    color = NeonCyan, fontSize = 10.sp)
                            }
                        }
                    }
                }
                if (car && showCarPanels) ResponsiveDialog(onDismissRequest = { showCarPanels = false }) {
                    val dialogHeight = maxHeight
                    Surface(Modifier.fillMaxWidth().height(dialogHeight), color = CockpitBackground, shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("Cambiar panel", "Switch panel"), Modifier.weight(1f), color = TextWhite, fontSize = 28.sp)
                                IconButton(onClick = { showCarPanels = false }) { Icon(Icons.Default.Close, tr("Cerrar selector de paneles", "Close panel selector")) }
                            }
                            CarMenu(customDashboards.dashboards.mapIndexed { index, dashboard ->
                                CarMenuEntry(dashboard.localizedName, tr("${dashboard.gridLayout.slotCount} indicadores", "${dashboard.gridLayout.slotCount} gauges"), Icons.Default.DashboardCustomize) {
                                    showCarPanels = false
                                    finishEditing()
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            }, Modifier.weight(1f))
                        }
                    }
                }
                @Composable
                fun EditorPanel(panelModifier: Modifier) {
                    DashboardEditorPanel(customDashboard.localizedName, selectedSlot, selectedSlot?.let(customDashboard::getWidgetForSlot)?.localizedTitle,
                        editorActions, columns = panelColumns,
                        onClearSelection = { selectedSlot = null }, onDone = finishEditing, modifier = panelModifier)
                }
                if (!isEditMode) DashboardCanvas(Modifier.weight(1f).fillMaxWidth())
                else if (car) {
                    DashboardCanvas(Modifier.weight(1f).fillMaxWidth())
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        OutlinedButton(onClick = { showCarTools = true }, modifier = Modifier.weight(1f)) { Text(tr("Herramientas", "Tools")) }
                        Button(onClick = finishEditing, modifier = Modifier.weight(1f)) { Text(tr("Listo", "Done")) }
                    }
                } else if (sidePanel) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardCanvas(Modifier.weight(1f).fillMaxHeight())
                        EditorPanel(Modifier.width(panelWidth).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardCanvas(Modifier.weight(1f).fillMaxWidth())
                        EditorPanel(Modifier.fillMaxWidth().height(panelHeight))
                    }
                }
            }
            if (isSimulationMode && !fullScreen && !isEditMode && !car) {
                Spacer(Modifier.height(4.dp))
                ControlPedalSlider(onThrottleChanged = onThrottleChanged)
            }
        }
    }
    if (car && showCarTools && isEditMode) {
        ResponsiveDialog(onDismissRequest = { showCarTools = false }) {
            val compactToolsHeader = maxWidth < 600.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            androidx.compose.material3.Surface(Modifier.fillMaxWidth().height(maxHeight), color = DarkSurface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (selectedSlot == null) (if (compactToolsHeader) tr("Herramientas", "Tools") else tr("Herramientas del panel", "Panel tools")) else tr("Indicador ${selectedSlot!! + 1}", "Gauge ${selectedSlot!! + 1}"),
                            Modifier.weight(1f), color = TextWhite, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (compactToolsHeader) IconButton(onClick = { showCarTools = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver al panel", "Back to panel"), tint = TextWhite)
                        } else TextButton(onClick = { showCarTools = false }) { Text(tr("Volver al panel", "Back to panel")) }
                    }
                    CarMenu(editorActions.map { action ->
                        CarMenuEntry(action.label, action.detail.orEmpty(), action.icon, enabled = action.enabled) {
                            if (action.enabled) { showCarTools = false; action.onClick() }
                        }
                    }, Modifier.weight(1f))
                }
            }
        }
    }
    if (showCreateTab) DashboardTabNameDialog(tr("Nueva pestaña", "New tab"), "", chooseLayout = true,
        onDismiss = { showCreateTab = false }, onSave = { name, layout ->
            onCreateTab(name, layout)
            showCreateTab = false
        })
    renameTabId?.let { id ->
        val dashboard = customDashboards.dashboards.first { it.id == id }
        DashboardTabNameDialog(tr("Nombre de la pestaña", "Tab name"), dashboard.localizedName, onDismiss = { renameTabId = null },
            onSave = { name, _ -> onRenameTab(id, name); renameTabId = null })
    }
    if (showOrderDialog) DashboardTabOrderDialog(customDashboards, onMoveTab, onRename = { id ->
        showOrderDialog = false
        renameTabId = id
    }, onDismiss = { showOrderDialog = false })
    if (showGridDialog) SelectGridLayoutDialog(customDashboard.gridLayout,
        onDismiss = { showGridDialog = false }, onSelectLayout = { onSelectGridLayout(it); showGridDialog = false })
    if (showAddDialog) AddWidgetDialog(pidsList.filter { !it.supportChecked || it.isSupported || it.id == targetSlotForAdd?.let { slot -> customDashboard.getWidgetForSlot(slot)?.pidId } }
        .sortedByDescending { it.supportChecked && it.isSupported }, initialWidget = targetSlotForAdd?.let(customDashboard::getWidgetForSlot), onDismiss = { showAddDialog = false; targetSlotForAdd = null },
        onAdd = { widget ->
            targetSlotForAdd?.let { onSetWidgetInSlot(it, widget) }
            showAddDialog = false
            targetSlotForAdd = null
        })
    transferSource?.let { source -> DashboardTransferDialog(customDashboard, source, duplicateWidget,
        onDismiss = { transferSource = null }, onSelect = { destination ->
            onTransferWidget(source, destination, duplicateWidget); transferSource = null
        }) }
    deleteTabId?.let { id -> DashboardDeleteDialog(customDashboards.dashboards.firstOrNull { it.id == id }?.localizedName.orEmpty(),
        onDismiss = { deleteTabId = null }, onDelete = { onDeleteTab(id); deleteTabId = null }) }
    if (showBulkSensors) DashboardBulkSensorsDialog(customDashboard, pidsList, onDismiss = { showBulkSensors = false }, onAdd = {
        onAddSensors(it); showBulkSensors = false
    })
    if (showDesignFiles) DashboardDesignFilesDialog(storedDashboards, pidsList, onImportDesigns, onDismiss = { showDesignFiles = false })
    if (showPresetsDialog) PresetsDialog(pidsList, onDismiss = { showPresetsDialog = false },
        onSelectPreset = { onApplyPreset(it); showPresetsDialog = false },
        onResetDefault = { onResetDefault(); showPresetsDialog = false },
        onClearDashboard = { onClearDashboard(); showPresetsDialog = false })
}

private data class DashboardEditorAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val detail: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/** Todas las herramientas comparten un panel que solo ocupa espacio mientras se edita. */
@Composable
private fun DashboardEditorPanel(name: String, selectedSlot: Int?, selectedTitle: String?,
    actions: List<DashboardEditorAction>, columns: Int, onClearSelection: () -> Unit,
    onDone: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberLazyGridState()
    val toolsDesc = tr("Herramientas del dashboard", "Dashboard tools")
    val finishDesc = tr("Terminar edición", "Finish editing")
    LaunchedEffect(selectedSlot) { scroll.scrollToItem(0) }
    Surface(modifier.semantics { contentDescription = toolsDesc }, color = DarkSurface,
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BorderDark)) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Modo edición", "Edit mode"), color = RacingOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val slotLabel = selectedTitle ?: tr("Ranura vacía", "Empty slot")
                    Text(if (selectedSlot == null) name else tr("Indicador ${selectedSlot + 1} · $slotLabel", "Gauge ${selectedSlot + 1} · $slotLabel"),
                        color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (selectedSlot != null) IconButton(onClearSelection, Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, tr("Ver opciones del dash", "View dashboard options"), tint = TextMuted, modifier = Modifier.size(22.dp))
                }
                Button(onDone, Modifier.heightIn(min = 48.dp).semantics { contentDescription = finishDesc },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)) {
                    Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Listo", "Done"), fontSize = 13.sp)
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(columns), state = scroll, modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedSlot == null) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(tr("Toca un indicador para editarlo", "Tap a gauge to edit it"), color = TextMuted, fontSize = 12.sp)
                }
                items(actions, key = { it.label }) { action ->
                    Row(Modifier.fillMaxWidth().heightIn(min = if (LocalCarMode.current) 76.dp else 56.dp).clip(RoundedCornerShape(8.dp))
                        .background(CardBackground).border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(action.icon, null, tint = if (action.enabled) RacingOrange else TextMuted,
                            modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(action.label, color = if (action.enabled) TextWhite else TextMuted, fontSize = 13.sp)
                            action.detail?.let { Text(it, color = TextMuted, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

/** La rejilla completa del coche ocupa el visor, sin reordenación ni paginación. */
@Composable
private fun DashboardGridViewport(config: CustomDashboardConfig, editing: Boolean, onEdit: () -> Unit,
    onChoosePanel: (() -> Unit)?, isFullScreen: Boolean, onToggleFullScreen: (() -> Unit)?,
    modifier: Modifier = Modifier, content: @Composable (List<Int>?, Int?, Boolean) -> Unit) {
    val car = LocalCarMode.current
    val scroll = rememberScrollState()
    val gaugesDesc = tr("Indicadores del dashboard", "Dashboard gauges")
    val switchPanelDesc = tr("Cambiar panel", "Switch panel")
    val fullScreenDesc = if (isFullScreen) tr("Salir de pantalla completa", "Exit full screen") else tr("Pantalla completa", "Full screen")
    val editDesc = tr("Editar dashboard", "Edit dashboard")
    BoxWithConstraints(modifier) {
        if (car) {
            val scale = LocalDensity.current.fontScale
            val controlsHeight = if (editing || isFullScreen) 0.dp else maxOf(40.dp, 36.dp * scale)
            val controlsSpacing = if (editing || isFullScreen) 0.dp else 6.dp
            val grid = DashboardLayoutPolicy.carGrid(config.gridLayout, maxWidth.value,
                (maxHeight - if (editing || isFullScreen) 0.dp else controlsHeight + controlsSpacing).value)
            Column(Modifier.fillMaxSize().semantics { contentDescription = gaugesDesc }) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content(grid.slots, grid.columns, grid.split) }
                if (!editing && !isFullScreen) Row(
                    Modifier.fillMaxWidth().padding(top = controlsSpacing).height(controlsHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { onChoosePanel?.invoke() },
                            enabled = onChoosePanel != null,
                            modifier = Modifier
                                .height(controlsHeight)
                                .then(if (onChoosePanel != null) Modifier.semantics { contentDescription = switchPanelDesc } else Modifier),
                            color = CockpitSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (onChoosePanel != null) CockpitElevated else BorderDark.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.DashboardCustomize,
                                    contentDescription = null,
                                    tint = CockpitAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                androidx.compose.material3.Text(
                                    text = config.localizedName,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (onChoosePanel != null) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = CockpitAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (onToggleFullScreen != null) {
                            Surface(
                                onClick = onToggleFullScreen,
                                modifier = Modifier
                                    .size(controlsHeight)
                                    .semantics {
                                        contentDescription = fullScreenDesc
                                    },
                                color = if (isFullScreen) CockpitElevated else CockpitSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isFullScreen) CockpitAccent else CockpitElevated)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(
                                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = null,
                                        tint = if (isFullScreen) CockpitAccent else TextWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(controlsHeight)
                            .semantics { contentDescription = editDesc },
                        color = CockpitSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CockpitAccent.copy(alpha = 0.35f))
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = CockpitAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            val viewportHeight = maxHeight
            val minimumHeight = DashboardLayoutPolicy.measure(config.gridLayout, maxWidth.value, LocalDensity.current.fontScale, config.density).minimumHeight.dp
            val needsScroll = minimumHeight > maxHeight
            Box(Modifier.fillMaxSize().semantics { contentDescription = gaugesDesc }
                .verticalScroll(scroll, enabled = needsScroll)) {
                Box(Modifier.fillMaxWidth().height(maxOf(viewportHeight, minimumHeight))) { content(null, null, false) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardTabNameDialog(title: String, initialName: String, chooseLayout: Boolean = false,
    onDismiss: () -> Unit, onSave: (String, DashboardGridLayout) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var layout by remember { mutableStateOf(DashboardGridLayout.GRID_1X4) }
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val nameRequester = remember { BringIntoViewRequester() }
    var nameFocused by remember { mutableStateOf(false) }
    LaunchedEffect(keyboardVisible, nameFocused) {
        if (keyboardVisible && nameFocused) {
            kotlinx.coroutines.delay(200)
            nameRequester.bringIntoView()
        }
    }
    ResponsiveDialog(onDismissRequest = onDismiss) {
        val short = maxHeight < 240.dp * density.fontScale
        val hideHeading = keyboardVisible && maxHeight < 180.dp * density.fontScale
        Surface(Modifier.fillMaxWidth().heightIn(max = maxHeight), color = CardBackground,
            shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
            Column(Modifier.padding(if (short) 6.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(if (short) 4.dp else 8.dp)) {
                if (!hideHeading) Text(title, color = TextWhite, fontSize = if (short) 14.sp else 18.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, onValueChange = { name = it.take(40) }, label = { Text(tr("Nombre", "Name")) },
                        singleLine = true, modifier = Modifier.fillMaxWidth().bringIntoViewRequester(nameRequester)
                            .onFocusChanged { nameFocused = it.isFocused })
                    if (chooseLayout) {
                        Text(tr("Disposición de rejilla", "Grid layout"), color = RacingOrange, fontSize = 12.sp)
                        (if (LocalCarMode.current) DashboardGridLayout.carLayouts else DashboardGridLayout.entries).forEach { option ->
                            Row(Modifier.fillMaxWidth().heightIn(min = if (LocalCarMode.current) 76.dp else 56.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (layout == option) RacingOrange.copy(alpha = 0.14f) else DarkSurface)
                                .border(1.dp, if (layout == option) RacingOrange else BorderDark, RoundedCornerShape(8.dp))
                                .semantics { selected = layout == option }.clickable { layout = option }
                                .padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (LocalCarMode.current) option.carBadge else option.badge, color = RacingOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(if (LocalCarMode.current) 92.dp else 56.dp))
                                Text(if (LocalCarMode.current) option.carTitle else option.title, color = TextWhite, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (layout == option) Icon(Icons.Default.Check, null, tint = RacingOrange, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss, modifier = Modifier.heightIn(min = if (short) 48.dp else 56.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(tr("Cancelar", "Cancel"), fontSize = 12.sp) }
                    Button(onClick = { onSave(name.trim(), layout) }, enabled = name.isNotBlank(),
                        modifier = Modifier.heightIn(min = if (short) 48.dp else 56.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RacingOrange)) {
                        Text(if (chooseLayout) tr("Crear", "Create") else tr("Guardar", "Save"), color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTabOrderDialog(tabs: CustomDashboardTabs, onMove: (String, Int) -> Unit,
    onRename: (String) -> Unit, onDismiss: () -> Unit) {
    ResponsiveDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().heightIn(max = minOf(maxHeight, 480.dp)), color = CardBackground,
            shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderDark)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Ordenar pestañas", "Reorder tabs"), color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(tr("Usa las flechas para cambiar el orden.", "Use the arrows to change the order."), color = TextMuted, fontSize = 12.sp)
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tabs.dashboards, key = { it.id }) { dashboard ->
                        val index = tabs.dashboards.indexOf(dashboard)
                        Row(Modifier.fillMaxWidth().racingPanel(8.dp).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", color = RacingOrange, fontSize = 12.sp)
                            Text(dashboard.localizedName, Modifier.weight(1f).padding(horizontal = 8.dp), color = TextWhite,
                                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onRename(dashboard.id) }) {
                                Icon(Icons.Default.Edit, tr("Renombrar ${dashboard.localizedName}", "Rename ${dashboard.localizedName}"), tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMove(dashboard.id, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Default.ArrowUpward, tr("Mover ${dashboard.localizedName} antes", "Move ${dashboard.localizedName} up"), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMove(dashboard.id, index + 1) }, enabled = index < tabs.dashboards.lastIndex) {
                                Icon(Icons.Default.ArrowDownward, tr("Mover ${dashboard.localizedName} después", "Move ${dashboard.localizedName} down"), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                TextButton(onDismiss, Modifier.align(Alignment.End)) { Text(tr("Listo", "Done")) }
            }
        }
    }
}

/** Distribución única para todas las matrices; las composiciones divididas conservan su protagonista. */
@Composable
private fun CustomDashboardGridContainer(config: CustomDashboardConfig, availablePids: List<ObdPid>,
    vehicleState: VehicleState, connectionState: ConnectionState, isEditMode: Boolean,
    selectedSlot: Int?, onTapSlot: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleSlots: List<Int>? = null, carColumns: Int? = null, carSplit: Boolean = false) {
    @Composable
    fun Slot(index: Int, slotModifier: Modifier) {
        val widget = config.getWidgetForSlot(index)
        val slotDesc = tr("Ranura ${index + 1}", "Slot ${index + 1}")
        Box(slotModifier.semantics { contentDescription = slotDesc }) {
            if (widget == null) EmptySlotCard(index, { onTapSlot(index) }, isEditMode = isEditMode,
                isSelected = selectedSlot == index)
            else DashboardSlotWidget(widget, index, widget.resolvePid(availablePids), vehicleState, connectionState,
                isEditMode, selectedSlot == index, { onTapSlot(index) }, if (LocalCarMode.current) DashboardDensity.COMFORTABLE else config.density)
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val metrics = DashboardLayoutPolicy.measure(config.gridLayout, maxWidth.value, LocalDensity.current.fontScale, config.density, LocalCarMode.current)
        val gap = if (LocalCarMode.current) DashboardLayoutPolicy.carGrid(config.gridLayout, maxWidth.value, maxHeight.value).gap.dp else metrics.gap.dp
        val columns = carColumns ?: metrics.columns
        if (if (LocalCarMode.current) carSplit else config.gridLayout.isSplit && columns == 2) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                if (config.gridLayout == DashboardGridLayout.SPLIT_1_2) Slot(0, Modifier.weight(1.3f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    val indices = if (config.gridLayout == DashboardGridLayout.SPLIT_1_2) listOf(1, 2) else listOf(0, 1)
                    indices.forEach { Slot(it, Modifier.weight(1f).fillMaxWidth()) }
                }
                if (config.gridLayout == DashboardGridLayout.SPLIT_2_1) Slot(2, Modifier.weight(1.3f).fillMaxHeight())
            }
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            (visibleSlots ?: (0 until config.gridLayout.slotCount).toList()).chunked(columns).forEach { indices ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    indices.forEach { Slot(it, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - indices.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Tarjeta de ranura vacía con estilo deportivo oscuro y toque para asignar indicador directamente
 */
@Composable
private fun EmptySlotCard(slotIndex: Int, onClick: () -> Unit, modifier: Modifier = Modifier,
    isEditMode: Boolean = false, isSelected: Boolean = false) {
    if (LocalCarMode.current) {
        BoxWithConstraints(modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) CockpitElevated else CockpitSurface)
            .then(if (isEditMode) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)) {
            val spacious = maxHeight >= 120.dp * LocalDensity.current.fontScale
            val titleSize = if (maxWidth < 180.dp) 16.sp else 20.sp
            val subSize = if (maxWidth < 180.dp) 14.sp else 18.sp
            Column(Modifier.fillMaxSize().padding(if (spacious) 12.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (isEditMode && spacious) Icon(Icons.Default.Add, null, tint = CockpitAccent, modifier = Modifier.size(28.dp))
                androidx.compose.material3.Text(tr("Ranura ${slotIndex + 1}", "Slot ${slotIndex + 1}"), color = TextWhite, fontSize = titleSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                androidx.compose.material3.Text(if (isEditMode) tr("Añadir", "Add") else tr("Libre", "Free"), color = CockpitMuted, fontSize = subSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxSize().racingPanel(12.dp)
        .then(if (isSelected) Modifier.border(2.dp, RacingOrange, RoundedCornerShape(12.dp)) else Modifier)
        .then(if (isEditMode) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).padding(6.dp),
        contentAlignment = Alignment.Center) {
        val compact = maxHeight < 84.dp * LocalDensity.current.fontScale
        if (compact) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isEditMode) Icon(Icons.Default.Add, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(22.dp))
            Text(tr("Ranura ${slotIndex + 1}", "Slot ${slotIndex + 1}"), color = TextWhite, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isEditMode) Box(Modifier.size(32.dp).background(RacingOrange.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(22.dp))
            }
            Text(tr("Ranura ${slotIndex + 1}", "Slot ${slotIndex + 1}"), color = TextWhite, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (isEditMode) tr("Toca para asignar", "Tap to assign") else tr("Ranura libre", "Empty slot"), color = TextMuted, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1)
        }
    }
}

/** El indicador se selecciona con un toque; todas sus acciones viven en el panel de edición. */
@Composable
private fun DashboardSlotWidget(widget: DashboardWidget, slotIndex: Int, definition: ObdPid?,
    vehicleState: VehicleState, connectionState: ConnectionState, isEditMode: Boolean, isSelected: Boolean,
    onSelect: () -> Unit, density: DashboardDensity) {
    val editGaugeDesc = tr("Editar indicador ${slotIndex + 1}", "Edit gauge ${slotIndex + 1}")
    val selectGaugeLabel = tr("Seleccionar indicador", "Select gauge")
    Box(Modifier.fillMaxSize().then(if (isEditMode) Modifier
        .border(if (isSelected) 2.dp else 1.dp, (if (LocalCarMode.current) CockpitAccent else RacingOrange).copy(alpha = if (isSelected) 1f else 0.4f), RoundedCornerShape(if (LocalCarMode.current) 28.dp else 12.dp))
        .semantics { contentDescription = editGaugeDesc; selected = isSelected }
        .clickable(role = Role.Button, onClickLabel = selectGaugeLabel, onClick = onSelect) else Modifier)) {
        val status = definition?.telemetryStatus(vehicleState, connectionState) ?: SensorTelemetryStatus.MISSING
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CustomWidgetItem(widget, definition, vehicleState, connectionState, compact = density == DashboardDensity.COMPACT)
            }
            if (!LocalCarMode.current && density != DashboardDensity.COMPACT && status != SensorTelemetryStatus.AVAILABLE && status != SensorTelemetryStatus.SIMULATED) {
                Text(status.label, Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                    color = TextMuted, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Diálogo modal para seleccionar una disposición de rejilla
 */
@Composable
private fun SelectGridLayoutDialog(
    currentLayout: DashboardGridLayout,
    onDismiss: () -> Unit,
    onSelectLayout: (DashboardGridLayout) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val car = LocalCarMode.current
    val layouts = if (car) DashboardGridLayout.carLayouts else DashboardGridLayout.entries

    ResponsiveDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = minOf(maxHeight, 520.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141922),
            border = BorderStroke(1.dp, Color(0xFF283244))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = RacingOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("Distribución", "Layout"),
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                tr("Elige cómo organizar tus indicadores", "Choose how to arrange your gauges"),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Cerrar", "Close"), tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(layouts) { layout ->
                        val isSelected = layout == currentLayout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .heightIn(min = if (car) 132.dp else 0.dp)
                                .background(if (car) (if (isSelected) CockpitElevated else CockpitSurface) else if (isSelected) Color(0x33FF6D00) else Color(0xFF1B222E))
                                .border(
                                    1.dp,
                                    if (isSelected) RacingOrange else Color(0xFF283445),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectLayout(layout)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini representación gráfica esquemática de la grid
                            GridLayoutMiniPreview(
                                layout = layout,
                                isSelected = isSelected,
                                modifier = Modifier.size(width = if (car) 92.dp else 54.dp, height = if (car) 54.dp else 36.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        text = if (car) layout.carTitle else layout.title,
                                        color = if (isSelected) RacingOrange else TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) RacingOrange else Color(0xFF2B374A))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (car) layout.carBadge else layout.badge,
                                            color = if (isSelected) Color.Black else NeonCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = layout.subtitle,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = tr("Seleccionado", "Selected"),
                                    tint = RacingOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Previsualización en miniatura con cajas de bordes definidos de una disposición de cuadrícula
 */
@Composable
private fun GridLayoutMiniPreview(layout: DashboardGridLayout, isSelected: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val columns = layout.preferredColumns
        val rows = if (layout.isSplit) 2 else (layout.slotCount + columns - 1) / columns
        val color = if (isSelected) RacingOrange else NeonCyan.copy(alpha = 0.55f)
        val gap = 2.dp.toPx()
        fun cell(x: Float, y: Float, w: Float, h: Float) = drawRoundRect(color,
            androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(w.coerceAtLeast(1f), h.coerceAtLeast(1f)),
            androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))
        val width = (size.width - (columns - 1) * gap) / columns
        val height = (size.height - (rows - 1) * gap) / rows
        if (layout.isSplit) {
            val primary = if (layout == DashboardGridLayout.SPLIT_1_2) 0 else 1
            cell(primary * (width + gap), 0f, width, size.height)
            repeat(2) { cell((1 - primary) * (width + gap), it * (height + gap), width, height) }
        } else repeat(layout.slotCount) { index -> cell((index % columns) * (width + gap), (index / columns) * (height + gap), width, height) }
    }
}

@Composable
private fun CustomWidgetItem(
    widget: DashboardWidget,
    definition: ObdPid?,
    vehicleState: VehicleState,
    connectionState: ConnectionState,
    compact: Boolean = false
) {
    val reading = definition?.currentReading(vehicleState, connectionState)
    val value = reading?.value ?: Float.NaN
    val unit = trUnit(reading?.units ?: definition?.units.orEmpty())
    val displayValue = widget.displayValue(reading)
    val title = widget.localizedTitle

    if (LocalCarMode.current) {
        CarInstrument(widget, definition, reading, vehicleState, connectionState)
        return
    }

    val widgetColor = runCatching { Color(android.graphics.Color.parseColor(widget.colorHex)) }.getOrDefault(RacingOrange)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .racingPanel()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (compact || maxHeight < 90.dp * LocalDensity.current.fontScale || maxWidth < 115.dp * LocalDensity.current.fontScale) {
            Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = TextMuted, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(3.dp))
                FittedValue(if (compact) displayValue else "$displayValue $unit", 22.sp,
                    color = widgetColor, textAlign = TextAlign.Center)
                if (compact) {
                    val status = definition?.telemetryStatus(vehicleState, connectionState) ?: SensorTelemetryStatus.MISSING
                    Text(if (status == SensorTelemetryStatus.AVAILABLE || status == SensorTelemetryStatus.SIMULATED) unit else status.label,
                        color = TextMuted, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            return@BoxWithConstraints
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when (widget.type) {
                WidgetType.ANALOG_GAUGE -> {
                    val (minVal, maxVal) = widget.scale(definition)
                    val step = (maxVal - minVal) / 5f
                    val redline = if (definition?.id == com.torqueobd2.dash.data.model.SensorIds.RPM) 4500f else maxVal
                    AnalogGauge(
                        value = value,
                        minValue = minVal,
                        formattedValue = displayValue,
                        maxValue = maxVal,
                        title = title,
                        unit = unit,
                        redlineStart = redline,
                        majorTickStep = step,
                        primaryColor = widgetColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WidgetType.DIGITAL_HUD -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = title,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            FittedValue(
                                text = displayValue,
                                color = TextWhite,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = unit,
                                color = widgetColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                WidgetType.HORIZONTAL_BAR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = title, color = TextMuted, fontSize = 11.sp, maxLines = 1,
                                modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                            FittedValue(
                                text = "${displayValue} $unit",
                                color = TextWhite,
                                modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val (minVal, maxVal) = widget.scale(definition)
                        val progress = if (reading == null) 0f else ((reading.value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = widgetColor,
                            trackColor = Color(0xFF1B2430)
                        )
                    }
                }

                WidgetType.SPARKLINE_CHART -> {
                    val points = vehicleState.sparklineHistory[definition?.id] ?: emptyList()
                    RealtimeChart(
                        title = title,
                        points = points,
                        currentValue = displayValue,
                        unit = unit,
                        lineColor = widgetColor,
                        modifier = Modifier.fillMaxSize(),
                        scaleMin = widget.scaleMin.takeIf { widget.hasCustomScale }, scaleMax = widget.scaleMax.takeIf { widget.hasCustomScale }
                    )
                }

                WidgetType.COMPACT_TILE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = title, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp))
                        FittedValue(
                            text = "${displayValue} $unit",
                            color = widgetColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }


    }
}

/**
 * Diálogo interactivo de añadir widget al Dashboard.
 * Diseñado específicamente para pantallas apaisadas (horizontal) y verticales sin que nada se corte
 * ni se colapsen los selectores.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AddWidgetDialog(
    availablePids: List<ObdPid>,
    initialWidget: DashboardWidget? = null,
    onDismiss: () -> Unit,
    onAdd: (DashboardWidget) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var selectedType by remember { mutableStateOf(initialWidget?.type ?: WidgetType.ANALOG_GAUGE) }
    var selectedPid by remember {
        mutableStateOf(if (initialWidget != null) initialWidget.pidId.takeIf { it.isNotBlank() }
            ?: initialWidget.resolvePid(availablePids)?.id.orEmpty()
        else availablePids.firstOrNull { it.isSupported && it.supportChecked }?.id ?: availablePids.firstOrNull()?.id.orEmpty())
    }
    var customTitle by remember { mutableStateOf(initialWidget?.localizedTitle.orEmpty()) }
    var lastAutoTitle by remember {
        mutableStateOf(availablePids.firstOrNull()?.name ?: tr("Revoluciones del motor", "Engine RPM"))
    }
    var selectedColor by remember { mutableStateOf(initialWidget?.colorHex ?: "#FF6D00") }
    var decimals by remember { mutableStateOf(initialWidget?.decimals) }
    var customScale by remember { mutableStateOf(initialWidget?.hasCustomScale == true) }
    var scaleMin by remember { mutableStateOf(initialWidget?.scaleMin?.toString().orEmpty()) }
    var scaleMax by remember { mutableStateOf(initialWidget?.scaleMax?.toString().orEmpty()) }
    val minimum = scaleMin.replace(',', '.').toFloatOrNull()
    val maximum = scaleMax.replace(',', '.').toFloatOrNull()
    val validScale = !customScale || (minimum != null && maximum != null && minimum.isFinite() && maximum.isFinite() && minimum < maximum && (maximum - minimum).isFinite())
    var pidSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = listOf(
        "ALL" to tr("Todos", "All"),
        "PERFORMANCE" to tr("Prestaciones", "Performance"),
        "ENGINE" to tr("Motor", "Engine"),
        "TEMP" to tr("Temperaturas", "Temperatures"),
        "TURBO" to tr("Turbo / Presión", "Turbo / Pressure"),
        "DPF" to tr("Diésel / DPF", "Diesel / DPF"),
        "FUEL_ELEC" to tr("Consumo / Batería", "Fuel / Battery"),
        "OXYGEN" to tr("Oxígeno / Mezcla", "Oxygen / Mixture"),
        "TRANSMISSION" to tr("Transmisión", "Transmission"),
        "DIAGNOSTICS" to tr("Diagnóstico", "Diagnostics")
    )

    // Filtrar PIDs por búsqueda y categoría
    val filteredPids = remember(availablePids, pidSearchQuery, selectedCategory) {
        availablePids.filter { pid ->
            val matchesQuery = pidSearchQuery.isBlank() ||
                pid.name.contains(pidSearchQuery, ignoreCase = true) ||
                pid.shortName.contains(pidSearchQuery, ignoreCase = true) ||
                pid.units.contains(pidSearchQuery, ignoreCase = true) ||
                pid.modeAndPid.contains(pidSearchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "OXYGEN" -> pid.category == PidCategory.OXYGEN
                "TRANSMISSION" -> pid.category == PidCategory.TRANSMISSION
                "DIAGNOSTICS" -> pid.category == PidCategory.DIAGNOSTICS
                "PERFORMANCE" -> pid.id in listOf("calc_power_hp", "calc_torque_nm", "010D", "010C", "0162", "0161", "0163", "0104", "0143") ||
                    pid.shortName in listOf("Potencia", "Power", "Par", "Torque", "Velocidad", "Speed", "RPM", "Par actual ECU", "Actual ECU torque", "Par pedido ECU", "Driver torque", "Par referencia", "Ref torque", "Carga", "Load", "Carga absoluta", "Absolute load")
                "ENGINE" -> pid.category == PidCategory.ENGINE ||
                    pid.id in listOf("010C", "0104", "0111", "0110") ||
                    pid.shortName in listOf("RPM", "Carga", "Load", "Acelerador", "Throttle", "MAF")
                "TEMP" -> pid.units.contains("°C") ||
                    pid.id in listOf("0105", "010F", "013C") ||
                    pid.shortName in listOf("Temp Liq", "Coolant", "Temp Adm", "Intake T", "EGT")
                "TURBO" -> pid.id in listOf("010B", "calc_boost_bar", "h_dpf_diff_pres") ||
                    pid.shortName in listOf("MAP", "DPF diff.pres") ||
                    pid.units in listOf("kPa", "bar", "hPa", "mbar", "Pa") ||
                    pid.shortName.contains("Turbo", ignoreCase = true) ||
                    pid.shortName.contains("VGT", ignoreCase = true) ||
                    pid.shortName.contains("Wastegate", ignoreCase = true) ||
                    pid.shortName.contains("Boost", ignoreCase = true)
                "DPF" -> pid.category == PidCategory.EXHAUST_DPF ||
                    pid.shortName.contains("DPF", ignoreCase = true) ||
                    pid.shortName.contains("FAP", ignoreCase = true) ||
                    pid.shortName.contains("EGT", ignoreCase = true) ||
                    pid.shortName.contains("AdBlue", ignoreCase = true)
                "FUEL_ELEC" -> pid.category == PidCategory.FUEL ||
                    pid.category == PidCategory.ELECTRICAL ||
                    pid.id in listOf("calc_fuel_rate_lh", "012F", "0142") ||
                    pid.shortName in listOf("Consumo", "Fuel Rate", "Nivel Dep", "Fuel Lvl", "Voltaje", "Voltage")
                else -> true
            }
            matchesQuery && matchesCategory
        }
    }

    val typeOptions = listOf(
        WidgetType.ANALOG_GAUGE to tr("Analógico", "Analog"),
        WidgetType.DIGITAL_HUD to tr("Digital", "Digital"),
        WidgetType.HORIZONTAL_BAR to tr("Barra", "Bar"),
        WidgetType.SPARKLINE_CHART to tr("Gráfico", "Chart"),
        WidgetType.COMPACT_TILE to tr("Compacto", "Compact")
    )

    val colorOptions = listOf(
        "#FF6D00" to tr("Naranja", "Orange"),
        "#00E5FF" to tr("Cian", "Cyan"),
        "#00E676" to tr("Verde", "Green"),
        "#FF1744" to tr("Rojo", "Red"),
        "#FFD600" to tr("Amarillo", "Yellow"),
        "#D500F9" to tr("Púrpura", "Purple")
    )

    fun onSelectSensor(pid: ObdPid) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (selectedPid != pid.id) customScale = false
        selectedPid = pid.id
        if (customTitle.isBlank() || customTitle == lastAutoTitle) {
            customTitle = pid.name
            lastAutoTitle = pid.name
        }
    }

    val fontScale = uiLayoutScale()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val searchRequester = remember { BringIntoViewRequester() }
    val titleRequester = remember { BringIntoViewRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var titleFocused by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(initialWidget != null) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    val carPicker = LocalCarMode.current
    LaunchedEffect(keyboardVisible, searchFocused, titleFocused) {
        if (keyboardVisible) {
            kotlinx.coroutines.delay(120)
            if (searchFocused) searchRequester.bringIntoView()
            if (titleFocused) titleRequester.bringIntoView()
        }
    }

    @Composable
    fun SensorList(listModifier: Modifier) {
        LazyColumn(listModifier.racingPanel(10.dp).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (filteredPids.isEmpty()) item { Text(tr("No hay sensores que coincidan.", "No matching sensors found."), color = TextMuted, fontSize = 12.sp) }
            items(filteredPids, key = { it.id }) { pid ->
                val selected = selectedPid == pid.id
                Column(Modifier.fillMaxWidth().heightIn(min = if (carPicker) 76.dp else 56.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (selected) RacingOrange.copy(alpha = 0.14f) else Color.Transparent)
                    .border(1.dp, if (selected) RacingOrange else BorderDark, RoundedCornerShape(8.dp))
                    .clickable { onSelectSensor(pid) }.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(pid.shortName, Modifier.weight(1f), color = if (selected) RacingOrange else TextWhite,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        FittedValue(trUnit(pid.units), 10.sp, Modifier.weight(0.65f), color = TextMuted, textAlign = TextAlign.End)
                        if (selected) Icon(Icons.Default.Check, contentDescription = tr("Seleccionado", "Selected"), tint = RacingOrange,
                            modifier = Modifier.padding(start = 6.dp).size(16.dp))
                    }
                    Text(pid.name, color = TextMuted, fontSize = 10.sp, maxLines = if (carPicker) 1 else 2, overflow = TextOverflow.Ellipsis)
                    if (!carPicker) Text("${pid.modeAndPid}${if (pid.header.isNotBlank()) " · ECU ${pid.header}" else ""} · ${pid.supportDescription}" + if (!pid.isEnabled) tr(" · Desactivado", " · Disabled") else "",
                        color = TextMuted, fontSize = 10.sp)
                }
            }
        }
    }

    @Composable
    fun SensorSelector(selectorModifier: Modifier, bounded: Boolean) {
        if (carPicker) {
            Column(selectorModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(pidSearchQuery, { pidSearchQuery = it }, singleLine = true,
                    placeholder = { Text(tr("Buscar sensor…", "Search sensor…")) },
                    modifier = Modifier.fillMaxWidth().bringIntoViewRequester(searchRequester)
                        .onFocusChanged { searchFocused = it.isFocused })
                if (!keyboardVisible) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showTypeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(typeOptions.first { it.first == selectedType }.second, maxLines = 1)
                        }
                        DropdownMenu(showTypeMenu, { showTypeMenu = false }) {
                            typeOptions.forEach { (type, label) -> DropdownMenuItem(text = { Text(label) }, onClick = {
                                selectedType = type; showTypeMenu = false
                            }) }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showCategoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(categories.firstOrNull { it.first == selectedCategory }?.second ?: categories.first().second, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(showCategoryMenu, { showCategoryMenu = false }) {
                            categories.forEach { (catKey, catLabel) -> DropdownMenuItem(text = { Text(catLabel) }, onClick = {
                                selectedCategory = catKey; showCategoryMenu = false
                            }) }
                        }
                    }
                }
                SensorList(if (bounded) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().height(176.dp * fontScale))
            }
            return
        }
        Column(selectorModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Tipo de indicador", "Gauge type"), color = RacingOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(typeOptions) { (type, label) ->
                    val selected = selectedType == type
                    Box(Modifier.heightIn(min = if (LocalCarMode.current) 76.dp else 48.dp).widthIn(min = 80.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (selected) RacingOrange else DarkSurface)
                        .border(1.dp, if (selected) RacingOrange else BorderDark, RoundedCornerShape(8.dp))
                        .clickable { selectedType = type }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (selected) Color.Black else TextWhite, fontSize = 14.sp, maxLines = 1)
                    }
                }
            }
            OutlinedTextField(value = pidSearchQuery, onValueChange = { pidSearchQuery = it },
                placeholder = { Text(tr("Buscar sensor…", "Search sensor…"), fontSize = 12.sp) }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextWhite, fontSize = if (LocalCarMode.current) 24.sp else 14.sp),
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(searchRequester)
                    .onFocusChanged { searchFocused = it.isFocused })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories, key = { it.first }) { (catKey, catLabel) ->
                    Box(Modifier.heightIn(min = if (LocalCarMode.current) 76.dp else 48.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (selectedCategory == catKey) RacingOrange.copy(alpha = 0.16f) else DarkSurface)
                        .clickable { selectedCategory = catKey }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(catLabel, fontSize = 13.sp, color = if (selectedCategory == catKey) RacingOrange else TextMuted)
                    }
                }
            }
            SensorList(if (bounded) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().height(176.dp * fontScale))
        }
    }

    @Composable
    fun IndicatorOptions() {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (initialWidget != null && availablePids.none { it.id == selectedPid }) {
                Text(tr("Sensor no encontrado. Puedes conservar su vínculo o seleccionar otro en «Sensor».", "Sensor not found. You can keep its link or select another under 'Sensor'."), color = WarningAmber, fontSize = 12.sp)
            }
            Text(tr("Vista previa · Ejemplo", "Preview · Example"), color = RacingOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().height(96.dp * fontScale).racingPanel().padding(6.dp)) {
                LivePreviewWidget(selectedType, availablePids.find { it.id == selectedPid }?.shortName.orEmpty(),
                    customTitle.ifBlank { availablePids.find { it.id == selectedPid }?.name.orEmpty() }, selectedColor,
                    trUnit(availablePids.find { it.id == selectedPid }?.units ?: ""),
                    minimum = if (customScale && validScale) minimum!! else availablePids.find { it.id == selectedPid }?.minValue ?: 0f,
                    maximum = if (customScale && validScale) maximum!! else availablePids.find { it.id == selectedPid }?.maxValue ?: 100f,
                    decimals = decimals)
            }
            OutlinedTextField(value = customTitle, onValueChange = { customTitle = it.take(120) },
                label = { Text(tr("Título del indicador", "Gauge title"), fontSize = 12.sp) }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextWhite, fontSize = if (LocalCarMode.current) 24.sp else 14.sp),
                placeholder = { Text(availablePids.find { it.id == selectedPid }?.name.orEmpty(), fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(titleRequester)
                    .onFocusChanged { titleFocused = it.isFocused },
                trailingIcon = {
                    IconButton(onClick = {
                        val name = availablePids.find { it.id == selectedPid }?.name ?: selectedPid
                        customTitle = name
                        lastAutoTitle = name
                    }) { Icon(Icons.Default.Refresh, contentDescription = tr("Restablecer título", "Reset title"), tint = TextMuted) }
                })
            Text(tr("Color", "Color"), color = TextMuted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                colorOptions.forEach { (hex, name) ->
                    val color = when (hex) {
                        "#00E5FF" -> NeonCyan; "#00E676" -> NeonGreen; "#FF1744" -> Color(0xFFFF1744)
                        "#FFD600" -> Color(0xFFFFD600); "#D500F9" -> Color(0xFFD500F9); else -> RacingOrange
                    }
                    val colorDesc = tr("Color $name", "Color $name")
                    Box(Modifier.size(if (LocalCarMode.current) 76.dp else 56.dp).clip(CircleShape).background(color)
                        .border(if (selectedColor == hex) 3.dp else 1.dp, if (selectedColor == hex) TextWhite else BorderDark, CircleShape)
                        .semantics { contentDescription = colorDesc }
                        .clickable { selectedColor = hex }, contentAlignment = Alignment.Center) {
                        if (selectedColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
            Text(tr("Decimales", "Decimals"), color = TextMuted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf<Int?>(null) + (0..4).toList()).forEach { value ->
                    FilterChip(selected = decimals == value, onClick = { decimals = value },
                        modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 56.dp),
                        label = { Text(value?.toString() ?: "Auto") })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Escala personalizada", "Custom scale"), Modifier.weight(1f), color = TextWhite)
                androidx.compose.material3.Switch(customScale, onCheckedChange = { enabled ->
                    customScale = enabled
                    if (enabled && scaleMin.isBlank()) scaleMin = (availablePids.find { it.id == selectedPid }?.minValue ?: 0f).toString()
                    if (enabled && scaleMax.isBlank()) scaleMax = (availablePids.find { it.id == selectedPid }?.maxValue ?: 100f).toString()
                })
            }
            if (customScale) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(scaleMin, { scaleMin = it.take(20) }, Modifier.weight(1f), label = { Text(tr("Mínimo", "Minimum")) }, singleLine = true, isError = !validScale)
                    OutlinedTextField(scaleMax, { scaleMax = it.take(20) }, Modifier.weight(1f), label = { Text(tr("Máximo", "Maximum")) }, singleLine = true, isError = !validScale)
                }
                if (!validScale) Text(tr("Introduce números finitos: mínimo menor que máximo.", "Enter finite numbers: minimum less than maximum."), color = com.torqueobd2.dash.ui.theme.DangerRed, fontSize = 12.sp)
            }
            Text(
                tr(
                    "La escala cambia diales y barras; los decimales cambian la presentación. Se conserva el valor recibido de la ECU.",
                    "Scale changes dials and bars; decimals change formatting. Raw ECU values are preserved."
                ),
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }

    ResponsiveDialog(onDismissRequest = onDismiss) {
            val dialogHeight = minOf(maxHeight, 620.dp * fontScale)
            val columns = maxWidth >= 680.dp * fontScale
            val short = dialogHeight < 260.dp * fontScale
            val hideHeading = keyboardVisible && dialogHeight < 180.dp * fontScale
            val sensorScroll = rememberScrollState()
            Surface(Modifier.fillMaxWidth().height(dialogHeight), shape = RoundedCornerShape(18.dp),
                color = CardBackground, border = BorderStroke(1.dp, BorderDark)) {
                Column(Modifier.fillMaxSize().padding(if (short) 6.dp else 10.dp)) {
                    if (!hideHeading) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (carPicker) (if (showAppearance) tr("Apariencia", "Appearance") else tr("Elegir sensor", "Choose sensor")) else if (initialWidget == null) tr("Añadir indicador", "Add gauge") else tr("Editar indicador", "Edit gauge"), Modifier.weight(1f), color = TextWhite, fontSize = if (short) 14.sp else 20.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (carPicker) IconButton(onClick = { showAppearance = !showAppearance }) {
                            Icon(Icons.Default.Tune, if (showAppearance) tr("Elegir sensor", "Choose sensor") else tr("Configurar apariencia", "Configure appearance"), tint = RacingOrange)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = tr("Cerrar", "Close"), tint = TextMuted)
                        }
                    }
                    Spacer(Modifier.height(if (short) 2.dp else 6.dp))
                    if (carPicker) {
                        if (showAppearance) Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) { IndicatorOptions() }
                        else SensorSelector(Modifier.weight(1f).fillMaxWidth(), bounded = true)
                    } else if (columns) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SensorSelector(Modifier.weight(1.1f).fillMaxHeight()
                                .verticalScroll(sensorScroll), bounded = false)
                            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { IndicatorOptions() }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !showAppearance, onClick = { showAppearance = false },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(tr("Sensor", "Sensor")) })
                            FilterChip(selected = showAppearance, onClick = { showAppearance = true },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(tr("Apariencia", "Appearance")) })
                        }
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (showAppearance) IndicatorOptions()
                            else SensorSelector(Modifier.fillMaxWidth(), bounded = false)
                        }
                    }
                    Spacer(Modifier.height(if (short) 2.dp else 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = if (short) 48.dp else 56.dp),
                            contentPadding = PaddingValues(if (short) 4.dp else 8.dp)) {
                            FittedValue(tr("Cancelar", "Cancel"), 13.sp, color = TextMuted, textAlign = TextAlign.Center)
                        }
                        Button(onClick = {
                            val title = customTitle.ifBlank { availablePids.find { it.id == selectedPid }?.name ?: selectedPid }
                            onAdd((initialWidget ?: DashboardWidget()).copy(type = selectedType, pidId = selectedPid,
                                pidShortName = availablePids.find { it.id == selectedPid }?.shortName ?: initialWidget?.pidShortName.orEmpty(), title = title,
                                colorHex = selectedColor, decimals = decimals,
                                scaleMin = minimum.takeIf { customScale }, scaleMax = maximum.takeIf { customScale }))
                        }, enabled = validScale && (availablePids.any { it.id == selectedPid && (!it.supportChecked || it.isSupported) } || (initialWidget != null && initialWidget.pidId == selectedPid)),
                            modifier = Modifier.weight(1f).heightIn(min = if (short) 48.dp else 56.dp),
                            contentPadding = PaddingValues(if (short) 4.dp else 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RacingOrange)) {
                            FittedValue(if (initialWidget == null) tr("Añadir", "Add") else tr("Guardar indicador", "Save gauge"), 13.sp, color = Color.Black, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
    }
}

/**
 * Previsualización en vivo en la pantalla de añadir widget
 */
@Composable
private fun LivePreviewWidget(
    type: WidgetType,
    pidShortName: String,
    title: String,
    colorHex: String,
    unit: String, minimum: Float = 0f, maximum: Float = 100f, decimals: Int? = null
) {
    val widgetColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(RacingOrange)

    val displayUnit = trUnit(if (unit.isNotBlank()) unit else when (pidShortName) {
        "Potencia", "Power" -> "CV"
        "Par", "Torque" -> "Nm"
        "RPM" -> "RPM"
        "Velocidad", "Speed" -> "km/h"
        "MAP" -> "kPa"
        "Temp Liq", "Coolant" -> "°C"
        else -> ""
    })

    val demo = minimum + (maximum - minimum) * 0.65f
    val demoText = demo.telemetryText(decimals ?: 1)
    when (type) {
        WidgetType.ANALOG_GAUGE -> {
            AnalogGauge(
                value = demo,
                formattedValue = demoText,
                minValue = minimum,
                maxValue = maximum,
                title = title,
                unit = displayUnit,
                redlineStart = maximum,
                majorTickStep = (maximum - minimum) / 5f,
                primaryColor = widgetColor,
                modifier = Modifier.fillMaxHeight()
            )
        }
        WidgetType.DIGITAL_HUD -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = demoText,
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = displayUnit, color = widgetColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        WidgetType.HORIZONTAL_BAR -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = title, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    FittedValue("$demoText $displayUnit", 11.sp, Modifier.weight(0.7f), color = TextWhite, textAlign = TextAlign.End)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { 0.72f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = widgetColor,
                    trackColor = Color(0xFF1B2430)
                )
            }
        }
        WidgetType.SPARKLINE_CHART -> {
            RealtimeChart(
                title = title,
                points = listOf(20f, 35f, 45f, 70f, 60f, 85f, 78f),
                currentValue = demoText,
                unit = displayUnit,
                lineColor = widgetColor,
                modifier = Modifier.fillMaxSize()
            )
        }
        WidgetType.COMPACT_TILE -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = title, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                FittedValue("$demoText $displayUnit", 20.sp, color = widgetColor,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Diálogo para seleccionar plantillas prediseñadas o vaciar el dashboard
 */
@Composable
private fun PresetsDialog(
    availablePids: List<ObdPid>,
    onDismiss: () -> Unit,
    onSelectPreset: (DashboardPreset) -> Unit,
    onResetDefault: () -> Unit,
    onClearDashboard: () -> Unit
) {
    val presets = DashboardPresets.forPids(availablePids)

    ResponsiveDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = minOf(maxHeight, 480.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Plantillas", "Templates"), color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Cerrar", "Close"), tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presets, key = { it.id }) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B222E))
                                .border(1.dp, Color(0xFF283445), RoundedCornerShape(8.dp))
                                .clickable { onSelectPreset(preset) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = preset.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = preset.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = preset.subtitle, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onResetDefault,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tr("Restablecer por Defecto", "Reset to Default"), fontSize = 10.sp, color = NeonCyan)
                            }
                            OutlinedButton(
                                onClick = onClearDashboard,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tr("Vaciar Dashboard", "Clear Dashboard"), fontSize = 10.sp, color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }
    }
}
