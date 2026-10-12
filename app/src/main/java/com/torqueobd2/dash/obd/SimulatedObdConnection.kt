package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionStatus
import com.torqueobd2.dash.data.parser.EquationEvaluator
import com.torqueobd2.dash.i18n.tr
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

    private val _statusMessage = MutableStateFlow(tr("Simulador detenido", "Simulator stopped"))
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
        _statusMessage.value = tr("Iniciando simulador ELM327...", "Starting ELM327 simulator...")
        delay(150)
        _status.value = ConnectionStatus.INITIALIZING_ELM
        _statusMessage.value = tr("Configurando protocolos AT...", "Configuring AT protocols...")
        delay(150)
        _status.value = ConnectionStatus.CONNECTED
        _statusMessage.value = tr(
            "Simulador conectado (Honda i-DTEC 2.2 / OBD-II)",
            "Simulator connected (Honda i-DTEC 2.2 / OBD-II)"
        )
        return true
    }

    override suspend fun disconnect() {
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = tr("Simulador desconectado", "Simulator disconnected")
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
                cmd == "AT RV" -> "${String.format(java.util.Locale.US, "%.1f", batteryVolts)}V\r\r>"
                else -> "OK\r\r>"
            }
        }

        // PIDs OBD-II estándar (Modo 01)
        if (cmd.startsWith("01")) {
            val pid = cmd.substring(2)
            fun hex16(v: Int): String {
                val clamped = v.coerceIn(0, 65535)
                return String.format("%02X %02X", (clamped shr 8) and 0xFF, clamped and 0xFF)
            }
            return when (pid) {
                "00" -> "41 00 BE 3E B8 13\r\r>" // PIDs soportados 01-20
                "02" -> "41 02 03 00\r\r>"
                "03" -> "41 03 02 00\r\r>"
                "04" -> { // Carga motor: A*100/255
                    val a = ((engineLoad * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 04 %02X\r\r>", a)
                }
                "05" -> { // Refrigerante: A-40
                    val a = (coolantTemp + 40).toInt().coerceIn(0, 255)
                    String.format("41 05 %02X\r\r>", a)
                }
                "06", "08" -> String.format("41 %s %02X\r\r>", pid, (128 + (throttlePedal * 0.08f).toInt()).coerceIn(0, 255))
                "07", "09" -> String.format("41 %s 82\r\r>", pid)
                "0A" -> "41 0A 78\r\r>"
                "0B" -> { // MAP en kPa (100 kPa atm + boost)
                    val mapKpa = (101.3f + boostBar * 100f).toInt().coerceIn(0, 255)
                    String.format("41 0B %02X\r\r>", mapKpa)
                }
                "0C" -> { // RPM: ((A*256)+B)/4
                    val rpmVal = (currentRpm * 4).toInt()
                    "41 0C ${hex16(rpmVal)}\r\r>"
                }
                "0D" -> { // Velocidad km/h: A
                    val speedVal = currentSpeed.toInt().coerceIn(0, 255)
                    String.format("41 0D %02X\r\r>", speedVal)
                }
                "0E" -> { // Avance encendido: A/2-64
                    val adv = ((12f + throttlePedal * 0.25f + 64f) * 2f).toInt().coerceIn(0, 255)
                    String.format("41 0E %02X\r\r>", adv)
                }
                "0F" -> { // Temp admisión: A-40
                    val a = (intakeTemp + 40).toInt().coerceIn(0, 255)
                    String.format("41 0F %02X\r\r>", a)
                }
                "10" -> { // MAF g/s: ((A*256)+B)/100
                    val mafVal = ((dpfFlowM3h * 0.35f) * 100f).toInt().coerceIn(0, 65535)
                    "41 10 ${hex16(mafVal)}\r\r>"
                }
                "11" -> { // Acelerador: A*100/255
                    val a = ((throttlePedal * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 11 %02X\r\r>", a)
                }
                "12" -> "41 12 04\r\r>"
                "13" -> "41 13 03\r\r>"
                "14", "15", "16", "17", "18", "19", "1A", "1B" -> "41 $pid 96 80\r\r>"
                "1C" -> "41 1C 06\r\r>"
                "1D" -> "41 1D 03\r\r>"
                "1E" -> "41 1E 00\r\r>"
                "1F" -> "41 1F ${hex16(dpfTimeSec.toInt())}\r\r>"
                "20" -> "41 20 B0 1F A0 15\r\r>" // PIDs soportados 21-40
                "21" -> "41 21 00 00\r\r>"
                "22" -> "41 22 0F A0\r\r>"
                "23" -> { // Rail diésel: INT16(A:B)*10 kPa
                    val railRaw = (3500f + throttlePedal * 145f).toInt().coerceIn(0, 65535)
                    "41 23 ${hex16(railRaw)}\r\r>"
                }
                "24", "25", "26", "27", "28", "29", "2A", "2B" -> "41 $pid 80 00 40 00\r\r>"
                "2C" -> {
                    val egr = ((if (throttlePedal < 40f) 35f else 5f) * 255f / 100f).toInt().coerceIn(0, 255)
                    String.format("41 2C %02X\r\r>", egr)
                }
                "2D" -> "41 2D 80\r\r>"
                "2E" -> "41 2E 40\r\r>"
                "2F" -> { // Nivel combustible %: A*100/255
                    val a = ((fuelLevel * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 2F %02X\r\r>", a)
                }
                "30" -> "41 30 2A\r\r>"
                "31" -> "41 31 12 C0\r\r>"
                "32" -> "41 32 00 40\r\r>"
                "33" -> "41 33 65\r\r>"
                "34", "35", "36", "37", "38", "39", "3A", "3B" -> "41 $pid 80 00 80 00\r\r>"
                "3C", "3D", "3E", "3F" -> {
                    val catRaw = ((egtTemp + 40f) * 10f).toInt()
                    "41 $pid ${hex16(catRaw)}\r\r>"
                }
                "40" -> "41 40 7E DC 9F 0D\r\r>" // PIDs soportados 41-60
                "41" -> "41 41 00 07 69 00\r\r>"
                "42" -> { // Voltaje ECU: ((A*256)+B)/1000
                    val v = (batteryVolts * 1000f).toInt()
                    "41 42 ${hex16(v)}\r\r>"
                }
                "43" -> "41 43 ${hex16(((engineLoad * 255f) / 100f).toInt())}\r\r>"
                "44" -> "41 44 80 00\r\r>"
                "45", "47", "48", "49", "4A", "4B", "4C", "5A", "8D" -> {
                    val a = ((throttlePedal * 255) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 %s %02X\r\r>", pid, a)
                }
                "46" -> "41 46 3D\r\r>" // 21 °C ambiente
                "4D", "4E" -> "41 $pid 00 3C\r\r>"
                "4F" -> "41 4F 02 08 80 1A\r\r>"
                "50" -> "41 50 28 00 00 00\r\r>"
                "51" -> "41 51 04\r\r>" // Diésel
                "52" -> "41 52 00\r\r>"
                "53" -> "41 53 4E 20\r\r>"
                "54" -> "41 54 80 00\r\r>"
                "55", "56", "57", "58" -> "41 $pid 80 80\r\r>"
                "59" -> "41 59 ${hex16((3600f + throttlePedal * 145f).toInt())}\r\r>"
                "5B" -> "41 5B B3\r\r>"
                "5C" -> { // Temp aceite: A-40
                    val oilTemp = (coolantTemp + 6f + 40f).toInt().coerceIn(0, 255)
                    String.format("41 5C %02X\r\r>", oilTemp)
                }
                "5D" -> "41 5D ${hex16(((2.5f + 210f) * 128f).toInt())}\r\r>"
                "5E" -> { // Consumo L/h: ((A*256)+B)/20
                    val lH = (0.8f + (throttlePedal / 100f) * 14f) * 20f
                    "41 5E ${hex16(lH.toInt())}\r\r>"
                }
                "5F" -> "41 5F 12\r\r>" // Euro VI B
                "60" -> "41 60 FF FF FF 03\r\r>" // PIDs soportados 61-80
                "61" -> String.format("41 61 %02X\r\r>", (125 + (throttlePedal * 0.9f).toInt()).coerceIn(0, 255))
                "62" -> String.format("41 62 %02X\r\r>", (125 + (engineLoad * 0.85f).toInt()).coerceIn(0, 255))
                "63" -> "41 63 01 5E\r\r>" // 350 Nm par de referencia
                "64" -> "41 64 82 96 B4 C8 BE\r\r>"
                "65" -> {
                    val recGear = (currentGear.coerceIn(1, 6) shl 4) or (if (currentSpeed < 1f) 4 else 2)
                    String.format("41 65 1F %02X\r\r>", recGear)
                }
                "66" -> {
                    val mafRaw = ((dpfFlowM3h * 0.35f) * 32f).toInt()
                    "41 66 01 ${hex16(mafRaw)} 00 00\r\r>"
                }
                "67" -> {
                    val c1 = (coolantTemp + 40f).toInt().coerceIn(0, 255)
                    val c2 = (coolantTemp + 38f).toInt().coerceIn(0, 255)
                    String.format("41 67 03 %02X %02X\r\r>", c1, c2)
                }
                "68" -> {
                    val i1 = (intakeTemp + 40f).toInt().coerceIn(0, 255)
                    val i2 = (intakeTemp + 43f).toInt().coerceIn(0, 255)
                    String.format("41 68 03 %02X %02X 00 00 00 00\r\r>", i1, i2)
                }
                "69" -> "41 69 07 40 3F 80 00 00 00\r\r>"
                "6A" -> "41 6A 03 C8 C6 00 00\r\r>"
                "6B" -> "41 6B 01 87 00 00 00 00 00\r\r>"
                "6C", "72" -> {
                    val pos = ((throttlePedal * 255f) / 100f).toInt().coerceIn(0, 255)
                    String.format("41 %s 03 %02X %02X 00 00\r\r>", pid, pos, pos)
                }
                "6D" -> {
                    val railT = (3500f + throttlePedal * 145f).toInt()
                    val railA = (3480f + throttlePedal * 145f).toInt()
                    "41 6D 07 ${hex16(railT)} ${hex16(railA)} 5F 00 00 00 00 00\r\r>"
                }
                "6E" -> {
                    val injP = (3500f + throttlePedal * 145f).toInt()
                    "41 6E 03 ${hex16(injP)} ${hex16(injP)} 00 00 00 00\r\r>"
                }
                "6F" -> "41 6F 01 65 00\r\r>"
                "70" -> {
                    val boostT = ((101.3f + boostBar * 100f) * 32f).toInt()
                    val boostA = ((100.5f + boostBar * 100f) * 32f).toInt()
                    "41 70 03 ${hex16(boostT)} ${hex16(boostA)} 00 00 00 00\r\r>"
                }
                "71" -> {
                    val vgt = ((30f + throttlePedal * 0.6f) * 255f / 100f).toInt().coerceIn(0, 255)
                    String.format("41 71 03 %02X %02X 00 00 00 00\r\r>", vgt, vgt)
                }
                "73" -> {
                    val exhP = ((101.3f + dpfDiffPresKpa) * 100f).toInt()
                    "41 73 01 ${hex16(exhP)} 00 00\r\r>"
                }
                "74" -> {
                    val turboRpm = (2500f + boostBar * 8500f).toInt()
                    "41 74 01 ${hex16(turboRpm)} 00 00\r\r>"
                }
                "75", "76" -> {
                    val cIn = (intakeTemp + 40f).toInt().coerceIn(0, 255)
                    val cOut = (intakeTemp + 68f).toInt().coerceIn(0, 255)
                    val tIn = ((egtTemp + 40f) * 10f).toInt()
                    val tOut = ((egtTemp - 35f + 40f) * 10f).toInt()
                    String.format("41 %s 0F %02X %02X %s %s\r\r>", pid, cIn, cOut, hex16(tIn), hex16(tOut))
                }
                "77" -> {
                    val cact1 = (intakeTemp + 58f).toInt().coerceIn(0, 255)
                    val cact2 = (intakeTemp + 42f).toInt().coerceIn(0, 255)
                    String.format("41 77 03 %02X %02X 00 00\r\r>", cact1, cact2)
                }
                "78", "79", "98", "99" -> {
                    val e1 = ((egtTemp + 40f) * 10f).toInt()
                    val e2 = ((egtTemp - 20f + 40f) * 10f).toInt()
                    val e3 = ((egtTemp - 45f + 40f) * 10f).toInt()
                    "41 $pid 07 ${hex16(e1)} ${hex16(e2)} ${hex16(e3)} 00 00\r\r>"
                }
                "7A", "7B" -> {
                    val diff = (dpfDiffPresKpa * 100f).toInt()
                    val inP = ((101.3f + dpfDiffPresKpa) * 100f).toInt()
                    val outP = (101.3f * 100f).toInt()
                    "41 $pid 07 ${hex16(diff)} ${hex16(inP)} ${hex16(outP)}\r\r>"
                }
                "7C" -> {
                    val tIn = ((egtTemp - 15f + 40f) * 10f).toInt()
                    val tOut = ((egtTemp - 40f + 40f) * 10f).toInt()
                    "41 7C 03 ${hex16(tIn)} ${hex16(tOut)} 00 00 00 00\r\r>"
                }
                "7D", "7E" -> "41 $pid 01\r\r>"
                "7F" -> "41 7F 03 00 05 7E 40 00 01 19 40 00 00 00 00\r\r>"
                "80" -> "41 80 3F A3 3E 05\r\r>" // PIDs soportados 81-A0
                "83", "A1", "A7", "A8" -> "41 $pid 03 00 78 00 19 00 00 00 00\r\r>"
                "84" -> "41 84 5A\r\r>"
                "85" -> "41 85 0F 00 FA 00 F0 BF 00 01 E8 48\r\r>"
                "86" -> "41 86 01 00 B4 00 00\r\r>"
                "87" -> {
                    val mapHi = ((101.3f + boostBar * 100f) * 32f).toInt()
                    "41 87 01 ${hex16(mapHi)} 00 00\r\r>"
                }
                "88" -> "41 88 00 00 00 00 00 00 00 00\r\r>"
                "8E" -> "41 8E 8A\r\r>"
                "8F" -> "41 8F 03 01 09 C4 00 00 00\r\r>"
                "9A" -> "41 9A 0F C0 57 80 00 64 00\r\r>"
                "9B" -> "41 9B 03 44 82\r\r>"
                "9C" -> "41 9C 03 80 00 80 00 00 00 00 00 00 00 00 00 00 00 00 00\r\r>"
                "9D" -> "41 9D 00 78 00 78\r\r>"
                "9E" -> "41 9E ${hex16((dpfFlowM3h * 6f).toInt())}\r\r>"
                "9F" -> "41 9F 01 FF 00 00 00 00 00 00 00\r\r>"
                "A0" -> "41 A0 FC C0 40 01\r\r>" // PIDs soportados A1-C0
                "A2" -> "41 A2 ${hex16(((8f + throttlePedal * 0.45f) * 32f).toInt())}\r\r>"
                "A3" -> "41 A3 01 00 40 00 00 00 00 00 00\r\r>"
                "A4" -> {
                    val gearByte = (currentGear.coerceIn(0, 6) shl 4)
                    val ratioRaw = (3800 / currentGear.coerceAtLeast(1)).coerceIn(500, 5000)
                    String.format("41 A4 03 %02X %s\r\r>", gearByte, hex16(ratioRaw))
                }
                "A5" -> "41 A5 03 20 00 14\r\r>"
                "A6" -> "41 A6 00 13 24 B8\r\r>"
                "A9" -> "41 A9 01 00\r\r>"
                "AA" -> "41 AA FA\r\r>"
                "B2" -> "41 B2 F2\r\r>"
                "C0" -> "41 C0 3E 00 60 00\r\r>" // PIDs soportados C1-E0
                "C3" -> "41 C3 01 30 D4 00 00\r\r>"
                "C4" -> "41 C4 03 00 00 00 00 00 00 00 00\r\r>"
                "C5" -> "41 C5 01 01 90 00 00\r\r>"
                "C6" -> "41 C6 0E 00 61 A8 02 1C 00 28\r\r>"
                "C7" -> "41 C7 00 09 92 5C\r\r>"
                "D2" -> "41 D2 03 D9 CC\r\r>"
                "D3" -> "41 D3 00 13 24 B8\r\r>"
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
