package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory

/**
 * Campos SAE J1979, incluidos varios campos por respuesta. No genera estimaciones.
 * Referencias de escalas y bits: CSS Electronics y el catálogo publicado por OBDb/SAEJ1979.
 * https://www.csselectronics.com/pages/obd2-pid-table-on-board-diagnostics-j1979
 * https://github.com/OBDb/SAEJ1979/blob/main/signalsets/v3/default.json
 */
internal object StandardPidCatalog {
    fun additionalPids(): List<ObdPid> = buildList {
        fun sensor(pid: String, short: String, name: String, equation: String, unit: String,
                   min: Float = 0f, max: Float = 100f, category: PidCategory = PidCategory.ENGINE,
                   present: String = "", invalid: Map<String, Set<Int>> = emptyMap(),
                   labels: Map<Int, String> = emptyMap()) {
            add(ObdPid(id = "SAE_01${pid}_$short", name = name, shortName = short,
                modeAndPid = "01$pid", equation = equation, units = unit, minValue = min, maxValue = max,
                category = category, availabilityEquation = present, unavailableRawValues = invalid,
                valueLabels = labels))
        }
        val fuel = PidCategory.FUEL
        val oxygen = PidCategory.OXYGEN
        val exhaust = PidCategory.EXHAUST_DPF
        val diagnostic = PidCategory.DIAGNOSTICS
        val electrical = PidCategory.ELECTRICAL
        val transmission = PidCategory.TRANSMISSION
        val onOff = mapOf(0 to "Apagado", 1 to "Encendido")
        sensor("01", "MIL", "Testigo de avería motor comunicado por la ECU", "(A&128)/128", "", max = 1f, category = diagnostic, labels = onOff)
        sensor("01", "DTC confirmados", "Número de códigos de avería confirmados", "A&127", "códigos", max = 127f, category = diagnostic)
        // El nombre de banco no se deduce de la posición de la sonda: puede haber 2 o 4 bancos.
        val fuelStates = mapOf(1 to "Lazo abierto", 2 to "Lazo cerrado", 4 to "Abierto por carga",
            8 to "Abierto por fallo", 16 to "Cerrado con fallo")
        sensor("03", "Sistema fuel 1", "Estado del sistema de combustible 1 (ECU)", "A", "", max = 255f, category = fuel, labels = fuelStates)
        sensor("03", "Sistema fuel 2", "Estado del sistema de combustible 2 (ECU)", "B", "", max = 255f, category = fuel, labels = fuelStates)
        for ((pid, short, name) in listOf(
            Triple("06", "STFT B1", "Corrección de combustible a corto plazo, banco 1"),
            Triple("07", "LTFT B1", "Corrección de combustible a largo plazo, banco 1"),
            Triple("08", "STFT B2", "Corrección de combustible a corto plazo, banco 2"),
            Triple("09", "LTFT B2", "Corrección de combustible a largo plazo, banco 2"))) {
            sensor(pid, short, name, "(A-128)*100/128", "%", -100f, 99.21875f, fuel)
        }
        sensor("0A", "Presión fuel", "Presión relativa de combustible", "A*3", "kPa", max = 765f, category = fuel)
        sensor("0E", "Avance encendido", "Avance de encendido comunicado por la ECU", "A/2-64", "°", -64f, 63.5f)
        sensor("12", "Aire secundario", "Código de estado del aire secundario comandado", "A", "código", max = 255f, category = exhaust)
        for (index in 1..8) {
            val pid = "%02X".format(0x13 + index)
            sensor(pid, "O2 $index V", "Sonda de oxígeno $index: tensión", "A/200", "V", max = 1.275f, category = oxygen)
            // J1979: FF en el byte de trim significa que este campo no se utiliza.
            sensor(pid, "O2 $index STFT", "Sonda de oxígeno $index: corrección a corto plazo", "(B-128)*100/128", "%",
                -100f, 99.21875f, oxygen, invalid = mapOf("B" to setOf(255)))
        }
        sensor("1C", "Norma OBD", "Código de norma OBD declarada por la ECU", "A", "código", max = 255f, category = diagnostic)
        sensor("1F", "Tiempo motor", "Tiempo de funcionamiento desde arranque", "INT16(A:B)", "s", max = 65535f)
        sensor("21", "Distancia MIL", "Distancia recorrida con MIL encendido", "INT16(A:B)", "km", max = 65535f, category = diagnostic)
        sensor("22", "Rail relativo", "Presión de rail respecto a vacío del colector", "INT16(A:B)*0.079", "kPa", max = 5177.265f, category = fuel)
        sensor("23", "Rail diésel", "Presión relativa de rail (diésel / inyección directa)", "INT16(A:B)*10", "kPa", max = 655350f, category = fuel)
        for (index in 1..8) {
            val pid = "%02X".format(0x23 + index)
            sensor(pid, "O2 $index equiv V", "Sonda de oxígeno $index: relación de equivalencia (tensión)", "INT16(A:B)/32768", "ratio", max = 2f, category = oxygen)
            sensor(pid, "O2 $index banda V", "Sonda de oxígeno $index: tensión de banda ancha", "INT16(C:D)/8192", "V", max = 8f, category = oxygen)
        }
        sensor("2C", "EGR objetivo", "Apertura EGR comandada por la ECU", "A*100/255", "%", category = exhaust)
        sensor("2D", "Error EGR", "Error EGR comunicado por la ECU", "(A-128)*100/128", "%", -100f, 99.21875f, exhaust)
        sensor("2E", "Purga EVAP", "Purga evaporativa comandada", "A*100/255", "%", category = fuel)
        sensor("30", "Calentamientos", "Calentamientos desde borrado de averías", "A", "ciclos", max = 255f, category = diagnostic)
        sensor("31", "Distancia DTC", "Distancia desde borrado de averías", "INT16(A:B)", "km", max = 65535f, category = diagnostic)
        sensor("32", "Presión EVAP", "Presión de vapor EVAP (entero de 16 bits con signo)", "(SIGNED(A)*256+B)/4", "Pa", -8192f, 8191.75f, fuel)
        sensor("33", "Barométrica", "Presión barométrica absoluta comunicada por la ECU", "A", "kPa", max = 255f)
        for (index in 1..8) {
            val pid = "%02X".format(0x33 + index)
            sensor(pid, "O2 $index equiv I", "Sonda de oxígeno $index: relación de equivalencia (corriente)", "INT16(A:B)/32768", "ratio", max = 2f, category = oxygen)
            sensor(pid, "O2 $index mA", "Sonda de oxígeno $index: corriente de banda ancha", "INT16(C:D)/256-128", "mA", -128f, 127.9961f, oxygen)
        }
        for ((offset, location) in listOf("B1 S1", "B2 S1", "B1 S2", "B2 S2").withIndex()) {
            sensor("%02X".format(0x3C + offset), "Catalizador $location", "Temperatura catalizador $location", "INT16(A:B)/10-40", "°C", -40f, 6513.5f, exhaust)
        }
        sensor("43", "Carga absoluta", "Carga absoluta comunicada por la ECU", "INT16(A:B)*100/255", "%", max = 25700f)
        sensor("44", "Equiv objetivo", "Relación de equivalencia aire/combustible comandada", "INT16(A:B)/32768", "ratio", max = 2f, category = oxygen)
        sensor("45", "Mariposa relativa", "Posición relativa de mariposa", "A*100/255", "%")
        sensor("46", "Temp ambiente", "Temperatura ambiente comunicada por la ECU", "A-40", "°C", -40f, 215f)
        sensor("47", "Mariposa B", "Posición absoluta de mariposa B", "A*100/255", "%")
        sensor("48", "Mariposa C", "Posición absoluta de mariposa C", "A*100/255", "%")
        for ((offset, letter) in listOf("D", "E", "F").withIndex()) {
            sensor("%02X".format(0x49 + offset), "Pedal $letter", "Posición del pedal del acelerador $letter", "A*100/255", "%")
        }
        sensor("4C", "Mariposa objetivo", "Posición comandada del actuador de mariposa", "A*100/255", "%")
        sensor("4D", "Tiempo MIL", "Tiempo de funcionamiento con MIL encendido", "INT16(A:B)", "min", max = 65535f, category = diagnostic)
        sensor("4E", "Tiempo DTC", "Tiempo desde borrado de averías", "INT16(A:B)", "min", max = 65535f, category = diagnostic)
        sensor("5F", "Norma emisiones", "Código de requisitos de emisiones declarado por la ECU", "A", "código", max = 255f, category = diagnostic)
        sensor("51", "Tipo fuel ECU", "Código de tipo de combustible declarado por la ECU", "A", "código", max = 255f, category = fuel, invalid = mapOf("A" to setOf(0)))
        sensor("52", "Etanol", "Porcentaje de etanol en el combustible", "A*100/255", "%", category = fuel)
        sensor("53", "EVAP absoluta", "Presión de vapor EVAP absoluta", "INT16(A:B)/200", "kPa", max = 327.675f, category = fuel)
        sensor("54", "EVAP alternativa", "Presión de vapor EVAP alternativa", "INT16(A:B)-32767", "Pa", -32767f, 32768f, fuel)
        for ((pid, duration, banks) in listOf(Triple("55", "corto", listOf(1, 3)), Triple("56", "largo", listOf(1, 3)),
                                             Triple("57", "corto", listOf(2, 4)), Triple("58", "largo", listOf(2, 4)))) {
            for ((offset, bank) in banks.withIndex()) {
                val byte = if (offset == 0) "A" else "B"
                sensor(pid, "O2 trim $duration B$bank", "Corrección secundaria de oxígeno a $duration plazo, banco $bank",
                    "($byte-128)*100/128", "%", -100f, 99.21875f, oxygen)
            }
        }
        sensor("59", "Rail absoluto", "Presión absoluta de rail de combustible", "INT16(A:B)*10", "kPa", max = 655350f, category = fuel)
        sensor("5A", "Pedal relativo", "Posición relativa del pedal del acelerador", "A*100/255", "%")
        sensor("5B", "Batería HV restante", "Nivel restante de batería híbrida comunicado por la ECU", "A*100/255", "%", category = electrical)
        sensor("5C", "Temp aceite", "Temperatura del aceite del motor", "A-40", "°C", -40f, 215f)
        sensor("5D", "Avance inyección", "Momento de inyección de combustible comunicado por la ECU", "INT16(A:B)/128-210", "°", -210f, 301.9922f, fuel)
        sensor("61", "Par pedido ECU", "Par solicitado por el conductor respecto al par de referencia", "A-125", "%", -125f, 130f)
        sensor("62", "Par actual ECU", "Par actual comunicado por la ECU respecto al par de referencia", "A-125", "%", -125f, 130f)
        sensor("63", "Par referencia", "Par de referencia del motor comunicado por la ECU (no par instantáneo)", "INT16(A:B)", "Nm", max = 65535f)
        for ((offset, byte) in listOf("A", "B", "C", "D", "E").withIndex()) {
            val point = if (offset == 0) "ralentí" else "punto ${offset + 1}"
            sensor("64", "Par mapa $point", "Par de mapa del motor en $point (ECU)", "$byte-125", "%", -125f, 130f)
        }
        sensor("65", "Marcha recomendada", "Marcha recomendada por la ECU (no marcha engranada)", "(B&240)/16", "", max = 15f, category = transmission, present = "A&16")
        sensor("65", "Precalentamiento", "Testigo de bujías de precalentamiento comunicado por la ECU", "(B&8)/8", "", max = 1f, category = diagnostic, present = "A&8", labels = onOff)
        sensor("65", "Punto muerto MT", "Estado de punto muerto de transmisión manual (ECU)", "(B&4)/4", "", max = 1f, category = transmission, present = "A&4")
        sensor("65", "Estado N/D AT", "Estado neutral/drive de transmisión automática (código ECU)", "(B&2)/2", "código", max = 1f, category = transmission, present = "A&2")
        sensor("65", "PTO activo", "Estado de toma de fuerza comunicado por la ECU", "B&1", "", max = 1f, category = diagnostic, present = "A&1", labels = onOff)
        sensor("66", "MAF A", "Flujo de aire del sensor MAF A", "INT16(B:C)/32", "g/s", max = 2047.96875f, present = "A&1")
        sensor("66", "MAF B", "Flujo de aire del sensor MAF B", "INT16(D:E)/32", "g/s", max = 2047.96875f, present = "A&2")
        sensor("67", "Refrigerante 1", "Temperatura de refrigerante, sensor 1", "B-40", "°C", -40f, 215f, present = "A&1")
        sensor("67", "Refrigerante 2", "Temperatura de refrigerante, sensor 2", "C-40", "°C", -40f, 215f, present = "A&2")
        for ((offset, byte) in listOf("B", "C", "D", "E", "F", "G").withIndex()) {
            val bank = offset / 3 + 1
            val position = offset % 3 + 1
            sensor("68", "Admisión B$bank S$position", "Temperatura de admisión, banco $bank sensor $position", "$byte-40", "°C",
                -40f, 215f, present = "A&${1 shl offset}")
        }
        for ((offset, byte) in listOf("B", "C", "D", "E", "F", "G").withIndex()) {
            val valve = if (offset < 3) "A" else "B"
            val field = listOf("objetivo", "actual", "error")[offset % 3]
            val error = field == "error"
            sensor("69", "EGR $valve $field", "EGR $valve: $field comunicado por la ECU",
                if (error) "($byte-128)*100/128" else "$byte*100/255", "%",
                if (error) -100f else 0f, if (error) 99.21875f else 100f, exhaust, present = "A&${1 shl offset}")
        }
        for ((offset, byte) in listOf("B", "C", "D", "E").withIndex()) {
            val valve = if (offset < 2) "A" else "B"
            val field = if (offset % 2 == 0) "objetivo" else "relativa"
            sensor("6C", "Mariposa $valve $field", "Mariposa $valve: posición $field", "$byte*100/255", "%", present = "A&${1 shl offset}")
            val wgField = if (offset % 2 == 0) "objetivo" else "actual"
            sensor("72", "Wastegate $valve $wgField", "Wastegate $valve: posición $wgField", "$byte*100/255", "%", present = "A&${1 shl offset}")
        }
        sensor("6D", "Rail A objetivo", "Presión relativa de rail A comandada", "INT16(B:C)*10", "kPa", max = 655350f, category = fuel, present = "A&1")
        sensor("6D", "Rail A actual", "Presión relativa de rail A comunicada por la ECU", "INT16(D:E)*10", "kPa", max = 655350f, category = fuel, present = "A&2")
        sensor("6D", "Temp fuel A", "Temperatura de combustible en rail A", "F-40", "°C", -40f, 215f, fuel, present = "A&4")
        sensor("6D", "Rail B objetivo", "Presión relativa de rail B comandada", "INT16(G:H)*10", "kPa", max = 655350f, category = fuel, present = "A&8")
        sensor("6D", "Rail B actual", "Presión relativa de rail B comunicada por la ECU", "INT16(I:J)*10", "kPa", max = 655350f, category = fuel, present = "A&16")
        sensor("6D", "Temp fuel B", "Temperatura de combustible en rail B", "K-40", "°C", -40f, 215f, fuel, present = "A&32")
        for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I").withIndex()) {
            val turbo = if (offset < 2) "A" else "B"
            val field = if (offset % 2 == 0) "objetivo" else "actual"
            sensor("70", "Turbo $turbo $field", "Presión turbo $turbo $field comunicada por la ECU", "INT16($pair)/32", "kPa",
                max = 2047.96875f, present = "A&${listOf(1, 2, 8, 16)[offset]}")
        }
        for ((offset, bytes) in listOf("B:C:D:E", "F:G:H:I", "J:K:L:M").withIndex()) {
            val label = listOf("total", "ralentí", "PTO")[offset]
            sensor("7F", "Tiempo $label ECU", "Tiempo acumulado de motor: $label", "INT32($bytes)", "s", max = 4294967295f,
                present = "A&${1 shl offset}")
        }
        sensor("84", "Temp colector", "Temperatura superficial del colector", "A-40", "°C", -40f, 215f)
        sensor("8D", "Mariposa G", "Posición absoluta de mariposa G", "A*100/255", "%")
        sensor("8E", "Par fricción ECU", "Par de fricción del motor respecto al par de referencia", "A-125", "%", -125f, 130f)
        sensor("9A", "Voltaje HV", "Tensión de batería híbrida / eléctrica", "INT16(C:D)/64", "V", max = 1023.9844f, category = electrical, present = "A&2")
        sensor("9A", "Corriente HV", "Corriente de batería híbrida / eléctrica", "(SIGNED(E)*256+F)/10", "A", -3276.8f, 3276.7f, electrical, present = "A&4")
        sensor("9D", "Caudal fuel motor", "Caudal másico de combustible del motor", "INT16(A:B)/50", "g/s", max = 1310.7f, category = fuel)
        sensor("9D", "Caudal fuel vehículo", "Caudal másico de combustible del vehículo", "INT16(C:D)/50", "g/s", max = 1310.7f, category = fuel)
        sensor("9E", "Caudal escape ECU", "Caudal másico de escape del motor", "INT16(A:B)/5", "kg/h", max = 13107f, category = exhaust)
        for (index in 0..7) {
            val bank = index / 2 + 1
            val system = if (index % 2 == 0) "A" else "B"
            val byte = ('B'.code + index).toChar()
            sensor("9F", "Uso fuel $system B$bank", "Uso del sistema de combustible $system en banco $bank", "$byte*100/255", "%",
                category = fuel, present = "A&${1 shl index}")
        }
        sensor("A2", "Fuel por ciclo", "Cantidad de combustible por ciclo de cilindro", "INT16(A:B)/32", "mg/ciclo", max = 2047.96875f, category = fuel)
        sensor("A6", "Odómetro ECU", "Odómetro comunicado por la ECU", "INT32(A:B:C:D)/10", "km", max = 429496729.5f, category = diagnostic)
        sensor("AA", "Límite velocidad ECU", "Límite actual de velocidad comunicado por la ECU", "A", "km/h", max = 255f)
        sensor("B2", "Salud batería HV", "Estado de salud de batería de tracción comunicado por la ECU", "A*100/255", "%", category = electrical)
        sensor("D2", "Energía cert. ECU", "Estado de energía certificada de batería informado por la ECU", "B*100/255", "%", category = electrical, present = "A&1")
        sensor("D2", "Autonomía cert. ECU", "Estado de autonomía certificada informado por la ECU (porcentaje)", "C*100/255", "%", category = electrical, present = "A&2")
        sensor("D3", "Odómetro motor", "Odómetro del motor comunicado por la ECU", "INT32(A:B:C:D)/10", "km", max = 429496729.5f, category = diagnostic)
    }
}
