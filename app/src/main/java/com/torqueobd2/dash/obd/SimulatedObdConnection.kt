package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.parser.EquationEvaluator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Conexión simulada que emula el comportamiento de un ELM327 conectado
 * a un motor diésel con turbo (como el 2.2 / 1.6 Honda i-DTEC).
 * Modela física real de aceleración, marchas, presión turbo, temperatura EGT,
 * y filtro de partículas DPF.
 */
class SimulatedObdConnection : ObdConnection {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow("Simulador detenido")
    override val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Estado físico del motor
    private var isEngineRunning = true
    private var manualThrottle: Float? = null // Si el usuario usa el pedal interactivo

    // Variables dinámicas
    var currentRpm = 850f
    var currentSpeed = 0f
    var currentGear = 1
    var coolantTemp = 86f
    var intakeTemp = 24f
    var boostBar = 0f
    var engineLoad = 15f
    var throttlePedal = 0f
    var fuelLevel = 62f
    var batteryVolts = 13.9f

    // Variables de escape y DPF de Honda iDTEC
    var dpfDistKm = 345f
    var dpfFuelL = 23.5f
    var dpfTimeSec = 19200f
    var dpfDiffPresKpa = 1.3f
    var egtTemp = 280f
    var calcPmGrams = 14.8f
    var dpfRegenState = 0L
    var dpfActiveRegen = 0L
    var oilLevelMm = 98.4f
    var dpfFlowM3h = 120f

    private var simulationTick = 0L
    private var diagnosticHeaders = false
    private var demoCleared = false

    override suspend fun connect(): Boolean {
        _status.value = ConnectionStatus.CONNECTING
        _statusMessage.value = "Iniciando simulador ELM327..."
        delay(150)
        _status.value = ConnectionStatus.INITIALIZING_ELM
        _statusMessage.value = "Configurando protocolos AT..."
        delay(150)
        _status.value = ConnectionStatus.CONNECTED
        _statusMessage.value = "Simulador conectado (Honda i-DTEC 2.2 / OBD-II)"
        return true
    }

    override suspend fun disconnect() {
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Simulador desconectado"
    }

    override fun isConnected(): Boolean {
        return _status.value == ConnectionStatus.CONNECTED
    }

    /**
     * Permite fijar un nivel de acelerador manual desde la UI (0..100)
     */
    fun setManualThrottle(percent: Float?) {
        manualThrottle = percent?.coerceIn(0f, 100f)
    }

    private var lastPhysicsUpdateTime = System.currentTimeMillis()

