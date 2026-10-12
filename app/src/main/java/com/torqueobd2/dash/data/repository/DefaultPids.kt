package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory
import com.torqueobd2.dash.i18n.tr

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
            id = "SAE_010C_RPM",
            name = tr("Revoluciones del Motor", "Engine RPM"),
            shortName = "RPM",
            modeAndPid = "010C",
            equation = "((A*256)+B)/4",
            minValue = 0f,
            maxValue = 8000f,
            units = "RPM",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_010D_Velocidad",
            name = tr("Velocidad del Vehículo", "Vehicle Speed"),
            shortName = tr("Velocidad", "Speed"),
            modeAndPid = "010D",
            equation = "A",
            minValue = 0f,
            maxValue = 260f,
            units = "km/h",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_0105_Temp Liq",
            name = tr("Temperatura Refrigerante", "Coolant Temperature"),
            shortName = tr("Temp Liq", "Coolant"),
            modeAndPid = "0105",
            equation = "A-40",
            minValue = -40f,
            maxValue = 150f,
            units = "°C",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_0104_Carga",
            name = tr("Carga Calculada del Motor", "Calculated Engine Load"),
            shortName = tr("Carga", "Engine Load"),
            modeAndPid = "0104",
            equation = "A*100/255",
            minValue = 0f,
            maxValue = 100f,
            units = "%",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_0111_Acelerador",
            name = tr("Posición de Mariposa (ECU)", "Throttle Position (ECU)"),
            shortName = tr("Acelerador", "Throttle"),
            modeAndPid = "0111",
            equation = "A*100/255",
            minValue = 0f,
            maxValue = 100f,
            units = "%",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_010B_MAP",
            name = tr("Presión Absoluta Colector (MAP)", "Intake Manifold Pressure (MAP)"),
            shortName = "MAP",
            modeAndPid = "010B",
            equation = "A",
            minValue = 0f,
            maxValue = 255f,
            units = "kPa",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_010F_Temp Adm",
            name = tr("Temperatura Aire Admisión", "Intake Air Temperature"),
            shortName = tr("Temp Adm", "Intake Temp"),
            modeAndPid = "010F",
            equation = "A-40",
            minValue = -40f,
            maxValue = 100f,
            units = "°C",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_0110_MAF",
            name = tr("Flujo Masa de Aire (MAF)", "Mass Air Flow (MAF)"),
            shortName = "MAF",
            modeAndPid = "0110",
            equation = "((A*256)+B)/100",
            minValue = 0f,
            maxValue = 400f,
            units = "g/s",
            category = PidCategory.ENGINE
        ),
        ObdPid(
            id = "SAE_012F_Nivel Dep",
            name = tr("Nivel de Combustible", "Fuel Tank Level"),
            shortName = tr("Nivel Dep", "Fuel Level"),
            modeAndPid = "012F",
            equation = "A*100/255",
            minValue = 0f,
            maxValue = 100f,
            units = "%",
            category = PidCategory.FUEL
        ),
        ObdPid(
            id = "SAE_0142_Voltaje",
            name = tr("Voltaje del Módulo de Control", "Control Module Voltage"),
            shortName = tr("Voltaje", "Voltage"),
            modeAndPid = "0142",
            equation = "((A*256)+B)/1000",
            minValue = 0f,
            maxValue = 18f,
            units = "V",
            category = PidCategory.ELECTRICAL
        ),
        ObdPid(
            id = "SAE_015E_Consumo",
            name = tr("Caudal de Combustible (ECU)", "Engine Fuel Rate (ECU)"),
            shortName = tr("Consumo", "Fuel Rate"),
            modeAndPid = "015E",
            equation = "((A*256)+B)/20",
            minValue = 0f,
            maxValue = 50f,
            units = "L/h",
            category = PidCategory.FUEL
        )
    ) + StandardPidCatalog.additionalPids()

    /**
     * Devuelve la lista parseada de los PIDs de Honda iDTEC.
     */
    fun getHondaIdtecPids(): List<ObdPid> {
        val result = com.torqueobd2.dash.data.parser.TorqueCsvParser.parseString(SAMPLE_HONDA_IDTEC_CSV)
        return result.pids.map { it.copy(id = "HONDA_${it.shortName}") }
    }

    const val SAMPLE_VAG_TDI_TSI_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"VAG Masa de hollín calculada en DPF","VAG DPF hollín calc","22114E","INT16(A:B)/100",0,50,"g","7E0"
