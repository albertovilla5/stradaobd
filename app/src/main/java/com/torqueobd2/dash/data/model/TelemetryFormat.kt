package com.torqueobd2.dash.data.model

import java.util.Locale

/** Ausencia de telemetría nunca se presenta como un cero medido. */
fun Float.telemetryText(decimals: Int = 0): String =
    if (isFinite()) String.format(Locale.US, "%.${decimals}f", this) else "—"
