package com.torqueobd2.dash.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.ui.theme.CarbonBlack
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.NeonCyan
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite
import java.io.InputStream

/**
 * Gestor e importador de PIDs de Torque Pro desde archivos CSV o texto directo.
 */
@Composable
fun PidManagerScreen(
    pids: List<ObdPid>,
    onTogglePid: (String, Boolean) -> Unit,
    onImportCsvFromStream: (InputStream) -> Unit,
    onImportCsvFromString: (String) -> Unit,
    onLoadSampleHonda: () -> Unit,
    onLoadStandard: () -> Unit,
    notificationMessage: String?,
    onClearNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedCsvText by remember { mutableStateOf("") }

    // Selector de archivos del sistema para CSV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    onImportCsvFromStream(stream)
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp)
    ) {
        // Encabezado
        Text(
            text = "GESTOR DE PIDS (TORQUE PRO)",
            color = RacingOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Importa archivos CSV de Torque Pro con ecuaciones personalizadas y comandos de inicialización.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Botones de acción rápida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Importar CSV de almacenamiento
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Abrir CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Cargar Preset Honda iDTEC (del CSV del usuario)
            Button(
                onClick = onLoadSampleHonda,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B38)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Honda i-DTEC", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pegar texto CSV
            OutlinedButton(
                onClick = { showPasteDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextWhite)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Pegar CSV", fontSize = 12.sp, color = TextWhite)
            }

            // Restaurar OBD2 Estándar
            OutlinedButton(
                onClick = onLoadStandard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextWhite)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Estándar", fontSize = 12.sp, color = TextWhite)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conteo de PIDs activos
        val enabledCount = pids.count { it.isEnabled }
        Text(
            text = "PIDs registrados (${pids.size} total | $enabledCount activos en polling):",
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de PIDs
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(pids, key = { it.id }) { pid ->
                PidItemCard(pid = pid, onToggle = { onTogglePid(pid.id, it) })
            }
        }
    }

    // Diálogo para pegar CSV directamente
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text(text = "Pegar contenido CSV de Torque Pro", color = TextWhite) },
            text = {
                Column {
                    Text(
                        text = "Pega aquí las líneas del archivo CSV (incluye nombres, ModeAndPID, Ecuación, Min, Max, Units):",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pastedCsvText,
                        onValueChange = { pastedCsvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = {
                            Text(
                                text = DefaultPids.SAMPLE_HONDA_IDTEC_CSV.take(160) + "...",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedCsvText.isNotBlank()) {
                            onImportCsvFromString(pastedCsvText)
                            pastedCsvText = ""
                        }
                        showPasteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange)
                ) {
                    Text("Importar PIDs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = Color(0xFF1D222B)
        )
    }

    // Diálogo / Notificación de resultado de importación
    if (notificationMessage != null) {
        AlertDialog(
            onDismissRequest = onClearNotification,
            title = { Text("Importación de PIDs", color = TextWhite) },
            text = { Text(notificationMessage, color = TextWhite) },
            confirmButton = {
                Button(onClick = onClearNotification, colors = ButtonDefaults.buttonColors(containerColor = RacingOrange)) {
                    Text("Aceptar")
                }
            },
            containerColor = Color(0xFF1D222B)
        )
    }
}

@Composable
private fun PidItemCard(
    pid: ObdPid,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (pid.isEnabled) Color(0xFF2E3847) else Color(0xFF1E242E), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pid.shortName,
                        color = if (pid.isEnabled) RacingOrange else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222936))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pid.modeAndPid,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (pid.units.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${pid.units}]",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = pid.name,
                    color = TextWhite,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Eq: ${pid.equation}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Switch(
                checked = pid.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RacingOrange,
                    checkedTrackColor = Color(0xFF5A2A04),
                    uncheckedThumbColor = Color(0xFF6B7280),
                    uncheckedTrackColor = Color(0xFF1F2937)
                )
            )
        }
    }
}
