package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Pilot01OpenRuleTraceabilityTest {
    @Test
    fun solgardenKeepsMoneyButSurfacesPassiveSupplementInterpretationAndTravelSource() {
        val start = LocalDate.of(2026, 8, 11)
        val dates = (0..7).map { start.plusDays(it.toLong()) }

        val activeAndResting = listOf(
            PlannedBlock(
                TimeKind.ACTIVE_WORK,
                LocalTime.of(7, 0),
                LocalTime.of(22, 0),
            ),
            PlannedBlock(
                TimeKind.RESTING_NIGHT_WATCH,
                LocalTime.of(23, 0),
                LocalTime.of(7, 0),
            ),
        )

        val plans = mapOf(
            start to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(7, 0),
                    LocalTime.of(15, 0),
                ),
            ),
            start.plusDays(1) to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(7, 0),
                    LocalTime.of(22, 0),
                ),
            ),
            start.plusDays(2) to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(7, 0),
                    LocalTime.of(22, 0),
                ),
            ),
            start.plusDays(3) to activeAndResting,
            start.plusDays(4) to activeAndResting,
            start.plusDays(5) to activeAndResting,
            start.plusDays(6) to activeAndResting,
            start.plusDays(7) to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(7, 0),
                    LocalTime.of(16, 0),
                ),
                PlannedBlock(
                    TimeKind.TRAVEL_WITH_RESPONSIBILITY,
                    LocalTime.of(16, 0),
                    LocalTime.of(23, 0),
                ),
            ),
        )

        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = start.atTime(6, 0),
            tripEnd = start.plusDays(7).atTime(23, 0),
        ) as TariffRuntimeCalculationResult.Success

        val calculation = result.calculation

        fun amount(id: String): BigDecimal =
            calculation.lines.single { it.id == id }.amount.setScale(2)

        assertEquals(
            BigDecimal("51307.42"),
            calculation.paymentBasisAmount.setScale(2),
        )

        assertEquals(BigDecimal("37955.16"), amount("active"))
        assertEquals(BigDecimal("3551.36"), amount("resting-night"))
        assertEquals(BigDecimal("1420.59"), amount("resting-evening-night"))
        assertEquals(BigDecimal("408.43"), amount("resting-weekend"))
        assertEquals(BigDecimal("4794.48"), amount("evening-night"))
        assertEquals(BigDecimal("2297.40"), amount("weekend"))
        assertEquals(BigDecimal("880.00"), amount("stay-allowance"))

        assertTrue(
            calculation.applicableUnresolvedRuleIds.isEmpty(),
        )

        val workingInterpretations =
            FerieturRules
                .applicableWorkingInterpretationRules(
                    calculation.lines,
                )

        assertEquals(
            listOf("D25_8_9_X20"),
            workingInterpretations.map { it.id },
        )

        val rule =
            workingInterpretations.single()

        assertEquals(
            RuleStatus.WORKING_INTERPRETATION,
            rule.status,
        )

        val binding = FerieturRuleSources.forRule("D25_8_9_X20")
        assertEquals(
            listOf("8.9", "12.1.1", "12.2.2", "12.2.3", "20.3", "20.4"),
            binding.sections,
        )

        val active = calculation.lines.single { it.id == "active" }
        assertTrue(active.source.contains("9.6"))
        assertTrue(active.source.contains("20.3"))

        listOf(
            "resting-evening-night",
            "resting-weekend",
        ).forEach { id ->
            val line = calculation.lines.single { it.id == id }
            assertTrue(line.includedInKnownTotal)
            assertEquals(
                PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
                line.paymentTreatment,
            )
        }
    }
}