"VAG Masa de hollín medida en DPF","VAG DPF hollín med","22114F","INT16(A:B)/100",0,50,"g","7E0"
"VAG Masa de ceniza de aceite en DPF","VAG DPF ceniza","221150","INT16(A:B)/100",0,100,"g","7E0"
"VAG Distancia desde última regeneración DPF","VAG DPF dist reg","221156","INT16(A:B)",0,2000,"km","7E0"
"VAG Tiempo desde última regeneración DPF","VAG DPF tiempo reg","221157","INT16(A:B)",0,10000,"min","7E0"
"VAG Presión diferencial DPF","VAG DPF dif pres","22114C","(SIGNED(A)*256+B)/10",-50,500,"hPa","7E0"
"VAG Temperatura EGT 1 antes de turbina","VAG EGT 1 turbo","221148","INT16(A:B)/10-273.15",-40,1000,"°C","7E0"
"VAG Temperatura EGT 2 entrada DPF","VAG EGT 2 ent DPF","221149","INT16(A:B)/10-273.15",-40,1000,"°C","7E0"
"VAG Temperatura EGT 3 salida DPF","VAG EGT 3 sal DPF","22114A","INT16(A:B)/10-273.15",-40,1000,"°C","7E0"
"VAG Temperatura de aceite motor","VAG Temp aceite","22202F","INT16(A:B)/10-273.15",-40,180,"°C","7E0"
"VAG Temperatura de aceite transmisión DSG","VAG Temp DSG","221041","A-40",-40,215,"°C","7E1"
"VAG Tensión de batería en módulo motor","VAG Voltaje bat","221008","INT16(A:B)/1000",0,18,"V","7E0""""

    const val SAMPLE_FORD_TDCI_ECOBOOST_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"Ford Presión de sobrealimentación (Boost)","Ford Boost","221440","((A*256)+B)*0.03125-101.3",-100,250,"kPa","7E0"
"Ford Temperatura de culata (CHT)","Ford Temp CHT","221624","A*2",-40,250,"°C","7E0"
"Ford Temperatura de aceite motor","Ford Temp aceite","221310","((A*256)+B)/100-40",-40,180,"°C","7E0"
"Ford Carga de hollín en filtro DPF","Ford DPF hollín","22FD8C","A*100/255",0,100,"%","7E0"
"Ford Presión diferencial del filtro DPF","Ford DPF dif pres","22F47A","((A*256)+B)/100",0,100,"kPa","7E0"
"Ford Distancia desde última regeneración DPF","Ford DPF dist reg","22F480","(A*256)+B",0,5000,"km","7E0"
"Ford Temperatura de transmisión automática (TFT)","Ford Temp ATF","221E1C","(SIGNED(A)*256+B)/16",-40,180,"°C","7E0"
"Ford Estado de carga batería 12V (BMS SOC)","Ford BMS SOC","224028","A",0,100,"%","726"
"Ford Presión neumático delantero izquierdo (TPMS)","Ford TPMS DI","222813","((A*256)+B)/20",0,400,"kPa","726"
"Ford Presión neumático delantero derecho (TPMS)","Ford TPMS DD","222814","((A*256)+B)/20",0,400,"kPa","726"
"Ford Presión neumático trasero izquierdo (TPMS)","Ford TPMS TI","222815","((A*256)+B)/20",0,400,"kPa","726"
"Ford Presión neumático trasero derecho (TPMS)","Ford TPMS TD","222816","((A*256)+B)/20",0,400,"kPa","726""""

    const val SAMPLE_PSA_HDI_PURETECH_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"PSA Carga de hollín en filtro FAP","PSA FAP hollín","22D43C","A",0,100,"%","7E0"
