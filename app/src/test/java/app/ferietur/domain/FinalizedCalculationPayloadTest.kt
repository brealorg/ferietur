package app.ferietur.domain

import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizedCalculationPayloadTest {
    @Test
    fun singleRuntimeMapsLosslesslyToPreliminaryPayload() {
        val preliminary = emptyPreliminary(BigDecimal("321.45"))
        val runtime = TariffRuntimeCalculation.SingleContext(
            preliminary = preliminary,
            rulesetVersion = "rules-1",
            provenance = listOf(contextA),
        )

        val frozen = FinalizedCalculationPayload.fromRuntime(runtime)

        assertEquals(FinalizedCalculationPayloadMode.PRELIMINARY, frozen.mode)
        assertEquals(preliminary, frozen.preliminaryOrNull)
        assertEquals(preliminary.paymentBasisAmount, frozen.paymentBasisAmount)
    }

    @Test
    fun segmentedRuntimeMapsScopedLinesWithoutSyntheticHourlyRate() {
        val runtime = segmentedRuntime()

        val frozen = FinalizedCalculationPayload.fromRuntime(runtime) as FinalizedCalculationPayload.SegmentedContexts

        assertEquals(FinalizedCalculationPayloadMode.SEGMENTED_CONTEXTS, frozen.mode)
        assertNull(frozen.preliminaryOrNull)
        assertEquals(BigDecimal("700.00"), frozen.knownAmount)
        assertEquals(BigDecimal("700.00"), frozen.paymentBasisAmount)
        assertEquals(listOf(0, 1), frozen.lineEntries.map { it.sliceIndex })
        assertEquals(listOf(TariffCalculationLineScope.SEGMENT_LOCAL, TariffCalculationLineScope.SEGMENT_LOCAL), frozen.lineEntries.map { it.scope })
    }

    @Test
    fun snapshotVersionFourRoundTripsSegmentedPayloadWithoutInventingPreliminaryCalculation() {
        val runtime = segmentedRuntime()
        val payload = FinalizedCalculationPayload.fromRuntime(runtime)
        val contexts = FinalizedTariffContextSnapshots.fromRuntime(runtime)
        val snapshot = FinalizedTripSnapshot(
            id = "77777777-7777-4777-8777-777777777777",
            createdAt = LocalDateTime.of(2031, 6, 1, 12, 0),
            appVersionName = "test",
            appVersionCode = 1,
            rulesetVersion = runtime.rulesetVersion,
            tariffPackageId = contexts.first().tariffPackageId,
            tariffRateSetId = contexts.first().tariffRateSetId,
            salaryTableId = contexts.first().salaryTableId,
            salaryTableEffectiveFrom = contexts.first().salaryTableEffectiveFrom,
            salaryTableSourceLabel = contexts.first().salaryTableSourceLabel,
            tariffContexts = contexts,
            title = "Segmented snapshot",
            tripStart = firstDate.atTime(8, 0),
            tripEnd = secondDate.atTime(10, 0),
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            salaryStep = 32,
            annualSalary = contexts.first().annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyList(),
            workBlocks = emptyList(),
            calculationPayload = payload,
            settlement = SettlementSnapshot(
                calculatedAmount = payload.paymentBasisAmount,
                proposedAmount = payload.paymentBasisAmount,
                usesFullCalculation = true,
                reason = "",
            ),
            findings = emptyList(),
            unresolvedRules = emptyList(),
        )

        val encoded = FinalizedTripSnapshotCodec.encode(snapshot)
        val version = ByteBuffer.wrap(Base64.getDecoder().decode(encoded)).int
        val decoded = FinalizedTripSnapshotCodec.decode(encoded)

        assertEquals(4, version)
        assertEquals(snapshot, decoded)
        assertTrue(decoded.hasSegmentedCalculation)
        assertTrue(decoded.hasMultipleTariffContexts)
        assertNull(decoded.preliminaryCalculationOrNull)
        assertEquals(BigDecimal("700.00"), decoded.calculationPayload.paymentBasisAmount)
    }

    private fun segmentedRuntime(): TariffRuntimeCalculation.SegmentedContexts {
        val lineA = CalculationLine(
            id = "active",
            title = "Aktivt arbeid A",
            detail = "",
            amount = BigDecimal("300.00"),
            source = "test",
            explanation = "",
        )
        val lineB = lineA.copy(title = "Aktivt arbeid B", amount = BigDecimal("400.00"))
        val plan = SegmentedTariffCalculationPlan(
            rulesetVersion = "rules-1",
            tripStart = firstDate.atTime(8, 0),
            tripEnd = secondDate.atTime(10, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            slices = listOf(
                calculationSlice(firstDate, contextA, "rate-a", "salary-a"),
                calculationSlice(secondDate, contextB, "rate-b", "salary-b"),
            ),
        )
        val scopePlan = TariffWholeTripScopePlan(
            rulesetVersion = "rules-1",
            stayAllowancePolicy = StayAllowanceScopePolicy(BigDecimal("110.00"), 360),
            shortNoticePolicy = ShortNoticeScopePolicy(120, 30),
            restingWatchScopes = emptyList(),
        )
        val segmented = SegmentedTariffMonetaryCalculation(
            plan = plan,
            scopePlan = scopePlan,
            lineEntries = listOf(
                TariffScopedCalculationLine(TariffCalculationLineScope.SEGMENT_LOCAL, lineA, sliceIndex = 0),
                TariffScopedCalculationLine(TariffCalculationLineScope.SEGMENT_LOCAL, lineB, sliceIndex = 1),
            ),
        )
        return TariffRuntimeCalculation.SegmentedContexts(
            segmented = segmented,
            rulesetVersion = "rules-1",
            provenance = listOf(contextA, contextB),
        )
    }

    private fun calculationSlice(
        date: LocalDate,
        context: TariffRuntimeProvenanceSlice,
        rateId: String,
        salaryId: String,
    ): TariffCalculationSlice {
        val tariff = TariffPackage(
            id = context.tariffPackageId,
            label = "test",
            effectiveFrom = firstDate,
            effectiveTo = secondDate,
            rulesetVersion = context.rulesetVersion,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/tariff",
        )
        val rateSet = FerieturTariffRates.current.copy(
            id = rateId,
            tariffPackageId = tariff.id,
            effectiveFrom = date,
            effectiveTo = date,
        )
        val salary = SalaryTableDescriptor(
            id = salaryId,
            effectiveFrom = date,
            verifiedThrough = date,
            tariffPackageId = tariff.id,
            sourceLabel = context.salaryTableSourceLabel,
            sourcePageUrl = "https://example.invalid/salary",
        )
        val start = if (date == firstDate) firstDate.atTime(8, 0) else date.atStartOfDay()
        val end = if (date == firstDate) secondDate.atStartOfDay() else secondDate.atTime(10, 0)
        return TariffCalculationSlice(
            segment = TariffCalculationSegment(date, date, tariff, rateSet, salary),
            windowStart = start,
            windowEnd = end,
            dates = listOf(date),
            annualSalary = context.annualSalary,
            hourlyRate = context.hourlyRate,
            workBlocks = emptyList(),
        )
    }

    private fun emptyPreliminary(hourlyRate: BigDecimal): PreliminaryCalculation = PreliminaryCalculation(
        hourlyRate = hourlyRate,
        rosterMinutes = 0,
        rosterUncoveredMinutes = 0,
        rosterUncoveredEvidence = emptyList(),
        activeMinutes = 0,
        activeInsideRosterMinutes = 0,
        activeOutsideRosterMinutes = 0,
        payableActiveWorkMinutes = 0,
        payableTravelWithResponsibilityMinutes = 0,
        restingNightMinutes = 0,
        restingNightOutsideRosterMinutes = 0,
        eveningNightMinutes = 0,
        weekendMinutes = 0,
        holidayMinutes = 0,
        stayAllowanceDays = 0,
        lines = emptyList(),
        dayAudits = emptyList(),
        knownAmount = BigDecimal.ZERO.setScale(2),
        paymentBasisAmount = BigDecimal.ZERO.setScale(2),
        alreadyCoveredByNormalRosterAmount = BigDecimal.ZERO.setScale(2),
        excludedKnownRuleAmount = BigDecimal.ZERO.setScale(2),
        applicableUnresolvedRuleIds = emptySet(),
    )

    private val firstDate = LocalDate.of(2031, 6, 30)
    private val secondDate = LocalDate.of(2031, 7, 1)
    private val contextA = TariffRuntimeProvenanceSlice(
        start = firstDate,
        end = firstDate,
        tariffPackageId = "snapshot-tariff",
        rulesetVersion = "rules-1",
        tariffRateSetId = "rate-a",
        salaryTableId = "salary-a",
        salaryTableEffectiveFrom = firstDate,
        salaryTableSourceLabel = "salary A",
        annualSalary = BigDecimal("585000"),
        hourlyRate = BigDecimal("300.00"),
    )
    private val contextB = TariffRuntimeProvenanceSlice(
        start = secondDate,
        end = secondDate,
        tariffPackageId = "snapshot-tariff",
        rulesetVersion = "rules-1",
        tariffRateSetId = "rate-b",
        salaryTableId = "salary-b",
        salaryTableEffectiveFrom = secondDate,
        salaryTableSourceLabel = "salary B",
        annualSalary = BigDecimal("780000"),
        hourlyRate = BigDecimal("400.00"),
    )
}
