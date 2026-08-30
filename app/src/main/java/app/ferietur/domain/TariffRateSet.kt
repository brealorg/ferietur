package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class WeekendRateSpec(
    val percentage: BigDecimal,
    val minimumPerHour: BigDecimal,
    val label: String,
)

data class TariffRateSet(
    val id: String,
    val tariffPackageId: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate,
    val weeklyDivisors: Map<WeeklyBasis, Int>,
    val eveningNightFraction: BigDecimal,
    val weekendRates: Map<WeekendProfile, WeekendRateSpec>,
    val holidaySupplementNumerator: BigDecimal,
    val holidaySupplementDenominator: BigDecimal,
    val chapter20ActiveMultiplier: BigDecimal,
    val passiveWorkDivisor: Int,
    val stayAllowancePerDay: BigDecimal,
    val stayAllowanceRemainderThresholdMinutes: Long,
    val activeNightRoundingStepMinutes: Int,
    val activeNightRoundUpRemainderAtMinutes: Int,
    val shortNoticeMaxMinutes: Long,
    val overtimeRoundingStepMinutes: Long,
    val overtimeStandardFraction: BigDecimal,
    val overtimeHighFraction: BigDecimal,
    val specialOvertimeFraction133: BigDecimal,
    val specialOvertimePercentageLabel: String,
    val overtimeHighStart: LocalTime,
    val overtimeHighEnd: LocalTime,
    val eveningStart: LocalTime,
    val nightEnd: LocalTime,
    val nightWatchSupplementEnd: LocalTime,
    val travelSleepWindowStart: LocalTime,
    val travelSleepWindowEnd: LocalTime,
) {
    init {
        require(id.isNotBlank())
        require(tariffPackageId.isNotBlank())
        require(!effectiveTo.isBefore(effectiveFrom)) {
            "Satssettet $id har sluttdato før startdato."
        }
        require(weeklyDivisors.keys == WeeklyBasis.entries.toSet()) {
            "Satssettet $id mangler timeverksdivisor for én eller flere arbeidsuker."
        }
        require(weeklyDivisors.values.all { it > 0 })
        require(weekendRates.keys == WeekendProfile.entries.toSet()) {
            "Satssettet $id mangler helgesats for én eller flere profiler."
        }
        require(passiveWorkDivisor > 0)
        require(stayAllowanceRemainderThresholdMinutes >= 0)
        require(activeNightRoundingStepMinutes > 0)
        require(activeNightRoundUpRemainderAtMinutes in 1..activeNightRoundingStepMinutes)
        require(shortNoticeMaxMinutes >= 0)
        require(overtimeRoundingStepMinutes > 0)
        require(holidaySupplementDenominator.signum() > 0)
        require(specialOvertimePercentageLabel.isNotBlank())
    }

    fun covers(date: LocalDate): Boolean =
        !date.isBefore(effectiveFrom) && !date.isAfter(effectiveTo)

    fun coversRange(start: LocalDate, end: LocalDate): Boolean =
        !end.isBefore(start) && covers(start) && covers(end)

    fun weeklyDivisor(basis: WeeklyBasis): Int =
        requireNotNull(weeklyDivisors[basis]) { "Mangler divisor for $basis i $id" }

    fun weekendRate(profile: WeekendProfile): WeekendRateSpec =
        requireNotNull(weekendRates[profile]) { "Mangler helgesats for $profile i $id" }
}

class TariffRateSetCatalog(rateSets: List<TariffRateSet>) {
    private val ordered = rateSets.sortedWith(
        compareBy<TariffRateSet> { it.tariffPackageId }.thenBy { it.effectiveFrom },
    )
    private val byId = ordered.associateBy { it.id }

    init {
        require(byId.size == ordered.size) { "Duplisert satssett-ID." }
        ordered.groupBy { it.tariffPackageId }.forEach { (packageId, packageRates) ->
            packageRates.zipWithNext().forEach { (previous, next) ->
                require(next.effectiveFrom.isAfter(previous.effectiveTo)) {
                    "Overlappende satssett i tariffpakken $packageId: ${previous.id} og ${next.id}"
                }
            }
        }
    }

    fun forId(id: String): TariffRateSet? = byId[id]

    fun requireById(id: String): TariffRateSet =
        requireNotNull(forId(id)) { "Ukjent tariff-satssett: $id" }

