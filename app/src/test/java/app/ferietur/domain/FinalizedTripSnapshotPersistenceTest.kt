package app.ferietur.domain

import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizedTripSnapshotPersistenceTest {
    @Test
    fun finalizedSnapshotBinaryCodecRoundTripsEveryField() {
        val snapshot = snapshot(
            id = "11111111-1111-4111-8111-111111111111",
            createdAt = LocalDateTime.of(2026, 8, 25, 11, 15, 30),
        )

        val encoded = FinalizedTripSnapshotCodec.encode(snapshot)
        val decoded = FinalizedTripSnapshotCodec.decode(encoded)

        assertEquals(snapshot, decoded)
    }

    @Test
    fun savedTripSchemaSixPersistsCurrentSnapshotHistoryAndMigrationProvenance() {
        val current = snapshot(
            id = "22222222-2222-4222-8222-222222222222",
            createdAt = LocalDateTime.of(2026, 8, 25, 11, 16, 0),
        )
        val historical = snapshot(
            id = "33333333-3333-4333-8333-333333333333",
            createdAt = LocalDateTime.of(2026, 8, 25, 11, 10, 0),
        )
        val draft = draft().copy(
            screen = "SUMMARY",
            finalizedSnapshot = current,
            finalizationHistory = listOf(historical),
            migrationHistory = setOf(SavedTripDraftMigrator.MIGRATION_V6_SCHEMA),
        )

        val writer = StringWriter()
        SavedTripDraftCodec.write(draft, writer)
        val serialized = writer.toString()
        val decoded = SavedTripDraftCodec.readDecoded(StringReader(serialized))

        assertTrue(serialized.contains("schemaVersion=6"))
        assertEquals(6, decoded.sourceSchemaVersion)
        assertEquals(current, decoded.draft.finalizedSnapshot)
        assertEquals(listOf(historical), decoded.draft.finalizationHistory)
        assertEquals(draft.migrationHistory, decoded.draft.migrationHistory)
    }

    @Test
    fun sameSecondFinalizationsUseDifferentUuidIdentity() {
        val createdAt = LocalDateTime.of(2026, 8, 25, 11, 20, 0)
        val first = snapshot(createdAt = createdAt)
        val second = snapshot(createdAt = createdAt)

        UUID.fromString(first.id)
        UUID.fromString(second.id)
        assertNotEquals(first.id, second.id)
        assertEquals(createdAt, first.createdAt)
        assertEquals(createdAt, second.createdAt)
    }

    @Test
    fun snapshotCarriesActualBuildRulesetAndSalaryTableMetadata() {
        val snapshot = snapshot(
            appVersionName = "1.0.0-rc1",
            appVersionCode = 100,
            rulesetVersion = "2026.3",
            salaryTableId = "oslo-salary-2026-05-01",
            salaryEffective = LocalDate.of(2026, 5, 1),
            salarySource = "Oslo kommune lønnstabell fra 01.05.2026",
        )

        assertEquals("1.0.0-rc1", snapshot.appVersionName)
        assertEquals(100, snapshot.appVersionCode)
        assertEquals("2026.3", snapshot.rulesetVersion)
        assertEquals("oslo-salary-2026-05-01", snapshot.salaryTableId)
        assertEquals(LocalDate.of(2026, 5, 1), snapshot.salaryTableEffectiveFrom)
        assertEquals("Oslo kommune lønnstabell fra 01.05.2026", snapshot.salaryTableSourceLabel)
    }

    private fun snapshot(
        id: String = UUID.randomUUID().toString(),
        createdAt: LocalDateTime = LocalDateTime.of(2026, 8, 25, 11, 0),
        appVersionName: String = "0.5.4-r2",
        appVersionCode: Int = 49,
        rulesetVersion: String = FERIETUR_RULESET_VERSION,
        salaryTableId: String = OsloSalaryTable2026.tableId,
        salaryEffective: LocalDate = OsloSalaryTable2026.effectiveFromDate,
        salarySource: String = OsloSalaryTable2026.sourceLabel,
    ): FinalizedTripSnapshot {
        val start = LocalDate.of(2026, 8, 25)
        val end = start.plusDays(1)
        val dates = listOf(start, end)
        val plans = mapOf(
            start to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(8, 0),
                    LocalTime.of(16, 0),
                ),
            ),
        )
        val tripStart = LocalDateTime.of(start, LocalTime.of(7, 0))
        val tripEnd = LocalDateTime.of(end, LocalTime.of(20, 0))
        val annualSalary = BigDecimal("614600")
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            dates,
            emptyMap(),
            plans,
            annualSalary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            tripStart,
            tripEnd,
        )
        return FinalizedTripSnapshotBuilder.build(
            title = "Snapshot test",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            salaryStep = 32,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            tripStart = tripStart,
            tripEnd = tripEnd,
            settlement = SettlementSnapshot(
                calculatedAmount = calculation.paymentBasisAmount,
                proposedAmount = calculation.paymentBasisAmount,
                usesFullCalculation = true,
                reason = "",
            ),
            snapshotId = id,
            createdAt = createdAt,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            rulesetVersion = rulesetVersion,
            salaryTableId = salaryTableId,
            salaryTableEffectiveFrom = salaryEffective,
            salaryTableSourceLabel = salarySource,
        )
    }

    private fun draft(): SavedTripDraft {
        val start = LocalDate.of(2026, 8, 25)
        return SavedTripDraft(
            id = "draft",
            updatedAtEpochMillis = 1L,
            screen = "TRIP",
            title = "Draft",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            startDate = start,
            endDate = start.plusDays(1),
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = emptyMap(),
            outboundArrival = LocalDateTime.of(start, LocalTime.of(10, 0)),
            returnDeparture = LocalDateTime.of(start.plusDays(1), LocalTime.of(17, 0)),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
        )
    }
}
