package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.i18n.tr

class PidFieldUnavailableException : IllegalArgumentException(tr("Campo no disponible en la ECU", "Field unavailable in the ECU"))

/** Aplica la decodificación y las condiciones de presencia del campo, sin respaldos. */
object PidDecoder {
    fun decode(pid: ObdPid, variables: Map<String, Double>): Double {
        if (pid.availabilityEquation.isNotBlank() &&
            EquationEvaluator.evaluate(pid.availabilityEquation, variables) == 0.0) {
            throw PidFieldUnavailableException()
        }
        for ((name, unavailable) in pid.unavailableRawValues) {
            val raw = variables[name] ?: throw IllegalArgumentException(tr("Falta el byte $name", "Missing byte $name"))
            if (raw.toInt() in unavailable) throw PidFieldUnavailableException()
        }
        return EquationEvaluator.evaluate(pid.equation, variables)
    }
}
