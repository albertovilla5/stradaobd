package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Diseños, sin lecturas ni comandos de inicialización: importar no modifica la biblioteca de sensores. */
object DashboardDesignCodec {
    const val MAX_BYTES = 2_000_000
    private fun integer(json: JSONObject, key: String): Int {
        val value = json.get(key)
        require(value is Number && value.toDouble().isFinite() && value.toDouble() == value.toInt().toDouble()) { "$key debe ser un número entero" }
        return (value as Number).toInt()
    }
    fun write(tabs: CustomDashboardTabs): String = JSONObject().apply {
        put("format", "TorqueOBD2Dash.designs"); put("version", 1)
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
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "El archivo de diseños supera 2 MB" }
        val root = JSONObject(text)
        require(root.optString("format") == "TorqueOBD2Dash.designs" && integer(root, "version") == 1) { "Formato o versión de diseños no admitido" }
        val configs = root.getJSONArray("dashboards")
        require(configs.length() in 1..64) { "El archivo debe contener entre 1 y 64 pestañas" }
        return (0 until configs.length()).map { i ->
            val config = configs.getJSONObject(i)
            val name = config.getString("name").trim()
            require(name.isNotEmpty() && name.length <= 80) { "Nombre de pestaña inválido" }
            val layout = DashboardGridLayout.entries.firstOrNull { it.id == config.getString("grid") }
                ?: error("Rejilla no admitida")
            val carLayout = if (config.isNull("carGrid")) null else {
                DashboardGridLayout.carLayouts.firstOrNull { it.id == config.getString("carGrid") }
                    ?: error("Rejilla de coche no admitida")
            }
            val density = DashboardDensity.entries.firstOrNull { it.name == config.optString("density", "COMFORTABLE") }
                ?: error("Presentación no admitida")
            val widgets = config.getJSONArray("widgets")
            require(widgets.length() <= 64) { "Demasiados indicadores en una pestaña" }
            val slots = mutableMapOf<Int, DashboardWidget>()
            for (j in 0 until widgets.length()) {
                val json = widgets.getJSONObject(j)
                val index = integer(json, "slot")
                require(index in 0..63 && index !in slots) { "Ranura fuera de rango o duplicada" }
                val type = WidgetType.entries.firstOrNull { it.name == json.getString("type") } ?: error("Tipo de indicador no admitido")
                val size = WidgetSize.entries.firstOrNull { it.name == json.optString("size", "STANDARD") } ?: error("Tamaño de indicador no admitido")
                val color = json.getString("color")
                require(color.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { "Color inválido" }
                val title = json.getString("title")
                require(title.isNotBlank() && title.length <= 120) { "Título del indicador inválido" }
                val decimals = if (json.isNull("decimals")) null else integer(json, "decimals")
                require(decimals == null || decimals in 0..4) { "Los decimales deben estar entre 0 y 4" }
                val min = if (json.isNull("scaleMin")) null else json.getDouble("scaleMin").toFloat()
                val max = if (json.isNull("scaleMax")) null else json.getDouble("scaleMax").toFloat()
                require((min == null && max == null) || (min != null && max != null && min.isFinite() && max.isFinite() && min < max && (max - min).isFinite())) { "Escala inválida: mínimo menor que máximo" }
                val pid = json.optString("pidId", "")
                val short = json.optString("pidShortName", "")
                require(pid.length <= 256 && short.length <= 256) { "Identidad de sensor demasiado larga" }
                slots[index] = DashboardWidget(type = type, pidId = pid, pidShortName = short, title = title,
                    colorHex = color.uppercase(), size = size, decimals = decimals, scaleMin = min, scaleMax = max)
            }
            CustomDashboardConfig(name = name, gridLayout = layout, slots = slots, widgets = emptyList(), density = density, carGridLayout = carLayout)
        }
    }
}