"PSA Presión diferencial del filtro FAP","PSA FAP dif pres","22D434","INT16(A:B)/10",0,500,"mbar","7E0"
"PSA Distancia desde última regeneración FAP","PSA FAP ult reg","22D43D","INT16(A:B)",0,2000,"km","7E0"
"PSA Distancia media entre regeneraciones FAP","PSA FAP dist media","22D43E","INT16(A:B)",0,2000,"km","7E0"
"PSA Temperatura de escape entrada FAP","PSA Temp ent FAP","22D436","INT16(A:B)/10-40",-40,1000,"°C","7E0"
"PSA Nivel de aditivo FAP / AdBlue","PSA Nivel aditivo","22D4A0","A",0,100,"%","7E0""""

    const val SAMPLE_BMW_DIESEL_GASOLINA_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"BMW Masa de hollín acumulada en DPF","BMW DPF hollín","22581E","INT16(A:B)/100",0,60,"g","7E0"
"BMW Masa de ceniza acumulada en DPF","BMW DPF ceniza","22581F","INT16(A:B)/100",0,100,"g","7E0"
"BMW Contrapresión de gases de escape DPF","BMW DPF pres esc","22581B","INT16(A:B)/10",0,500,"hPa","7E0"
"BMW Estado de regeneración activa DPF","BMW DPF regen","225820","A",0,1,"estado","7E0"
"BMW Temperatura de aceite motor","BMW Temp aceite","22580F","A-48",-48,207,"°C","7E0"
"BMW Temperatura aceite caja automática ZF","BMW Temp ATF","22400A","A-40",-40,160,"°C","7E1""""

    const val SAMPLE_TOYOTA_LEXUS_HYBRID_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6
"Toyota Estado de carga batería híbrida (SOC HV)","Toyota SOC HV","2198","A*100/255",0,100,"%","7E2"
"Toyota Temperatura batería híbrida TB1","Toyota Temp HV 1","2187","(A*256+B)/100-40",-40,80,"°C","7E2"
"Toyota Temperatura inversor MG1","Toyota Temp MG1","2170","A-40",-40,150,"°C","7E2"
"Toyota Temperatura inversor MG2","Toyota Temp MG2","2171","A-40",-40,150,"°C","7E2"
"Toyota Acumulación de partículas PM en DPF D-4D","Toyota DPF PM","21E1","A*100/255",0,150,"%","7E0"
"Toyota Temperatura fluido transmisión ATF / e-CVT","Toyota Temp ATF","2182","((A*256+B)/256)-40",-40,150,"°C","7E0""""

    const val SAMPLE_HYUNDAI_KIA_CRDI_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"Hyundai/Kia Masa de hollín en DPF","HK DPF hollín","22E005","INT16(C:D)/100",0,40,"g","7E0"
"Hyundai/Kia Presión diferencial DPF","HK DPF dif pres","22E004","INT16(E:F)/10",0,200,"hPa","7E0"
"Hyundai/Kia Distancia desde última regeneración DPF","HK DPF dist reg","22E006","INT16(A:B)",0,2000,"km","7E0"
"Hyundai/Kia Temperatura EGT 1 antes de DPF","HK EGT 1","22E002","INT16(A:B)/10-40",-40,1000,"°C","7E0"
"Hyundai/Kia Temperatura EGT 2 después de DPF","HK EGT 2","22E002","INT16(C:D)/10-40",-40,1000,"°C","7E0"
"Hyundai/Kia Temperatura de transmisión AT / DCT","HK Temp Trans","220101","E-40",-40,160,"°C","7E1""""

    const val SAMPLE_RENAULT_NISSAN_DCI_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"Renault/Nissan Masa de hollín en FAP","RN FAP hollín","22242F","INT16(A:B)/100",0,50,"g","7E0"
"Renault/Nissan Presión diferencial FAP","RN FAP dif pres","22242A","INT16(A:B)",0,500,"mbar","7E0"
"Renault/Nissan Temperatura entrada turbina","RN Temp turbina","222424","INT16(A:B)/10-40",-40,1000,"°C","7E0"
"Renault/Nissan Temperatura entrada FAP","RN Temp ent FAP","222426","INT16(A:B)/10-40",-40,1000,"°C","7E0"
"Renault/Nissan Estado de regeneración FAP","RN FAP regen","222434","A",0,1,"estado","7E0"
"Renault/Nissan Dilución de aceite motor","RN Dilución aceite","222440","INT16(A:B)/100",0,20,"%","7E0""""

    const val SAMPLE_GM_OPEL_CDTI_CSV = """"Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
