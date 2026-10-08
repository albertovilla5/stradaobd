package com.torqueobd2.dash.data.repository

import java.io.Reader

/** Definiciones genéricas offline; los códigos propietarios no se asignan a una marca por su nombre. */
class DtcCatalog(private val definitions: Map<String, String>) {
    data class Description(val text: String, val source: String)
    fun describe(code: String): Description {
        spanish[code]?.let { return Description(it, "Definición genérica · Español") }
        definitions[code]?.let { return Description(it, "Catálogo genérico · Inglés") }
        return Description("Definición no incluida. Consulta la documentación del fabricante para este código y esta ECU.", "Sin definición verificada")
    }
    companion object {
        private fun isGeneric(code: String): Boolean = when (code.firstOrNull()) {
            'P' -> code.getOrNull(1) in listOf('0', '2') || (code.startsWith("P3") && code.substring(2).toInt(16) >= 0x400)
            'B', 'C', 'U' -> code.getOrNull(1) in listOf('0', '3')
            else -> false
        }
        fun read(reader: Reader): DtcCatalog = DtcCatalog(reader.buffered().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split('\t', limit = 2)
                if (parts.size == 2 && parts[0].matches(Regex("[PCBU][0-3][0-9A-F]{3}")) && isGeneric(parts[0]) && parts[1].isNotBlank()) parts[0] to parts[1] else null
            }.toMap()
        })
        private val spanish = mapOf(
            "P0100" to "Circuito del sensor de caudal de aire (MAF)",
            "P0101" to "Caudal de aire (MAF): rango o funcionamiento",
            "P0102" to "Señal baja del circuito MAF", "P0103" to "Señal alta del circuito MAF",
            "P0105" to "Circuito de presión absoluta del colector (MAP)",
            "P0106" to "Presión MAP: rango o funcionamiento", "P0107" to "Señal baja del circuito MAP",
            "P0108" to "Señal alta del circuito MAP", "P0110" to "Circuito de temperatura del aire de admisión",
            "P0115" to "Circuito de temperatura del refrigerante", "P0116" to "Temperatura del refrigerante: rango o funcionamiento",
            "P0117" to "Señal baja del circuito de temperatura del refrigerante", "P0118" to "Señal alta del circuito de temperatura del refrigerante",
            "P0120" to "Circuito del sensor de posición de mariposa/pedal A", "P0128" to "Refrigerante por debajo de la temperatura de regulación del termostato",
            "P0130" to "Circuito de sonda de oxígeno: banco 1, sensor 1", "P0131" to "Señal baja de sonda de oxígeno: banco 1, sensor 1",
            "P0132" to "Señal alta de sonda de oxígeno: banco 1, sensor 1", "P0133" to "Respuesta lenta de sonda de oxígeno: banco 1, sensor 1",
            "P0134" to "Sin actividad de sonda de oxígeno: banco 1, sensor 1", "P0135" to "Circuito calefactor de sonda de oxígeno: banco 1, sensor 1",
            "P0141" to "Circuito calefactor de sonda de oxígeno: banco 1, sensor 2",
            "P0171" to "Mezcla demasiado pobre: banco 1", "P0172" to "Mezcla demasiado rica: banco 1",
            "P0174" to "Mezcla demasiado pobre: banco 2", "P0175" to "Mezcla demasiado rica: banco 2",
            "P0190" to "Circuito del sensor de presión de la rampa de combustible", "P0191" to "Presión de combustible: rango o funcionamiento",
            "P0200" to "Circuito de los inyectores", "P0234" to "Presión de sobrealimentación excesiva",
            "P0299" to "Presión de sobrealimentación insuficiente", "P0300" to "Fallos de combustión aleatorios o en varios cilindros",
            "P0325" to "Circuito del sensor de detonación 1", "P0335" to "Circuito del sensor de posición del cigüeñal A",
            "P0340" to "Circuito del sensor de posición del árbol de levas A: banco 1 o sensor único",
            "P0400" to "Caudal de recirculación de gases de escape (EGR)", "P0401" to "Caudal EGR insuficiente",
            "P0402" to "Caudal EGR excesivo", "P0403" to "Circuito de control EGR",
            "P0420" to "Eficiencia del catalizador por debajo del umbral: banco 1",
            "P0430" to "Eficiencia del catalizador por debajo del umbral: banco 2",
            "P0440" to "Sistema de control de emisiones por evaporación (EVAP)", "P0442" to "Fuga pequeña en el sistema EVAP",
            "P0455" to "Fuga grande en el sistema EVAP", "P0456" to "Fuga muy pequeña en el sistema EVAP",
            "P0500" to "Sensor de velocidad del vehículo", "P0505" to "Sistema de control de ralentí",
            "P0560" to "Tensión del sistema", "P0562" to "Tensión del sistema baja", "P0563" to "Tensión del sistema alta",
            "P0600" to "Enlace de comunicación serie", "P0606" to "Procesador del módulo de control",
            "P0700" to "Sistema de control de la transmisión: solicitud de encendido del testigo",
            "P2002" to "Eficiencia del filtro de partículas por debajo del umbral: banco 1",
            "P2463" to "Restricción del filtro de partículas por acumulación de hollín",
            "U0100" to "Pérdida de comunicación con el módulo de control del motor/propulsión A",
            "U0101" to "Pérdida de comunicación con el módulo de control de la transmisión",
            "U0121" to "Pérdida de comunicación con el módulo de control ABS",
            "U0140" to "Pérdida de comunicación con el módulo de control de carrocería"
        ) + (1..12).associate { "P030$it".let { code -> if (it < 10) code else "P03%02d".format(it) } to "Fallo de combustión en el cilindro $it" }
    }
}
