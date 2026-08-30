package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class TariffEffectiveDateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(!end.isBefore(start))
    }

    companion object {
        /**
         * Maps the half-open trip interval [tripStart, tripEnd) to the dates on
         * which tariff work can actually occur. A trip ending exactly at 00:00
         * does not consume the new calendar date merely because the UI stores an
         * inclusive end date.
         */
        fun forTrip(tripStart: LocalDateTime, tripEnd: LocalDateTime): TariffEffectiveDateRange? {
            if (!tripEnd.isAfter(tripStart)) return null
            val lastOccupiedDate = if (tripEnd.toLocalTime() == LocalTime.MIDNIGHT) {
                tripEnd.toLocalDate().minusDays(1)
            } else {
                tripEnd.toLocalDate()
            }
            if (lastOccupiedDate.isBefore(tripStart.toLocalDate())) return null
            return TariffEffectiveDateRange(tripStart.toLocalDate(), lastOccupiedDate)
        }
    }
}

enum class TariffSegmentationFailureReason {
    INVALID_RANGE,
    UNSUPPORTED_DATE,
    INCOHERENT_SOURCES,
    SEMANTIC_RULESET_CHANGE,
}

data class TariffCalculationSegment(
    val start: LocalDate,
    val end: LocalDate,
    val tariffPackage: TariffPackage,
    val rateSet: TariffRateSet,
    val salaryTable: SalaryTableDescriptor,
) {
    init {
        require(!end.isBefore(start)) { "Tariffsegmentet har sluttdato før startdato." }
        require(tariffPackage.coversRange(start, end)) {
            "Tariffpakken ${tariffPackage.id} dekker ikke segmentet $start–$end."
        }
        require(rateSet.tariffPackageId == tariffPackage.id) {
            "Satssettet ${rateSet.id} tilhører ikke tariffpakken ${tariffPackage.id}."
        }
        require(rateSet.coversRange(start, end)) {
            "Satssettet ${rateSet.id} dekker ikke segmentet $start–$end."
        }
        require(salaryTable.tariffPackageId == tariffPackage.id) {
            "Lønnstabellen ${salaryTable.id} tilhører ikke tariffpakken ${tariffPackage.id}."
        }
        require(salaryTable.coversRange(start, end)) {
            "Lønnstabellen ${salaryTable.id} dekker ikke segmentet $start–$end."
        }
    }
}

sealed interface TariffSegmentationResult {
    data class Success(
        val rulesetVersion: String,
        val segments: List<TariffCalculationSegment>,
    ) : TariffSegmentationResult {
        init {
            require(rulesetVersion.isNotBlank())
            require(segments.isNotEmpty())
            require(segments.all { it.tariffPackage.rulesetVersion == rulesetVersion })
            segments.zipWithNext().forEach { (previous, next) ->
                require(next.start == previous.end.plusDays(1)) {
                    "Tariffsegmentene må dekke perioden sammenhengende uten hull eller overlapp."
                }
            }
        }

        val isSplit: Boolean get() = segments.size > 1
        val changesRateSet: Boolean get() = segments.map { it.rateSet.id }.distinct().size > 1
        val changesSalaryTable: Boolean get() = segments.map { it.salaryTable.id }.distinct().size > 1
    }

    data class Failure(
        val reason: TariffSegmentationFailureReason,
        val date: LocalDate?,
        val detail: String,
    ) : TariffSegmentationResult
}

class TariffSegmentPlanner(
    private val packageForDate: (LocalDate) -> TariffPackage?,
    private val rateSetForDate: (String, LocalDate) -> TariffRateSet?,
    private val salaryTableForDate: (LocalDate) -> SalaryTableDescriptor?,
) {
    fun planRange(start: LocalDate, end: LocalDate): TariffSegmentationResult {
        if (end.isBefore(start)) {
            return TariffSegmentationResult.Failure(
                reason = TariffSegmentationFailureReason.INVALID_RANGE,
                date = start,
                detail = "Sluttdato $end er før startdato $start.",
            )
        }

        val segments = mutableListOf<TariffCalculationSegment>()
        var cursor = start
        var expectedRulesetVersion: String? = null

        while (!cursor.isAfter(end)) {
            val tariffPackage = packageForDate(cursor)
                ?: return unsupported(cursor, "Ingen tariffpakke dekker datoen $cursor.")

            val rulesetVersion = expectedRulesetVersion
            if (rulesetVersion == null) {
                expectedRulesetVersion = tariffPackage.rulesetVersion
            } else if (tariffPackage.rulesetVersion != rulesetVersion) {
                return TariffSegmentationResult.Failure(
                    reason = TariffSegmentationFailureReason.SEMANTIC_RULESET_CHANGE,
                    date = cursor,
                    detail = "Regelsettet endres fra $rulesetVersion til ${tariffPackage.rulesetVersion} ved $cursor. " +
                        "Ferietur kan segmentere rene sats- og lønnstabellendringer automatisk, " +
                        "men en semantisk regelendring må implementeres og valideres eksplisitt.",
                )
            }

            val rateSet = rateSetForDate(tariffPackage.id, cursor)
                ?: return unsupported(
                    cursor,
                    "Ingen verifisert tariff-sats dekker $cursor i tariffpakken ${tariffPackage.id}.",
                )
            val salaryTable = salaryTableForDate(cursor)
                ?: return unsupported(cursor, "Ingen verifisert lønnstabell dekker datoen $cursor.")

            if (rateSet.tariffPackageId != tariffPackage.id || salaryTable.tariffPackageId != tariffPackage.id) {
                return TariffSegmentationResult.Failure(
                    reason = TariffSegmentationFailureReason.INCOHERENT_SOURCES,
                    date = cursor,
                    detail = "Tariffpakke, satssett og lønnstabell peker ikke på samme tariffgrunnlag ved $cursor.",
                )
            }

            val segmentEnd = minOf(
                end,
                tariffPackage.effectiveTo,
                rateSet.effectiveTo,
                salaryTable.verifiedThrough,
            )
            if (segmentEnd.isBefore(cursor)) {
                return TariffSegmentationResult.Failure(
                    reason = TariffSegmentationFailureReason.INCOHERENT_SOURCES,
                    date = cursor,
                    detail = "Kildenes gyldighetsperioder kan ikke danne et gyldig segment ved $cursor.",
                )
            }

            segments += TariffCalculationSegment(
                start = cursor,
                end = segmentEnd,
                tariffPackage = tariffPackage,
                rateSet = rateSet,
                salaryTable = salaryTable,
            )

            if (segmentEnd == end) break
            cursor = segmentEnd.plusDays(1)
        }

        return TariffSegmentationResult.Success(
            rulesetVersion = requireNotNull(expectedRulesetVersion),
            segments = segments,
        )
    }

    private fun unsupported(date: LocalDate, detail: String): TariffSegmentationResult.Failure =
        TariffSegmentationResult.Failure(
            reason = TariffSegmentationFailureReason.UNSUPPORTED_DATE,
            date = date,
            detail = detail,
        )
}