; init string: AT SP6\nAT SH 7E0
"Opel/GM Saturación de hollín en DPF","Opel DPF hollín","223279","A",0,100,"%","7E0"
"Opel/GM Distancia desde última regeneración DPF","Opel DPF dist reg","22327A","INT16(A:B)",0,2000,"km","7E0"
"Opel/GM Estado de regeneración activa DPF","Opel DPF regen","223278","A",0,100,"%","7E0"
"Opel/GM Vida útil restante de aceite motor","Opel Vida aceite","221154","A*100/255",0,100,"%","7E0"
"Opel/GM Temperatura fluido transmisión ATF","Opel Temp ATF","221940","A-40",-40,160,"°C","7E1""""

    private val manufacturerPidEnglishTranslations: Map<String, Pair<String, String>> = mapOf(
        // VAG
        "VAG_VAG DPF hollín calc" to ("VAG DPF calculated soot mass" to "VAG DPF soot calc"),
        "VAG_VAG DPF hollín med" to ("VAG DPF measured soot mass" to "VAG DPF soot meas"),
        "VAG_VAG DPF ceniza" to ("VAG DPF oil ash mass" to "VAG DPF ash"),
        "VAG_VAG DPF dist reg" to ("VAG Distance since last DPF regeneration" to "VAG DPF reg dist"),
        "VAG_VAG DPF tiempo reg" to ("VAG Time since last DPF regeneration" to "VAG DPF reg time"),
        "VAG_VAG DPF dif pres" to ("VAG DPF differential pressure" to "VAG DPF diff pres"),
        "VAG_VAG EGT 1 turbo" to ("VAG EGT 1 before turbine" to "VAG EGT 1 turbo"),
        "VAG_VAG EGT 2 ent DPF" to ("VAG EGT 2 DPF inlet" to "VAG EGT 2 DPF in"),
        "VAG_VAG EGT 3 sal DPF" to ("VAG EGT 3 DPF outlet" to "VAG EGT 3 DPF out"),
        "VAG_VAG Temp aceite" to ("VAG Engine oil temperature" to "VAG Oil temp"),
        "VAG_VAG Temp DSG" to ("VAG DSG transmission oil temperature" to "VAG DSG temp"),
        "VAG_VAG Voltaje bat" to ("VAG Battery voltage at engine module" to "VAG Batt voltage"),
        // Ford
        "FORD_Ford Boost" to ("Ford Boost pressure" to "Ford Boost"),
        "FORD_Ford Temp CHT" to ("Ford Cylinder head temperature (CHT)" to "Ford CHT temp"),
        "FORD_Ford Temp aceite" to ("Ford Engine oil temperature" to "Ford Oil temp"),
        "FORD_Ford DPF hollín" to ("Ford DPF soot load" to "Ford DPF soot"),
        "FORD_Ford DPF dif pres" to ("Ford DPF differential pressure" to "Ford DPF diff pres"),
        "FORD_Ford DPF dist reg" to ("Ford Distance since last DPF regeneration" to "Ford DPF reg dist"),
        "FORD_Ford Temp ATF" to ("Ford Automatic transmission temperature (TFT)" to "Ford ATF temp"),
        "FORD_Ford BMS SOC" to ("Ford 12V battery state of charge (BMS SOC)" to "Ford BMS SOC"),
        "FORD_Ford TPMS DI" to ("Ford Front left tire pressure (TPMS)" to "Ford TPMS FL"),
        "FORD_Ford TPMS DD" to ("Ford Front right tire pressure (TPMS)" to "Ford TPMS FR"),
        "FORD_Ford TPMS TI" to ("Ford Rear left tire pressure (TPMS)" to "Ford TPMS RL"),
        "FORD_Ford TPMS TD" to ("Ford Rear right tire pressure (TPMS)" to "Ford TPMS RR"),
        // PSA
        "PSA_PSA FAP hollín" to ("PSA FAP soot load" to "PSA FAP soot"),
        "PSA_PSA FAP dif pres" to ("PSA FAP differential pressure" to "PSA FAP diff pres"),
        "PSA_PSA FAP ult reg" to ("PSA Distance since last FAP regeneration" to "PSA FAP last reg"),
        "PSA_PSA FAP dist media" to ("PSA Average distance between FAP regenerations" to "PSA FAP avg dist"),
        "PSA_PSA Temp ent FAP" to ("PSA FAP inlet exhaust temperature" to "PSA FAP in temp"),
        "PSA_PSA Nivel aditivo" to ("PSA FAP additive / AdBlue level" to "PSA Additive lvl"),
        // BMW
        "BMW_BMW DPF hollín" to ("BMW Accumulated DPF soot mass" to "BMW DPF soot"),
        "BMW_BMW DPF ceniza" to ("BMW Accumulated DPF ash mass" to "BMW DPF ash"),
        "BMW_BMW DPF pres esc" to ("BMW DPF exhaust backpressure" to "BMW DPF backpres"),
        "BMW_BMW DPF regen" to ("BMW Active DPF regeneration status" to "BMW DPF regen"),
        "BMW_BMW Temp aceite" to ("BMW Engine oil temperature" to "BMW Oil temp"),
        "BMW_BMW Temp ATF" to ("BMW ZF automatic gearbox oil temperature" to "BMW ATF temp"),
        // Toyota
        "TOYOTA_Toyota SOC HV" to ("Toyota Hybrid battery state of charge (HV SOC)" to "Toyota HV SOC"),
        "TOYOTA_Toyota Temp HV 1" to ("Toyota Hybrid battery temperature TB1" to "Toyota HV temp 1"),
        "TOYOTA_Toyota Temp MG1" to ("Toyota MG1 inverter temperature" to "Toyota MG1 temp"),
        "TOYOTA_Toyota Temp MG2" to ("Toyota MG2 inverter temperature" to "Toyota MG2 temp"),
        "TOYOTA_Toyota DPF PM" to ("Toyota D-4D DPF particulate matter accumulation" to "Toyota DPF PM"),
        "TOYOTA_Toyota Temp ATF" to ("Toyota ATF / e-CVT transmission fluid temperature" to "Toyota ATF temp"),
        // Hyundai / Kia
        "HK_HK DPF hollín" to ("Hyundai/Kia DPF soot mass" to "HK DPF soot"),
        "HK_HK DPF dif pres" to ("Hyundai/Kia DPF differential pressure" to "HK DPF diff pres"),
        "HK_HK DPF dist reg" to ("Hyundai/Kia Distance since last DPF regeneration" to "HK DPF reg dist"),
        "HK_HK EGT 1" to ("Hyundai/Kia EGT 1 before DPF" to "HK EGT 1"),
        "HK_HK EGT 2" to ("Hyundai/Kia EGT 2 after DPF" to "HK EGT 2"),
        "HK_HK Temp Trans" to ("Hyundai/Kia AT / DCT transmission temperature" to "HK Trans Temp"),
        // Renault / Nissan
        "RN_RN FAP hollín" to ("Renault/Nissan FAP soot mass" to "RN FAP soot"),
        "RN_RN FAP dif pres" to ("Renault/Nissan FAP differential pressure" to "RN FAP diff pres"),
        "RN_RN Temp turbina" to ("Renault/Nissan Turbine inlet temperature" to "RN Turbine temp"),
        "RN_RN Temp ent FAP" to ("Renault/Nissan FAP inlet temperature" to "RN FAP in temp"),
        "RN_RN FAP regen" to ("Renault/Nissan FAP regeneration status" to "RN FAP regen"),
        "RN_RN Dilución aceite" to ("Renault/Nissan Engine oil dilution" to "RN Oil dilution"),
        // Opel / GM
        "OPEL_Opel DPF hollín" to ("Opel/GM DPF soot saturation" to "Opel DPF soot"),
        "OPEL_Opel DPF dist reg" to ("Opel/GM Distance since last DPF regeneration" to "Opel DPF reg dist"),
        "OPEL_Opel DPF regen" to ("Opel/GM Active DPF regeneration status" to "Opel DPF regen"),
        "OPEL_Opel Vida aceite" to ("Opel/GM Remaining engine oil life" to "Opel Oil life"),
        "OPEL_Opel Temp ATF" to ("Opel/GM ATF transmission fluid temperature" to "Opel ATF temp")
    )

    data class ManufacturerPidPack(
        val id: String,
        val name: String,
        val subtitle: String,
        val csv: String,
        val pids: List<ObdPid>
    )

    private fun parsePack(prefix: String, id: String, name: String, subtitle: String, csv: String): ManufacturerPidPack {
        val parsed = com.torqueobd2.dash.data.parser.TorqueCsvParser.parseString(csv).pids.map {
            val canonicalId = "${prefix}_${it.shortName}"
            val enPair = manufacturerPidEnglishTranslations[canonicalId]
            if (enPair != null) {
                it.copy(
                    id = canonicalId,
                    name = tr(it.name, enPair.first),
                    shortName = tr(it.shortName, enPair.second)
                )
            } else {
                it.copy(id = canonicalId)
            }
        }
        return ManufacturerPidPack(id = id, name = name, subtitle = subtitle, csv = csv, pids = parsed)
    }

    /**
     * Conjuntos predefinidos de PIDs extendidos de fabricante (estilo Torque Pro).
     */
    fun getManufacturerPacks(): List<ManufacturerPidPack> = listOf(
        ManufacturerPidPack(
            id = "honda_idtec",
            name = "Honda i-DTEC (1.6 / 2.2)",
            subtitle = tr(
                "DPF, regeneración, presión diferencial, EGT y nivel de aceite",
                "DPF, regeneration, differential pressure, EGT, and oil level"
            ),
            csv = SAMPLE_HONDA_IDTEC_CSV,
            pids = getHondaIdtecPids()
        ),
        parsePack(
            prefix = "VAG",
            id = "vag_tdi_tsi",
            name = "VW / Audi / Seat / Škoda (TDI / TSI)",
            subtitle = tr(
                "Hollín y ceniza DPF, regeneración, EGTs, aceite y DSG (Modo 22)",
                "DPF soot & ash, regeneration, EGTs, oil, and DSG (Mode 22)"
            ),
            csv = SAMPLE_VAG_TDI_TSI_CSV
        ),
        parsePack(
            prefix = "FORD",
            id = "ford_tdci_ecoboost",
            name = "Ford (TDCi / EcoBoost)",
            subtitle = tr(
                "Boost, culata CHT, DPF, temperatura ATF, batería BMS y TPMS",
                "Boost, CHT cylinder head, DPF, ATF temperature, BMS battery, and TPMS"
            ),
            csv = SAMPLE_FORD_TDCI_ECOBOOST_CSV
        ),
        parsePack(
            prefix = "PSA",
            id = "psa_hdi_puretech",
            name = "Peugeot / Citroën / DS / Opel (HDi / BlueHDi)",
            subtitle = tr(
                "Carga y presión FAP, historial de regeneración, EGT y aditivo",
                "FAP load & pressure, regeneration history, EGT, and additive"
            ),
            csv = SAMPLE_PSA_HDI_PURETECH_CSV
        ),
        parsePack(
            prefix = "BMW",
            id = "bmw_diesel_gasolina",
            name = "BMW (N47 / B47 / N57 / B48 / B58)",
            subtitle = tr(
                "Hollín y ceniza DPF, contrapresión escape, aceite y caja ZF",
                "DPF soot & ash, exhaust backpressure, oil, and ZF gearbox"
            ),
            csv = SAMPLE_BMW_DIESEL_GASOLINA_CSV
        ),
        parsePack(
            prefix = "TOYOTA",
            id = "toyota_lexus_hybrid",
            name = "Toyota / Lexus (Hybrid HSD & D-4D)",
            subtitle = tr(
                "SOC y temperatura batería HV, inversores MG1/MG2, DPF y e-CVT",
                "HV battery SOC & temperature, MG1/MG2 inverters, DPF, and e-CVT"
            ),
            csv = SAMPLE_TOYOTA_LEXUS_HYBRID_CSV
        ),
        parsePack(
            prefix = "HK",
            id = "hyundai_kia_crdi",
            name = "Hyundai / Kia (CRDi / T-GDi)",
            subtitle = tr(
                "Hollín y presión diferencial DPF, EGT 1/2 y temperatura DCT/AT",
                "DPF soot & differential pressure, EGT 1/2, and DCT/AT temperature"
            ),
            csv = SAMPLE_HYUNDAI_KIA_CRDI_CSV
        ),
        parsePack(
            prefix = "RN",
            id = "renault_nissan_dci",
            name = "Renault / Nissan / Dacia (dCi / TCe)",
            subtitle = tr(
                "Masa de hollín FAP, presión diferencial, turbina y dilución de aceite",
                "FAP soot mass, differential pressure, turbine, and oil dilution"
            ),
            csv = SAMPLE_RENAULT_NISSAN_DCI_CSV
        ),
        parsePack(
            prefix = "OPEL",
            id = "gm_opel_cdti",
            name = "Opel / GM (CDTI / Ecotec)",
            subtitle = tr(
                "Saturación y regeneración DPF, vida útil de aceite y temperatura ATF",
                "DPF saturation & regeneration, oil life, and ATF temperature"
            ),
            csv = SAMPLE_GM_OPEL_CDTI_CSV
        )
    )

    fun getAllBuiltInPidsById(): Map<String, ObdPid> =
        (getStandardPids() + getManufacturerPacks().flatMap { it.pids }).associateBy { it.id }
}

