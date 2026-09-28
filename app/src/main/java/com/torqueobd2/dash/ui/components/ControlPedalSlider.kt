package com.torqueobd2.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.torqueobd2.dash.ui.theme.CardBackground
import com.torqueobd2.dash.ui.theme.NeonGreen
import com.torqueobd2.dash.ui.theme.RacingOrange
import com.torqueobd2.dash.ui.theme.TextMuted
import com.torqueobd2.dash.ui.theme.TextWhite

/**
 * Control interactivo para el modo de simulación que permite acelerar el vehículo manualmente
 * o activar el ciclo de conducción inteligente automático.
 */
@Composable
fun ControlPedalSlider(
    onThrottleChanged: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isManualMode by remember { mutableStateOf(false) }
    var throttleValue by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, Color(0xFF28303D), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PEDAL VIRTUAL DE ACELERACIÓN",
                    color = RacingOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isManualMode) "Modo Manual Interactivo" else "Ciclo de Conducción Automático",
                    color = if (isManualMode) TextWhite else NeonGreen,
                    fontSize = 11.sp
                )
            }

            FilledTonalButton(
                onClick = {
                    isManualMode = !isManualMode
                    if (isManualMode) {
                        onThrottleChanged(throttleValue)
                    } else {
                        onThrottleChanged(null) // Automático
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isManualMode) RacingOrange else Color(0xFF232A36),
                    contentColor = if (isManualMode) Color.Black else TextWhite
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isManualMode) "Pasar a Auto" else "Control Manual",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isManualMode) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = throttleValue,
                    onValueChange = {
                        throttleValue = it
                        onThrottleChanged(it)
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = RacingOrange,
                        activeTrackColor = RacingOrange,
                        inactiveTrackColor = Color(0xFF2B3340)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${throttleValue.toInt()}%",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
