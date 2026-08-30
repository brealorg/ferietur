package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TariffRateSetTest {
    private val rates = FerieturTariffRates.dok25_2026_2028

    @Test
    fun currentRateSetOwnsAllPreviouslyEmbedded2026CalculationConstants() {
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, rates.tariffPackageId)
        assertEquals(LocalDate.of(2026, 5, 1), rates.effectiveFrom)
        assertEquals(LocalDate.of(2028, 4, 30), rates.effectiveTo)
        assertEquals(1950, rates.weeklyDivisor(WeeklyBasis.HOURS_37_5))
        assertEquals(1846, rates.weeklyDivisor(WeeklyBasis.HOURS_35_5))
        assertEquals(1846, rates.weeklyDivisor(WeeklyBasis.DOK25_8_2_2))
        assertEquals(1747, rates.weeklyDivisor(WeeklyBasis.HOURS_33_6))
        assertEquals(BigDecimal("0.40"), rates.eveningNightFraction)
        assertEquals(BigDecimal("0.23"), rates.weekendRate(WeekendProfile.STANDARD).percentage)
        assertEquals(BigDecimal("73"), rates.weekendRate(WeekendProfile.STANDARD).minimumPerHour)
        assertEquals(BigDecimal("0.30"), rates.weekendRate(WeekendProfile.EXTENDED_30).percentage)
        assertEquals(BigDecimal("110"), rates.weekendRate(WeekendProfile.EXTENDED_30).minimumPerHour)
        assertEquals(BigDecimal("0.35"), rates.weekendRate(WeekendProfile.EXTENDED_35).percentage)
        assertEquals(BigDecimal("135"), rates.weekendRate(WeekendProfile.EXTENDED_35).minimumPerHour)
        assertEquals(BigDecimal(4), rates.holidaySupplementNumerator)
        assertEquals(BigDecimal(3), rates.holidaySupplementDenominator)
        assertEquals(BigDecimal("1.50"), rates.chapter20ActiveMultiplier)
        assertEquals(BigDecimal("110"), rates.stayAllowancePerDay)
        assertEquals(360L, rates.stayAllowanceRemainderThresholdMinutes)
        assertEquals(3, rates.passiveWorkDivisor)
        assertEquals(30, rates.activeNightRoundingStepMinutes)
        assertEquals(15, rates.activeNightRoundUpRemainderAtMinutes)
        assertEquals(120L, rates.shortNoticeMaxMinutes)
        assertEquals(30L, rates.overtimeRoundingStepMinutes)
        assertEquals(BigDecimal("0.50"), rates.overtimeStandardFraction)
        assertEquals(BigDecimal.ONE, rates.overtimeHighFraction)
        assertEquals(BigDecimal("1.3333333333"), rates.specialOvertimeFraction133)
        assertEquals("133 1/3", rates.specialOvertimePercentageLabel)
        assertEquals(LocalTime.of(20, 0), rates.overtimeHighStart)
        assertEquals(LocalTime.of(7, 0), rates.overtimeHighEnd)
        assertEquals(LocalTime.of(17, 0), rates.eveningStart)
        assertEquals(LocalTime.of(6, 0), rates.nightEnd)
        assertEquals(LocalTime.of(8, 0), rates.nightWatchSupplementEnd)
        assertEquals(LocalTime.of(23, 0), rates.travelSleepWindowStart)
        assertEquals(LocalTime.of(7, 0), rates.travelSleepWindowEnd)
    }

    @Test
    fun tariffPackageAndDateRangeResolveTheExactRateSetIdentity() {
        val tariff = FerieturTariffs.dok25_2026_2028

        assertSame(
            rates,
            FerieturTariffRates.requireForRange(
                tariff.id,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 7),
            ),
        )
        assertSame(rates, FerieturTariffRates.requireById(rates.id))
    }

    @Test
    fun catalogSupportsFutureRateRevisionsWithoutMixingOneTripAcrossBoundary() {
        val first = rates.copy(
            id = "test-rates-a",
            effectiveFrom = LocalDate.of(2030, 5, 1),
            effectiveTo = LocalDate.of(2031, 4, 30),
        )
        val second = rates.copy(
            id = "test-rates-b",
            effectiveFrom = LocalDate.of(2031, 5, 1),
            effectiveTo = LocalDate.of(2032, 4, 30),
            stayAllowancePerDay = BigDecimal("999"),
        )
        val catalog = TariffRateSetCatalog(listOf(first, second))

        assertEquals(first.id, catalog.forDate(first.tariffPackageId, first.effectiveTo)?.id)
        assertEquals(second.id, catalog.forDate(second.tariffPackageId, second.effectiveFrom)?.id)
        assertNull(catalog.forRange(first.tariffPackageId, first.effectiveTo, second.effectiveFrom))
    }

    @Test
    fun tripPlanEngineUsesInjectedRateSetForStayAllowance() {
        val custom = rates.copy(stayAllowancePerDay = BigDecimal("999"))
        val startDate = LocalDate.of(2026, 8, 10)
        val endDate = startDate.plusDays(1)

        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(startDate, endDate),
            roster = emptyMap(),
            plans = emptyMap(),
            annualSalary = BigDecimal("614600"),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(startDate, LocalTime.of(8, 0)),
            tripEnd = LocalDateTime.of(endDate, LocalTime.of(9, 0)),
            rateSet = custom,
        )

        val allowance = calculation.lines.single { it.id == "stay-allowance" }
        assertEquals("999.00", allowance.amount.toPlainString())
        assertEquals("1 døgn × 999 kr", allowance.detail)
        assertEquals(true, allowance.explanation.contains("999 kroner per døgn"))
    }

    @Test
    fun tariffMathConsumesInjectedRateSetRatherThanHiddenConstants() {
        val custom = rates.copy(
            weeklyDivisors = rates.weeklyDivisors + (WeeklyBasis.HOURS_35_5 to 2000),
            eveningNightFraction = BigDecimal("0.50"),
            stayAllowancePerDay = BigDecimal("999"),
        )

        assertEquals("307.30", TariffMath.hourlyRate(BigDecimal("614600"), WeeklyBasis.HOURS_35_5, custom).toPlainString())
        assertEquals("153.65", TariffMath.eveningNightRate(BigDecimal("307.30"), custom).toPlainString())
        assertEquals("999", custom.stayAllowancePerDay.toPlainString())
    }
}
