package com.torqueobd2.dash.ui.components

import com.torqueobd2.dash.ui.automotive.CarText as Text
import com.torqueobd2.dash.ui.automotive.CarTextButton as TextButton
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torqueobd2.dash.ui.theme.*

@Composable
fun ControlPedalSlider(onThrottleChanged: (Float?) -> Unit, modifier: Modifier = Modifier,
    onExpandedChanged: (Boolean) -> Unit = {}) {
    var manual by rememberSaveable { mutableStateOf(false) }
    var throttle by rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(manual) { onExpandedChanged(manual) }
    Column(modifier.fillMaxWidth().racingPanel(12.dp).padding(horizontal = 12.dp, vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(com.torqueobd2.dash.i18n.tr("Simulador de conducción", "Driving simulator"), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (manual) com.torqueobd2.dash.i18n.tr("Acelerador manual", "Manual throttle") else com.torqueobd2.dash.i18n.tr("Ciclo automático", "Automatic cycle"), color = TextMuted, fontSize = 10.sp)
            }
            TextButton(onClick = { manual = !manual; onThrottleChanged(if (manual) throttle else null) }) {
                Text(if (manual) com.torqueobd2.dash.i18n.tr("Usar automático", "Use automatic") else com.torqueobd2.dash.i18n.tr("Control manual", "Manual control"), color = RacingOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (manual) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Slider(throttle, onValueChange = { throttle = it; onThrottleChanged(it) }, valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = RacingOrange, activeTrackColor = RacingOrange, inactiveTrackColor = BorderDark),
                    modifier = Modifier.weight(1f))
                FittedValue("${throttle.toInt()}%", 16.sp, Modifier.width(45.dp), color = TextWhite)
            }
        }
    }
}
