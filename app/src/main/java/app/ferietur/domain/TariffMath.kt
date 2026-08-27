package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class WeeklyBasis(val divisor: Int) {
    HOURS_37_5(1950),
    HOURS_35_5(1846),
    DOK25_8_2_2(1846),
    HOURS_33_6(1747),
}

enum class WeekendProfile(
    val percentage: BigDecimal,
    val minimumPerHour: BigDecimal,
    val label: String,
) {
    STANDARD(BigDecimal("0.23"), BigDecimal("73"), "23 % · min. 73 kr/t"),
    EXTENDED_30(BigDecimal("0.30"), BigDecimal("110"), "30 % · min. 110 kr/t"),
    EXTENDED_35(BigDecimal("0.35"), BigDecimal("135"), "35 % · min. 135 kr/t"),
}

data class RestingNightResult(
    val workTimeMinutes: Int,
    val payEquivalentMinutes: BigDecimal,
)

object TariffMath {
    fun hourlyRate(annualSalary: BigDecimal, basis: WeeklyBasis): BigDecimal =
        annualSalary.divide(BigDecimal(basis.divisor), 2, RoundingMode.HALF_UP)

    fun eveningNightRate(hourlyRate: BigDecimal): BigDecimal =
        hourlyRate.multiply(BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP)

    fun weekendRate(hourlyRate: BigDecimal, profile: WeekendProfile): BigDecimal {
        val percentageRate = hourlyRate.multiply(profile.percentage).setScale(2, RoundingMode.HALF_UP)
        return if (percentageRate > profile.minimumPerHour) percentageRate else profile.minimumPerHour.setScale(2, RoundingMode.HALF_UP)
    }

    fun holidaySupplementRate(hourlyRate: BigDecimal): BigDecimal =
        hourlyRate.multiply(BigDecimal(4)).divide(BigDecimal(3), 2, RoundingMode.HALF_UP)

    fun restingNight(minutes: Int): RestingNightResult {
        require(minutes >= 0)
        return RestingNightResult(
            workTimeMinutes = minutes,
            payEquivalentMinutes = BigDecimal(minutes).divide(BigDecimal(3), 8, RoundingMode.HALF_UP),
        )
    }

    fun passiveNight(minutes: Int): RestingNightResult = restingNight(minutes)

    fun roundActiveNightMinutes(totalMinutes: Int): Int {
        require(totalMinutes >= 0)
        val wholeHalfHours = totalMinutes / 30
        val remainder = totalMinutes % 30
        return (wholeHalfHours + if (remainder >= 15) 1 else 0) * 30
    }
}
