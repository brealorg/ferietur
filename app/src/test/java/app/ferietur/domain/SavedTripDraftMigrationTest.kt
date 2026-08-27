package app.ferietur.domain

import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedTripDraftMigrationTest {
    @Test
    fun v5SummaryIsMigratedToControlAndRequiresExplicitRefinalization() {
        val decoded = SavedTripDraftCodec.readDecoded(
            StringReader(v5Properties(screen = "SUMMARY")),
        )

        assertEquals(5, decoded.sourceSchemaVersion)
        assertEquals("CONTROL", decoded.draft.screen)
        assertNull(decoded.draft.finalizedSnapshot)
        assertTrue(
            SavedTripDraftMigrator.MIGRATION_V6_REFINALIZE_LEGACY_SUMMARY in
                decoded.draft.migrationHistory,
        )
        assertTrue(
            SavedTripDraftMigrator.MIGRATION_V6_SCHEMA in
                decoded.draft.migrationHistory,
        )
    }

    @Test
    fun oldA39A1DamageMigrationDoesNotBranchOnNamedTripTitle() {
        val decoded = SavedTripDraftCodec.readDecoded(
            StringReader(
                v5Properties(
                    screen = "TRIP_PLAN",
                    title = "En helt annen tittel",
                    addTravelOnlyRegressionShape = true,
                ),
            ),
        )

        assertEquals("En helt annen tittel", decoded.draft.title)
        assertTrue(decoded.draft.plans.values.flatten().any { it.kind == TimeKind.ACTIVE_WORK })
        assertTrue(
            SavedTripDraftMigrator.MIGRATION_V6_A39A1_PLAN_REPAIR in
                decoded.draft.migrationHistory,
        )
    }

    @Test
    fun migratedDraftWritesAsV6WithProvenance() {
        val migrated = SavedTripDraftCodec.read(
            StringReader(v5Properties(screen = "SUMMARY")),
        )
        val writer = StringWriter()
        SavedTripDraftCodec.write(migrated, writer)
        val saved = Properties().apply { load(StringReader(writer.toString())) }

        assertEquals("6", saved.getProperty("schemaVersion"))
        assertTrue(
            saved.getProperty("migrationHistory")
                .contains(SavedTripDraftMigrator.MIGRATION_V6_SCHEMA),
        )
        assertFalse(writer.toString().contains("solgården", ignoreCase = true))
    }

    private fun v5Properties(
        screen: String,
        title: String = "Legacy",
        addTravelOnlyRegressionShape: Boolean = false,
    ): String {
        val p = Properties().apply {
            setProperty("schemaVersion", "5")
            setProperty("id", "legacy-v5")
            setProperty("updatedAtEpochMillis", "1")
            setProperty("screen", screen)
            setProperty("title", title)
            setProperty("employerKind", "OSLO_KOMMUNE")
            setProperty("payingParty", "UNSPECIFIED")
            setProperty("rosterComparisonMode", "DO_NOT_USE_NORMAL_ROSTER")
            setProperty("fundingMode", "VACATION_SEPARATE")
            if (addTravelOnlyRegressionShape) {
                setProperty("startDate", "2026-08-11")
                setProperty("endDate", "2026-08-18")
                setProperty("startTime", "06:00")
                setProperty("endTime", "23:00")
                setProperty(
                    "plan.2026-08-11.0",
                    "TRAVEL_WITH_RESPONSIBILITY|06:00|11:00|NOT_CLARIFIED",
                )
                setProperty(
                    "plan.2026-08-18.0",
                    "TRAVEL_WITH_RESPONSIBILITY|16:00|23:00|NOT_CLARIFIED",
                )
                setProperty("outboundArrival", "2026-08-11T11:00")
                setProperty("returnDeparture", "2026-08-18T16:00")
            } else {
                setProperty("startDate", "2026-08-25")
                setProperty("endDate", "2026-08-26")
                setProperty("startTime", "07:00")
                setProperty("endTime", "20:00")
                setProperty("outboundArrival", "2026-08-25T10:00")
                setProperty("returnDeparture", "2026-08-26T17:00")
            }
            setProperty("salaryStep", "32")
            setProperty("weeklyBasis", "HOURS_35_5")
            setProperty("weekendProfile", "STANDARD")
            setProperty("payslipChecked", "true")
            setProperty("rosterGapConfirmed", "false")
            setProperty("settlementMode", "FULL_CALCULATION")
            setProperty("settlementAmountText", "")
            setProperty("settlementReason", "")
        }
        return StringWriter().also { p.store(it, "legacy v5") }.toString()
    }
}
