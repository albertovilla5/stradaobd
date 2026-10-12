package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.tr
import org.json.JSONArray
import org.json.JSONObject

/** Diseños, sin lecturas ni comandos de inicialización: importar no modifica la biblioteca de sensores. */
object DashboardDesignCodec {
    const val MAX_BYTES = 2_000_000
    private fun integer(json: JSONObject, key: String): Int {
        val value = json.get(key)
        require(value is Number && value.toDouble().isFinite() && value.toDouble() == value.toInt().toDouble()) {
            tr("$key debe ser un número entero", "$key must be an integer")
        }
        return (value as Number).toInt()
    }
    private val SUPPORTED_FORMATS = setOf("StradaOBD.designs", "TorqueOBD2Dash.designs")
    fun write(tabs: CustomDashboardTabs): String = JSONObject().apply {
        put("format", "StradaOBD.designs"); put("version", 1)
        put("dashboards", JSONArray().apply {
            tabs.dashboards.forEach { config -> put(JSONObject().apply {
                put("name", config.name); put("grid", config.gridLayout.id); put("carGrid", config.carGridLayout?.id ?: JSONObject.NULL); put("density", config.density.name)
                put("widgets", JSONArray().apply {
                    config.resolvedSlots().toSortedMap().forEach { (index, widget) -> put(JSONObject().apply {
                        put("slot", index); put("type", widget.type.name); put("size", widget.size.name); put("pidId", widget.pidId)
                        put("pidShortName", widget.pidShortName); put("title", widget.title); put("color", widget.colorHex)
                        put("decimals", widget.decimals ?: JSONObject.NULL)
                        put("scaleMin", widget.scaleMin ?: JSONObject.NULL); put("scaleMax", widget.scaleMax ?: JSONObject.NULL)
                    }) }
                })
            }) }
        })
    }.toString(2)

    fun read(text: String): List<CustomDashboardConfig> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { tr("El archivo de diseños supera 2 MB", "Design file exceeds 2 MB") }
        val root = JSONObject(text)
        require(root.optString("format") in SUPPORTED_FORMATS && integer(root, "version") == 1) {
            tr("Formato o versión de diseños no admitido", "Unsupported design format or version")
        }
        val configs = root.getJSONArray("dashboards")
        require(configs.length() in 1..64) { tr("El archivo debe contener entre 1 y 64 pestañas", "File must contain between 1 and 64 tabs") }
        return (0 until configs.length()).map { i ->
            val config = configs.getJSONObject(i)
            val name = config.getString("name").trim()
            require(name.isNotEmpty() && name.length <= 80) { tr("Nombre de pestaña inválido", "Invalid tab name") }
            val layout = DashboardGridLayout.entries.firstOrNull { it.id == config.getString("grid") }
                ?: error(tr("Rejilla no admitida", "Unsupported grid layout"))
            val carLayout = if (config.isNull("carGrid")) null else {
                DashboardGridLayout.carLayouts.firstOrNull { it.id == config.getString("carGrid") }
                    ?: error(tr("Rejilla de coche no admitida", "Unsupported car grid layout"))
            }
            val density = DashboardDensity.entries.firstOrNull { it.name == config.optString("density", "COMFORTABLE") }
                ?: error(tr("Presentación no admitida", "Unsupported density mode"))
            val widgets = config.getJSONArray("widgets")
            require(widgets.length() <= 64) { tr("Demasiados indicadores en una pestaña", "Too many gauges in a tab") }
            val slots = mutableMapOf<Int, DashboardWidget>()
            for (j in 0 until widgets.length()) {
                val json = widgets.getJSONObject(j)
                val index = integer(json, "slot")
                require(index in 0..63 && index !in slots) { tr("Ranura fuera de rango o duplicada", "Slot out of range or duplicated") }
                val type = WidgetType.entries.firstOrNull { it.name == json.getString("type") }
                    ?: error(tr("Tipo de indicador no admitido", "Unsupported gauge type"))
                val size = WidgetSize.entries.firstOrNull { it.name == json.optString("size", "STANDARD") }
                    ?: error(tr("Tamaño de indicador no admitido", "Unsupported gauge size"))
                val color = json.getString("color")
                require(color.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { tr("Color inválido", "Invalid color") }
                val title = json.getString("title")
                require(title.isNotBlank() && title.length <= 120) { tr("Título del indicador inválido", "Invalid gauge title") }
                val decimals = if (json.isNull("decimals")) null else integer(json, "decimals")
                require(decimals == null || decimals in 0..4) { tr("Los decimales deben estar entre 0 y 4", "Decimals must be between 0 and 4") }
                val min = if (json.isNull("scaleMin")) null else json.getDouble("scaleMin").toFloat()
                val max = if (json.isNull("scaleMax")) null else json.getDouble("scaleMax").toFloat()
                require((min == null && max == null) || (min != null && max != null && min.isFinite() && max.isFinite() && min < max && (max - min).isFinite())) {
                    tr("Escala inválida: mínimo menor que máximo", "Invalid scale: minimum must be less than maximum")
                }
                val pid = json.optString("pidId", "")
                val short = json.optString("pidShortName", "")
                require(pid.length <= 256 && short.length <= 256) { tr("Identidad de sensor demasiado larga", "Sensor identity too long") }
                slots[index] = DashboardWidget(type = type, pidId = pid, pidShortName = short, title = title,
                    colorHex = color.uppercase(), size = size, decimals = decimals, scaleMin = min, scaleMax = max)
            }
            CustomDashboardConfig(name = name, gridLayout = layout, slots = slots, widgets = emptyList(), density = density, carGridLayout = carLayout)
        }
    }
}
