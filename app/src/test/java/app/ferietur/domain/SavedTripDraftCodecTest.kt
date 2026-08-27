package app.ferietur.domain

import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedTripDraftCodecTest {
    @Test
    fun roundTripKeepsEditableTripState() {
        val first = LocalDate.of(2026, 4, 1)
        val draft = SavedTripDraft(
            id = "trip-test",
            updatedAtEpochMillis = 123456789L,
            screen = "TRIP_PLAN",
            title = "Påsketur med æøå",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = first,
            endDate = first.plusDays(7),
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(23, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.EXTENDED_30,
            payslipChecked = true,
            rosterGapConfirmed = true,
            roster = mapOf(first to "D1", first.plusDays(1) to "LV"),
            plans = mapOf(
                first to listOf(
                    PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(7, 0), LocalTime.of(11, 0)),
                    PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(11, 0), LocalTime.of(22, 0)),
                    PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
                    PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY),
                ),
                first.plusDays(1) to listOf(
                    PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP, LocalTime.of(23, 0), LocalTime.of(1, 0), TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY),
                ),
                first.plusDays(2) to listOf(
                    PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(23, 0), LocalTime.of(1, 0), TravelNoticeStatus.NOT_CLARIFIED),
                ),
            ),
            outboundArrival = LocalDateTime.of(first, LocalTime.of(11, 0)),
            returnDeparture = LocalDateTime.of(first.plusDays(7), LocalTime.of(16, 0)),
            outboundTravelKind = TimeKind.TRAVEL_WITH_RESPONSIBILITY,
            returnTravelKind = TimeKind.TRAVEL_UNCERTAIN,
            settlementMode = "CUSTOM_AGREEMENT",
            settlementAmountText = "48 000,00",
            settlementReason = "Avtalt beløp\nmed verge",
        )

        val text = StringWriter().also { SavedTripDraftCodec.write(draft, it) }.toString()
        val restored = SavedTripDraftCodec.read(StringReader(text))

        assertEquals(draft, restored)
    }

    @Test
    fun schemaThreeTravelMigratesNoticeConservativelyToNotClarified() {
        val text = """
            schemaVersion=3
            id=schema3-trip
            updatedAtEpochMillis=42
            screen=TRIP_PLAN
            title=Gammel reise
            employerKind=OSLO_KOMMUNE
            payingParty=UNSPECIFIED
            rosterComparisonMode=USE_NORMAL_ROSTER
            startDate=2026-08-10
            endDate=2026-08-11
            startTime=08:00
            endTime=10:00
            salaryStep=32
            weeklyBasis=HOURS_35_5
            weekendProfile=STANDARD
            payslipChecked=true
            rosterGapConfirmed=true
            outboundArrival=2026-08-10T08:00
            returnDeparture=2026-08-11T10:00
            settlementMode=FULL_CALCULATION
            settlementAmountText=
            settlementReason=
            plan.2026-08-10.0=TRAVEL_WITHOUT_RESPONSIBILITY|08:00|10:00
        """.trimIndent()

        val restored = SavedTripDraftCodec.read(StringReader(text))
        val block = restored.plans.getValue(LocalDate.of(2026, 8, 10)).single()

        assertEquals(TravelNoticeStatus.NOT_CLARIFIED, block.travelNoticeStatus)
    }

    @Test
    fun schemaOneMigratesWithoutGuessingEmployerOrPayer() {
        val text = """
            schemaVersion=1
            id=legacy-trip
            updatedAtEpochMillis=42
            screen=SUMMARY
            title=Gammel tur
            fundingMode=VACATION_SEPARATE
            startDate=2026-08-11
            endDate=2026-08-18
            startTime=06:00
            endTime=23:00
            salaryStep=32
            weeklyBasis=HOURS_35_5
            weekendProfile=STANDARD
            payslipChecked=true
            outboundArrival=2026-08-11T11:00
            returnDeparture=2026-08-18T16:00
            settlementMode=FULL_CALCULATION
            settlementAmountText=
            settlementReason=
            roster.2026-08-11=D1
            plan.2026-08-11.0=ACTIVE_WORK|07:00|22:00
        """.trimIndent()

        val restored = SavedTripDraftCodec.read(StringReader(text))

        assertEquals(EmployerKind.UNSPECIFIED, restored.employerKind)
        assertEquals(PayingParty.UNSPECIFIED, restored.payingParty)
        assertEquals(RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER, restored.rosterComparisonMode)
        // Schema v6 normalizes legacy non-manual roster codes
        // at the versioned migration boundary.
        assertEquals(
            true,
            restored.roster[LocalDate.of(2026, 8, 11)] == null,
        )
        assertEquals(
            true,
            SavedTripDraftMigrator.MIGRATION_V6_MANUAL_ROSTER in restored.migrationHistory,
        )
        assertEquals(1, restored.plans[LocalDate.of(2026, 8, 11)]?.size)
    }
}
