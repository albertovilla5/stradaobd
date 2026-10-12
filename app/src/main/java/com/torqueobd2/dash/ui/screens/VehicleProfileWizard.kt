package com.torqueobd2.dash.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.ui.automotive.*
import com.torqueobd2.dash.ui.theme.*
import com.torqueobd2.dash.ui.components.ResponsiveDialog
import com.torqueobd2.dash.ui.components.FittedValue
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private val DraftSaver = Saver<VehicleProfileDraft, ArrayList<String>>(
    save = { draft -> ArrayList(VehicleField.entries.map { draft.value(it) } + listOf(draft.fuel?.name.orEmpty(), draft.hideUnsupported.toString())) },
    restore = { values -> VehicleProfileDraft(VehicleField.entries.associateWith { values[it.ordinal] },
        FuelType.entries.find { it.name == values[VehicleField.entries.size] }, values.last().toBoolean()) }
)

/** One question at a time; keyboard and content shrink while the main actions remain visible. */
@Composable
fun VehicleProfileWizard(initialProfile: VehicleProfile?, initialSetup: Boolean = false,
    onCancel: (() -> Unit)?, onSave: (VehicleProfile) -> Unit,
    onImportBackup: ((java.io.InputStream) -> com.torqueobd2.dash.data.repository.AppBackupSnapshot)? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val onCarModeChanged = LocalOnCarModeChanged.current
    val onLanguagePreferenceChanged = com.torqueobd2.dash.i18n.LocalOnAppLanguagePreferenceChanged.current
    var importError by rememberSaveable { mutableStateOf<String?>(null) }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && onImportBackup != null) {
            scope.launch {
                try {
                    val snapshot = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use(onImportBackup)
                            ?: error(tr("No se pudo abrir el archivo seleccionado.", "Could not open selected file."))
                    }
                    importError = null
                    onCarModeChanged(snapshot.carMode)
                    onLanguagePreferenceChanged(snapshot.languagePreference)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    importError = e.message ?: tr("No se pudo restaurar la copia.", "Could not restore backup.")
                }
            }
        }
    }
    val id = rememberSaveable(initialProfile?.id) { initialProfile?.id ?: UUID.randomUUID().toString() }
    var draft by rememberSaveable(initialProfile?.id, stateSaver = DraftSaver) {
        mutableStateOf(initialProfile?.let(VehicleProfileDraft::from) ?: VehicleProfileDraft())
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var numberPad by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    val field = VehicleField.entries[step]
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val typing = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    fun navigate(next: Int) { focus.clearFocus(); keyboard?.hide(); attempted = false; step = next }
    fun advance() {
        attempted = true
        if (field == VehicleField.REVIEW) {
            val missing = draft.firstInvalidField()
            if (missing != null) navigate(missing.ordinal) else { focus.clearFocus(); keyboard?.hide(); onSave(draft.toProfile(id)) }
        } else if (draft.error(field) == null) navigate(step + 1)
    }
    BackHandler(enabled = step > 0) { navigate(step - 1) }
    BoxWithConstraints(Modifier.fillMaxSize().background(CockpitBackground).padding(if (typing) 8.dp else 16.dp)) {
        val compact = maxHeight < 360.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val columns = if (maxWidth >= 500.dp * LocalDensity.current.fontScale) 2 else 1
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (typing) 4.dp else 12.dp)) {
            if (!typing) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CarText(if (initialSetup) tr("Configura tu vehículo", "Set up your vehicle") else if (initialProfile == null) tr("Nuevo vehículo", "New vehicle") else tr("Editar vehículo", "Edit vehicle"),
                        color = TextWhite, fontSize = 28.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!compact) CarText(field.title, color = CockpitMuted, fontSize = 24.sp)
                }
                CarText("${step + 1} / ${VehicleField.entries.size}", color = CockpitAccent, fontSize = 24.sp)
                if (onCancel != null) CarIconButton(onCancel) { CarIcon(Icons.Default.Close, tr("Cerrar ficha", "Close profile")) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().background(CockpitSurface, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState()).padding(if (typing) 8.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (field) {
                    VehicleField.FUEL -> {
                        CarText(tr("Elige el combustible", "Choose fuel type"), color = TextWhite, fontSize = 24.sp)
                        FuelType.entries.chunked(columns).forEach { fuels ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                fuels.forEach { fuel ->
                                    val title = fuel.shortDisplayName
                                    CarButton(onClick = { draft = draft.copy(fuel = fuel) }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (draft.fuel == fuel) CockpitAccent else CockpitElevated,
                                            contentColor = if (draft.fuel == fuel) CockpitBackground else TextWhite)) { CarText(title, fontSize = 24.sp) }
                                }
                            }
                        }
                    }
                    VehicleField.OPTIONS -> {
                        CarText(tr("Mostrar sensores compatibles", "Show supported sensors"), color = TextWhite, fontSize = 24.sp)
                        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)
                            .toggleable(draft.hideUnsupported, role = Role.Switch) { draft = draft.copy(hideUnsupported = it) },
                            verticalAlignment = Alignment.CenterVertically) {
                            CarText(tr("Ocultar sensores no disponibles", "Hide unavailable sensors"), Modifier.weight(1f), color = CockpitMuted, fontSize = 24.sp)
                            Switch(draft.hideUnsupported, onCheckedChange = null)
                        }
                    }
                    VehicleField.REVIEW -> {
                        CarText(tr("Comprueba la ficha", "Review vehicle profile"), color = TextWhite, fontSize = 28.sp)
                        VehicleField.entries.filter { it !in listOf(VehicleField.REVIEW, VehicleField.OPTIONS) }.forEach { item ->
                            CarText("${item.title}: ${if (item == VehicleField.FUEL) draft.fuel?.displayName else draft.value(item).ifBlank { tr("Automático", "Automatic") }} ${item.unit}",
                                color = CockpitMuted, fontSize = 24.sp)
                        }
                        CarText(if (draft.hideUnsupported) tr("Sensores no disponibles ocultos", "Unavailable sensors hidden") else tr("Todos los sensores visibles", "All sensors visible"), color = CockpitMuted, fontSize = 24.sp)
                        if (initialSetup) {
                            CarText(
                                tr(
                                    "Seguridad vial y privacidad: usa StradaOBD de forma responsable, no manipules la pantalla en marcha y recuerda que todos tus datos permanecen en tu dispositivo.",
                                    "Road safety & privacy: use StradaOBD responsibly, do not interact with the screen while driving, and remember all your data stays on your device."
                                ),
                                color = WarningAmber,
                                fontSize = 20.sp
                            )
                            CarOutlinedButton(
                                onClick = { showPrivacyDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CarIcon(Icons.Default.Security, contentDescription = null, tint = NeonCyan)
                                Spacer(Modifier.width(8.dp))
                                CarText(tr("Ver Política de Privacidad y Aviso de Seguridad", "View Privacy Policy & Safety Notice"), fontSize = 20.sp)
                            }
                        }
                    }
                    else -> {
                        if (!typing) CarText(field.title, color = TextWhite, fontSize = 28.sp)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fun adjust(sign: Int) {
                                val number = (draft.number(field) ?: 0f) + sign * field.step
                                draft = draft.set(field, String.format(Locale.US, if (field.step < 1) "%.1f" else "%.0f", number))
                            }
                            val fieldDesc = tr("Dato: ${field.title}", "Field: ${field.title}")
                            if (field.numeric && !typing) CarIconButton({ adjust(-1) }) { CarIcon(Icons.Default.Remove, tr("Reducir ${field.title}", "Decrease ${field.title}"), tint = CockpitAccent) }
                            if (field.numeric) Surface(onClick = { numberPad = true }, Modifier.weight(1f).heightIn(min = 76.dp)
                                .semantics { contentDescription = fieldDesc }, color = CockpitElevated, shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    CarText(if (field.unit.isBlank()) field.title else tr("Valor · ${field.unit}", "Value · ${field.unit}"), color = CockpitMuted, fontSize = 24.sp)
                                    CarText(draft.value(field).ifBlank { tr("Introducir", "Enter") }, color = TextWhite, fontSize = 32.sp)
                                }
                            } else {
                            OutlinedTextField(draft.value(field), { draft = draft.set(field, it) }, Modifier.weight(1f)
                                .heightIn(min = 76.dp).semantics { contentDescription = fieldDesc },
                                label = { Text(field.title, fontSize = 24.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp),
                                singleLine = field != VehicleField.INIT, maxLines = if (field == VehicleField.INIT) 3 else 1,
                                isError = attempted && draft.error(field) != null,
                                keyboardOptions = KeyboardOptions(keyboardType = if (field.numeric) KeyboardType.Decimal else KeyboardType.Text,
                                    imeAction = if (field == VehicleField.INIT) ImeAction.Default else ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { advance() }))
                            }
                            if (field.numeric && !typing) CarIconButton({ adjust(1) }) { CarIcon(Icons.Default.Add, tr("Aumentar ${field.title}", "Increase ${field.title}"), tint = CockpitAccent) }
                        }
                        if (!typing && field == VehicleField.NAME) CarText(tr("Escribe la marca y el modelo de tu coche.", "Enter your car's make and model."), color = CockpitMuted, fontSize = 24.sp)
                        if (!typing && field == VehicleField.NAME && initialSetup && onImportBackup != null) {
                            CarOutlinedButton(
                                onClick = {
                                    try {
                                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                                    } catch (_: android.content.ActivityNotFoundException) {
                                        importError = tr("Este dispositivo no dispone de selector de archivos.", "This device does not have a file picker.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CarIcon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan)
                                Spacer(Modifier.width(8.dp))
                                CarText(tr("Importar copia de seguridad (.json)", "Import backup (.json)"), fontSize = 20.sp)
                            }
                            importError?.let { err ->
                                CarText(err, color = DangerRed, fontSize = 20.sp)
                            }
                        }
                        if (!typing && field == VehicleField.INIT) CarText(tr("Opcional. Déjalo vacío para la configuración automática del adaptador.", "Optional. Leave blank for automatic adapter setup."), color = CockpitMuted, fontSize = 24.sp)
                        if (!typing && field == VehicleField.EFFICIENCY) CarText(tr("Ajuste inicial: 85 %. Puedes modificarlo.", "Default: 85%. You can adjust it."), color = CockpitMuted, fontSize = 24.sp)
                        if (!typing && field == VehicleField.BOOST) CarText(tr("Sin corrección: 0 bar. También admite valores negativos.", "No correction: 0 bar. Also accepts negative values."), color = CockpitMuted, fontSize = 24.sp)
                    }
                }
                if (attempted) draft.error(field)?.let { CarText(it, color = DangerRed, fontSize = 24.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) CarOutlinedButton({ navigate(step - 1) }, Modifier.weight(1f)) { CarText(tr("Anterior", "Back"), fontSize = 24.sp) }
                if (typing && onCancel != null) CarIconButton(onCancel) { CarIcon(Icons.Default.Close, tr("Cerrar ficha", "Close profile")) }
                CarButton({ advance() }, Modifier.weight(1f)) { CarText(if (field == VehicleField.REVIEW) tr("Guardar", "Save") else tr("Siguiente", "Next"), fontSize = 24.sp) }
            }
        }
    }
    if (numberPad) VehicleNumberPad(field, draft.value(field), onDismiss = { numberPad = false }, onConfirm = {
        draft = draft.set(field, it); numberPad = false
    })
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