    /**
     * Avanza un paso en la simulación de física automotriz en tiempo real
     */
    fun updatePhysics() {
        val now = System.currentTimeMillis()
        val dtMillis = (now - lastPhysicsUpdateTime).coerceIn(0L, 500L)
        if (dtMillis < 35L) {
            // Evita avanzar la física múltiples veces durante una misma ráfaga de PIDs por ciclo de polling
            return
        }
        lastPhysicsUpdateTime = now
        val dt = (dtMillis / 1000f).coerceIn(0.01f, 0.1f) // segundos reales transcurridos

        simulationTick++
        val t = now / 1000.0 // tiempo real en segundos

        val targetThrottle = manualThrottle ?: run {
            // Ciclo de conducción suave de 24 segundos:
            // 0s-3s: Parado al ralentí (0 km/h, 820 RPM)
            // 3s-14s: Aceleración progresiva por marchas
            // 14s-19s: Crucero a velocidad constante
            // 19s-24s: Desaceleración suave
            val cycleTime = (t % 24.0)
            when {
                cycleTime < 3.0 -> 0f
                cycleTime < 14.0 -> {
                    val p = ((cycleTime - 3.0) / 11.0).toFloat()
                    (p * 55f + 15f).coerceIn(15f, 70f)
                }
                cycleTime < 19.0 -> 25f
                else -> 0f
            }
        }

        // Suavizado del pedal
        throttlePedal += (targetThrottle - throttlePedal) * (3f * dt)
        engineLoad = 15f + throttlePedal * 0.75f

        // Dinámica de marchas y RPM realista
        val gearRatios = floatArrayOf(0f, 7.8f, 14.2f, 22.0f, 31.5f, 41.0f, 52.0f)
        if (throttlePedal > 10f) {
            // Acelerando
            val rpmGain = (throttlePedal * 25f) * dt
            currentRpm += rpmGain
            if (currentRpm > 3800f && currentGear < 6) {
                currentGear++
                currentRpm = (currentRpm * 0.68f).coerceAtLeast(1800f)
            } else if (currentRpm > 4500f) {
                currentRpm = 4500f
            }
        } else {
            // Desacelerando
            currentRpm -= 600f * dt
            if (currentRpm < 1400f && currentGear > 1) {
                currentGear--
                currentRpm = (currentRpm * 1.35f).coerceAtMost(2600f)
            }
            if (currentRpm < 820f) {
                currentRpm = 820f
                if (currentGear == 1) currentSpeed = 0f
            }
        }

        val ratio = gearRatios.getOrElse(currentGear) { 20f }
        val targetSpeed = if (currentRpm <= 850f && currentGear <= 1 && throttlePedal < 5f) {
            0f
        } else {
            (currentRpm / 1000f) * ratio
        }
        currentSpeed += (targetSpeed - currentSpeed) * (2f * dt)
        if (currentSpeed < 0.2f) currentSpeed = 0f

        // Presión de turbo (boost relativo) en bar: 0 a 1.6 bar
        val targetBoost = if (throttlePedal > 15f) (throttlePedal / 100f) * 1.55f else 0.05f
        boostBar += (targetBoost - boostBar) * (3f * dt)

        // Temperatura de escape (EGT): 220°C a 680°C según carga y turbo
        val targetEgt = 220f + engineLoad * 4.2f + boostBar * 60f
        egtTemp += (targetEgt - egtTemp) * (1.5f * dt)

        // Presión diferencial DPF: depende del caudal de escape y saturación de hollín
        val targetFlow = 60f + (currentRpm / 1000f) * 55f + boostBar * 80f
        dpfFlowM3h += (targetFlow - dpfFlowM3h) * (2f * dt)

        val sootFactor = (calcPmGrams / 30f).coerceIn(0.5f, 2.5f)
        val targetDiffPres = (dpfFlowM3h / 150f) * 2.8f * sootFactor + (boostBar * 1.5f)
        dpfDiffPresKpa += (targetDiffPres - dpfDiffPresKpa) * (2f * dt)

        // Simulación de avance de distancia y PM
        dpfDistKm += (currentSpeed / 3600f) * dt
        dpfTimeSec += dt
        calcPmGrams += 0.0001f * dt

        // Nivel de aceite estable
        oilLevelMm = 98.2f + sin(t * 0.05).toFloat() * 0.3f
    }

