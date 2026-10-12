package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.i18n.AppLanguage
import com.torqueobd2.dash.i18n.tr
import java.io.Reader

/** Definiciones genéricas offline; los códigos propietarios no se asignan a una marca por su nombre. */
class DtcCatalog(private val definitions: Map<String, String>) {
    data class Description(val text: String, val source: String)

    fun describe(code: String, language: AppLanguage = AppI18n.currentLanguage): Description {
        when (language) {
            AppLanguage.ES -> {
                spanish[code]?.let { return Description(it, "Definición genérica · Español") }
                definitions[code]?.let { return Description(it, "Catálogo genérico · Inglés") }
            }
            AppLanguage.FR -> {
                french[code]?.let {
                    return Description(it, tr("Definición genérica · Francés", "Generic definition · French", language))
                }
                definitions[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
                english[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
            }
            AppLanguage.DE -> {
                german[code]?.let {
                    return Description(it, tr("Definición genérica · Alemán", "Generic definition · German", language))
                }
                definitions[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
                english[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
            }
            AppLanguage.PT -> {
                portuguese[code]?.let {
                    return Description(it, tr("Definición genérica · Portugués", "Generic definition · Portuguese", language))
                }
                definitions[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
                english[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
            }
            AppLanguage.IT -> {
                italian[code]?.let {
                    return Description(it, tr("Definición genérica · Italiano", "Generic definition · Italian", language))
                }
                definitions[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
                english[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
            }
            AppLanguage.EN -> {
                definitions[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
                english[code]?.let {
                    return Description(it, tr("Catálogo genérico · Inglés", "Generic definition · English", language))
                }
            }
        }
        spanish[code]?.let {
            return Description(it, tr("Definición genérica · Español", "Generic definition · Spanish", language))
        }
        return Description(
            tr(
                "Definición no incluida. Consulta la documentación del fabricante para este código y esta ECU.",
                "Definition not included. Check the manufacturer documentation for this code and ECU.",
                language
            ),
            tr("Sin definición verificada", "No verified definition", language)
        )
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

        private val english = mapOf(
            "P0100" to "Mass or Volume Air Flow (MAF) Circuit",
            "P0101" to "Mass or Volume Air Flow (MAF) Circuit Range/Performance",
            "P0102" to "Mass or Volume Air Flow (MAF) Circuit Low Input",
            "P0103" to "Mass or Volume Air Flow (MAF) Circuit High Input",
            "P0105" to "Manifold Absolute Pressure (MAP) Circuit",
            "P0106" to "Manifold Absolute Pressure (MAP) Circuit Range/Performance",
            "P0107" to "Manifold Absolute Pressure (MAP) Circuit Low Input",
            "P0108" to "Manifold Absolute Pressure (MAP) Circuit High Input",
            "P0110" to "Intake Air Temperature Sensor 1 Circuit",
            "P0115" to "Engine Coolant Temperature Sensor 1 Circuit",
            "P0116" to "Engine Coolant Temperature Sensor 1 Circuit Range/Performance",
            "P0117" to "Engine Coolant Temperature Sensor 1 Circuit Low",
            "P0118" to "Engine Coolant Temperature Sensor 1 Circuit High",
            "P0120" to "Throttle/Pedal Position Sensor/Switch A Circuit",
            "P0128" to "Coolant Thermostat (Coolant Temperature Below Thermostat Regulating Temperature)",
            "P0130" to "O2 Sensor Circuit (Bank 1 Sensor 1)",
            "P0131" to "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1)",
            "P0132" to "O2 Sensor Circuit High Voltage (Bank 1 Sensor 1)",
            "P0133" to "O2 Sensor Circuit Slow Response (Bank 1 Sensor 1)",
            "P0134" to "O2 Sensor Circuit No Activity Detected (Bank 1 Sensor 1)",
            "P0135" to "O2 Sensor Heater Circuit (Bank 1 Sensor 1)",
            "P0141" to "O2 Sensor Heater Circuit (Bank 1 Sensor 2)",
            "P0171" to "System Too Lean (Bank 1)",
            "P0172" to "System Too Rich (Bank 1)",
            "P0174" to "System Too Lean (Bank 2)",
            "P0175" to "System Too Rich (Bank 2)",
            "P0190" to "Fuel Rail Pressure Sensor Circuit",
            "P0191" to "Fuel Rail Pressure Sensor Circuit Range/Performance",
            "P0200" to "Injector Circuit / Open",
            "P0234" to "Turbocharger/Supercharger Overboost Condition",
            "P0299" to "Turbocharger/Supercharger Underboost",
            "P0300" to "Random/Multiple Cylinder Misfire Detected",
            "P0325" to "Knock Sensor 1 Circuit (Bank 1 or Single Sensor)",
            "P0335" to "Crankshaft Position Sensor A Circuit",
            "P0340" to "Camshaft Position Sensor A Circuit (Bank 1 or Single Sensor)",
            "P0400" to "Exhaust Gas Recirculation (EGR) Flow",
            "P0401" to "Exhaust Gas Recirculation (EGR) Flow Insufficient Detected",
            "P0402" to "Exhaust Gas Recirculation (EGR) Flow Excessive Detected",
            "P0403" to "Exhaust Gas Recirculation (EGR) Control Circuit",
            "P0420" to "Catalyst System Efficiency Below Threshold (Bank 1)",
            "P0430" to "Catalyst System Efficiency Below Threshold (Bank 2)",
            "P0440" to "Evaporative Emission System (EVAP)",
            "P0442" to "Evaporative Emission System Leak Detected (Small Leak)",
            "P0455" to "Evaporative Emission System Leak Detected (Large Leak)",
            "P0456" to "Evaporative Emission System Leak Detected (Very Small Leak)",
            "P0500" to "Vehicle Speed Sensor A",
            "P0505" to "Idle Air Control System",
            "P0560" to "System Voltage",
            "P0562" to "System Voltage Low",
            "P0563" to "System Voltage High",
            "P0600" to "Serial Communication Link",
            "P0606" to "ECM/PCM Processor",
            "P0700" to "Transmission Control System (MIL Request)",
            "P2002" to "Diesel Particulate Filter Efficiency Below Threshold (Bank 1)",
            "P2463" to "Diesel Particulate Filter Restriction - Soot Accumulation",
            "U0100" to "Lost Communication With ECM/PCM A",
            "U0101" to "Lost Communication With TCM",
            "U0121" to "Lost Communication With Anti-Lock Brake System (ABS) Control Module",
            "U0140" to "Lost Communication With Body Control Module"
        ) + (1..12).associate { "P030$it".let { code -> if (it < 10) code else "P03%02d".format(it) } to "Cylinder $it Misfire Detected" }

        private val french = mapOf(
            "P0100" to "Circuit du débitmètre d'air massique ou volumique (MAF)",
            "P0101" to "Débitmètre d'air (MAF) : plage ou performance",
            "P0102" to "Signal bas du circuit MAF", "P0103" to "Signal haut du circuit MAF",
            "P0105" to "Circuit de pression absolue du collecteur (MAP)",
            "P0106" to "Pression MAP : plage ou performance", "P0107" to "Signal bas du circuit MAP",
            "P0108" to "Signal haut du circuit MAP", "P0110" to "Circuit de température d'air d'admission",
            "P0115" to "Circuit de température du liquide de refroidissement", "P0116" to "Température du liquide de refroidissement : plage ou performance",
            "P0117" to "Signal bas du circuit de température de refroidissement", "P0118" to "Signal haut du circuit de température de refroidissement",
            "P0120" to "Circuit du capteur de position papillon/pédale A", "P0128" to "Liquide de refroidissement sous la température de régulation du thermostat",
            "P0130" to "Circuit de sonde à oxygène : banc 1, capteur 1", "P0131" to "Signal bas de sonde à oxygène : banc 1, capteur 1",
            "P0132" to "Signal haut de sonde à oxygène : banc 1, capteur 1", "P0133" to "Réponse lente de sonde à oxygène : banc 1, capteur 1",
            "P0134" to "Aucune activité de sonde à oxygène : banc 1, capteur 1", "P0135" to "Circuit de chauffage de sonde à oxygène : banc 1, capteur 1",
            "P0141" to "Circuit de chauffage de sonde à oxygène : banc 1, capteur 2",
            "P0171" to "Mélange trop pauvre : banc 1", "P0172" to "Mélange trop riche : banc 1",
            "P0174" to "Mélange trop pauvre : banc 2", "P0175" to "Mélange trop riche : banc 2",
            "P0190" to "Circuit du capteur de pression de rampe d'injection", "P0191" to "Pression de carburant : plage ou performance",
            "P0200" to "Circuit des injecteurs", "P0234" to "Pression de suralimentation excessive",
            "P0299" to "Pression de suralimentation insuffisante", "P0300" to "Ratés d'allumage aléatoires ou sur plusieurs cylindres",
            "P0325" to "Circuit du capteur de cliquetis 1", "P0335" to "Circuit du capteur de position du vilebrequin A",
            "P0340" to "Circuit du capteur de position d'arbre à cames A : banc 1 ou capteur unique",
            "P0400" to "Débit de recirculation des gaz d'échappement (EGR)", "P0401" to "Débit EGR insuffisant",
            "P0402" to "Débit EGR excessif", "P0403" to "Circuit de commande EGR",
            "P0420" to "Efficacité du catalyseur inférieure au seuil : banc 1",
            "P0430" to "Efficacité du catalyseur inférieure au seuil : banc 2",
            "P0440" to "Système de contrôle des émissions par évaporation (EVAP)", "P0442" to "Petite fuite détectée dans le système EVAP",
            "P0455" to "Grande fuite détectée dans le système EVAP", "P0456" to "Très petite fuite détectée dans le système EVAP",
            "P0500" to "Capteur de vitesse du véhicule", "P0505" to "Système de régulation du ralenti",
            "P0560" to "Tension du système", "P0562" to "Tension du système basse", "P0563" to "Tension du système haute",
            "P0600" to "Liaison de communication série", "P0606" to "Processeur du module de commande ECM/PCM",
            "P0700" to "Système de commande de boîte de vitesses : demande d'allumage du témoin MIL",
            "P2002" to "Efficacité du filtre à particules inférieure au seuil : banc 1",
            "P2463" to "Restriction du filtre à particules : accumulation de suie",
            "U0100" to "Perte de communication avec le module de commande moteur ECM/PCM A",
            "U0101" to "Perte de communication avec le module de commande de transmission TCM",
            "U0121" to "Perte de communication avec le module de commande ABS",
            "U0140" to "Perte de communication avec le module de commande carrosserie BCM"
        ) + (1..12).associate { "P030$it".let { code -> if (it < 10) code else "P03%02d".format(it) } to "Raté d'allumage détecté sur le cylindre $it" }

        private val german = mapOf(
            "P0100" to "Stromkreis Luftmassen-/Luftmengenmesser (MAF)",
            "P0101" to "Luftmassenmesser (MAF): Bereich oder Funktion",
            "P0102" to "MAF-Stromkreis: Signal zu niedrig", "P0103" to "MAF-Stromkreis: Signal zu hoch",
            "P0105" to "Stromkreis Saugrohr-Absolutdrucksensor (MAP)",
            "P0106" to "MAP-Druck: Bereich oder Funktion", "P0107" to "MAP-Stromkreis: Signal zu niedrig",
            "P0108" to "MAP-Stromkreis: Signal zu hoch", "P0110" to "Stromkreis Ansauglufttemperatursensor",
            "P0115" to "Stromkreis Kühlmitteltemperatursensor", "P0116" to "Kühlmitteltemperatur: Bereich oder Funktion",
            "P0117" to "Kühlmitteltemperatursensor: Signal zu niedrig", "P0118" to "Kühlmitteltemperatursensor: Signal zu hoch",
            "P0120" to "Stromkreis Drosselklappen-/Fahrpedalsensor A", "P0128" to "Kühlmitteltemperatur unterhalb der Thermostat-Regeltemperatur",
            "P0130" to "Stromkreis Lambdasonde: Bank 1, Sensor 1", "P0131" to "Lambdasonde Spannung zu niedrig: Bank 1, Sensor 1",
            "P0132" to "Lambdasonde Spannung zu hoch: Bank 1, Sensor 1", "P0133" to "Lambdasonde träges Ansprechverhalten: Bank 1, Sensor 1",
            "P0134" to "Lambdasonde keine Aktivität erkannt: Bank 1, Sensor 1", "P0135" to "Heizstromkreis Lambdasonde: Bank 1, Sensor 1",
            "P0141" to "Heizstromkreis Lambdasonde: Bank 1, Sensor 2",
            "P0171" to "Gemisch zu mager: Bank 1", "P0172" to "Gemisch zu fett: Bank 1",
            "P0174" to "Gemisch zu mager: Bank 2", "P0175" to "Gemisch zu fett: Bank 2",
            "P0190" to "Stromkreis Kraftstoffverteilerrohr-Drucksensor", "P0191" to "Kraftstoffdrucksensor: Bereich oder Funktion",
            "P0200" to "Stromkreis Einspritzventile", "P0234" to "Ladedruckregelung: Ladedruck zu hoch (Overboost)",
            "P0299" to "Ladedruckregelung: Ladedruck zu niedrig (Underboost)", "P0300" to "Zufällige/mehrere Zylinder Verbrennungsaussetzer erkannt",
            "P0325" to "Stromkreis Klopfsensor 1", "P0335" to "Stromkreis Kurbelwellenpositionssensor A",
            "P0340" to "Stromkreis Nockenwellenpositionssensor A: Bank 1 oder Einzelsensor",
            "P0400" to "Abgasrückführungssystem (AGR): Durchfluss", "P0401" to "Abgasrückführung (AGR): Durchfluss zu gering",
            "P0402" to "Abgasrückführung (AGR): Durchfluss zu hoch", "P0403" to "Steuerstromkreis Abgasrückführung (AGR)",
            "P0420" to "Katalysator-Umwandlungsrate unter Schwellenwert: Bank 1",
            "P0430" to "Katalysator-Umwandlungsrate unter Schwellenwert: Bank 2",
            "P0440" to "Kraftstoffverdunstungs-Rückhaltesystem (EVAP)", "P0442" to "EVAP-System: kleines Leck erkannt",
            "P0455" to "EVAP-System: großes Leck erkannt", "P0456" to "EVAP-System: sehr kleines Leck erkannt",
            "P0500" to "Fahrzeuggeschwindigkeitssensor A", "P0505" to "Leerlaufregelsystem",
            "P0560" to "Systemspannung", "P0562" to "Systemspannung zu niedrig", "P0563" to "Systemspannung zu hoch",
            "P0600" to "Serielle Kommunikationsverbindung", "P0606" to "Steuergeräteprozessor ECM/PCM",
            "P0700" to "Getriebesteuerungssystem: Anforderung Warnleuchte (MIL)",
            "P2002" to "Wirkungsgrad Dieselpartikelfilter unter Schwellenwert: Bank 1",
            "P2463" to "Dieselpartikelfilter verstopft: Rußansammlung",
            "U0100" to "Kommunikationsverlust zum Motorsteuergerät ECM/PCM A",
            "U0101" to "Kommunikationsverlust zum Getriebesteuergerät TCM",
            "U0121" to "Kommunikationsverlust zum ABS-Steuergerät",
            "U0140" to "Kommunikationsverlust zum Bordnetz-/Karosseriesteuergerät BCM"
        ) + (1..12).associate { "P030$it".let { code -> if (it < 10) code else "P03%02d".format(it) } to "Verbrennungsaussetzer Zylinder $it erkannt" }

        private val portuguese = mapOf(
            "P0100" to "Circuito do sensor de fluxo de massa de ar (MAF)",
            "P0101" to "Fluxo de ar (MAF): gama ou desempenho",
            "P0102" to "Sinal baixo do circuito MAF", "P0103" to "Sinal alto do circuito MAF",
            "P0105" to "Circuito de pressão absoluta do coletor (MAP)",
            "P0106" to "Pressão MAP: gama ou desempenho", "P0107" to "Sinal baixo do circuito MAP",
            "P0108" to "Sinal alto do circuito MAP", "P0110" to "Circuito de temperatura do ar de admissão",
            "P0115" to "Circuito de temperatura do líquido de arrefecimento", "P0116" to "Temperatura do líquido de arrefecimento: gama ou desempenho",
            "P0117" to "Sinal baixo do circuito de temperatura do líquido de arrefecimento", "P0118" to "Sinal alto do circuito de temperatura do líquido de arrefecimento",
            "P0120" to "Circuito do sensor de posição da borboleta/pedal A", "P0128" to "Líquido de arrefecimento abaixo da temperatura de regulação do termóstato",
            "P0130" to "Circuito da sonda de oxigénio: banco 1, sensor 1", "P0131" to "Sinal baixo da sonda de oxigénio: banco 1, sensor 1",
            "P0132" to "Sinal alto da sonda de oxigénio: banco 1, sensor 1", "P0133" to "Resposta lenta da sonda de oxigénio: banco 1, sensor 1",
            "P0134" to "Sem atividade da sonda de oxigénio: banco 1, sensor 1", "P0135" to "Circuito de aquecimento da sonda de oxigénio: banco 1, sensor 1",
            "P0141" to "Circuito de aquecimento da sonda de oxigénio: banco 1, sensor 2",
            "P0171" to "Mistura demasiado pobre: banco 1", "P0172" to "Mistura demasiado rica: banco 1",
            "P0174" to "Mistura demasiado pobre: banco 2", "P0175" to "Mistura demasiado rica: banco 2",
            "P0190" to "Circuito do sensor de pressão da rampa de combustível", "P0191" to "Pressão de combustível: gama ou desempenho",
            "P0200" to "Circuito dos injetores", "P0234" to "Pressão de sobrealimentação excessiva",
            "P0299" to "Pressão de sobrealimentação insuficiente", "P0300" to "Falhas de ignição aleatórias ou em vários cilindros",
            "P0325" to "Circuito do sensor de detonação 1", "P0335" to "Circuito do sensor de posição da cambota A",
            "P0340" to "Circuito do sensor de posição da árvore de cames A: banco 1 ou sensor único",
            "P0400" to "Fluxo de recirculação de gases de escape (EGR)", "P0401" to "Fluxo EGR insuficiente",
            "P0402" to "Fluxo EGR excessivo", "P0403" to "Circuito de controlo EGR",
            "P0420" to "Eficiência do catalisador abaixo do limiar: banco 1",
            "P0430" to "Eficiência do catalisador abaixo do limiar: banco 2",
            "P0440" to "Sistema de controlo de emissões evaporativas (EVAP)", "P0442" to "Fuga pequena no sistema EVAP",
            "P0455" to "Fuga grande no sistema EVAP", "P0456" to "Fuga muito pequena no sistema EVAP",
            "P0500" to "Sensor de velocidade do veículo", "P0505" to "Sistema de controlo de ralenti",
            "P0560" to "Tensão do sistema", "P0562" to "Tensão do sistema baixa", "P0563" to "Tensão do sistema alta",
            "P0600" to "Ligação de comunicação série", "P0606" to "Processador do módulo de controlo",
            "P0700" to "Sistema de controlo da transmissão: pedido de ativação da luz MIL",
            "P2002" to "Eficiência do filtro de partículas abaixo do limiar: banco 1",
            "P2463" to "Restrição do filtro de partículas por acumulação de fuligem",
            "U0100" to "Perda de comunicação com o módulo de controlo do motor ECM/PCM A",
            "U0101" to "Perda de comunicação com o módulo de controlo da transmissão TCM",
            "U0121" to "Perda de comunicação com o módulo de controlo ABS",
            "U0140" to "Perda de comunicação com o módulo de controlo da carroçaria"
        ) + (1..12).associate { "P030$it".let { code -> if (it < 10) code else "P03%02d".format(it) } to "Falha de ignição detetada no cilindro $it" }

        private val italian = mapOf(
            "P0100" to "Circuito sensore portata aria massica o volumetrica (MAF)",
            "P0101" to "Portata aria (MAF): campo di misura o prestazioni",
            "P0102" to "Segnale basso circuito MAF", "P0103" to "Segnale alto circuito MAF",
            "P0105" to "Circuito pressione assoluta collettore (MAP)",
            "P0106" to "Pressione MAP: campo di misura o prestazioni", "P0107" to "Segnale basso circuito MAP",
            "P0108" to "Segnale alto circuito MAP", "P0110" to "Circuito temperatura aria aspirata",
            "P0115" to "Circuito temperatura liquido refrigerante", "P0116" to "Temperatura refrigerante: campo di misura o prestazioni",
            "P0117" to "Segnale basso circuito temperatura refrigerante", "P0118" to "Segnale alto circuito temperatura refrigerante",
            "P0120" to "Circuito sensore posizione farfalla/pedale A", "P0128" to "Temperatura refrigerante inferiore alla temperatura di regolazione del termostato",
            "P0130" to "Circuito sonda lambda: bancata 1, sensore 1", "P0131" to "Segnale basso sonda lambda: bancata 1, sensore 1",
            "P0132" to "Segnale alto sonda lambda: bancata 1, sensore 1", "P0133" to "Risposta lenta sonda lambda: bancata 1, sensore 1",
            "P0134" to "Nessuna attività rilevata sonda lambda: bancata 1, sensore 1", "P0135" to "Circuito riscaldatore sonda lambda: bancata 1, sensore 1",
            "P0141" to "Circuito riscaldatore sonda lambda: bancata 1, sensore 2",
            "P0171" to "Miscela troppo magra: bancata 1", "P0172" to "Miscela troppo ricca: bancata 1",
            "P0174" to "Miscela troppo magra: bancata 2", "P0175" to "Miscela troppo ricca: bancata 2",
            "P0190" to "Circuito sensore pressione rail carburante", "P0191" to "Pressione carburante: campo di misura o prestazioni",
            "P0200" to "Circuito iniettori", "P0234" to "Pressione di sovralimentazione eccessiva",
            "P0299" to "Pressione di sovralimentazione insufficiente", "P0300" to "Mancate accensioni casuali o su più cilindri",
            "P0325" to "Circuito sensore di detonazione 1", "P0335" to "Circuito sensore posizione albero motore A",
            "P0340" to "Circuito sensore posizione albero a camme A: bancata 1 o sensore singolo",
            "P0400" to "Flusso ricircolo gas di scarico (EGR)", "P0401" to "Flusso EGR insufficiente",
            "P0402" to "Flusso EGR eccessivo", "P0403" to "Circuito di controllo EGR",
            "P0420" to "Efficienza catalizzatore sotto la soglia: bancata 1",
            "P0430" to "Efficienza catalizzatore sotto la soglia: bancata 2",
            "P0440" to "Sistema controllo emissioni evaporative (EVAP)", "P0442" to "Piccola perdita rilevata nel sistema EVAP",
            "P0455" to "Grande perdita rilevata nel sistema EVAP", "P0456" to "Perdita molto piccola rilevata nel sistema EVAP",
            "P0500" to "Sensore velocità veicolo", "P0505" to "Sistema di controllo del minimo",
            "P0560" to "Tensione di sistema", "P0562" to "Tensione di sistema bassa", "P0563" to "Tensione di sistema alta",
            "P0600" to "Collegamento di comunicazione seriale", "P0606" to "Processore centralina di controllo ECM/PCM",
            "P0700" to "Sistema di controllo cambio: richiesta accensione spia MIL",
            "P2002" to "Efficienza filtro antiparticolato sotto la soglia: bancata 1",
            "P2463" to "Restrizione filtro antiparticolato per accumulo di particolato",
            "U0100" to "Perdita di comunicazione con centralina motore ECM/PCM A",
            "U0101" to "Perdita di comunicazione con centralina cambio TCM",
            "U0121" to "Perdita di comunicazione con centralina ABS",
            "U0140" to "Perdita di comunicazione con centralina carrozzeria BCM"
        ) + (1..12).associate { "P030$it".let { code -> if (it < 10) code else "P03%02d".format(it) } to "Mancata accensione rilevata nel cilindro $it" }
    }
}
