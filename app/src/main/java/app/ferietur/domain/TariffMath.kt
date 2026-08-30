package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class WeeklyBasis {
    HOURS_37_5,
    HOURS_35_5,
    DOK25_8_2_2,
    HOURS_33_6;

    val divisor: Int get() = FerieturTariffRates.current.weeklyDivisor(this)
}

enum class WeekendProfile {
    STANDARD,
    EXTENDED_30,
    EXTENDED_35;

    val percentage: BigDecimal get() = FerieturTariffRates.current.weekendRate(this).percentage
    val minimumPerHour: BigDecimal get() = FerieturTariffRates.current.weekendRate(this).minimumPerHour
    val label: String get() = FerieturTariffRates.current.weekendRate(this).label
}

data class RestingNightResult(
    val workTimeMinutes: Int,
    val payEquivalentMinutes: BigDecimal,
)

object TariffMath {
    fun hourlyRate(
        annualSalary: BigDecimal,
        basis: WeeklyBasis,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): BigDecimal =
        annualSalary.divide(BigDecimal(rateSet.weeklyDivisor(basis)), 2, RoundingMode.HALF_UP)

    fun eveningNightRate(
        hourlyRate: BigDecimal,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): BigDecimal =
        hourlyRate.multiply(rateSet.eveningNightFraction).setScale(2, RoundingMode.HALF_UP)

    fun weekendRate(
        hourlyRate: BigDecimal,
        profile: WeekendProfile,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): BigDecimal {
        val spec = rateSet.weekendRate(profile)
        val percentageRate = hourlyRate.multiply(spec.percentage).setScale(2, RoundingMode.HALF_UP)
        return if (percentageRate > spec.minimumPerHour) percentageRate else spec.minimumPerHour.setScale(2, RoundingMode.HALF_UP)
    }

    fun holidaySupplementRate(
        hourlyRate: BigDecimal,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): BigDecimal =
        hourlyRate
            .multiply(rateSet.holidaySupplementNumerator)
            .divide(rateSet.holidaySupplementDenominator, 2, RoundingMode.HALF_UP)

    fun restingNight(
        minutes: Int,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): RestingNightResult {
        require(minutes >= 0)
        return RestingNightResult(
            workTimeMinutes = minutes,
            payEquivalentMinutes = BigDecimal(minutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP),
        )
    }

    fun passiveNight(
        minutes: Int,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): RestingNightResult = restingNight(minutes, rateSet)

    fun roundActiveNightMinutes(
        totalMinutes: Int,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): Int {
        require(totalMinutes >= 0)
        val step = rateSet.activeNightRoundingStepMinutes
        val wholeSteps = totalMinutes / step
        val remainder = totalMinutes % step
        return (wholeSteps + if (remainder >= rateSet.activeNightRoundUpRemainderAtMinutes) 1 else 0) * step
    }
}