    override suspend fun sendCommand(command: String): String {
        updatePhysics()
        val cmd = command.trim().uppercase()

        val compact = cmd.replace(" ", "")
        if (compact == "ATH1") diagnosticHeaders = true
        if (compact == "ATH0" || compact == "ATD") diagnosticHeaders = false
        if (compact == "ATDPN") return "7\r>"
        fun diagnostic(payload: String): String {
            val bytes = payload.split(" ")
            return if (diagnosticHeaders) "18DAF110 %02X %s\r>".format(bytes.size, payload) else "$payload\r>"
        }
        when (compact) {
            "0101" -> return diagnostic(if (demoCleared) "41 01 00 78 69 69" else "41 01 82 08 69 00")
            "03" -> return diagnostic(if (demoCleared) "43 00" else "43 02 03 00 01 33")
            "07" -> return diagnostic(if (demoCleared) "47 00" else "47 01 01 71")
            "0A" -> return diagnostic("4A 01 04 20")
            "04" -> { demoCleared = true; return diagnostic("44") }
            "020200" -> return diagnostic(if (demoCleared) "42 02 00 00 00" else "42 02 00 03 00")
            "020000" -> return diagnostic("42 00 00 08 18 00 00") // Refrigerante, RPM y velocidad.
            "020500" -> return diagnostic("42 05 00 7E")
            "020C00" -> return diagnostic("42 0C 00 0C 80")
            "020D00" -> return diagnostic("42 0D 00 00")
        }
        // Comandos de inicialización AT
        if (cmd.startsWith("AT")) {
            return when {
                cmd == "AT Z" || cmd == "AT WS" -> "ELM327 v1.5\r\r>"
                cmd == "AT E0" || cmd == "AT L0" || cmd == "AT S0" || cmd == "AT H0" || cmd == "AT H1" -> "OK\r\r>"
                cmd == "AT SP 0" || cmd == "AT SP0" || cmd.startsWith("AT SP") -> "OK\r\r>"
                cmd.startsWith("AT SH") -> "OK\r\r>" // Set Header
                cmd == "AT I" || cmd == "ATI" -> "ELM327 v1.5\r\r>"
                cmd == "AT DP" -> "ISO 15765-4 (CAN 29/500)\r\r>"
                cmd == "AT RV" -> "${String.format("%.1f", batteryVolts)}V\r\r>"
                else -> "OK\r\r>"
            }
        }

        // PIDs OBD-II estándar (Modo 01)
        if (cmd.startsWith("01")) {
            val pid = cmd.substring(2)
            return when (pid) {
                "00" -> "41 00 BE 3E B8 11\r\r>" // PIDs soportados 01-20
                "0C" -> { // RPM: ((A*256)+B)/4
                    val rpmVal = (currentRpm * 4).toInt()
                    val a = (rpmVal shr 8) and 0xFF
                    val b = rpmVal and 0xFF
                    String.format("41 0C %02X %02X\r\r>", a, b)
                }
                "0D" -> { // Velocidad km/h: A
                    val speedVal = currentSpeed.toInt().coerceIn(0, 255)
                    String.format("41 0D %02X\r\r>", speedVal)
                }
                "05" -> { // Refrigerante: A-40
                    val a = (coolantTemp + 40).toInt().coerceIn(0, 255)
                    String.format("41 05 %02X\r\r>", a)
                }
                "04" -> { // Carga motor: A*100/255
                    val a = ((engineLoad * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 04 %02X\r\r>", a)
                }
                "11" -> { // Acelerador: A*100/255
                    val a = ((throttlePedal * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 11 %02X\r\r>", a)
                }
                "0B" -> { // MAP en kPa (100 kPa atm + boost)
                    val mapKpa = (101.3f + boostBar * 100f).toInt().coerceIn(0, 255)
                    String.format("41 0B %02X\r\r>", mapKpa)
                }
                "0F" -> { // Temp admisión: A-40
                    val a = (intakeTemp + 40).toInt().coerceIn(0, 255)
                    String.format("41 0F %02X\r\r>", a)
                }
                "10" -> { // MAF g/s: ((A*256)+B)/100
                    val mafVal = ((dpfFlowM3h * 0.35f) * 100f).toInt().coerceIn(0, 65535)
                    val a = (mafVal shr 8) and 0xFF
                    val b = mafVal and 0xFF
                    String.format("41 10 %02X %02X\r\r>", a, b)
                }
                "2F" -> { // Nivel combustible %: A*100/255
                    val a = ((fuelLevel * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 2F %02X\r\r>", a)
                }
                "42" -> { // Voltaje ECU: ((A*256)+B)/1000
                    val v = (batteryVolts * 1000f).toInt()
                    val a = (v shr 8) and 0xFF
                    val b = v and 0xFF
                    String.format("41 42 %02X %02X\r\r>", a, b)
                }
                "5E" -> { // Consumo L/h: ((A*256)+B)/20
                    val lH = (0.8f + (throttlePedal / 100f) * 14f) * 20f
                    val v = lH.toInt()
                    val a = (v shr 8) and 0xFF
                    val b = v and 0xFF
                    String.format("41 5E %02X %02X\r\r>", a, b)
                }
                else -> "41 $pid 00\r\r>"
            }
        }

        // PIDs específicos Honda iDTEC (Modo 22)
        val cleanCmd = cmd.replace("0X", "")
        if (cleanCmd == "222512") {
            // "0x222512": contiene DPF dist, DPF fuel, DPF time, DPF diff pres, EGT 1, PM value, regen state, active reg
            // Construimos un array de 45 bytes para satisfacer todas las variables:
            // A=0, B=1, ... O=14 (EGT=O*4), T=19 (PM=T*0.3), V=21, W=22 (dist=INT16(V:W)/10),
            // X=23, Y=24 (fuel=INT16(X:Y)/100), Z=25, AA=26 (time=INT16(Z:AA)*60),
            // AB=27, AC=28, AH=33, AI=34 (diff.pres=INT16(AH:AI)/100),
            // AM=38, AN=39, AO=40, AP=41 (reg.state=INT32(AM:AN:AO:AP))
            val raw = IntArray(45)

            // EGT: O * 4 = egtTemp -> O = egtTemp / 4
            raw[EquationEvaluator.variableNameToIndex("O")] = (egtTemp / 4f).toInt().coerceIn(0, 255)

            // PM: T * 0.3 = calcPmGrams -> T = calcPmGrams / 0.3
            raw[EquationEvaluator.variableNameToIndex("T")] = (calcPmGrams / 0.3f).toInt().coerceIn(0, 255)

            // DPF dist: INT16(V:W)/10 = dpfDistKm -> INT16(V:W) = dpfDistKm * 10
            val dist16 = (dpfDistKm * 10f).toInt().coerceIn(0, 65535)
            raw[EquationEvaluator.variableNameToIndex("V")] = (dist16 shr 8) and 0xFF
            raw[EquationEvaluator.variableNameToIndex("W")] = dist16 and 0xFF

            // DPF fuel: INT16(X:Y)/100 = dpfFuelL -> INT16 = dpfFuelL * 100
            val fuel16 = (dpfFuelL * 100f).toInt().coerceIn(0, 65535)
            raw[EquationEvaluator.variableNameToIndex("X")] = (fuel16 shr 8) and 0xFF
            raw[EquationEvaluator.variableNameToIndex("Y")] = fuel16 and 0xFF

            // DPF time: INT16(Z:AA)*60 = dpfTimeSec -> INT16 = dpfTimeSec / 60
            val time16 = (dpfTimeSec / 60f).toInt().coerceIn(0, 65535)
            raw[EquationEvaluator.variableNameToIndex("Z")] = (time16 shr 8) and 0xFF
            raw[EquationEvaluator.variableNameToIndex("AA")] = time16 and 0xFF

            // DPF diff pres: INT16(AH:AI)/100 = dpfDiffPresKpa -> INT16 = dpfDiffPresKpa * 100
            val diff16 = (dpfDiffPresKpa * 100f).toInt().coerceIn(0, 65535)
            raw[EquationEvaluator.variableNameToIndex("AH")] = (diff16 shr 8) and 0xFF
            raw[EquationEvaluator.variableNameToIndex("AI")] = diff16 and 0xFF

            // DPF reg status & active
            raw[EquationEvaluator.variableNameToIndex("AM")] = 0
            raw[EquationEvaluator.variableNameToIndex("AN")] = 0
            raw[EquationEvaluator.variableNameToIndex("AO")] = 0
            raw[EquationEvaluator.variableNameToIndex("AP")] = if (calcPmGrams > 22f) 1 else 0

            val sb = StringBuilder("62 25 12 ")
            for (b in raw) {
                sb.append(String.format("%02X ", b))
            }
            sb.append("\r\r>")
            return sb.toString()
        }

        if (cleanCmd == "222510") {
            // "iDTEC Engine oil level (filtered)": "AS/2", ModeAndPID "0x222510"
            // AS es la variable para el byte del nivel de aceite: AS = oilLevelMm * 2
            val raw = IntArray(46)
            val asIdx = EquationEvaluator.variableNameToIndex("AS")
            raw[asIdx] = (oilLevelMm * 2f).toInt().coerceIn(0, 255)

            val sb = StringBuilder("62 25 10 ")
            for (b in raw) {
                sb.append(String.format("%02X ", b))
            }
            sb.append("\r\r>")
            return sb.toString()
        }

        if (cleanCmd == "222516") {
            // "iDTEC DPF Exhaust gas flow (calculated) ?": "INT16(AB:AC)/10", ModeAndPID "0x222516"
            val raw = IntArray(30)
            val flow16 = (dpfFlowM3h * 10f).toInt().coerceIn(0, 65535)
            val abIdx = EquationEvaluator.variableNameToIndex("AB")
            val acIdx = EquationEvaluator.variableNameToIndex("AC")
            raw[abIdx] = (flow16 shr 8) and 0xFF
            raw[acIdx] = flow16 and 0xFF

            val sb = StringBuilder("62 25 16 ")
            for (b in raw) {
                sb.append(String.format("%02X ", b))
            }
            sb.append("\r\r>")
            return sb.toString()
        }

        return "NO DATA\r\r>"
    }
}
