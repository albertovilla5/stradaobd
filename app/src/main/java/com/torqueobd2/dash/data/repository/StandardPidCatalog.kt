package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory
import com.torqueobd2.dash.i18n.tr

/**
 * Campos SAE J1979, incluidos varios campos por respuesta. No genera estimaciones.
 * Referencias de escalas y bits: CSS Electronics y el catálogo publicado por OBDb/SAEJ1979.
 * https://www.csselectronics.com/pages/obd2-pid-table-on-board-diagnostics-j1979
 * https://github.com/OBDb/SAEJ1979/blob/main/signalsets/v3/default.json
 */
internal object StandardPidCatalog {
    fun additionalPids(): List<ObdPid> = buildList {
        fun sensor(
            pid: String,
            short: String,
            name: String,
            equation: String,
            unit: String,
            min: Float = 0f,
            max: Float = 100f,
            category: PidCategory = PidCategory.ENGINE,
            present: String = "",
            invalid: Map<String, Set<Int>> = emptyMap(),
            labels: Map<Int, String> = emptyMap(),
            shortEn: String = short,
            nameEn: String = name
        ) {
            add(
                ObdPid(
                    id = "SAE_01${pid}_$short",
                    name = tr(name, nameEn),
                    shortName = tr(short, shortEn),
                    modeAndPid = "01$pid",
                    equation = equation,
                    units = unit,
                    minValue = min,
                    maxValue = max,
                    category = category,
                    availabilityEquation = present,
                    unavailableRawValues = invalid,
                    valueLabels = labels
                )
            )
        }
        val fuel = PidCategory.FUEL
        val oxygen = PidCategory.OXYGEN
        val exhaust = PidCategory.EXHAUST_DPF
        val diagnostic = PidCategory.DIAGNOSTICS
        val electrical = PidCategory.ELECTRICAL
        val transmission = PidCategory.TRANSMISSION
        val onOff = mapOf(0 to tr("Apagado", "Off"), 1 to tr("Encendido", "On"))
        val monitorStatus = mapOf(0 to tr("Completado", "Complete"), 1 to tr("Incompleto", "Incomplete"))
        val obdStandards = mapOf(
            1 to "OBD-II CARB", 2 to "OBD EPA", 3 to "OBD + OBD-II", 4 to "OBD-I",
            5 to tr("Sin OBD", "Not OBD compliant"), 6 to tr("EOBD (Europa)", "EOBD (Europe)"), 7 to "EOBD + OBD-II", 8 to "EOBD + OBD",
            9 to "EOBD + OBD + OBD-II", 10 to tr("JOBD (Japón)", "JOBD (Japan)"), 11 to "JOBD + OBD-II",
            12 to "JOBD + EOBD", 13 to "JOBD + EOBD + OBD-II", 17 to "EMD", 18 to "EMD+",
            19 to "HD OBD-C", 20 to "HD OBD", 21 to "WWH OBD", 23 to "HD EOBD-I",
            24 to "HD EOBD-I N", 25 to "HD EOBD-II", 26 to "HD EOBD-II N", 28 to "OBDBr-1",
            29 to "OBDBr-2", 30 to tr("KOBD (Corea)", "KOBD (Korea)"), 31 to "IOBD I", 32 to "IOBD II",
            33 to "HD EOBD-IV"
        )
        val fuelTypes = mapOf(
            1 to tr("Gasolina", "Gasoline"), 2 to tr("Metanol", "Methanol"), 3 to tr("Etanol", "Ethanol"), 4 to tr("Diésel", "Diesel"),
            5 to tr("GLP", "LPG"), 6 to tr("GNC", "CNG"), 7 to tr("Propano", "Propane"), 8 to tr("Eléctrico", "Electric"),
            9 to tr("Bifuel gasolina", "Bifuel running Gasoline"), 10 to tr("Bifuel metanol", "Bifuel running Methanol"), 11 to tr("Bifuel etanol", "Bifuel running Ethanol"),
            12 to tr("Bifuel GLP", "Bifuel running LPG"), 13 to tr("Bifuel GNC", "Bifuel running CNG"), 14 to tr("Bifuel propano", "Bifuel running Propane"),
            15 to tr("Bifuel eléctrico", "Bifuel running Electricity"), 16 to tr("Bifuel eléctrico/combustión", "Bifuel electric and combustion"),
            17 to tr("Híbrido gasolina", "Hybrid gasoline"), 18 to tr("Híbrido etanol", "Hybrid ethanol"), 19 to tr("Híbrido diésel", "Hybrid diesel"),
            20 to tr("Híbrido eléctrico", "Hybrid electric"), 21 to tr("Híbrido eléctrico/combustión", "Hybrid running electric and combustion"),
            22 to tr("Híbrido regenerativo", "Hybrid regenerative"), 23 to tr("Bifuel diésel", "Bifuel running diesel")
        )
        val emissionStandards = mapOf(
            14 to "Euro IV B1", 15 to "Euro V B2", 16 to "Euro EEV C",
            17 to "Euro VI A", 18 to "Euro VI B", 19 to "Euro VI C", 20 to "Euro VI D"
        )
        val secondaryAirStates = mapOf(
            1 to tr("Aguas arriba", "Upstream"),
            2 to tr("Aguas abajo", "Downstream of catalytic converter"),
            4 to tr("Atmósfera / apagado", "Outside atmosphere / off"),
            8 to tr("Diagnóstico activo", "Pump commanded on for diagnostics")
        )
        sensor("01", "MIL", "Testigo de avería motor comunicado por la ECU", "(A&128)/128", "", max = 1f, category = diagnostic, labels = onOff,
            shortEn = "MIL", nameEn = "Malfunction Indicator Lamp (MIL) reported by ECU")
        sensor("01", "DTC confirmados", "Número de códigos de avería confirmados", "A&127", "códigos", max = 127f, category = diagnostic,
            shortEn = "Confirmed DTCs", nameEn = "Number of confirmed emission-related DTCs")
        sensor("01", "Tipo monitores ECU", "Tipo de monitores de encendido declarados por la ECU", "(B&8)/8", "", max = 1f, category = diagnostic,
            labels = mapOf(0 to tr("Encendido por chispa", "Spark ignition"), 1 to tr("Encendido por compresión", "Compression ignition")),
            shortEn = "ECU monitor type", nameEn = "Ignition monitor type reported by ECU")
        sensor("01", "Monitor fallos enc.", "Monitor continuo de fallos de encendido", "(B&16)/16", "", max = 1f, category = diagnostic, present = "B&1", labels = monitorStatus,
            shortEn = "Misfire monitor", nameEn = "Continuous misfire monitor")
        sensor("01", "Monitor sist. fuel", "Monitor continuo del sistema de combustible", "(B&32)/32", "", max = 1f, category = diagnostic, present = "B&2", labels = monitorStatus,
            shortEn = "Fuel sys. monitor", nameEn = "Continuous fuel system monitor")
        sensor("01", "Monitor componentes", "Monitor continuo de componentes integrales", "(B&64)/64", "", max = 1f, category = diagnostic, present = "B&4", labels = monitorStatus,
            shortEn = "Components monitor", nameEn = "Continuous comprehensive components monitor")
        sensor("01", "Monitor EGR/VVT", "Monitor de emisiones del sistema EGR / distribución VVT", "(D&128)/128", "", max = 1f, category = diagnostic, present = "C&128", labels = monitorStatus,
            shortEn = "EGR/VVT monitor", nameEn = "EGR / VVT system emissions monitor")
        sensor("01", "Monitor catalizador", "Monitor de eficiencia del catalizador (encendido por chispa)", "D&1", "", max = 1f, category = diagnostic, present = "(1-((B&8)/8))*(C&1)", labels = monitorStatus,
            shortEn = "Catalyst monitor", nameEn = "Catalyst efficiency monitor (spark ignition)")
        sensor("01", "Monitor cat. calentado", "Monitor de catalizador calentado (encendido por chispa)", "(D&2)/2", "", max = 1f, category = diagnostic, present = "(1-((B&8)/8))*(C&2)", labels = monitorStatus,
            shortEn = "Heated cat. monitor", nameEn = "Heated catalyst monitor (spark ignition)")
        sensor("01", "Monitor sistema EVAP", "Monitor del sistema evaporativo EVAP (encendido por chispa)", "(D&4)/4", "", max = 1f, category = diagnostic, present = "(1-((B&8)/8))*(C&4)", labels = monitorStatus,
            shortEn = "EVAP monitor", nameEn = "Evaporative system (EVAP) monitor (spark ignition)")
        sensor("01", "Monitor aire sec.", "Monitor del sistema de aire secundario (encendido por chispa)", "(D&8)/8", "", max = 1f, category = diagnostic, present = "(1-((B&8)/8))*(C&8)", labels = monitorStatus,
            shortEn = "Sec. air monitor", nameEn = "Secondary air system monitor (spark ignition)")
        sensor("01", "Monitor sondas O2", "Monitor de sondas de oxígeno (encendido por chispa)", "(D&32)/32", "", max = 1f, category = diagnostic, present = "(1-((B&8)/8))*(C&32)", labels = monitorStatus,
            shortEn = "O2 sensor monitor", nameEn = "Oxygen sensor monitor (spark ignition)")
        sensor("01", "Monitor calentador O2", "Monitor de calentadores de sonda O2 (encendido por chispa)", "(D&64)/64", "", max = 1f, category = diagnostic, present = "(1-((B&8)/8))*(C&64)", labels = monitorStatus,
            shortEn = "O2 heater monitor", nameEn = "Oxygen sensor heater monitor (spark ignition)")
        sensor("01", "Monitor cat. NMHC", "Monitor de catalizador NMHC (encendido por compresión)", "D&1", "", max = 1f, category = diagnostic, present = "((B&8)/8)*(C&1)", labels = monitorStatus,
            shortEn = "NMHC cat. monitor", nameEn = "NMHC catalyst monitor (compression ignition)")
        sensor("01", "Monitor NOx/SCR", "Monitor de postratamiento NOx / SCR (encendido por compresión)", "(D&2)/2", "", max = 1f, category = diagnostic, present = "((B&8)/8)*(C&2)", labels = monitorStatus,
            shortEn = "NOx/SCR monitor", nameEn = "NOx / SCR aftertreatment monitor (compression ignition)")
        sensor("01", "Monitor presión turbo", "Monitor del sistema de presión de sobrealimentación (diésel)", "(D&8)/8", "", max = 1f, category = diagnostic, present = "((B&8)/8)*(C&8)", labels = monitorStatus,
            shortEn = "Boost pres. monitor", nameEn = "Boost pressure system monitor (diesel)")
        sensor("01", "Monitor sensor escape", "Monitor de sensores de gases de escape (encendido por compresión)", "(D&32)/32", "", max = 1f, category = diagnostic, present = "((B&8)/8)*(C&32)", labels = monitorStatus,
            shortEn = "Exhaust gas monitor", nameEn = "Exhaust gas sensor monitor (compression ignition)")
        sensor("01", "Monitor filtro DPF", "Monitor del filtro de partículas DPF (encendido por compresión)", "(D&64)/64", "", max = 1f, category = diagnostic, present = "((B&8)/8)*(C&64)", labels = monitorStatus,
            shortEn = "DPF monitor", nameEn = "PM filter (DPF) monitor (compression ignition)")
        sensor("02", "DTC congelado", "Código DTC que generó el cuadro congelado", "INT16(A:B)", "código", max = 65535f, category = diagnostic,
            shortEn = "Freeze DTC", nameEn = "DTC that caused freeze frame storage")
        // El nombre de banco no se deduce de la posición de la sonda: puede haber 2 o 4 bancos.
        val fuelStates = mapOf(
            1 to tr("Lazo abierto", "Open loop"),
            2 to tr("Lazo cerrado", "Closed loop"),
            4 to tr("Abierto por carga", "Open loop (load/decel)"),
            8 to tr("Abierto por fallo", "Open loop (system fault)"),
            16 to tr("Cerrado con fallo", "Closed loop (O2 fault)")
        )
        sensor("03", "Sistema fuel 1", "Estado del sistema de combustible 1 (ECU)", "A", "", max = 255f, category = fuel, labels = fuelStates,
            shortEn = "Fuel system 1", nameEn = "Fuel system 1 status (ECU)")
        sensor("03", "Sistema fuel 2", "Estado del sistema de combustible 2 (ECU)", "B", "", max = 255f, category = fuel, labels = fuelStates,
            shortEn = "Fuel system 2", nameEn = "Fuel system 2 status (ECU)")
        for ((pid, short, name, nameEn) in listOf(
            listOf("06", "STFT B1", "Corrección de combustible a corto plazo, banco 1", "Short term fuel trim, Bank 1"),
            listOf("07", "LTFT B1", "Corrección de combustible a largo plazo, banco 1", "Long term fuel trim, Bank 1"),
            listOf("08", "STFT B2", "Corrección de combustible a corto plazo, banco 2", "Short term fuel trim, Bank 2"),
            listOf("09", "LTFT B2", "Corrección de combustible a largo plazo, banco 2", "Long term fuel trim, Bank 2")
        )) {
            sensor(pid, short, name, "(A-128)*100/128", "%", -100f, 99.21875f, fuel, shortEn = short, nameEn = nameEn)
        }
        sensor("0A", "Presión fuel", "Presión relativa de combustible", "A*3", "kPa", max = 765f, category = fuel,
            shortEn = "Fuel pressure", nameEn = "Fuel gauge pressure")
        sensor("0E", "Avance encendido", "Avance de encendido comunicado por la ECU", "A/2-64", "°", -64f, 63.5f,
            shortEn = "Timing advance", nameEn = "Ignition timing advance reported by ECU")
        sensor("12", "Aire secundario", "Código de estado del aire secundario comandado", "A", "código", max = 255f, category = exhaust, labels = secondaryAirStates,
            shortEn = "Secondary air", nameEn = "Commanded secondary air status code")
        sensor("13", "Sondas O2 2B", "Máscara de sondas de oxígeno presentes (2 bancos)", "A", "máscara", max = 255f, category = oxygen,
            shortEn = "O2 sensors 2B", nameEn = "Oxygen sensors present bitmask (2 banks)")
        for (index in 1..8) {
            val pid = "%02X".format(0x13 + index)
            sensor(pid, "O2 $index V", "Sonda de oxígeno $index: tensión", "A/200", "V", max = 1.275f, category = oxygen,
                shortEn = "O2 $index V", nameEn = "Oxygen sensor $index: voltage")
            // J1979: FF en el byte de trim significa que este campo no se utiliza.
            sensor(pid, "O2 $index STFT", "Sonda de oxígeno $index: corrección a corto plazo", "(B-128)*100/128", "%",
                -100f, 99.21875f, oxygen, invalid = mapOf("B" to setOf(255)),
                shortEn = "O2 $index STFT", nameEn = "Oxygen sensor $index: short term fuel trim")
        }
        sensor("1C", "Norma OBD", "Código de norma OBD declarada por la ECU", "A", "código", max = 255f, category = diagnostic, labels = obdStandards,
            shortEn = "OBD standard", nameEn = "OBD standard code declared by ECU")
        sensor("1D", "Sondas O2 4B", "Máscara de sondas de oxígeno presentes (4 bancos)", "A", "máscara", max = 255f, category = oxygen,
            shortEn = "O2 sensors 4B", nameEn = "Oxygen sensors present bitmask (4 banks)")
        sensor("1E", "PTO aux", "Estado de toma de fuerza auxiliar (PTO)", "A&1", "", max = 1f, category = diagnostic, labels = onOff,
            shortEn = "Aux PTO", nameEn = "Auxiliary input / Power Take-Off (PTO) status")
        sensor("1F", "Tiempo motor", "Tiempo de funcionamiento desde arranque", "INT16(A:B)", "s", max = 65535f,
            shortEn = "Run time", nameEn = "Run time since engine start")
        sensor("21", "Distancia MIL", "Distancia recorrida con MIL encendido", "INT16(A:B)", "km", max = 65535f, category = diagnostic,
            shortEn = "MIL distance", nameEn = "Distance traveled with MIL on")
        sensor("22", "Rail relativo", "Presión de rail respecto a vacío del colector", "INT16(A:B)*0.079", "kPa", max = 5177.265f, category = fuel,
            shortEn = "Rel. rail pres.", nameEn = "Fuel rail pressure relative to manifold vacuum")
        sensor("23", "Rail diésel", "Presión relativa de rail (diésel / inyección directa)", "INT16(A:B)*10", "kPa", max = 655350f, category = fuel,
            shortEn = "Diesel rail", nameEn = "Fuel rail gauge pressure (diesel / gasoline direct injection)")
        for (index in 1..8) {
            val pid = "%02X".format(0x23 + index)
            sensor(pid, "O2 $index equiv V", "Sonda de oxígeno $index: relación de equivalencia (tensión)", "INT16(A:B)/32768", "ratio", max = 2f, category = oxygen,
                shortEn = "O2 $index equiv V", nameEn = "Oxygen sensor $index: fuel-air equivalence ratio (voltage)")
            sensor(pid, "O2 $index banda V", "Sonda de oxígeno $index: tensión de banda ancha", "INT16(C:D)/8192", "V", max = 8f, category = oxygen,
                shortEn = "O2 $index wide V", nameEn = "Oxygen sensor $index: wideband voltage")
        }
        sensor("2C", "EGR objetivo", "Apertura EGR comandada por la ECU", "A*100/255", "%", category = exhaust,
            shortEn = "Commanded EGR", nameEn = "Commanded EGR opening reported by ECU")
        sensor("2D", "Error EGR", "Error EGR comunicado por la ECU", "(A-128)*100/128", "%", -100f, 99.21875f, exhaust,
            shortEn = "EGR error", nameEn = "EGR error reported by ECU")
        sensor("2E", "Purga EVAP", "Purga evaporativa comandada", "A*100/255", "%", category = fuel,
            shortEn = "EVAP purge", nameEn = "Commanded evaporative purge")
        sensor("30", "Calentamientos", "Calentamientos desde borrado de averías", "A", "ciclos", max = 255f, category = diagnostic,
            shortEn = "Warm-ups", nameEn = "Warm-ups since codes cleared")
        sensor("31", "Distancia DTC", "Distancia desde borrado de averías", "INT16(A:B)", "km", max = 65535f, category = diagnostic,
            shortEn = "Clr. DTC dist.", nameEn = "Distance traveled since codes cleared")
        sensor("32", "Presión EVAP", "Presión de vapor EVAP (entero de 16 bits con signo)", "(SIGNED(A)*256+B)/4", "Pa", -8192f, 8191.75f, fuel,
            shortEn = "EVAP pressure", nameEn = "Evap. system vapor pressure (16-bit signed)")
        sensor("33", "Barométrica", "Presión barométrica absoluta comunicada por la ECU", "A", "kPa", max = 255f,
            shortEn = "Barometric", nameEn = "Absolute barometric pressure reported by ECU")
        for (index in 1..8) {
            val pid = "%02X".format(0x33 + index)
            sensor(pid, "O2 $index equiv I", "Sonda de oxígeno $index: relación de equivalencia (corriente)", "INT16(A:B)/32768", "ratio", max = 2f, category = oxygen,
                shortEn = "O2 $index equiv I", nameEn = "Oxygen sensor $index: fuel-air equivalence ratio (current)")
            sensor(pid, "O2 $index mA", "Sonda de oxígeno $index: corriente de banda ancha", "INT16(C:D)/256-128", "mA", -128f, 127.9961f, oxygen,
                shortEn = "O2 $index mA", nameEn = "Oxygen sensor $index: wideband current")
        }
        for ((offset, location) in listOf("B1 S1", "B2 S1", "B1 S2", "B2 S2").withIndex()) {
            sensor("%02X".format(0x3C + offset), "Catalizador $location", "Temperatura catalizador $location", "INT16(A:B)/10-40", "°C", -40f, 6513.5f, exhaust,
                shortEn = "Catalyst $location", nameEn = "Catalyst temperature $location")
        }
        sensor("41", "Monitor ciclo fallos", "Monitor de fallos de encendido en ciclo actual", "(B&16)/16", "", max = 1f, category = diagnostic, present = "B&1", labels = monitorStatus,
            shortEn = "Drive misfire mon.", nameEn = "Misfire monitor this drive cycle")
        sensor("41", "Monitor ciclo fuel", "Monitor del sistema de combustible en ciclo actual", "(B&32)/32", "", max = 1f, category = diagnostic, present = "B&2", labels = monitorStatus,
            shortEn = "Drive fuel mon.", nameEn = "Fuel system monitor this drive cycle")
        sensor("41", "Monitor ciclo comp.", "Monitor de componentes integrales en ciclo actual", "(B&64)/64", "", max = 1f, category = diagnostic, present = "B&4", labels = monitorStatus,
            shortEn = "Drive comp. mon.", nameEn = "Comprehensive components monitor this drive cycle")
        sensor("43", "Carga absoluta", "Carga absoluta comunicada por la ECU", "INT16(A:B)*100/255", "%", max = 25700f,
            shortEn = "Absolute load", nameEn = "Absolute load value reported by ECU")
        sensor("44", "Equiv objetivo", "Relación de equivalencia aire/combustible comandada", "INT16(A:B)/32768", "ratio", max = 2f, category = oxygen,
            shortEn = "Cmd. equiv ratio", nameEn = "Commanded fuel-air equivalence ratio")
        sensor("45", "Mariposa relativa", "Posición relativa de mariposa", "A*100/255", "%",
            shortEn = "Rel. throttle", nameEn = "Relative throttle position")
        sensor("46", "Temp ambiente", "Temperatura ambiente comunicada por la ECU", "A-40", "°C", -40f, 215f,
            shortEn = "Ambient temp", nameEn = "Ambient air temperature reported by ECU")
        sensor("47", "Mariposa B", "Posición absoluta de mariposa B", "A*100/255", "%",
            shortEn = "Throttle B", nameEn = "Absolute throttle position B")
        sensor("48", "Mariposa C", "Posición absoluta de mariposa C", "A*100/255", "%",
            shortEn = "Throttle C", nameEn = "Absolute throttle position C")
        for ((offset, letter) in listOf("D", "E", "F").withIndex()) {
            sensor("%02X".format(0x49 + offset), "Pedal $letter", "Posición del pedal del acelerador $letter", "A*100/255", "%",
                shortEn = "Pedal $letter", nameEn = "Accelerator pedal position $letter")
        }
        sensor("4C", "Mariposa objetivo", "Posición comandada del actuador de mariposa", "A*100/255", "%",
            shortEn = "Cmd. throttle", nameEn = "Commanded throttle actuator position")
        sensor("4D", "Tiempo MIL", "Tiempo de funcionamiento con MIL encendido", "INT16(A:B)", "min", max = 65535f, category = diagnostic,
            shortEn = "MIL run time", nameEn = "Time run with MIL on")
        sensor("4E", "Tiempo DTC", "Tiempo desde borrado de averías", "INT16(A:B)", "min", max = 65535f, category = diagnostic,
            shortEn = "Clr. DTC time", nameEn = "Time since trouble codes cleared")
        sensor("4F", "Escala máx equiv", "Escala máxima de relación de equivalencia (ECU)", "A", "ratio", max = 255f, category = oxygen,
            shortEn = "Max equiv scale", nameEn = "Maximum value for fuel-air equivalence ratio (ECU)")
        sensor("4F", "Escala máx O2 V", "Escala máxima de tensión de sonda O2 (ECU)", "B", "V", max = 255f, category = oxygen,
            shortEn = "Max O2 V scale", nameEn = "Maximum value for oxygen sensor voltage (ECU)")
        sensor("4F", "Escala máx O2 mA", "Escala máxima de corriente de sonda O2 (ECU)", "C", "mA", max = 255f, category = oxygen,
            shortEn = "Max O2 mA scale", nameEn = "Maximum value for oxygen sensor current (ECU)")
        sensor("4F", "Escala máx MAP", "Escala máxima de presión absoluta de colector (ECU)", "D*10", "kPa", max = 2550f,
            shortEn = "Max MAP scale", nameEn = "Maximum value for intake manifold absolute pressure (ECU)")
        sensor("50", "Escala máx MAF", "Escala máxima de caudal de aire MAF (ECU)", "A*10", "g/s", max = 2550f,
            shortEn = "Max MAF scale", nameEn = "Maximum value for air flow rate from mass air flow sensor (ECU)")
        sensor("5F", "Norma emisiones", "Código de requisitos de emisiones declarado por la ECU", "A", "código", max = 255f, category = diagnostic, labels = emissionStandards,
            shortEn = "Emission std.", nameEn = "Emission requirements code declared by ECU")
        sensor("51", "Tipo fuel ECU", "Código de tipo de combustible declarado por la ECU", "A", "código", max = 255f, category = fuel, invalid = mapOf("A" to setOf(0)), labels = fuelTypes,
            shortEn = "ECU fuel type", nameEn = "Fuel type code declared by ECU")
        sensor("52", "Etanol", "Porcentaje de etanol en el combustible", "A*100/255", "%", category = fuel,
            shortEn = "Ethanol", nameEn = "Ethanol fuel percentage")
        sensor("53", "EVAP absoluta", "Presión de vapor EVAP absoluta", "INT16(A:B)/200", "kPa", max = 327.675f, category = fuel,
            shortEn = "Abs. EVAP pres.", nameEn = "Absolute Evap system vapor pressure")
        sensor("54", "EVAP alternativa", "Presión de vapor EVAP alternativa", "INT16(A:B)-32767", "Pa", -32767f, 32768f, fuel,
            shortEn = "Alt. EVAP pres.", nameEn = "Evap system vapor pressure (alternate)")
        for ((pid, durationEs, durationEn, banks) in listOf(
            listOf("55", "corto", "short", listOf(1, 3)),
            listOf("56", "largo", "long", listOf(1, 3)),
            listOf("57", "corto", "short", listOf(2, 4)),
            listOf("58", "largo", "long", listOf(2, 4))
        )) {
            val pidStr = pid as String
            val durEs = durationEs as String
            val durEn = durationEn as String
            @Suppress("UNCHECKED_CAST")
            val bankList = banks as List<Int>
            for ((offset, bank) in bankList.withIndex()) {
                val byte = if (offset == 0) "A" else "B"
                sensor(
                    pidStr,
                    "O2 trim $durEs B$bank",
                    "Corrección secundaria de oxígeno a $durEs plazo, banco $bank",
                    "($byte-128)*100/128",
                    "%",
                    -100f,
                    99.21875f,
                    oxygen,
                    shortEn = "O2 $durEn trim B$bank",
                    nameEn = "${durEn.replaceFirstChar { it.uppercase() }} term secondary oxygen sensor trim, bank $bank"
                )
            }
        }
        sensor("59", "Rail absoluto", "Presión absoluta de rail de combustible", "INT16(A:B)*10", "kPa", max = 655350f, category = fuel,
            shortEn = "Abs. rail pres.", nameEn = "Fuel rail absolute pressure")
        sensor("5A", "Pedal relativo", "Posición relativa del pedal del acelerador", "A*100/255", "%",
            shortEn = "Rel. pedal", nameEn = "Relative accelerator pedal position")
        sensor("5B", "Batería HV restante", "Nivel restante de batería híbrida comunicado por la ECU", "A*100/255", "%", category = electrical,
            shortEn = "HV batt. rem.", nameEn = "Hybrid battery pack remaining life reported by ECU")
        sensor("5C", "Temp aceite", "Temperatura del aceite del motor", "A-40", "°C", -40f, 215f,
            shortEn = "Oil temp", nameEn = "Engine oil temperature")
        sensor("5D", "Avance inyección", "Momento de inyección de combustible comunicado por la ECU", "INT16(A:B)/128-210", "°", -210f, 301.9922f, fuel,
            shortEn = "Fuel inj. timing", nameEn = "Fuel injection timing reported by ECU")
        sensor("61", "Par pedido ECU", "Par solicitado por el conductor respecto al par de referencia", "A-125", "%", -125f, 130f,
            shortEn = "Demanded torque", nameEn = "Driver's demand engine - percent torque")
        sensor("62", "Par actual ECU", "Par actual comunicado por la ECU respecto al par de referencia", "A-125", "%", -125f, 130f,
            shortEn = "Actual torque", nameEn = "Actual engine - percent torque reported by ECU")
        sensor("63", "Par referencia", "Par de referencia del motor comunicado por la ECU (no par instantáneo)", "INT16(A:B)", "Nm", max = 65535f,
            shortEn = "Ref. torque", nameEn = "Engine reference torque reported by ECU (not instantaneous)")
        for ((offset, byte) in listOf("A", "B", "C", "D", "E").withIndex()) {
            val pointEs = if (offset == 0) "ralentí" else "punto ${offset + 1}"
            val pointEn = if (offset == 0) "idle" else "point ${offset + 1}"
            sensor("64", "Par mapa $pointEs", "Par de mapa del motor en $pointEs (ECU)", "$byte-125", "%", -125f, 130f,
                shortEn = "Torque map $pointEn", nameEn = "Engine percent torque data at $pointEn (ECU)")
        }
        sensor("65", "Marcha recomendada", "Marcha recomendada por la ECU (no marcha engranada)", "(B&240)/16", "", max = 15f, category = transmission, present = "A&16",
            shortEn = "Rec. gear", nameEn = "ECU recommended gear (not current gear)")
        sensor("65", "Precalentamiento", "Testigo de bujías de precalentamiento comunicado por la ECU", "(B&8)/8", "", max = 1f, category = diagnostic, present = "A&8", labels = onOff,
            shortEn = "Glow plug lamp", nameEn = "Glow plug lamp status reported by ECU")
        sensor("65", "Punto muerto MT", "Estado de punto muerto de transmisión manual (ECU)", "(B&4)/4", "", max = 1f, category = transmission, present = "A&4",
            shortEn = "MT neutral", nameEn = "Manual transmission neutral / clutch state (ECU)")
        sensor("65", "Estado N/D AT", "Estado neutral/drive de transmisión automática (código ECU)", "(B&2)/2", "código", max = 1f, category = transmission, present = "A&2",
            shortEn = "AT N/D state", nameEn = "Automatic transmission neutral/drive status (ECU code)")
        sensor("65", "PTO activo", "Estado de toma de fuerza comunicado por la ECU", "B&1", "", max = 1f, category = diagnostic, present = "A&1", labels = onOff,
            shortEn = "PTO active", nameEn = "Power take-off (PTO) status reported by ECU")
        sensor("66", "MAF A", "Flujo de aire del sensor MAF A", "INT16(B:C)/32", "g/s", max = 2047.96875f, present = "A&1",
            shortEn = "MAF A", nameEn = "Mass air flow sensor A")
        sensor("66", "MAF B", "Flujo de aire del sensor MAF B", "INT16(D:E)/32", "g/s", max = 2047.96875f, present = "A&2",
            shortEn = "MAF B", nameEn = "Mass air flow sensor B")
        sensor("67", "Refrigerante 1", "Temperatura de refrigerante, sensor 1", "B-40", "°C", -40f, 215f, present = "A&1",
            shortEn = "Coolant 1", nameEn = "Engine coolant temperature, sensor 1")
        sensor("67", "Refrigerante 2", "Temperatura de refrigerante, sensor 2", "C-40", "°C", -40f, 215f, present = "A&2",
            shortEn = "Coolant 2", nameEn = "Engine coolant temperature, sensor 2")
        for ((offset, byte) in listOf("B", "C", "D", "E", "F", "G").withIndex()) {
            val bank = offset / 3 + 1
            val position = offset % 3 + 1
            sensor("68", "Admisión B$bank S$position", "Temperatura de admisión, banco $bank sensor $position", "$byte-40", "°C",
                -40f, 215f, present = "A&${1 shl offset}",
                shortEn = "Intake B$bank S$position", nameEn = "Intake air temperature, bank $bank sensor $position")
            sensor("6B", "Temp EGR B$bank S$position", "Temperatura de gases EGR, banco $bank sensor $position", "$byte-40", "°C",
                -40f, 215f, exhaust, present = "A&${1 shl offset}",
                shortEn = "EGR temp B$bank S$position", nameEn = "EGR temperature, bank $bank sensor $position")
        }
        for ((offset, byte) in listOf("B", "C", "D", "E", "F", "G").withIndex()) {
            val valve = if (offset < 3) "A" else "B"
            val fieldEs = listOf("objetivo", "actual", "error")[offset % 3]
            val fieldEn = listOf("commanded", "actual", "error")[offset % 3]
            val error = fieldEs == "error"
            sensor("69", "EGR $valve $fieldEs", "EGR $valve: $fieldEs comunicado por la ECU",
                if (error) "($byte-128)*100/128" else "$byte*100/255", "%",
                if (error) -100f else 0f, if (error) 99.21875f else 100f, exhaust, present = "A&${1 shl offset}",
                shortEn = "EGR $valve $fieldEn", nameEn = "EGR $valve: $fieldEn reported by ECU")
        }
        for ((offset, byte) in listOf("B", "C", "D", "E").withIndex()) {
            val valve = if (offset < 2) "A" else "B"
            val intakeFieldEs = if (offset % 2 == 0) "obj" else "pos"
            val intakeFieldEn = if (offset % 2 == 0) "cmd" else "pos"
            val intakeDescEs = if (offset % 2 == 0) "objetivo" else "posición"
            val intakeDescEn = if (offset % 2 == 0) "commanded" else "position"
            sensor("6A", "Admisión diésel $valve $intakeFieldEs", "Control de flujo de aire admisión diésel $valve: $intakeDescEs",
                "$byte*100/255", "%", present = "A&${1 shl offset}",
                shortEn = "Diesel intake $valve $intakeFieldEn", nameEn = "Diesel intake air flow control $valve: $intakeDescEn")
            val fieldEs = if (offset % 2 == 0) "objetivo" else "relativa"
            val fieldEn = if (offset % 2 == 0) "commanded" else "relative"
            sensor("6C", "Mariposa $valve $fieldEs", "Mariposa $valve: posición $fieldEs", "$byte*100/255", "%", present = "A&${1 shl offset}",
                shortEn = "Throttle $valve $fieldEn", nameEn = "Throttle actuator $valve: $fieldEn position")
            val wgFieldEs = if (offset % 2 == 0) "objetivo" else "actual"
            val wgFieldEn = if (offset % 2 == 0) "commanded" else "actual"
            sensor("72", "Wastegate $valve $wgFieldEs", "Wastegate $valve: posición $wgFieldEs", "$byte*100/255", "%", present = "A&${1 shl offset}",
                shortEn = "Wastegate $valve $wgFieldEn", nameEn = "Wastegate $valve: $wgFieldEn position")
        }
        sensor("6D", "Rail A objetivo", "Presión relativa de rail A comandada", "INT16(B:C)*10", "kPa", max = 655350f, category = fuel, present = "A&1",
            shortEn = "Rail A target", nameEn = "Commanded fuel rail A pressure")
        sensor("6D", "Rail A actual", "Presión relativa de rail A comunicada por la ECU", "INT16(D:E)*10", "kPa", max = 655350f, category = fuel, present = "A&2",
            shortEn = "Rail A actual", nameEn = "Fuel rail A gauge pressure reported by ECU")
        sensor("6D", "Temp fuel A", "Temperatura de combustible en rail A", "F-40", "°C", -40f, 215f, fuel, present = "A&4",
            shortEn = "Fuel temp A", nameEn = "Fuel rail A temperature")
        sensor("6D", "Rail B objetivo", "Presión relativa de rail B comandada", "INT16(G:H)*10", "kPa", max = 655350f, category = fuel, present = "A&8",
            shortEn = "Rail B target", nameEn = "Commanded fuel rail B pressure")
        sensor("6D", "Rail B actual", "Presión relativa de rail B comunicada por la ECU", "INT16(I:J)*10", "kPa", max = 655350f, category = fuel, present = "A&16",
            shortEn = "Rail B actual", nameEn = "Fuel rail B gauge pressure reported by ECU")
        sensor("6D", "Temp fuel B", "Temperatura de combustible en rail B", "K-40", "°C", -40f, 215f, fuel, present = "A&32",
            shortEn = "Fuel temp B", nameEn = "Fuel rail B temperature")
        for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I").withIndex()) {
            val system = if (offset < 2) "A" else "B"
            val fieldShortEs = if (offset % 2 == 0) "obj" else "act"
            val fieldShortEn = if (offset % 2 == 0) "cmd" else "act"
            val fieldDescEs = if (offset % 2 == 0) "objetivo" else "actual"
            val fieldDescEn = if (offset % 2 == 0) "commanded" else "actual"
            sensor("6E", "Presión iny. $system $fieldShortEs", "Presión de control de inyección $system: $fieldDescEs",
                "INT16($pair)*10", "kPa", max = 655350f, category = fuel, present = "A&${1 shl offset}",
                shortEn = "Inj. pres. $system $fieldShortEn", nameEn = "Injection control pressure $system: $fieldDescEn")
        }
        sensor("6F", "Entrada compresor A", "Presión de entrada al compresor del turbo A", "B", "kPa", max = 255f, present = "A&1",
            shortEn = "Comp. A inlet", nameEn = "Turbocharger compressor A inlet pressure")
        sensor("6F", "Entrada compresor B", "Presión de entrada al compresor del turbo B", "C", "kPa", max = 255f, present = "A&2",
            shortEn = "Comp. B inlet", nameEn = "Turbocharger compressor B inlet pressure")
        for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I").withIndex()) {
            val turbo = if (offset < 2) "A" else "B"
            val fieldEs = if (offset % 2 == 0) "objetivo" else "actual"
            val fieldEn = if (offset % 2 == 0) "commanded" else "actual"
            sensor("70", "Turbo $turbo $fieldEs", "Presión turbo $turbo $fieldEs comunicada por la ECU", "INT16($pair)/32", "kPa",
                max = 2047.96875f, present = "A&${listOf(1, 2, 8, 16)[offset]}",
                shortEn = "Boost $turbo $fieldEn", nameEn = "Boost pressure $turbo ($fieldEn) reported by ECU")
        }
        for ((offset, byte) in listOf("B", "C", "E", "F").withIndex()) {
            val turbo = if (offset < 2) "A" else "B"
            val fieldEs = if (offset % 2 == 0) "objetivo" else "posición"
            val fieldEn = if (offset % 2 == 0) "commanded" else "position"
            sensor("71", "VGT $turbo $fieldEs", "Geometría variable turbo $turbo (VGT): $fieldEs", "$byte*100/255", "%",
                present = "A&${listOf(1, 2, 8, 16)[offset]}",
                shortEn = "VGT $turbo $fieldEn", nameEn = "Variable geometry turbo $turbo (VGT): $fieldEn")
        }
        sensor("73", "Presión escape B1", "Presión de gases de escape, banco 1", "INT16(B:C)/100", "kPa", max = 655.35f, category = exhaust, present = "A&1",
            shortEn = "Exhaust pres. B1", nameEn = "Exhaust pressure, bank 1")
        sensor("73", "Presión escape B2", "Presión de gases de escape, banco 2", "INT16(D:E)/100", "kPa", max = 655.35f, category = exhaust, present = "A&2",
            shortEn = "Exhaust pres. B2", nameEn = "Exhaust pressure, bank 2")
        sensor("74", "RPM Turbo A", "Revoluciones del turbocompresor A", "INT16(B:C)*10", "RPM", max = 655350f, present = "A&1",
            shortEn = "Turbo A RPM", nameEn = "Turbocharger A speed")
        sensor("74", "RPM Turbo B", "Revoluciones del turbocompresor B", "INT16(D:E)*10", "RPM", max = 655350f, present = "A&2",
            shortEn = "Turbo B RPM", nameEn = "Turbocharger B speed")
        for ((pid, turbo) in listOf("75" to "A", "76" to "B")) {
            sensor(pid, "Temp comp. $turbo ent", "Temperatura entrada compresor turbo $turbo", "B-40", "°C", -40f, 215f, present = "A&1",
                shortEn = "Comp. $turbo in temp", nameEn = "Turbocharger $turbo compressor inlet temperature")
            sensor(pid, "Temp comp. $turbo sal", "Temperatura salida compresor turbo $turbo", "C-40", "°C", -40f, 215f, present = "A&2",
                shortEn = "Comp. $turbo out temp", nameEn = "Turbocharger $turbo compressor outlet temperature")
            sensor(pid, "Temp turbina $turbo ent", "Temperatura entrada turbina turbo $turbo", "INT16(D:E)/10-40", "°C", -40f, 6513.5f, exhaust, present = "A&4",
                shortEn = "Turbine $turbo in temp", nameEn = "Turbocharger $turbo turbine inlet temperature")
            sensor(pid, "Temp turbina $turbo sal", "Temperatura salida turbina turbo $turbo", "INT16(F:G)/10-40", "°C", -40f, 6513.5f, exhaust, present = "A&8",
                shortEn = "Turbine $turbo out temp", nameEn = "Turbocharger $turbo turbine outlet temperature")
        }
        for ((offset, byte) in listOf("B", "C", "D", "E").withIndex()) {
            val bank = offset / 2 + 1
            val pos = offset % 2 + 1
            sensor("77", "Intercooler B$bank S$pos", "Temperatura del intercooler (CACT), banco $bank sensor $pos", "$byte-40", "°C",
                -40f, 215f, present = "A&${1 shl offset}",
                shortEn = "Intercooler B$bank S$pos", nameEn = "Charge air cooler temperature (CACT), bank $bank sensor $pos")
        }
        for ((pid, bank, startSensor) in listOf(
            Triple("78", 1, 1), Triple("79", 2, 1),
            Triple("98", 1, 5), Triple("99", 2, 5)
        )) {
            for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I").withIndex()) {
                val sensorNum = startSensor + offset
                sensor(pid, "EGT B$bank S$sensorNum", "Temperatura de gases de escape (EGT), banco $bank sensor $sensorNum",
                    "INT16($pair)/10-40", "°C", -40f, 6513.5f, exhaust, present = "A&${1 shl offset}",
                    shortEn = "EGT B$bank S$sensorNum", nameEn = "Exhaust gas temperature (EGT), bank $bank sensor $sensorNum")
            }
        }
        for ((pid, bank) in listOf("7A" to 1, "7B" to 2)) {
            sensor(pid, "DPF B$bank dif. pres", "Presión diferencial del filtro de partículas DPF, banco $bank",
                "(SIGNED(B)*256+C)/100", "kPa", -327.68f, 327.67f, exhaust, present = "A&1",
                shortEn = "DPF B$bank diff. pres", nameEn = "DPF differential pressure, bank $bank")
            sensor(pid, "DPF B$bank pres. ent", "Presión de entrada al filtro de partículas DPF, banco $bank",
                "INT16(D:E)/100", "kPa", max = 655.35f, category = exhaust, present = "A&2",
                shortEn = "DPF B$bank inlet pres", nameEn = "DPF inlet pressure, bank $bank")
            sensor(pid, "DPF B$bank pres. sal", "Presión de salida del filtro de partículas DPF, banco $bank",
                "INT16(F:G)/100", "kPa", max = 655.35f, category = exhaust, present = "A&4",
                shortEn = "DPF B$bank outlet pres", nameEn = "DPF outlet pressure, bank $bank")
        }
        for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I").withIndex()) {
            val bank = offset / 2 + 1
            val posShortEs = if (offset % 2 == 0) "ent" else "sal"
            val posShortEn = if (offset % 2 == 0) "in" else "out"
            val posDescEs = if (offset % 2 == 0) "entrada al" else "salida del"
            val posDescEn = if (offset % 2 == 0) "inlet" else "outlet"
            sensor("7C", "Temp DPF B$bank $posShortEs", "Temperatura de $posDescEs DPF, banco $bank",
                "INT16($pair)/10-40", "°C", -40f, 6513.5f, exhaust, present = "A&${1 shl offset}",
                shortEn = "DPF temp B$bank $posShortEn", nameEn = "DPF $posDescEn temperature, bank $bank")
        }
        sensor("7D", "Estado NTE NOx", "Estado de zona de control NTE de NOx", "A", "código", max = 255f, category = exhaust,
            shortEn = "NOx NTE state", nameEn = "NOx NTE control area status")
        sensor("7E", "Estado NTE PM", "Estado de zona de control NTE de partículas PM", "A", "código", max = 255f, category = exhaust,
            shortEn = "PM NTE state", nameEn = "PM NTE control area status")
        for ((offset, bytes) in listOf("B:C:D:E", "F:G:H:I", "J:K:L:M").withIndex()) {
            val labelEs = listOf("total", "ralentí", "PTO")[offset]
            val labelEn = listOf("total", "idle", "PTO")[offset]
            sensor("7F", "Tiempo $labelEs ECU", "Tiempo acumulado de motor: $labelEs", "INT32($bytes)", "s", max = 4294967295f,
                present = "A&${1 shl offset}",
                shortEn = "ECU $labelEn time", nameEn = "Engine run time: $labelEn")
        }
        for (entry in listOf(
            listOf("83", "NOx", "NOx", "Concentración de NOx", "NOx concentration", 1),
            listOf("A1", "NOx corr.", "NOx corr.", "Concentración de NOx corregida", "Corrected NOx concentration", 1),
            listOf("A7", "NOx", "NOx", "Concentración de NOx", "NOx concentration", 3),
            listOf("A8", "NOx corr.", "NOx corr.", "Concentración de NOx corregida", "Corrected NOx concentration", 3)
        )) {
            val pidHex = entry[0] as String
            val shortPrefixEs = entry[1] as String
            val shortPrefixEn = entry[2] as String
            val namePrefixEs = entry[3] as String
            val namePrefixEn = entry[4] as String
            val baseIdx = entry[5] as Int
            for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I").withIndex()) {
                val bank = offset / 2 + 1
                val sensorNum = baseIdx + (offset % 2)
                sensor(pidHex, "$shortPrefixEs B$bank S$sensorNum", "$namePrefixEs, banco $bank sensor $sensorNum",
                    "INT16($pair)", "ppm", max = 65535f, category = exhaust, present = "A&${1 shl offset}",
                    shortEn = "$shortPrefixEn B$bank S$sensorNum", nameEn = "$namePrefixEn, bank $bank sensor $sensorNum")
            }
        }
        sensor("84", "Temp colector", "Temperatura superficial del colector", "A-40", "°C", -40f, 215f,
            shortEn = "Manifold temp", nameEn = "Manifold surface temperature")
        sensor("85", "AdBlue promedio", "Consumo medio de reactivo AdBlue (DEF)", "INT16(B:C)/100", "%", max = 655.35f, category = exhaust, present = "A&1",
            shortEn = "Avg AdBlue", nameEn = "Average reagent AdBlue (DEF) consumption")
        sensor("85", "AdBlue dosificado", "Consumo comandado de reactivo AdBlue (DEF)", "INT16(D:E)/100", "%", max = 655.35f, category = exhaust, present = "A&2",
            shortEn = "Cmd AdBlue", nameEn = "Commanded reagent AdBlue (DEF) dosing")
        sensor("85", "Nivel AdBlue", "Nivel del depósito de AdBlue (DEF)", "F*100/255", "%", category = exhaust, present = "A&4",
            shortEn = "AdBlue level", nameEn = "Reagent AdBlue (DEF) tank level")
        sensor("85", "AdBlue consumo total", "Reactivo AdBlue total consumido", "INT32(G:H:I:J)/100", "g", max = 42949672.95f, category = exhaust, present = "A&8",
            shortEn = "Total AdBlue used", nameEn = "Total reagent AdBlue (DEF) consumed")
        sensor("86", "PM masa B1", "Concentración másica de partículas PM, banco 1", "INT16(B:C)/100", "mg/m³", max = 655.35f, category = exhaust, present = "A&1",
            shortEn = "PM mass B1", nameEn = "Particulate matter (PM) mass concentration, bank 1")
        sensor("86", "PM masa B2", "Concentración másica de partículas PM, banco 2", "INT16(D:E)/100", "mg/m³", max = 655.35f, category = exhaust, present = "A&2",
            shortEn = "PM mass B2", nameEn = "Particulate matter (PM) mass concentration, bank 2")
        sensor("87", "MAP A alta res", "Presión absoluta del colector A (alta resolución)", "INT16(B:C)*0.03125", "kPa", max = 2047.96875f, present = "A&1",
            shortEn = "Hi-res MAP A", nameEn = "Intake manifold absolute pressure A (high resolution)")
        sensor("87", "MAP B alta res", "Presión absoluta del colector B (alta resolución)", "INT16(D:E)*0.03125", "kPa", max = 2047.96875f, present = "A&2",
            shortEn = "Hi-res MAP B", nameEn = "Intake manifold absolute pressure B (high resolution)")
        sensor("88", "SCR distancia ind.", "Distancia recorrida con inducción SCR activa", "INT16(E:F)", "km", max = 65535f, category = exhaust,
            shortEn = "SCR induc. dist.", nameEn = "Distance traveled while SCR inducement active")
        sensor("88", "SCR dist. 0-10k", "Distancia con inducción SCR en últimos 10.000 km", "INT16(G:H)", "km", max = 65535f, category = exhaust,
            shortEn = "SCR dist. 0-10k", nameEn = "Distance with SCR inducement in last 10,000 km")
        sensor("8D", "Mariposa G", "Posición absoluta de mariposa G", "A*100/255", "%",
            shortEn = "Throttle G", nameEn = "Absolute throttle position G")
        sensor("8E", "Par fricción ECU", "Par de fricción del motor respecto al par de referencia", "A-125", "%", -125f, 130f,
            shortEn = "Friction torque", nameEn = "Engine friction - percent torque")
        sensor("8F", "PM B1 activo", "Sensor de partículas PM banco 1: estado activo", "B&1", "", max = 1f, category = exhaust, present = "A&1", labels = onOff,
            shortEn = "PM B1 active", nameEn = "Particulate matter sensor bank 1: active status")
        sensor("8F", "PM B1 regen", "Sensor de partículas PM banco 1: regeneración", "(B&2)/2", "", max = 1f, category = exhaust, present = "A&1", labels = onOff,
            shortEn = "PM B1 regen", nameEn = "Particulate matter sensor bank 1: regeneration")
        sensor("8F", "PM B1 señal", "Sensor de partículas PM banco 1: salida normalizada", "INT16(C:D)/100", "%", max = 655.35f, category = exhaust, present = "A&2",
            shortEn = "PM B1 signal", nameEn = "Particulate matter sensor bank 1: normalized output")
        sensor("8F", "PM B2 activo", "Sensor de partículas PM banco 2: estado activo", "E&1", "", max = 1f, category = exhaust, present = "A&4", labels = onOff,
            shortEn = "PM B2 active", nameEn = "Particulate matter sensor bank 2: active status")
        sensor("8F", "PM B2 regen", "Sensor de partículas PM banco 2: regeneración", "(E&2)/2", "", max = 1f, category = exhaust, present = "A&4", labels = onOff,
            shortEn = "PM B2 regen", nameEn = "Particulate matter sensor bank 2: regeneration")
        sensor("8F", "PM B2 señal", "Sensor de partículas PM banco 2: salida normalizada", "INT16(F:G)/100", "%", max = 655.35f, category = exhaust, present = "A&8",
            shortEn = "PM B2 signal", nameEn = "Particulate matter sensor bank 2: normalized output")
        sensor("9A", "SOC batería HV", "Estado de carga de batería híbrida / eléctrica (SOC)", "B*100/255", "%", category = electrical, present = "A&1",
            shortEn = "HV battery SOC", nameEn = "Hybrid / EV battery state of charge (SOC)")
        sensor("9A", "Voltaje HV", "Tensión de batería híbrida / eléctrica", "INT16(C:D)/64", "V", max = 1023.9844f, category = electrical, present = "A&2",
            shortEn = "HV voltage", nameEn = "Hybrid / EV battery voltage")
        sensor("9A", "Corriente HV", "Corriente de batería híbrida / eléctrica", "(SIGNED(E)*256+F)/10", "A", -3276.8f, 3276.7f, electrical, present = "A&4",
            shortEn = "HV current", nameEn = "Hybrid / EV battery current")
        sensor("9A", "Modo carga HV", "Modo del sistema de carga híbrido / eléctrico", "G&3", "código", max = 3f, category = electrical, present = "A&8",
            labels = mapOf(0 to tr("Inactivo", "Idle"), 1 to tr("Cargando AC", "AC charging"), 2 to tr("Cargando DC", "DC charging")),
            shortEn = "HV charge mode", nameEn = "Hybrid / EV charging system mode")
        sensor("9B", "Temp AdBlue", "Temperatura del reactivo AdBlue (DEF)", "B-40", "°C", -40f, 215f, exhaust, present = "A&1",
            shortEn = "AdBlue temp", nameEn = "Reagent AdBlue (DEF) temperature")
        sensor("9B", "Concentración AdBlue", "Concentración de urea en AdBlue (DEF)", "C/4", "%", max = 63.75f, category = exhaust, present = "A&2",
            shortEn = "AdBlue conc.", nameEn = "Urea concentration in AdBlue (DEF)")
        for ((offset, pair) in listOf("B:C", "D:E", "F:G", "H:I", "J:K", "L:M", "N:O", "P:Q").withIndex()) {
            val bank = offset / 4 + 1
            val sensorNum = (offset / 2) % 2 + 3
            val isCurrent = offset % 2 == 1
            if (!isCurrent) {
                sensor("9C", "O2 B$bank S$sensorNum equiv", "Sonda O2 banda ancha B$bank S$sensorNum: relación de equivalencia",
                    "INT16($pair)/32768", "ratio", max = 2f, category = oxygen, present = "A&${1 shl offset}",
                    shortEn = "O2 B$bank S$sensorNum equiv", nameEn = "Wideband O2 sensor B$bank S$sensorNum: equivalence ratio")
            } else {
                sensor("9C", "O2 B$bank S$sensorNum mA", "Sonda O2 banda ancha B$bank S$sensorNum: corriente",
                    "INT16($pair)/256-128", "mA", -128f, 127.9961f, oxygen, present = "A&${1 shl offset}",
                    shortEn = "O2 B$bank S$sensorNum mA", nameEn = "Wideband O2 sensor B$bank S$sensorNum: current")
            }
        }
        sensor("9D", "Caudal fuel motor", "Caudal másico de combustible del motor", "INT16(A:B)/50", "g/s", max = 1310.7f, category = fuel,
            shortEn = "Eng. fuel rate", nameEn = "Engine fuel mass flow rate")
        sensor("9D", "Caudal fuel vehículo", "Caudal másico de combustible del vehículo", "INT16(C:D)/50", "g/s", max = 1310.7f, category = fuel,
            shortEn = "Veh. fuel rate", nameEn = "Vehicle fuel mass flow rate")
        sensor("9E", "Caudal escape ECU", "Caudal másico de escape del motor", "INT16(A:B)/5", "kg/h", max = 13107f, category = exhaust,
            shortEn = "Exhaust flow", nameEn = "Engine exhaust mass flow rate")
        for (index in 0..7) {
            val bank = index / 2 + 1
            val system = if (index % 2 == 0) "A" else "B"
            val byte = ('B'.code + index).toChar()
            sensor("9F", "Uso fuel $system B$bank", "Uso del sistema de combustible $system en banco $bank", "$byte*100/255", "%",
                category = fuel, present = "A&${1 shl index}",
                shortEn = "Fuel use $system B$bank", nameEn = "Fuel system $system usage in bank $bank")
        }
        sensor("A2", "Fuel por ciclo", "Cantidad de combustible por ciclo de cilindro", "INT16(A:B)/32", "mg/ciclo", max = 2047.96875f, category = fuel,
            shortEn = "Fuel per stroke", nameEn = "Cylinder fuel rate per stroke")
        sensor("A3", "EVAP B1 Pa", "Presión de vapor EVAP, banco 1", "(SIGNED(B)*256+C)/4", "Pa", -8192f, 8191.75f, fuel, present = "A&1",
            shortEn = "EVAP B1 Pa", nameEn = "Evap system vapor pressure, bank 1")
        sensor("A3", "EVAP B2 Pa", "Presión de vapor EVAP, banco 2", "(SIGNED(D)*256+E)/4", "Pa", -8192f, 8191.75f, fuel, present = "A&2",
            shortEn = "EVAP B2 Pa", nameEn = "Evap system vapor pressure, bank 2")
        sensor("A3", "EVAP B1 amplio", "Presión de vapor EVAP rango amplio, banco 1", "(SIGNED(F)*256+G)*1.25", "Pa", -40960f, 40958.75f, fuel, present = "A&4",
            shortEn = "EVAP B1 wide", nameEn = "Evap system wide range vapor pressure, bank 1")
        sensor("A3", "EVAP B2 amplio", "Presión de vapor EVAP rango amplio, banco 2", "(SIGNED(H)*256+I)*1.25", "Pa", -40960f, 40958.75f, fuel, present = "A&8",
            shortEn = "EVAP B2 wide", nameEn = "Evap system wide range vapor pressure, bank 2")
        sensor("A4", "Marcha actual ECU", "Marcha actual engranada de transmisión (ECU)", "(B&240)/16", "", max = 15f, category = transmission, present = "A&1",
            labels = mapOf(0 to tr("Punto muerto", "Neutral")),
            shortEn = "Actual gear", nameEn = "Transmission actual gear engaged (ECU)")
        sensor("A4", "Relación cambio", "Relación de transmisión actual comunicada por la ECU", "INT16(C:D)/1000", "ratio", max = 65.535f, category = transmission, present = "A&2",
            shortEn = "Gear ratio", nameEn = "Transmission actual gear ratio reported by ECU")
        sensor("A5", "Dosificación AdBlue", "Dosificación comandada de AdBlue (DEF)", "B/2", "%", max = 127.5f, category = exhaust, present = "A&1",
            shortEn = "AdBlue dosing", nameEn = "Commanded AdBlue (DEF) dosing")
        sensor("A5", "Caudal AdBlue", "Caudal de consumo de AdBlue (DEF)", "INT16(C:D)*0.05", "L/h", max = 3276.75f, category = exhaust, present = "A&2",
            shortEn = "AdBlue flow", nameEn = "AdBlue (DEF) consumption flow rate")
        sensor("A6", "Odómetro ECU", "Odómetro comunicado por la ECU", "INT32(A:B:C:D)/10", "km", max = 429496729.5f, category = diagnostic,
            shortEn = "ECU odometer", nameEn = "Odometer reported by ECU")
        sensor("A9", "ABS desactivado", "Estado del interruptor de desactivación ABS", "B&1", "", max = 1f, category = diagnostic, present = "A&1", labels = onOff,
            shortEn = "ABS disabled", nameEn = "ABS disable switch state")
        sensor("AA", "Límite velocidad ECU", "Límite actual de velocidad comunicado por la ECU", "A", "km/h", max = 255f,
            shortEn = "ECU speed limit", nameEn = "Current speed limit reported by ECU")
        sensor("B2", "Salud batería HV", "Estado de salud de batería de tracción comunicado por la ECU", "A*100/255", "%", category = electrical,
            shortEn = "HV batt. SOH", nameEn = "Traction battery state of health (SOH) reported by ECU")
        sensor("C3", "Nivel fuel A", "Entrada de nivel de combustible A", "INT16(B:C)/200", "%", max = 327.675f, category = fuel, present = "A&1",
            shortEn = "Fuel level A", nameEn = "Fuel level input A")
        sensor("C3", "Nivel fuel B", "Entrada de nivel de combustible B", "INT16(D:E)/200", "%", max = 327.675f, category = fuel, present = "A&2",
            shortEn = "Fuel level B", nameEn = "Fuel level input B")
        sensor("C4", "Tiempo aviso PM", "Tiempo de aviso del sistema de control de partículas", "INT32(B:C:D:E)", "s", max = 4294967295f, category = exhaust, present = "A&1",
            shortEn = "PM warn time", nameEn = "Particulate control warning time")
        sensor("C4", "Tiempo inducción PM", "Tiempo de inducción del sistema de partículas", "INT32(F:G:H:I)", "s", max = 4294967295f, category = exhaust, present = "A&2",
            shortEn = "PM induc. time", nameEn = "Particulate control inducement time")
        sensor("C5", "Presión fuel A", "Presión de combustible A (baja presión)", "INT16(B:C)", "kPa", max = 65535f, category = fuel, present = "A&1",
            shortEn = "Fuel pres. A", nameEn = "Fuel pressure A (low pressure)")
        sensor("C5", "Presión fuel B", "Presión de combustible B (baja presión)", "INT16(D:E)", "kPa", max = 65535f, category = fuel, present = "A&2",
            shortEn = "Fuel pres. B", nameEn = "Fuel pressure B (low pressure)")
        sensor("C6", "Nivel disp. regen DPF", "Nivel de disparo normalizado de regeneración DPF", "INT16(C:D)/500", "%", max = 131.07f, category = exhaust, present = "A&2",
            shortEn = "DPF regen trig.", nameEn = "Normalized DPF regeneration trigger level")
        sensor("C6", "Dist. media regen DPF", "Distancia media entre regeneraciones DPF", "INT16(E:F)", "km", max = 65535f, category = exhaust, present = "A&4",
            shortEn = "DPF avg reg dist", nameEn = "Average distance between DPF regenerations")
        sensor("C6", "Tiempo medio regen DPF", "Tiempo medio entre regeneraciones DPF", "INT16(G:H)", "min", max = 65535f, category = exhaust, present = "A&8",
            shortEn = "DPF avg reg time", nameEn = "Average time between DPF regenerations")
        sensor("C7", "Dist. reprogramación", "Distancia desde última reprogramación o cambio de ECU", "INT32(A:B:C:D)/10", "km", max = 429496729.5f, category = diagnostic,
            shortEn = "Reprog. dist.", nameEn = "Distance since reflash or module replacement")
        sensor("D2", "Energía cert. ECU", "Estado de energía certificada de batería informado por la ECU", "B*100/255", "%", category = electrical, present = "A&1",
            shortEn = "ECU cert. energy", nameEn = "Battery certified energy status reported by ECU")
        sensor("D2", "Autonomía cert. ECU", "Estado de autonomía certificada informado por la ECU (porcentaje)", "C*100/255", "%", category = electrical, present = "A&2",
            shortEn = "ECU cert. range", nameEn = "Battery certified range status reported by ECU (%)")
        sensor("D3", "Odómetro motor", "Odómetro del motor comunicado por la ECU", "INT32(A:B:C:D)/10", "km", max = 429496729.5f, category = diagnostic,
            shortEn = "Engine odometer", nameEn = "Engine odometer reported by ECU")
    }
}

