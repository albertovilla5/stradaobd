package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory

object DefaultPids {

    /**
     * CSV original de Torque Pro para Honda iDTEC (DPF / Regeneración / Aceite / EGT)
     * provisto por el usuario.
     */
    const val SAMPLE_HONDA_IDTEC_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP7\nAT SH DA10F1
"iDTEC Distance from last DPF regeneration success",         "DPF dist a/reg","0x222512","INT16(V:W)/10",  0,6553,  "km",""
"iDTEC Fuel consumption from last DPF regeneration success", "DPF fuel a/reg","0x222512","INT16(X:Y)/100", 0,6500,  "l",""
"iDTEC Elapsed time from last DPF regeneration success",     "DPF time a/reg","0x222512","INT16(Z:AA)*60", 0,365000,"s",""

"iDTEC DPF differential pressure","DPF diff.pres","0x222512","INT16(AH:AI)/100",0,100,"kPa",""
"iDTEC EGT 1","EGT","0x222512","O*4",0,1000,"°C",""
"iDTEC calculated PM value","DPF calc PM","0x222512","T*0.3",0,85,"g",""

"iDTEC DPF automatic regeneration control status value","DPF reg.state","0x222512","INT32(AM:AN:AO:AP)",0,4000000000000,"",""
"iDTEC DPF active regeneration","DPF active reg","0x222512","(AM & 127)+(AN & 207)+AO+(AP & 254)",0,4000000000000,"",""

"iDTEC Engine oil level (filtered)","Eng.oil lvl f","0x222510","AS/2",0,128,"mm",""

"iDTEC DPF Exhaust gas flow (calculated) ?","DPF calc.flow","0x222516","INT16(AB:AC)/10",0,6553,"m3/h","""""

    /**
     * PIDs OBD-II estándar SAE J1979 (Modo 01)
     */
    fun getStandardPids(): List<ObdPid> = listOf(
        ObdPid(
            name = "Revoluciones del Motor",
            shortName = "RPM",
            modeAndPid = "010C",
            equation = "((A*256)+B)/4",
            minValue = 0f,
            maxValue = 8000f,
            units = "RPM",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Velocidad del Vehículo",
            shortName = "Velocidad",
            modeAndPid = "010D",
            equation = "A",
            minValue = 0f,
            maxValue = 260f,
            units = "km/h",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Temperatura Refrigerante",
            shortName = "Temp Liq",
            modeAndPid = "0105",
            equation = "A-40",
            minValue = -40f,
            maxValue = 150f,
            units = "°C",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Carga Calculada del Motor",
            shortName = "Carga",
            modeAndPid = "0104",
            equation = "A*100/255",
            minValue = 0f,
            maxValue = 100f,
            units = "%",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Posición de Mariposa (ECU)",
            shortName = "Acelerador",
            modeAndPid = "0111",
            equation = "A*100/255",
            minValue = 0f,
            maxValue = 100f,
            units = "%",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Presión Absoluta Colector (MAP)",
            shortName = "MAP",
            modeAndPid = "010B",
            equation = "A",
            minValue = 0f,
            maxValue = 255f,
            units = "kPa",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Temperatura Aire Admisión",
            shortName = "Temp Adm",
            modeAndPid = "010F",
            equation = "A-40",
            minValue = -40f,
            maxValue = 100f,
            units = "°C",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Flujo Masa de Aire (MAF)",
            shortName = "MAF",
            modeAndPid = "0110",
            equation = "((A*256)+B)/100",
            minValue = 0f,
            maxValue = 400f,
            units = "g/s",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            name = "Nivel de Combustible",
            shortName = "Nivel Dep",
            modeAndPid = "012F",
            equation = "A*100/255",
            minValue = 0f,
            maxValue = 100f,
            units = "%",
            category = PidCategory.FUEL
        ),
        ObdPid(
            name = "Voltaje del Módulo de Control",
            shortName = "Voltaje",
            modeAndPid = "0142",
            equation = "((A*256)+B)/1000",
            minValue = 0f,
            maxValue = 18f,
            units = "V",
            category = PidCategory.ELECTRICAL
        ),
        ObdPid(
            name = "Caudal de Combustible (ECU)",
            shortName = "Consumo",
            modeAndPid = "015E",
            equation = "((A*256)+B)/20",
            minValue = 0f,
            maxValue = 50f,
            units = "L/h",
            category = PidCategory.FUEL
        )
    ).map { it.copy(id = "SAE_${it.modeAndPid}_${it.shortName}") } + StandardPidCatalog.additionalPids()

    /**
     * Devuelve la lista parseada de los PIDs de Honda iDTEC.
     */
    fun getHondaIdtecPids(): List<ObdPid> {
        val result = com.torqueobd2.dash.data.parser.TorqueCsvParser.parseString(SAMPLE_HONDA_IDTEC_CSV)
        return result.pids.map { it.copy(id = "HONDA_${it.shortName}") }
    }
}
