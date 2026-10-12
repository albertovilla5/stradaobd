package com.torqueobd2.dash.i18n

import com.torqueobd2.dash.data.model.ConnectionState
import com.torqueobd2.dash.data.model.CustomDashboardConfig
import com.torqueobd2.dash.data.model.DashboardGridLayout
import com.torqueobd2.dash.data.model.DashboardPresets
import com.torqueobd2.dash.data.model.DashboardWidget
import com.torqueobd2.dash.data.model.FuelType
import com.torqueobd2.dash.data.model.PollingMode
import com.torqueobd2.dash.data.model.SensorIds
import com.torqueobd2.dash.data.model.SensorTelemetryStatus
import com.torqueobd2.dash.data.model.VehicleField
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.data.repository.DtcCatalog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppI18nTest {

    @After
    fun tearDown() {
        AppI18n.setLanguage(AppLanguage.ES)
    }

    @Test
    fun `system language resolution maps supported locales and falls back to EN`() {
        assertEquals(AppLanguage.ES, AppLanguage.fromLocale(Locale("es", "ES")))
        assertEquals(AppLanguage.ES, AppLanguage.fromLocale(Locale("es", "MX")))
        assertEquals(AppLanguage.EN, AppLanguage.fromLocale(Locale.US))
        assertEquals(AppLanguage.EN, AppLanguage.fromLocale(Locale.UK))
        assertEquals(AppLanguage.FR, AppLanguage.fromLocale(Locale.FRANCE))
        assertEquals(AppLanguage.FR, AppLanguage.fromLocale(Locale.CANADA_FRENCH))
        assertEquals(AppLanguage.DE, AppLanguage.fromLocale(Locale.GERMANY))
        assertEquals(AppLanguage.PT, AppLanguage.fromLocale(Locale("pt", "PT")))
        assertEquals(AppLanguage.PT, AppLanguage.fromLocale(Locale("pt", "BR")))
        assertEquals(AppLanguage.IT, AppLanguage.fromLocale(Locale.ITALY))
        assertEquals(AppLanguage.EN, AppLanguage.fromLocale(Locale.JAPAN))
    }

    @Test
    fun `language preference resolves explicit or system locale`() {
        assertEquals(AppLanguagePreference.SYSTEM, AppLanguagePreference.fromCode(null))
        assertEquals(AppLanguagePreference.SYSTEM, AppLanguagePreference.fromCode("system"))
        assertEquals(AppLanguagePreference.ES, AppLanguagePreference.fromCode("es"))
        assertEquals(AppLanguagePreference.EN, AppLanguagePreference.fromCode("en"))
        assertEquals(AppLanguagePreference.FR, AppLanguagePreference.fromCode("fr"))
        assertEquals(AppLanguagePreference.DE, AppLanguagePreference.fromCode("de"))
        assertEquals(AppLanguagePreference.PT, AppLanguagePreference.fromCode("pt"))
        assertEquals(AppLanguagePreference.IT, AppLanguagePreference.fromCode("it"))

        assertEquals(AppLanguage.ES, AppLanguagePreference.SYSTEM.resolve(Locale("es", "ES")))
        assertEquals(AppLanguage.EN, AppLanguagePreference.SYSTEM.resolve(Locale.US))
        assertEquals(AppLanguage.FR, AppLanguagePreference.SYSTEM.resolve(Locale.FRANCE))
        assertEquals(AppLanguage.DE, AppLanguagePreference.SYSTEM.resolve(Locale.GERMANY))
        assertEquals(AppLanguage.PT, AppLanguagePreference.SYSTEM.resolve(Locale("pt", "PT")))
        assertEquals(AppLanguage.IT, AppLanguagePreference.SYSTEM.resolve(Locale.ITALY))
        assertEquals(AppLanguage.ES, AppLanguagePreference.ES.resolve(Locale.US))
        assertEquals(AppLanguage.EN, AppLanguagePreference.EN.resolve(Locale("es", "ES")))
        assertEquals(AppLanguage.FR, AppLanguagePreference.FR.resolve(Locale.US))
        assertEquals(AppLanguage.DE, AppLanguagePreference.DE.resolve(Locale.US))
        assertEquals(AppLanguage.PT, AppLanguagePreference.PT.resolve(Locale.US))
        assertEquals(AppLanguage.IT, AppLanguagePreference.IT.resolve(Locale.US))
    }

    @Test
    fun `tr and trUnit switch dynamically across all six languages`() {
        AppI18n.withLanguage(AppLanguage.ES) {
            assertEquals("Velocidad", tr("Velocidad", "Speed"))
            assertEquals("CV", trUnit("CV"))
            assertEquals("marcha", trUnit("marcha"))
            assertEquals("pasos", trUnit("pasos"))
        }

        AppI18n.withLanguage(AppLanguage.EN) {
            assertEquals("Speed", tr("Velocidad", "Speed"))
            assertEquals("HP", trUnit("CV"))
            assertEquals("gear", trUnit("marcha"))
            assertEquals("steps", trUnit("pasos"))
        }

        AppI18n.withLanguage(AppLanguage.FR) {
            assertEquals("Vitesse", tr("Velocidad", "Speed"))
            assertEquals("ch", trUnit("CV"))
            assertEquals("rapport", trUnit("marcha"))
            assertEquals("pas", trUnit("pasos"))
            assertEquals("2 véhicules enregistrés", tr("2 vehículos guardados", "2 saved vehicles"))
        }

        AppI18n.withLanguage(AppLanguage.DE) {
            assertEquals("Geschwindigkeit", tr("Velocidad", "Speed"))
            assertEquals("PS", trUnit("CV"))
            assertEquals("Gang", trUnit("marcha"))
            assertEquals("Schritte", trUnit("pasos"))
            assertEquals("2 gespeicherte Fahrzeuge", tr("2 vehículos guardados", "2 saved vehicles"))
        }

        AppI18n.withLanguage(AppLanguage.PT) {
            assertEquals("Velocidade", tr("Velocidad", "Speed"))
            assertEquals("cv", trUnit("CV"))
            assertEquals("mudança", trUnit("marcha"))
            assertEquals("passos", trUnit("pasos"))
            assertEquals("2 veículos guardados", tr("2 vehículos guardados", "2 saved vehicles"))
        }

        AppI18n.withLanguage(AppLanguage.IT) {
            assertEquals("Velocità", tr("Velocidad", "Speed"))
            assertEquals("CV", trUnit("CV"))
            assertEquals("marcia", trUnit("marcha"))
            assertEquals("passi", trUnit("pasos"))
            assertEquals("2 veicoli salvati", tr("2 vehículos guardados", "2 saved vehicles"))
        }
    }

    @Test
    fun `catalogs and models localize display text while keeping canonical IDs, units, and unique shortNames across all languages`() {
        val esStandard = AppI18n.withLanguage(AppLanguage.ES) { DefaultPids.getStandardPids() }
        val dtcCatalog = DtcCatalog(emptyMap())

        for (lang in AppLanguage.entries) {
            val std = AppI18n.withLanguage(lang) { DefaultPids.getStandardPids() }
            assertEquals("IDs must match for $lang", esStandard.map { it.id }, std.map { it.id })
            assertEquals("Units must match for $lang", esStandard.map { it.units }, std.map { it.units })
            assertEquals("Standard shortNames must be unique in $lang", std.size, std.map { it.shortName }.toSet().size)

            val packs = AppI18n.withLanguage(lang) { DefaultPids.getManufacturerPacks() }
            val packPids = packs.flatMap { it.pids }
            assertEquals("Manufacturer shortNames must be unique in $lang", packPids.size, packPids.map { it.shortName }.toSet().size)

            val dtcDesc = dtcCatalog.describe("P0100", lang)
            assertTrue("DTC P0100 description must not be blank in $lang", dtcDesc.text.isNotBlank())
            val misfireDesc = dtcCatalog.describe("P0304", lang)
            assertTrue("DTC P0304 must reference cylinder 4 in $lang", misfireDesc.text.contains("4"))
        }

        AppI18n.withLanguage(AppLanguage.ES) {
            assertTrue(ConnectionState().statusMessage.contains("adaptador", ignoreCase = true))
            assertEquals("Equilibrado", PollingMode.BALANCED.title)
            assertEquals("Gasolina (E5/E10)", FuelType.GASOLINE.displayName)
            assertEquals("Diésel", FuelType.DIESEL.shortDisplayName)
            assertEquals("Nombre del vehículo", VehicleField.NAME.title)
            assertEquals("Sin respuesta", SensorTelemetryStatus.NO_RESPONSE.label)
            assertTrue(dtcCatalog.describe("P0100").text.contains("caudal de aire", ignoreCase = true))
        }

        AppI18n.withLanguage(AppLanguage.EN) {
            assertTrue(ConnectionState().statusMessage.contains("adapter", ignoreCase = true))
            assertEquals("Balanced", PollingMode.BALANCED.title)
            assertEquals("Gasoline (E5/E10)", FuelType.GASOLINE.displayName)
            assertEquals("Diesel", FuelType.DIESEL.shortDisplayName)
            assertEquals("Vehicle name", VehicleField.NAME.title)
            assertEquals("No response", SensorTelemetryStatus.NO_RESPONSE.label)
            assertTrue(dtcCatalog.describe("P0100").text.contains("Mass or Volume Air Flow", ignoreCase = true))
        }

        AppI18n.withLanguage(AppLanguage.FR) {
            assertEquals("Équilibré", PollingMode.BALANCED.title)
            assertEquals("Nom du véhicule", VehicleField.NAME.title)
            assertTrue(dtcCatalog.describe("P0100").text.contains("débitmètre d'air", ignoreCase = true))
        }

        AppI18n.withLanguage(AppLanguage.DE) {
            assertEquals("Ausgewogen", PollingMode.BALANCED.title)
            assertEquals("Fahrzeugname", VehicleField.NAME.title)
            assertTrue(dtcCatalog.describe("P0100").text.contains("Luftmassen", ignoreCase = true))
        }

        AppI18n.withLanguage(AppLanguage.PT) {
            assertEquals("Equilibrado", PollingMode.BALANCED.title)
            assertEquals("Nome do veículo", VehicleField.NAME.title)
            assertTrue(dtcCatalog.describe("P0100").text.contains("fluxo de massa", ignoreCase = true))
        }

        AppI18n.withLanguage(AppLanguage.IT) {
            assertEquals("Bilanciato", PollingMode.BALANCED.title)
            assertEquals("Nome del veicolo", VehicleField.NAME.title)
            assertTrue(dtcCatalog.describe("P0100").text.contains("portata aria", ignoreCase = true))
        }
    }

    @Test
    fun `dashboard widgets and tabs dynamically localize default persisted names across all languages`() {
        val defaultTab = CustomDashboardConfig(name = "Principal", gridLayout = DashboardGridLayout.GRID_2X2)
        val customNamedTab = CustomDashboardConfig(name = "Track Day", gridLayout = DashboardGridLayout.GRID_2X2)
        val defaultWidget = DashboardWidget(pidId = SensorIds.RPM, pidShortName = "RPM", title = "Revoluciones")
        val customWidget = DashboardWidget(pidId = SensorIds.RPM, pidShortName = "RPM", title = "My Custom Tach")

        for (lang in AppLanguage.entries) {
            AppI18n.withLanguage(lang) {
                assertEquals(tr("Principal", "Main"), defaultTab.localizedName)
                assertEquals("Track Day", customNamedTab.localizedName)
                assertEquals(tr("Revoluciones", "Engine RPM"), defaultWidget.localizedTitle)
                assertEquals("My Custom Tach", customWidget.localizedTitle)
                assertEquals(tr("Mi Dashboard Personalizado", "My Custom Dashboard"), CustomDashboardConfig().localizedName)

                // Verify that widgets/tabs created while in `lang` also re-localize back to ES
                val createdInLangWidget = DashboardWidget(pidId = SensorIds.SPEED, pidShortName = "Velocidad", title = tr("Velocidad", "Speed"))
                val createdInLangTab = CustomDashboardConfig(name = tr("Mi dash", "My dash"))
                AppI18n.withLanguage(AppLanguage.ES) {
                    assertEquals("Velocidad", createdInLangWidget.localizedTitle)
                    assertEquals("Mi dash", createdInLangTab.localizedName)
                }
            }
        }
    }
}