/** Teclas de 76 dp; los números no dependen del pequeño teclado del sistema de la radio. */
@Composable
private fun VehicleNumberPad(field: VehicleField, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    var replace by rememberSaveable { mutableStateOf(initial.isNotEmpty()) }
    val decimalDesc = tr("Separador decimal", "Decimal separator")
    val signDesc = tr("Cambiar signo", "Toggle sign")
    val backspaceDesc = tr("Borrar dígito", "Delete digit")
    ResponsiveDialog(onDismiss) {
        val wide = maxWidth >= 440.dp
        val columns = if (wide) 5 else 3
        val dialogHeight = maxHeight
        Surface(Modifier.fillMaxWidth().height(dialogHeight), color = CockpitBackground, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!wide) CarText(field.title, color = CockpitMuted, fontSize = 24.sp)
                Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        if (wide) CarText(field.title, color = CockpitMuted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        FittedValue(value.ifBlank { "—" }, 40.sp, color = TextWhite)
                    }
                    if (wide) {
                        if (field != VehicleField.YEAR) CarIconButton({ if (!value.contains('.')) value += if (value.isBlank()) "0." else "." }) {
                            CarText(".", fontSize = 32.sp, modifier = Modifier.semantics { contentDescription = decimalDesc })
                        }
                        if (field == VehicleField.BOOST) CarIconButton({ value = if (value.startsWith('-')) value.drop(1) else "-$value" }) {
                            CarText("±", fontSize = 32.sp, modifier = Modifier.semantics { contentDescription = signDesc })
                        }
                        CarIconButton({ value = value.dropLast(1); replace = false }) { CarIcon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Backspace, backspaceDesc) }
                        CarIconButton(onDismiss) { CarIcon(Icons.Default.Close, tr("Cancelar número", "Cancel number")) }
                        CarIconButton({ onConfirm(value) }) { CarIcon(Icons.Default.Check, tr("Aceptar número", "Confirm number"), tint = CockpitAccent) }
                    }
                }
                val keys = if (wide) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                    else listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.chunked(columns).forEach { row ->
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                val keyDesc = if (key == "⌫") backspaceDesc else tr("Tecla $key", "Key $key")
                                Surface(onClick = {
                                    when (key) {
                                        "." -> if (field != VehicleField.YEAR && !value.contains('.')) value += if (value.isBlank()) "0." else "."
                                        "⌫" -> { value = value.dropLast(1); replace = false }
                                        else -> { if (replace) { value = key; replace = false } else if (value.length < 16) value += key }
                                    }
                                }, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 76.dp)
                                    .semantics { contentDescription = keyDesc },
                                    color = CockpitSurface, shape = RoundedCornerShape(20.dp)) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CarText(key, color = TextWhite, fontSize = 32.sp) }
                                }
                            }
                        }
                    }
                }
                if (!wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (field == VehicleField.BOOST) CarIconButton({ value = if (value.startsWith('-')) value.drop(1) else "-$value" }) { CarText("±", fontSize = 32.sp, modifier = Modifier.semantics { contentDescription = signDesc }) }
                    CarOutlinedButton(onDismiss, Modifier.weight(1f)) { CarText(tr("Cancelar", "Cancel"), fontSize = 24.sp) }
                    CarButton({ onConfirm(value) }, Modifier.weight(1f)) { CarText(tr("Aceptar", "OK"), fontSize = 24.sp) }
                }
            }
        }
    }
}
