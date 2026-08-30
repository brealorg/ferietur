package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizedCalculationPresentationTest {
    private val firstDate = LocalDate.of(2026, 8, 10)
    private val secondDate = firstDate.plusDays(1)
    private val thirdDate = secondDate.plusDays(1)
    private val tripStart = firstDate.atTime(8, 0)
    private val tripEnd = thirdDate.atTime(8, 0)

    @Test
    fun segmentedSnapshotPresentationUsesFrozenPayloadWithoutSyntheticHourlyRate() {
        val runtime = segmentedRuntime()
        val snapshot = FinalizedTripSnapshotBuilder.buildFromRuntime(
            title = "Segmentert presentasjon",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.OSLO_KOMMUNE,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            dates = listOf(firstDate, secondDate, thirdDate),
            roster = emptyMap(),
            plans = plans(),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            tripStart = tripStart,
            tripEnd = tripEnd,
            runtimeCalculation = runtime,
            settlement = SettlementSnapshot(
                calculatedAmount = runtime.paymentBasisAmount,
                proposedAmount = runtime.paymentBasisAmount,
                usesFullCalculation = true,
                reason = "",
            ),
            snapshotId = "99999999-9999-4999-8999-999999999999",
            createdAt = LocalDateTime.of(2026, 8, 30, 22, 30),
        )

        val presentation = snapshot.presentation

        assertTrue(snapshot.hasSegmentedCalculation)
        assertEquals(2, snapshot.tariffContexts.size)
        assertEquals(BigDecimal("920.00"), presentation.paymentBasisAmount)
        assertEquals(listOf(0, 1, 0), presentation.lineEntries.map { it.tariffContextIndex })
        assertEquals(BigDecimal("300.00"), presentation.dayAudits.single { it.date == firstDate }.paymentSubtotal)
        assertEquals(BigDecimal("400.00"), presentation.dayAudits.single { it.date == secondDate }.paymentSubtotal)
        assertEquals(BigDecimal("0.00"), presentation.dayAudits.single { it.date == thirdDate }.paymentSubtotal)
        assertNull(snapshot.preliminaryCalculationOrNull)
        assertThrows(IllegalArgumentException::class.java) { snapshot.calculation }
        assertTrue(snapshot.ruleBasis.contains("2 tariff-/lønnskontekster"))
    }

    @Test
    fun preliminarySnapshotPresentationKeepsStoredAuditExactly() {
        val dates = listOf(firstDate, secondDate, thirdDate)
        val annual = OsloSalaryTable2026.annualSalary(32)
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans(),
            annualSalary = annual,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = tripStart,
            tripEnd = tripEnd,
        )
        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "Single",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.OSLO_KOMMUNE,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            dates = dates,
            roster = emptyMap(),
            plans = plans(),
            salaryStep = 32,
            annualSalary = annual,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            tripStart = tripStart,
            tripEnd = tripEnd,
            settlement = SettlementSnapshot(
                calculation.paymentBasisAmount,
                calculation.paymentBasisAmount,
                true,
                "",
            ),
        )

        val presentation = snapshot.presentation

        assertEquals(calculation.lines, presentation.lines)
        assertEquals(calculation.dayAudits, presentation.dayAudits)
        assertEquals(calculation.rosterMinutes, presentation.rosterMinutes)
        assertEquals(calculation.rosterUncoveredEvidence, presentation.rosterUncoveredEvidence)
        assertEquals(calculation.activeInsideRosterMinutes, presentation.activeInsideRosterMinutes)
    }

    @Test
    fun segmentedPresentationReconstructsFrozenRosterGapFromSnapshotInputs() {
        val runtime = segmentedRuntime()
        val base = FinalizedTripSnapshotBuilder.buildFromRuntime(
            title = "Turnusgap",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.OSLO_KOMMUNE,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            dates = listOf(firstDate, secondDate, thirdDate),
            roster = emptyMap(),
            plans = plans(),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            tripStart = tripStart,
            tripEnd = tripEnd,
            runtimeCalculation = runtime,
            settlement = SettlementSnapshot(runtime.paymentBasisAmount, runtime.paymentBasisAmount, true, ""),
        )
        val snapshot = base.copy(
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            rosterGapConfirmed = true,
            roster = listOf(
                RosterSnapshotRow(
                    date = firstDate,
                    code = "D1",
                    label = "Dagvakt",
                    start = firstDate.atTime(7, 0),
                    end = firstDate.atTime(14, 30),
                ),
            ),
            workBlocks = listOf(
                WorkBlock(firstDate.atTime(8, 0), firstDate.atTime(10, 0), TimeKind.ACTIVE_WORK),
                WorkBlock(secondDate.atTime(8, 0), secondDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
            ),
        )

        val presentation = snapshot.presentation

        assertEquals(390L, presentation.rosterMinutes)
        assertEquals(270L, presentation.rosterUncoveredMinutes)
        assertEquals(1, presentation.rosterUncoveredEvidence.size)
        assertEquals(firstDate.atTime(10, 0), presentation.rosterUncoveredEvidence.single().start)
        assertEquals(firstDate.atTime(14, 30), presentation.rosterUncoveredEvidence.single().end)
        assertEquals(120L, presentation.activeInsideRosterMinutes)
    }

    private fun plans(): Map<LocalDate, List<PlannedBlock>> = mapOf(
        firstDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
        secondDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
    )

    private fun segmentedRuntime(): TariffRuntimeCalculation.SegmentedContexts {
        val tariff = FerieturTariffs.dok25_2026_2028
        val rateSet = FerieturTariffRates.current
        val salaryA = SalaryTableDescriptor(
            id = "presentation-salary-a",
            effectiveFrom = firstDate,
            verifiedThrough = firstDate,
            tariffPackageId = tariff.id,
            sourceLabel = "Presentasjon A",
            sourcePageUrl = "https://example.invalid/a",
        )
        val salaryB = SalaryTableDescriptor(
            id = "presentation-salary-b",
            effectiveFrom = secondDate,
            verifiedThrough = thirdDate,
            tariffPackageId = tariff.id,
            sourceLabel = "Presentasjon B",
            sourcePageUrl = "https://example.invalid/b",
        )
        val segmentA = TariffCalculationSegment(firstDate, firstDate, tariff, rateSet, salaryA)
        val segmentB = TariffCalculationSegment(secondDate, thirdDate, tariff, rateSet, salaryB)
        val plan = SegmentedTariffCalculationPlan(
            rulesetVersion = tariff.rulesetVersion,
            tripStart = tripStart,
            tripEnd = tripEnd,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            slices = listOf(
                TariffCalculationSlice(
                    segment = segmentA,
                    windowStart = tripStart,
                    windowEnd = secondDate.atStartOfDay(),
                    dates = listOf(firstDate),
                    annualSalary = BigDecimal("585000"),
                    hourlyRate = BigDecimal("300.00"),
                    workBlocks = listOf(
                        SegmentedWorkBlock(
                            0,
                            WorkBlock(firstDate.atTime(8, 0), firstDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
                        ),
                    ),
                ),
                TariffCalculationSlice(
                    segment = segmentB,
                    windowStart = secondDate.atStartOfDay(),
                    windowEnd = tripEnd,
                    dates = listOf(secondDate, thirdDate),
                    annualSalary = BigDecimal("780000"),
                    hourlyRate = BigDecimal("400.00"),
                    workBlocks = listOf(
                        SegmentedWorkBlock(
                            1,
                            WorkBlock(secondDate.atTime(8, 0), secondDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
                        ),
                    ),
                ),
            ),
        )
        val scopePlan = TariffWholeTripScopePlan(
            rulesetVersion = tariff.rulesetVersion,
            stayAllowancePolicy = StayAllowanceScopePolicy(
                amountPerDay = rateSet.stayAllowancePerDay,
                remainderThresholdMinutes = rateSet.stayAllowanceRemainderThresholdMinutes,
            ),
            shortNoticePolicy = ShortNoticeScopePolicy(
                maxMinutes = rateSet.shortNoticeMaxMinutes,
                overtimeRoundingStepMinutes = rateSet.overtimeRoundingStepMinutes,
            ),
            restingWatchScopes = emptyList(),
        )
        fun activeLine(idSuffix: String, start: LocalDateTime, amount: String) = CalculationLine(
            id = "active",
            title = "Aktivt arbeid $idSuffix",
            detail = "1 t",
            amount = BigDecimal(amount),
            source = "Dok. 25 2026–28, punkt 20.2",
            explanation = "Testlinje",
            evidence = listOf(CalculationEvidence(start, start.plusHours(1), 60, "Aktivt arbeid")),
        )
        val monetary = SegmentedTariffMonetaryCalculation(
            plan = plan,
            scopePlan = scopePlan,
            lineEntries = listOf(
                TariffScopedCalculationLine(
                    scope = TariffCalculationLineScope.SEGMENT_LOCAL,
                    line = activeLine("A", firstDate.atTime(8, 0), "300.00"),
                    sliceIndex = 0,
                ),
                TariffScopedCalculationLine(
                    scope = TariffCalculationLineScope.SEGMENT_LOCAL,
                    line = activeLine("B", secondDate.atTime(8, 0), "400.00"),
                    sliceIndex = 1,
                ),
                TariffScopedCalculationLine(
                    scope = TariffCalculationLineScope.WHOLE_TRIP,
                    line = CalculationLine(
                        id = "stay-allowance",
                        title = "Døgngodtgjøring",
                        detail = "2 døgn × 110 kr",
                        amount = BigDecimal("220.00"),
                        source = "Dok. 25 2026–28, punkt 20.6",
                        explanation = "Test",
                    ),
                ),
            ),
        )
        return TariffRuntimeCalculation.SegmentedContexts(
            segmented = monetary,
            rulesetVersion = tariff.rulesetVersion,
            provenance = listOf(
                TariffRuntimeProvenanceSlice(
                    start = firstDate,
                    end = firstDate,
                    tariffPackageId = tariff.id,
                    rulesetVersion = tariff.rulesetVersion,
                    tariffRateSetId = rateSet.id,
                    salaryTableId = salaryA.id,
                    salaryTableEffectiveFrom = salaryA.effectiveFrom,
                    salaryTableSourceLabel = salaryA.sourceLabel,
                    annualSalary = BigDecimal("585000"),
                    hourlyRate = BigDecimal("300.00"),
                ),
                TariffRuntimeProvenanceSlice(
                    start = secondDate,
                    end = thirdDate,
                    tariffPackageId = tariff.id,
                    rulesetVersion = tariff.rulesetVersion,
                    tariffRateSetId = rateSet.id,
                    salaryTableId = salaryB.id,
                    salaryTableEffectiveFrom = salaryB.effectiveFrom,
                    salaryTableSourceLabel = salaryB.sourceLabel,
                    annualSalary = BigDecimal("780000"),
                    hourlyRate = BigDecimal("400.00"),
                ),
            ),
        )
    }
}