    fun forDate(packageId: String, date: LocalDate): TariffRateSet? =
        ordered.lastOrNull { it.tariffPackageId == packageId && it.covers(date) }

    fun forRange(packageId: String, start: LocalDate, end: LocalDate): TariffRateSet? {
        if (end.isBefore(start)) return null
        val first = forDate(packageId, start) ?: return null
        val last = forDate(packageId, end) ?: return null
        return first.takeIf { it.id == last.id && it.coversRange(start, end) }
    }

    fun requireForRange(packageId: String, start: LocalDate, end: LocalDate): TariffRateSet =
        requireNotNull(forRange(packageId, start, end)) {
            "Ingen enkelt verifisert tariff-sats dekker hele turperioden $start–$end " +
                "i tariffpakken $packageId. En tur som krysser en satsgrense må behandles " +
                "med eksplisitt split-rate-støtte før den kan beregnes."
        }
}

object FerieturTariffRates {
    const val DOK25_2026_2028_RATE_SET_ID = "oslo-dok25-2026-2028-rates-2026.1"

    val dok25_2026_2028 = TariffRateSet(
        id = DOK25_2026_2028_RATE_SET_ID,
        tariffPackageId = FerieturTariffs.DOK25_2026_2028_ID,
        effectiveFrom = LocalDate.of(2026, 5, 1),
        effectiveTo = LocalDate.of(2028, 4, 30),
        weeklyDivisors = mapOf(
            WeeklyBasis.HOURS_37_5 to 1950,
            WeeklyBasis.HOURS_35_5 to 1846,
            WeeklyBasis.DOK25_8_2_2 to 1846,
            WeeklyBasis.HOURS_33_6 to 1747,
        ),
        eveningNightFraction = BigDecimal("0.40"),
        weekendRates = mapOf(
            WeekendProfile.STANDARD to WeekendRateSpec(BigDecimal("0.23"), BigDecimal("73"), "23 % · min. 73 kr/t"),
            WeekendProfile.EXTENDED_30 to WeekendRateSpec(BigDecimal("0.30"), BigDecimal("110"), "30 % · min. 110 kr/t"),
            WeekendProfile.EXTENDED_35 to WeekendRateSpec(BigDecimal("0.35"), BigDecimal("135"), "35 % · min. 135 kr/t"),
        ),
        holidaySupplementNumerator = BigDecimal(4),
        holidaySupplementDenominator = BigDecimal(3),
        chapter20ActiveMultiplier = BigDecimal("1.50"),
        passiveWorkDivisor = 3,
        stayAllowancePerDay = BigDecimal("110"),
        stayAllowanceRemainderThresholdMinutes = 6L * 60L,
        activeNightRoundingStepMinutes = 30,
        activeNightRoundUpRemainderAtMinutes = 15,
        shortNoticeMaxMinutes = 2L * 60L,
        overtimeRoundingStepMinutes = 30L,
        overtimeStandardFraction = BigDecimal("0.50"),
        overtimeHighFraction = BigDecimal.ONE,
        specialOvertimeFraction133 = BigDecimal("1.3333333333"),
        specialOvertimePercentageLabel = "133 1/3",
        overtimeHighStart = LocalTime.of(20, 0),
        overtimeHighEnd = LocalTime.of(7, 0),
        eveningStart = LocalTime.of(17, 0),
        nightEnd = LocalTime.of(6, 0),
        nightWatchSupplementEnd = LocalTime.of(8, 0),
        travelSleepWindowStart = LocalTime.of(23, 0),
        travelSleepWindowEnd = LocalTime.of(7, 0),
    )

    private val catalog = TariffRateSetCatalog(
        listOf(
            dok25_2026_2028,
        ),
    )

    val current: TariffRateSet get() = dok25_2026_2028

    fun forId(id: String): TariffRateSet? = catalog.forId(id)

    fun requireById(id: String): TariffRateSet = catalog.requireById(id)

    fun forDate(packageId: String, date: LocalDate): TariffRateSet? =
        catalog.forDate(packageId, date)

    fun forRange(packageId: String, start: LocalDate, end: LocalDate): TariffRateSet? =
        catalog.forRange(packageId, start, end)

    fun requireForRange(packageId: String, start: LocalDate, end: LocalDate): TariffRateSet =
        catalog.requireForRange(packageId, start, end)
}
