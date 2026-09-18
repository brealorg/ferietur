package app.ferietur.domain

import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalTime
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkPeriodRelationFoundationTest {
    private val date = LocalDate.of(2026, 8, 10)

    @Test
    fun plannedBlockProjectionPreservesHolidayPlanRelationAndTravelDuty() {
        val planned = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )

        val projected = planned.toWorkBlock(date)

        assertEquals(HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN, projected.holidayWorkPlanRelation)
        assertEquals(TravelDutyStatus.OFF_DUTY, projected.travelDutyStatus)
    }

    @Test
    fun visibleDayProjectionPreservesFactsWhenCrossMidnightPeriodIsClipped() {
        val planned = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(23, 0),
            end = LocalTime.of(2, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )

        val visible = TripPlanEngine.projectVisibleDay(
            date.plusDays(1),
            mapOf(date to listOf(planned)),
        ).single().block

        assertEquals(date.plusDays(1).atStartOfDay(), visible.start)
        assertEquals(date.plusDays(1).atTime(2, 0), visible.end)
        assertEquals(HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN, visible.holidayWorkPlanRelation)
        assertEquals(TravelDutyStatus.OFF_DUTY, visible.travelDutyStatus)
    }

    @Test
    fun travelOverlayPreservesRelationOnSplitBaseAndDutyOnTravel() {
        val base = PlannedBlock(
            kind = TimeKind.ACTIVE_WORK,
            start = LocalTime.of(7, 0),
            end = LocalTime.of(14, 0),
            holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
        )
        val travel = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )

        val result = TripPlanEngine.overlayTravelOnPlan(
            dates = listOf(date),
            basePlans = mapOf(date to listOf(base)),
            travelPlans = mapOf(date to listOf(travel)),
        ).getValue(date)

        val active = result.filter { it.kind == TimeKind.ACTIVE_WORK }
        assertEquals(2, active.size)
        assertTrue(active.all { it.holidayWorkPlanRelation == HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN })

        val restoredTravel = result.single { it.kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP }
        assertEquals(HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN, restoredTravel.holidayWorkPlanRelation)
        assertEquals(TravelDutyStatus.OFF_DUTY, restoredTravel.travelDutyStatus)
    }

    @Test
    fun savedDraftSchemaTenRoundTripsNonDefaultPeriodFacts() {
        val plan = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )
        val draft = SavedTripDraft(
            id = "b2a-codec",
            updatedAtEpochMillis = 1L,
            screen = "TRIP_PLAN",
            title = "B2A",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = date,
            endDate = date.plusDays(1),
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = mapOf(date to listOf(plan)),
            outboundArrival = date.atTime(8, 0),
            returnDeparture = date.plusDays(1).atTime(18, 0),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )

        val encoded = StringWriter().also { SavedTripDraftCodec.write(draft, it) }.toString()
        val decoded = SavedTripDraftCodec.read(StringReader(encoded))

        assertTrue(encoded.contains("schemaVersion=10"))
        assertEquals(plan, decoded.plans.getValue(date).single())
    }

    @Test
    fun finalizedSnapshotCurrentFormatRoundTripsPeriodFacts() {
        val next = date.plusDays(1)
        val plan = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(23, 0),
            end = LocalTime.of(1, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )
        val dates = listOf(date, next)
        val plans = mapOf(date to listOf(plan), next to emptyList())
        val annualSalary = OsloSalaryTable2026.annualSalary(32)
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = date.atTime(23, 0),
            tripEnd = next.atTime(1, 0),
        )
        val settlement = SettlementSnapshot(
            calculatedAmount = calculation.paymentBasisAmount,
            proposedAmount = calculation.paymentBasisAmount,
            usesFullCalculation = true,
            reason = "",
        )
        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "B2A snapshot",
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
            tripStart = date.atTime(23, 0),
            tripEnd = next.atTime(1, 0),
            settlement = settlement,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )

        val encoded = FinalizedTripSnapshotCodec.encode(snapshot)
        val version = ByteBuffer.wrap(Base64.getDecoder().decode(encoded)).int
        val decoded = FinalizedTripSnapshotCodec.decode(encoded)
        val restored = decoded.workBlocks.single()

        assertEquals(7, version)
        assertEquals(HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN, restored.holidayWorkPlanRelation)
        assertEquals(TravelDutyStatus.OFF_DUTY, restored.travelDutyStatus)
    }

    @Test
    fun segmentedReconstructionPreservesPeriodFactsAcrossEffectiveSlices() {
        val second = date.plusDays(1)
        val boundary = second.atStartOfDay()
        val source = WorkBlock(
            start = date.atTime(23, 0),
            end = second.atTime(1, 0),
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )
        val tariff = FerieturTariffs.dok25_2026_2028
        val rateSet = FerieturTariffRates.current
        val salaryTable = OsloSalaryTables.requireSupportedRange(date, second)
        val annualSalary = OsloSalaryTable2026.annualSalary(32)
        val hourlyRate = TariffMath.hourlyRate(annualSalary, WeeklyBasis.HOURS_35_5, rateSet)

        val plan = SegmentedTariffCalculationPlan(
            rulesetVersion = tariff.rulesetVersion,
            tripStart = source.start,
            tripEnd = source.end,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            slices = listOf(
                TariffCalculationSlice(
                    segment = TariffCalculationSegment(date, date, tariff, rateSet, salaryTable),
                    windowStart = source.start,
                    windowEnd = boundary,
                    dates = listOf(date),
                    annualSalary = annualSalary,
                    hourlyRate = hourlyRate,
                    workBlocks = listOf(SegmentedWorkBlock(0, source.copy(end = boundary))),
                ),
                TariffCalculationSlice(
                    segment = TariffCalculationSegment(second, second, tariff, rateSet, salaryTable),
                    windowStart = boundary,
                    windowEnd = source.end,
                    dates = listOf(second),
                    annualSalary = annualSalary,
                    hourlyRate = hourlyRate,
                    workBlocks = listOf(SegmentedWorkBlock(0, source.copy(start = boundary))),
                ),
            ),
        )

        val reconstructed =
            TariffSourceBlockReconstructor.reconstruct(plan) as TariffSourceBlockReconstructionResult.Success
        val restored = reconstructed.blocks.single().block

        assertEquals(source, restored)
    }
}
