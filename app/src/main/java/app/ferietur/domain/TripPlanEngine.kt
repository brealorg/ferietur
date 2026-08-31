package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CalculationCertainty {
    CONFIRMED,
    ASSUMPTION,
    OPEN,
}

enum class PaymentTreatment {
    INCLUDED_IN_PAYMENT_BASIS,
    ALREADY_COVERED_BY_NORMAL_ROSTER,
    OPEN,
}

data class CalculationEvidence(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val minutes: Long,
    val note: String,
)

data class CalculationLine(
    val id: String,
    val title: String,
    val detail: String,
    val amount: BigDecimal,
    val source: String,
    val explanation: String,
    val evidence: List<CalculationEvidence> = emptyList(),
    val certainty: CalculationCertainty = CalculationCertainty.CONFIRMED,
    val includedInKnownTotal: Boolean = true,
    val paymentTreatment: PaymentTreatment = PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
)

data class DayCalculationContribution(
    val lineId: String,
    val title: String,
    val minutes: Long,
    val amount: BigDecimal,
    val source: String,
    val certainty: CalculationCertainty,
    val includedInKnownTotal: Boolean,
    val paymentTreatment: PaymentTreatment,
    val evidence: List<CalculationEvidence>,
)

data class DayCalculationAudit(
    val date: LocalDate,
    val holidayLabels: List<String>,
    val contributions: List<DayCalculationContribution>,
    val knownSubtotal: BigDecimal,
    val paymentSubtotal: BigDecimal,
    val alreadyCoveredSubtotal: BigDecimal,
    val openSubtotal: BigDecimal,
)

data class CalculationWorktimeAudit(
    val rosterMinutes: Long,
    val rosterUncoveredMinutes: Long,
    val rosterUncoveredEvidence: List<CalculationEvidence>,
    val activeMinutes: Long,
    val activeInsideRosterMinutes: Long,
    val activeOutsideRosterMinutes: Long,
    val restingNightMinutes: Long,
    val restingNightOutsideRosterMinutes: Long,
)

data class PreliminaryCalculation(
    val hourlyRate: BigDecimal,
    val rosterMinutes: Long,
    val rosterUncoveredMinutes: Long,
    val rosterUncoveredEvidence: List<CalculationEvidence>,
    val activeMinutes: Long,
    val activeInsideRosterMinutes: Long,
    val activeOutsideRosterMinutes: Long,
    val payableActiveWorkMinutes: Long,
    val payableTravelWithResponsibilityMinutes: Long,
    val restingNightMinutes: Long,
    val restingNightOutsideRosterMinutes: Long,
    val eveningNightMinutes: Long,
    val weekendMinutes: Long,
    val holidayMinutes: Long,
    val stayAllowanceDays: Int,
    val lines: List<CalculationLine>,
    val dayAudits: List<DayCalculationAudit>,
    val knownAmount: BigDecimal,
    val paymentBasisAmount: BigDecimal,
    val alreadyCoveredByNormalRosterAmount: BigDecimal,
    val excludedKnownRuleAmount: BigDecimal,
    val applicableUnresolvedRuleIds: Set<String>,
)

enum class FindingSeverity {
    OK,
    REVIEW,
    CRITICAL,
    OPEN,
}

data class ControlFinding(
    val severity: FindingSeverity,
    val title: String,
    val detail: String,
)

data class DayProjectedBlock(
    val sourceDate: LocalDate,
    val block: WorkBlock,
    val continuesFromPreviousDay: Boolean,
    val continuesIntoNextDay: Boolean,
)

private data class RosterIntersection(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val shift: ShiftDefinition,
)

private data class ActiveEventGroup(
    val actualMinutes: Long,
    val roundedMinutes: Long,
    val evidence: List<CalculationEvidence>,
)

private data class OvertimeSupplementBand(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val actualMinutes: Long,
    val paidMinutes: Long,
    val supplementFraction: BigDecimal,
    val label: String,
    val special133Potential: Boolean,
)

object TripPlanEngine {
    private val activeKinds = setOf(
        TimeKind.ACTIVE_WORK,
        TimeKind.ACTIVE_NIGHT_WATCH,
        TimeKind.TRAVEL_WITH_RESPONSIBILITY,
    )

    fun planFromRoster(dates: List<LocalDate>, roster: Map<LocalDate, String>): Map<LocalDate, List<PlannedBlock>> =
        dates.associateWith { date ->
            RosterEntryCodec.decode(roster[date]).flatMap(::plannedBlocksForShift)
        }

    fun hasRosterOverlap(roster: Map<LocalDate, String>): Boolean {
        val intervals = roster.flatMap { (date, value) ->
            RosterEntryCodec.decode(value).mapNotNull { shift ->
                TurnusOverlapEngine.intervalFor(date, shift)
            }
        }.sortedBy { it.first }
        return intervals.zipWithNext().any { (first, second) -> second.first.isBefore(first.second) }
    }

    fun plannedBlocksForShift(shift: ShiftDefinition?): List<PlannedBlock> {
        if (shift?.start == null || shift.end == null || shift.category == ShiftCategory.OFF) return emptyList()
        val kind = if (shift.category == ShiftCategory.NIGHT) TimeKind.ACTIVE_NIGHT_WATCH else TimeKind.ACTIVE_WORK
        return listOf(PlannedBlock(kind, shift.start, shift.end))
    }

    fun projectDay(date: LocalDate, blocks: List<PlannedBlock>): List<WorkBlock> {
        val restingNight = blocks.firstOrNull {
            it.kind == TimeKind.RESTING_NIGHT_WATCH && !it.end.isAfter(it.start)
        }
        return blocks.map { block ->
            val blockDate = if (
                block.kind == TimeKind.ACTIVE_EVENT_ON_RESTING &&
                restingNight != null &&
                block.start.isBefore(restingNight.end)
            ) date.plusDays(1) else date
            block.toWorkBlock(blockDate)
        }.sortedBy { it.start }
    }

    fun projectRange(
        dates: List<LocalDate>,
        plans: Map<LocalDate, List<PlannedBlock>>,
    ): List<WorkBlock> = dates.flatMap { date ->
        projectDay(date, plans[date].orEmpty())
    }.sortedBy { it.start }

    fun projectVisibleDay(
        date: LocalDate,
        plans: Map<LocalDate, List<PlannedBlock>>,
    ): List<DayProjectedBlock> {
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        return listOf(date.minusDays(1), date).flatMap { sourceDate ->
            projectDay(sourceDate, plans[sourceDate].orEmpty()).mapNotNull { original ->
                val visible = intersection(original.start, original.end, dayStart, dayEnd) ?: return@mapNotNull null
                DayProjectedBlock(
                    sourceDate = sourceDate,
                    block = WorkBlock(visible.first, visible.second, original.kind, original.travelNoticeStatus),
                    continuesFromPreviousDay = original.start.isBefore(dayStart),
                    continuesIntoNextDay = original.end.isAfter(dayEnd),
                )
            }
        }.sortedBy { it.block.start }
    }

    fun normalizeTravelClassification(
        date: LocalDate,
        blocks: List<PlannedBlock>,
    ): List<PlannedBlock> {
        val travel = blocks.filter { it.kind.isTravelKind() }
        if (travel.isEmpty()) return blocks.sortedBy { it.start }
        val nonTravel = blocks.filterNot { it.kind.isTravelKind() }
        return overlayTravelOnPlan(
            dates = listOf(date),
            basePlans = mapOf(date to nonTravel),
            travelPlans = mapOf(date to travel),
        )[date].orEmpty().sortedBy { it.start }
    }

    fun overlapWithRoster(block: WorkBlock, roster: Map<LocalDate, String>): Long =
        rosterIntersections(block, roster).sumOf { ChronoUnit.MINUTES.between(it.start, it.end) }
            .coerceAtMost(durationMinutes(block))

    fun planOverlapWarnings(date: LocalDate, blocks: List<PlannedBlock>): List<String> =
        overlappingInput(projectDay(date, blocks))

    fun calculatePreliminary(
        fundingMode: FundingMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        annualSalary: BigDecimal,
        weeklyBasis: WeeklyBasis,
        weekendProfile: WeekendProfile,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): PreliminaryCalculation {
        val blocks = projectRange(dates, plans)
        return calculatePreliminaryFromProjectedBlocks(
            fundingMode = fundingMode,
            dates = dates,
            roster = roster,
            blocks = blocks,
            annualSalary = annualSalary,
            weeklyBasis = weeklyBasis,
            weekendProfile = weekendProfile,
            tripStart = tripStart,
            tripEnd = tripEnd,
            rateSet = rateSet,
        )
    }

    /**
     * Calculation core for already projected [WorkBlock]s.
     *
     * This is intentionally the same whole-trip calculation semantics as
     * [calculatePreliminary]. It exists so tariff-effective-date slices can be
     * prepared without converting clipped blocks back to [PlannedBlock]. It is
     * not, by itself, permission to sum independent slice calculations: stay
     * allowance, short-notice travel caps, and active-event rounding still have
     * whole-trip/per-watch scope and must be coordinated by the segmented
     * orchestration layer.
     */
    fun calculatePreliminaryFromProjectedBlocks(
        fundingMode: FundingMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        blocks: List<WorkBlock>,
        annualSalary: BigDecimal,
        weeklyBasis: WeeklyBasis,
        weekendProfile: WeekendProfile,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): PreliminaryCalculation {
        val tariffLabel = FerieturTariffs.requireById(rateSet.tariffPackageId).label
        val hourlyRate = TariffMath.hourlyRate(annualSalary, weeklyBasis, rateSet)
        val activeBlocks = normalizedActiveBlocks(blocks)
        val restingBlocks = blocks.filter { it.kind == TimeKind.RESTING_NIGHT_WATCH }
        val activeEventBlocks = blocks.filter { it.kind == TimeKind.ACTIVE_EVENT_ON_RESTING }
        val travelWithoutResponsibilityBlocks = blocks.filter { it.kind.isTravelWithoutResponsibility() }
        val uncertainTravelBlocks = blocks.filter { it.kind == TimeKind.TRAVEL_UNCERTAIN }

        val activeMinutes = activeBlocks.sumOf(::durationMinutes)
        val restingMinutes = restingBlocks.sumOf(::durationMinutes)
        val activeInside = activeBlocks.sumOf { overlapWithRoster(it, roster) }.coerceAtMost(activeMinutes)
        val restingInside = restingBlocks.sumOf { overlapWithRoster(it, roster) }.coerceAtMost(restingMinutes)
        val activeOutside = activeMinutes - activeInside
        val restingOutside = restingMinutes - restingInside

        val payableActiveBlocks = when (fundingMode) {
            FundingMode.TURNUS_PLUS_EXTERNAL -> activeBlocks.flatMap { block -> outsideWorkBlocks(block, roster) }
            FundingMode.VACATION_SEPARATE, FundingMode.MUNICIPAL_ALL, FundingMode.CUSTOM -> activeBlocks
        }
        val payableActiveWorkMinutes = payableActiveBlocks
            .filter { it.kind == TimeKind.ACTIVE_WORK || it.kind == TimeKind.ACTIVE_NIGHT_WATCH }
            .sumOf(::durationMinutes)
        val payableTravelWithResponsibilityMinutes = payableActiveBlocks
            .filter { it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY }
            .sumOf(::durationMinutes)
        val outsideActiveEvidence = payableActiveBlocks.map { block ->
            evidence(
                block,
                if (block.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY) "Reise med ansvar utenfor grunnturnusen" else "Arbeid utenfor grunnturnusen",
            )
        }
        val payableRestingBlocks = when (fundingMode) {
            FundingMode.TURNUS_PLUS_EXTERNAL -> restingBlocks.flatMap { outsideWorkBlocks(it, roster) }
            FundingMode.VACATION_SEPARATE, FundingMode.MUNICIPAL_ALL, FundingMode.CUSTOM -> restingBlocks
        }
        val outsideRestingEvidence = payableRestingBlocks.map { evidence(it, "Hvilende nattevakt utenfor grunnturnusen") }

        val lines = mutableListOf<CalculationLine>()
        val payableActiveMinutes = payableActiveBlocks.sumOf(::durationMinutes)
        val activeMultiplier = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) rateSet.chapter20ActiveMultiplier else BigDecimal.ONE
        if (payableActiveMinutes > 0) {
            val amount = moneyAmount(payForMinutes(hourlyRate, payableActiveMinutes).multiply(activeMultiplier))
            val breakdown = buildList {
                if (payableActiveWorkMinutes > 0) add("${minutesLabel(payableActiveWorkMinutes)} arbeid")
                if (payableTravelWithResponsibilityMinutes > 0) add("${minutesLabel(payableTravelWithResponsibilityMinutes)} reise med ansvar")
            }.joinToString(" + ")
            lines += CalculationLine(
                id = "active",
                title = when {
                    fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL && payableTravelWithResponsibilityMinutes > 0 -> "Arbeid og reise utenfor grunnturnusen"
                    fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL -> "Arbeid utenfor grunnturnusen"
                    payableTravelWithResponsibilityMinutes > 0 -> "Aktivt arbeid og reise med ansvar"
                    else -> "Aktivt arbeid"
                },
                detail = buildString {
                    append(minutesLabel(payableActiveMinutes))
                    if (breakdown.isNotBlank() && payableTravelWithResponsibilityMinutes > 0) append(" ($breakdown)")
                    append(" × ${moneyRate(hourlyRate)}")
                    if (activeMultiplier > BigDecimal.ONE) append(" × ${decimalLabel(activeMultiplier, 2)}")
                },
                amount = amount,
                source = when {
                    fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL && payableTravelWithResponsibilityMinutes > 0 -> "$tariffLabel, punkt 20.2 og 20.3"
                    fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL -> "$tariffLabel, punkt 20.2"
                    else -> "Lønnstabellen + $tariffLabel, punkt 9.6"
                },
                explanation = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
                    buildString {
                        append("I denne beregningsmodellen brukes grunnturnusen som sammenligningsgrunnlag for arbeid som er forutsatt dekket gjennom ordinær lønn. Timer modellen klassifiserer som arbeid i tillegg til grunnturnusen beregnes her med timelønn pluss ${percentLabel(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent. Dok. 25 punkt 20.2 fastsetter at arbeidstid ut over ordinær arbeidstid etter kapittel 8 kompenseres med timelønn pluss ${percentLabel(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent.")
                        if (payableTravelWithResponsibilityMinutes > 0) append(" Reise med ansvar for beboeren er med i disse timene fordi reisetid med aktivt tilsyn regnes som arbeidstid etter punkt 20.3.")
                        append(" Hvilken arbeidsplan og eventuell gjennomsnittsberegning som gjelder for ferieoppholdet må avklares med arbeidsgiver. På de samme minuttene som modellen behandler etter punkt 20.2, legger appen ikke til kveld-/nattillegg eller lørdags-/søndagstillegg fra kapittel 12. Punkt 12.1.1 gjelder ordinær tjeneste og sier uttrykkelig at kvelds-/nattillegget ikke utbetales for overtid; punkt 12.2.2 gjelder ordinær tjeneste og utelukker overtid. For særskilte høytidsdager bruker appen punkt 20.2 som den spesifikke ferieoppholdsregelen: kapittel 13 gjelder etter punkt 13.1 dersom ikke annet er fastsatt i tariffavtalen, mens punkt 20.2 fastsetter timelønn pluss ${percentLabel(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent for arbeidstid ut over ordinær arbeidstid under ferieoppholdet.")
                    }
                } else {
                    "Aktivt arbeid beregnes med timelønnen som følger av lønnstrinnet og den valgte arbeidsuken. Grunnturnusen brukes ikke som sammenligningsgrunnlag i denne beregningsmåten. Arbeidsgiverforhold og betalingsscenario håndteres separat."
                },
                evidence = outsideActiveEvidence,
                certainty = CalculationCertainty.ASSUMPTION,
            )
        }

        val payableRestingMinutes = payableRestingBlocks.sumOf(::durationMinutes)
        if (payableRestingMinutes > 0) {
            val amount = moneyAmount(payForMinutes(hourlyRate, payableRestingMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP))
            lines += CalculationLine(
                id = "resting-night",
                title = "Hvilende nattevakt",
                detail = "${minutesLabel(payableRestingMinutes)} arbeidstid → ${passiveTimeLabel(payableRestingMinutes, rateSet)} lønnsekvivalent",
                amount = amount,
                source = "$tariffLabel, punkt 20.4",
                explanation = "Hele den hvilende nattevakten teller som arbeidstid, men betalingen regnes i forholdet ${passiveRatioLabel(rateSet)}. Aktivt arbeid under vakten beregnes på en egen linje."
                ,
                evidence = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) outsideRestingEvidence else restingBlocks.map { evidence(it, "Hvilende nattevakt") },
            )
        }

        val restingEveningEvidence = payableRestingBlocks.flatMap { block ->
            eligibleEveningNightSegments(block, nightWatch = true, rateSet = rateSet)
        }
        val restingEveningMinutes = restingEveningEvidence.sumOf { it.minutes }
        if (restingEveningMinutes > 0) {
            val rate = TariffMath.eveningNightRate(hourlyRate, rateSet)
            val amount = moneyAmount(payForMinutes(rate, restingEveningMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP))
            lines += CalculationLine(
                id = "resting-evening-night",
                title = "Kveld- og nattillegg under hvilende nattevakt",
                detail = "${minutesLabel(restingEveningMinutes)} hvilende × ${passiveFractionLabel(rateSet)} × ${moneyRate(rate)}",
                amount = amount,
                source = "$tariffLabel, punkt 8.9, 12.1.1 og 20.4",
                explanation = "Kveld- og nattillegget under arbeid av passiv karakter betales også i forholdet ${passiveRatioLabel(rateSet)}. Det betyr at tillegget beregnes for ${passiveShareText(rateSet)} av den registrerte hvilende tiden.",
                evidence = restingEveningEvidence.map { it.copy(note = "${it.note} · tillegget betales ${passiveRatioLabel(rateSet)}") },
                certainty = CalculationCertainty.CONFIRMED,
            )
        }

        val restingWeekendEvidence = payableRestingBlocks.flatMap { block -> weekendSegmentsExcludingHoliday(block, weeklyBasis) }
        val restingWeekendMinutes = restingWeekendEvidence.sumOf { it.minutes }
        if (restingWeekendMinutes > 0) {
            val rate = TariffMath.weekendRate(hourlyRate, weekendProfile, rateSet)
            val amount = moneyAmount(payForMinutes(rate, restingWeekendMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP))
            lines += CalculationLine(
                id = "resting-weekend",
                title = "Lørdags- og søndagstillegg under hvilende nattevakt",
                detail = "${minutesLabel(restingWeekendMinutes)} hvilende × ${passiveFractionLabel(rateSet)} × ${moneyRate(rate)}",
                amount = amount,
                source = "$tariffLabel, punkt 8.9, 12.2.2 og 20.4",
                explanation = "Lørdags- og søndagstillegg under arbeid av passiv karakter betales i forholdet ${passiveRatioLabel(rateSet)}. Timer som samtidig ligger i en helge- eller høytidsperiode med høyere tillegg tas ikke med her.",
                evidence = restingWeekendEvidence.map { it.copy(note = "${it.note} · tillegget betales ${passiveRatioLabel(rateSet)}") },
                certainty = CalculationCertainty.CONFIRMED,
            )
        }

        val restingHolidayEvidence = payableRestingBlocks.flatMap { block -> holidaySupplementSegments(block, weeklyBasis) }
        val restingHolidayMinutes = restingHolidayEvidence.sumOf { it.minutes }
        if (restingHolidayMinutes > 0) {
            val rate = TariffMath.holidaySupplementRate(hourlyRate, rateSet)
            val amount = moneyAmount(payForMinutes(rate, restingHolidayMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP))
            lines += CalculationLine(
                id = "resting-holiday",
                title = "Helge- og høytidsdagstillegg under hvilende nattevakt",
                detail = "${minutesLabel(restingHolidayMinutes)} hvilende × ${passiveFractionLabel(rateSet)} × ${moneyRate(rate)}",
                amount = amount,
                source = "$tariffLabel, punkt 8.9, 12.2.3 og 20.4",
                explanation = "I helge- og høytidsperiodene i punkt 12.2.3 er tillegget ${mixedFractionLabel(rateSet.holidaySupplementNumerator, rateSet.holidaySupplementDenominator)} timelønn per time i tillegg til ordinær lønn. Under hvilende nattevakt betales også dette tillegget i forholdet ${passiveRatioLabel(rateSet)}.",
                evidence = restingHolidayEvidence.map { it.copy(note = "${it.note} · høytidstillegget betales ${passiveRatioLabel(rateSet)}") },
                certainty = CalculationCertainty.CONFIRMED,
            )
        }

        val eveningEvidence = eveningNightEvidence(fundingMode, activeBlocks, roster, tripStart, tripEnd, rateSet)
        val eveningMinutes = eveningEvidence.sumOf { it.minutes }
        if (eveningMinutes > 0) {
            val rate = TariffMath.eveningNightRate(hourlyRate, rateSet)
            lines += CalculationLine(
                id = "evening-night",
                title = "Kveld- og nattillegg",
                detail = "${minutesLabel(eveningMinutes)} × ${moneyRate(rate)} (${percentLabel(rateSet.eveningNightFraction)} %)",
                amount = moneyAmount(payForMinutes(rate, eveningMinutes)),
                source = "$tariffLabel, punkt 12.1.1",
                explanation = buildString {
                    append("I relevant turnus får du ${percentLabel(rateSet.eveningNightFraction)} prosent tillegg for ordinært arbeid mellom kl. ${clockLabel(rateSet.eveningStart)} og ${clockLabel(rateSet.nightEnd)}. Er perioden en nattevakt, fortsetter tillegget til vakten slutter, men ikke lenger enn til kl. ${clockLabel(rateSet.nightWatchSupplementEnd)}. Tillegget gis ikke for overtid.")
                    if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
                        append(" I denne beregningsmåten kommer posten fra grunnturnusen og vises bare for kontroll. Den er ikke med i betalingsgrunnlaget for turen.")
                    } else {
                        append(" Posten er med i det beregnede grunnlaget for turen.")
                    }
                },
                evidence = eveningEvidence,
                includedInKnownTotal = true,
                certainty = CalculationCertainty.CONFIRMED,
                paymentTreatment = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER else PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
            )
        }

        val weekendEvidence = weekendEvidence(fundingMode, activeBlocks, roster, weeklyBasis, tripStart, tripEnd)
        val weekendMinutes = weekendEvidence.sumOf { it.minutes }
        if (weekendMinutes > 0) {
            val rate = TariffMath.weekendRate(hourlyRate, weekendProfile, rateSet)
            lines += CalculationLine(
                id = "weekend",
                title = "Lørdags- og søndagstillegg",
                detail = "${minutesLabel(weekendMinutes)} × ${moneyRate(rate)} · ${rateSet.weekendRate(weekendProfile).label}",
                amount = moneyAmount(payForMinutes(rate, weekendMinutes)),
                source = "$tariffLabel, punkt 12.2.2",
                explanation = buildString {
                    append("Ordinært arbeid fra lørdag kl. 00:00 til søndag kl. 24:00 kan gi lørdags- og søndagstillegg. Timer som samtidig får helge- og høytidsdagstillegg etter punkt 12.2.3 tas ikke med her, fordi høytidstillegget behandles som den høyere særregelen for disse timene. Appen bruker helgesatsen du har kontrollert mot lønnsslippen.")
                    if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
                        append(" I denne beregningsmåten kommer posten fra grunnturnusen og vises bare for kontroll. Den er ikke med i betalingsgrunnlaget for turen.")
                    } else {
                        append(" Posten er med i det beregnede grunnlaget for turen.")
                    }
                },
                evidence = weekendEvidence,
                includedInKnownTotal = true,
                certainty = CalculationCertainty.CONFIRMED,
                paymentTreatment = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER else PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
            )
        }

        val holidayEvidence = holidayEvidence(fundingMode, activeBlocks, roster, weeklyBasis, tripStart, tripEnd)
        val holidayMinutes = holidayEvidence.sumOf { it.minutes }
        if (holidayMinutes > 0) {
            val rate = TariffMath.holidaySupplementRate(hourlyRate, rateSet)
            lines += CalculationLine(
                id = "holiday",
                title = "Helge- og høytidsdagstillegg",
                detail = "${minutesLabel(holidayMinutes)} × ${moneyRate(rate)}",
                amount = moneyAmount(payForMinutes(rate, holidayMinutes)),
                source = "$tariffLabel, punkt 12.2.3",
                explanation = buildString {
                    append("Ved ordinær tjeneste i helge- og høytidsperiodene i punkt 12.2.3 får du et tillegg på ${mixedFractionLabel(rateSet.holidaySupplementNumerator, rateSet.holidaySupplementDenominator)} av timelønnen per arbeidet time. Satsen som vises i regnestykket er allerede dette tillegget, altså timelønn × ${mixedFractionLabel(rateSet.holidaySupplementNumerator, rateSet.holidaySupplementDenominator)}. Den skal ikke ganges med samme faktor én gang til. Periodene er forskjellige for 33,6 timers uke og for 35,5/37,5 timer og tredelt turnus. Appen beregner datoene automatisk.")
                    if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
                        append(" I denne beregningsmåten kommer posten fra grunnturnusen og vises bare for kontroll. Den er ikke med i betalingsgrunnlaget for turen.")
                    } else {
                        append(" Posten er med i det beregnede grunnlaget for turen.")
                    }
                },
                evidence = holidayEvidence,
                includedInKnownTotal = true,
                certainty = CalculationCertainty.CONFIRMED,
                paymentTreatment = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER else PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
            )
        }

        // A4.1 rule priority: point 20.2 is the specific compensation rule for worktime
        // beyond ordinary hours during a chapter-20 holiday stay. Chapter-13 overtime
        // rules apply only where the tariff has not otherwise provided, cf. point 13.1.

        val allowanceDays = stayAllowanceDays(tripStart, tripEnd, rateSet)
        val stayRemainderMinutes = stayAllowanceRemainderMinutes(tripStart, tripEnd)
        val exactStayThresholdOpen = stayRemainderMinutes == rateSet.stayAllowanceRemainderThresholdMinutes
        if (allowanceDays > 0) {
            val allowance = moneyAmount(BigDecimal(allowanceDays).multiply(rateSet.stayAllowancePerDay))
            lines += CalculationLine(
                id = "stay-allowance",
                title = "Døgngodtgjøring ved ferieopphold",
                detail = "$allowanceDays døgn × ${decimalLabel(rateSet.stayAllowancePerDay)} kr",
                amount = allowance,
                source = "$tariffLabel, punkt 20.6",
                explanation = "Ved ferieopphold som omfattes av kapittel 20 får arbeidstakeren ${decimalLabel(rateSet.stayAllowancePerDay)} kroner per døgn i tillegg til lønnen. Et påbegynt døgn teller som et helt døgn når resttiden er mer enn ${durationWords(rateSet.stayAllowanceRemainderThresholdMinutes)}. Resttid under denne grensen gir ikke denne godtgjøringen. Nøyaktig ${durationWords(rateSet.stayAllowanceRemainderThresholdMinutes)} er ikke uttrykkelig regulert av ordlyden og behandles derfor som et åpent punkt.",
                evidence = listOf(
                    CalculationEvidence(tripStart, tripEnd, Duration.between(tripStart, tripEnd).toMinutes(), "Reisens samlede varighet"),
                ),
            )
        }
        if (exactStayThresholdOpen) {
            lines += CalculationLine(
                id = "stay-allowance-exact-threshold-open",
                title = "Døgngodtgjøring ved nøyaktig ${durationWords(rateSet.stayAllowanceRemainderThresholdMinutes)} må avklares",
                detail = "Resttid ${minutesLabel(stayRemainderMinutes)} · mulig 1 døgn × ${decimalLabel(rateSet.stayAllowancePerDay)} kr",
                amount = moneyAmount(rateSet.stayAllowancePerDay),
                source = "$tariffLabel, punkt 20.6",
                explanation = "Punkt 20.6 sier at resttid over ${durationWords(rateSet.stayAllowanceRemainderThresholdMinutes)} godtgjøres som fullt døgn, mens mindre enn ${durationWords(rateSet.stayAllowanceRemainderThresholdMinutes)} ikke godtgjøres. Ordlyden angir ikke uttrykkelig nøyaktig grenseverdi. Ferietur legger derfor ikke det mulige ekstradøgnet inn i kjent betalingsgrunnlag før dette er avklart.",
                evidence = listOf(
                    CalculationEvidence(tripEnd.minusMinutes(stayRemainderMinutes), tripEnd, stayRemainderMinutes, "Resttid etter hele døgn · nøyaktig tariffgrense"),
                ),
                certainty = CalculationCertainty.OPEN,
                includedInKnownTotal = false,
                paymentTreatment = PaymentTreatment.OPEN,
            )
        }

        if (activeEventBlocks.isNotEmpty()) {
            val grouped = activeEventsPerRestingWatch(restingBlocks, activeEventBlocks, rateSet)
            val actualMinutes = grouped.sumOf { it.actualMinutes }
            val paidMinutes = grouped.sumOf { it.roundedMinutes }
            if (paidMinutes > 0) {
                val activeRate = hourlyRate.multiply(rateSet.chapter20ActiveMultiplier).setScale(2, RoundingMode.HALF_UP)
                lines += CalculationLine(
                    id = "active-on-resting",
                    title = "Aktivt arbeid under hvilende nattevakt",
                    detail = "${minutesLabel(actualMinutes)} registrert → ${minutesLabel(paidMinutes)} betalt × ${moneyRate(activeRate)}",
                    amount = moneyAmount(payForMinutes(activeRate, paidMinutes)),
                    source = "$tariffLabel, punkt 20.4",
                    explanation = "Den aktive tiden summeres for hver hvilende nattevakt og avrundes deretter til nærmeste ${roundingUnitDefinite(rateSet.activeNightRoundingStepMinutes.toLong())}. ${rateSet.activeNightRoundUpRemainderAtMinutes - 1} minutter eller mindre strykes. ${rateSet.activeNightRoundUpRemainderAtMinutes} minutter eller mer rundes opp til neste ${roundingUnitDefinite(rateSet.activeNightRoundingStepMinutes.toLong())}. Den avrundede tiden betales med timelønn + ${percentLabel(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent.",
                    evidence = grouped.flatMap { it.evidence },
                    certainty = CalculationCertainty.CONFIRMED,
                    includedInKnownTotal = true,
                )
            } else {
                lines += CalculationLine(
                    id = "active-on-resting",
                    title = "Aktivt arbeid under hvilende nattevakt",
                    detail = "${minutesLabel(actualMinutes)} registrert → 0 min betalt etter avrunding",
                    amount = moneyAmount(BigDecimal.ZERO),
                    source = "$tariffLabel, punkt 20.4",
                    explanation = "Den aktive tiden summeres for hver hvilende nattevakt. Når samlet aktiv tid på en vakt er ${rateSet.activeNightRoundUpRemainderAtMinutes - 1} minutter eller mindre, strykes tiden etter avrundingsregelen i punkt 20.4.",
                    evidence = grouped.flatMap { it.evidence },
                    certainty = CalculationCertainty.CONFIRMED,
                    includedInKnownTotal = true,
                )
            }
        }

        var unresolvedSleepNightPayableMinutes = 0L
        var unresolvedTravelNoticePayableMinutes = 0L
        var shortNoticeSpecial133PotentialAmount = BigDecimal.ZERO
        if (travelWithoutResponsibilityBlocks.isNotEmpty()) {
            val payableOrdinaryTravelBlocks = mutableListOf<WorkBlock>()
            val payablePassiveNightBlocks = mutableListOf<WorkBlock>()
            val payableUnresolvedNightBlocks = mutableListOf<WorkBlock>()
            val shortNoticeOrdinaryTravelBlocks = mutableListOf<WorkBlock>()
            val unresolvedNoticeOrdinaryTravelBlocks = mutableListOf<WorkBlock>()

            travelWithoutResponsibilityBlocks.forEach { block ->
                val nightBlocks = travelNightBlocks(block, rateSet)
                val nonNightBlocks = travelNonNightBlocks(block, rateSet)
                val ordinaryParts = when (block.kind) {
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> nonNightBlocks
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP -> listOf(block)
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY -> nonNightBlocks
                    else -> emptyList()
                }
                val passiveParts = if (block.kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED) nightBlocks else emptyList()
                val unresolvedNightParts = if (block.kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY) nightBlocks else emptyList()

                val payableOrdinaryForJourney = payableTravelBlocks(ordinaryParts, fundingMode, roster)
                payableOrdinaryTravelBlocks += payableOrdinaryForJourney
                payablePassiveNightBlocks += payableTravelBlocks(passiveParts, fundingMode, roster)
                payableUnresolvedNightBlocks += payableTravelBlocks(unresolvedNightParts, fundingMode, roster)

                when (block.travelNoticeStatus) {
                    TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY -> Unit
                    TravelNoticeStatus.NOT_CLARIFIED -> {
                        unresolvedTravelNoticePayableMinutes += payableOrdinaryForJourney.sumOf(::durationMinutes)
                        unresolvedNoticeOrdinaryTravelBlocks += payableOrdinaryForJourney
                    }
                    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY -> {
                        shortNoticeOrdinaryTravelBlocks += payableOrdinaryForJourney
                    }
                }
            }

            val payableOrdinaryMinutes = payableOrdinaryTravelBlocks.sumOf(::durationMinutes)
            if (payableOrdinaryMinutes > 0) {
                val amount = moneyAmount(payForMinutes(hourlyRate, payableOrdinaryMinutes))
                lines += CalculationLine(
                    id = "travel-without-responsibility",
                    title = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) "Reise uten tilsynsansvar utenfor grunnturnusen" else "Reise uten tilsynsansvar",
                    detail = "${minutesLabel(payableOrdinaryMinutes)} × ${moneyRate(hourlyRate)}",
                    amount = amount,
                    source = "$tariffLabel, punkt 18.4 og 20.3",
                    explanation = buildString {
                        append("Punkt 20.3 viser til reisetidsreglene i punkt 18.4. Reisetid uten tilsynsansvar utenom ordinær arbeidstid godtgjøres med ordinær timelønn.")
                        if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
                            append(" I denne beregningsmodellen brukes grunnturnusen som sammenligningsgrunnlag, så bare den delen av den ordinære reisetiden som ligger utenfor grunnturnusen er med i betalingsgrunnlaget.")
                        } else {
                            append(" I denne separate turmodellen beregnes den registrerte ordinære reisetiden med ordinær timelønn.")
                        }
                        append(" Varseltidspunktet registreres separat. Ved kort varsel beholder disse timene ordinær reisetidsbetaling, og appen legger i tillegg til overtidsprosenten for inntil ${durationWords(rateSet.shortNoticeMaxMinutes)} etter punkt 18.4 og kapittel 13. Grensen brukes én gang for den registrerte turen, slik at oppdeling i flere reiseperioder ikke ganger den opp.")
                        if (travelWithoutResponsibilityBlocks.any { it.kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP && travelNightBlocks(it, rateSet).isNotEmpty() }) {
                            append(" For registrert nattreise der du har oppgitt at du ikke hadde tillatelse til å sove, brukes ordinær reisetidsbehandling også for nattdelen.")
                        }
                    },
                    evidence = payableOrdinaryTravelBlocks.map { evidence(it, "Reise uten tilsynsansvar · ordinær reisetid") },
                    certainty = CalculationCertainty.CONFIRMED,
                    includedInKnownTotal = true,
                    paymentTreatment = PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
                )
            }

            // Point 18.4 caps the short-notice overtime treatment at up to two hours of
            // travel time. FERIETUR01 applies that cap once across the current trip, not once
            // per UI block, so splitting one journey into several travel blocks cannot multiply
            // the two-hour entitlement. Point 13.3 rounding is applied afterwards per rate band.
            val shortNoticeCappedBlocks = takeFirstMinutes(shortNoticeOrdinaryTravelBlocks, rateSet.shortNoticeMaxMinutes)
            val shortNoticeOvertimeBands = overtimeSupplementBands(shortNoticeCappedBlocks, roster, rateSet)

            if (unresolvedTravelNoticePayableMinutes > 0) {
                // Show the monetary consequence of the unresolved fact without adding it to the
                // known payment basis. The possible amount is calculated as the positive delta
                // between today's confirmed short-notice supplement and a hypothetical state
                // where all currently unresolved ordinary-travel blocks are also short notice.
                // This keeps the shared two-hour cap intact across the whole trip.
                val currentShortNoticeSupplement = shortNoticeOvertimeBands.fold(BigDecimal.ZERO) { acc, band ->
                    acc.add(payForMinutes(hourlyRate.multiply(band.supplementFraction), band.paidMinutes))
                }
                val hypotheticalShortNoticeBands = overtimeSupplementBands(
                    takeFirstMinutes(shortNoticeOrdinaryTravelBlocks + unresolvedNoticeOrdinaryTravelBlocks, rateSet.shortNoticeMaxMinutes),
                    roster,
                    rateSet,
                )
                val hypotheticalShortNoticeSupplement = hypotheticalShortNoticeBands.fold(BigDecimal.ZERO) { acc, band ->
                    acc.add(payForMinutes(hourlyRate.multiply(band.supplementFraction), band.paidMinutes))
                }
                val possibleShortNoticeDelta = hypotheticalShortNoticeSupplement
                    .subtract(currentShortNoticeSupplement)
                    .max(BigDecimal.ZERO)
                val possibleAmount = moneyAmount(possibleShortNoticeDelta)
                val possibleText = if (possibleAmount > BigDecimal.ZERO) {
                    " · mulig tillegg ${moneyRate(possibleAmount)}"
                } else {
                    " · mulig beløpsendring må avklares"
                }
                lines += CalculationLine(
                    id = "travel-notice-open",
                    title = "Når du fikk vite om reisen må avklares",
                    detail = "${minutesLabel(unresolvedTravelNoticePayableMinutes)} reisetid utenfor ordinær arbeidstid · ordinær reisetidsbetaling er med$possibleText",
                    amount = possibleAmount,
                    source = "$tariffLabel, punkt 18.4",
                    explanation = "Ordinær timelønn for reisetiden er allerede med i betalingsgrunnlaget. Punkt 18.4 sier at dersom arbeidstakeren ikke fikk vite om reisen senest dagen i forveien, skal inntil ${durationWords(rateSet.shortNoticeMaxMinutes)} av reisetiden som kreves utført utenfor ordinær arbeidstid betales som overtid. Beløpet på denne åpne posten er derfor et mulig tillegg og er ikke inkludert i betalingsgrunnlaget. Oppgi om reisen var kjent senest dagen i forveien for å avklare posten.",
                    evidence = unresolvedNoticeOrdinaryTravelBlocks.map { evidence(it, "Varseltidspunkt for reisen er ikke avklart") },
                    certainty = CalculationCertainty.OPEN,
                    includedInKnownTotal = false,
                    paymentTreatment = PaymentTreatment.OPEN,
                )
            }

            if (shortNoticeOvertimeBands.isNotEmpty()) {
                val supplementAmount = shortNoticeOvertimeBands.fold(BigDecimal.ZERO) { acc, band ->
                    val supplementRate = hourlyRate.multiply(band.supplementFraction)
                    acc.add(payForMinutes(supplementRate, band.paidMinutes))
                }
                val actualMinutes = shortNoticeOvertimeBands.sumOf { it.actualMinutes }
                val paidMinutes = shortNoticeOvertimeBands.sumOf { it.paidMinutes }
                val rateLabels = shortNoticeOvertimeBands.map { it.label }.distinct().joinToString(" / ")
                lines += CalculationLine(
                    id = "travel-short-notice-overtime",
                    title = "Overtidstillegg ved kort varsel om reisen",
                    detail = "${minutesLabel(actualMinutes)} reisetid · ${minutesLabel(paidMinutes)} tilleggsgrunnlag · $rateLabels",
                    amount = moneyAmount(supplementAmount),
                    source = "$tariffLabel, punkt 18.4, 13.2, 13.3 og 13.7.1",
                    explanation = "Du har oppgitt at reisen ikke var kjent senest dagen i forveien. Punkt 18.4 gir da overtidsbetaling for inntil ${durationWords(rateSet.shortNoticeMaxMinutes)} av reisetiden som kreves utført utenfor ordinær arbeidstid. Den ordinære timelønnen for reisen står på reisetidslinjen; denne posten er overtidsdelen i tillegg. Beregningsmodellen bruker de første inntil ${durationWords(rateSet.shortNoticeMaxMinutes)} med kortvarslet ordinær reisetid i turen. Appen bruker ${percentLabel(rateSet.overtimeStandardFraction)} prosent mellom kl. ${clockLabel(rateSet.overtimeHighEnd)} og ${clockLabel(rateSet.overtimeHighStart)} og ${percentLabel(rateSet.overtimeHighFraction)} prosent mellom kl. ${clockLabel(rateSet.overtimeHighStart)} og ${clockLabel(rateSet.overtimeHighEnd)}, på søndager, offentlige helgedager og registrert ukentlig fridag. Overtidstillegget avrundes opp til påbegynt ${roundingUnitIndefinite(rateSet.overtimeRoundingStepMinutes)} etter punkt 13.3.",
                    evidence = shortNoticeOvertimeBands.map { band ->
                        CalculationEvidence(
                            start = band.start,
                            end = band.end,
                            minutes = band.actualMinutes,
                            note = "Kort varsel · ${band.label} · ${minutesLabel(band.actualMinutes)} faktisk → ${minutesLabel(band.paidMinutes)} tilleggsgrunnlag",
                        )
                    },
                    certainty = CalculationCertainty.CONFIRMED,
                    includedInKnownTotal = true,
                    paymentTreatment = PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
                )

                val specialBands = shortNoticeOvertimeBands.filter { it.special133Potential }
                if (specialBands.isNotEmpty()) {
                    val shortNoticeSpecial133PotentialMinutes = specialBands.sumOf { it.paidMinutes }
                    shortNoticeSpecial133PotentialAmount = specialBands.fold(BigDecimal.ZERO) { acc, band ->
                        val potentialFraction = rateSet.specialOvertimeFraction133.subtract(band.supplementFraction)
                        if (potentialFraction.signum() <= 0) acc else acc.add(payForMinutes(hourlyRate.multiply(potentialFraction), band.paidMinutes))
                    }
                    lines += CalculationLine(
                        id = "travel-short-notice-133-open",
                        title = "Særskilt overtidsprosent på helge-/høytidsdag må avklares",
                        detail = "${minutesLabel(shortNoticeSpecial133PotentialMinutes)} tilleggsgrunnlag · mulig ekstra ${moneyRate(shortNoticeSpecial133PotentialAmount)}",
                        amount = moneyAmount(shortNoticeSpecial133PotentialAmount),
                        source = "$tariffLabel, punkt 13.7.3 og 18.4",
                        explanation = "Punkt 13.7.3 gir ${specialOvertimePercentLabel(rateSet)} prosent overtidstillegg til arbeidstakere som har ordinær tjeneste på søn- og helgedager på de særskilt opplistede dagene. Appen kan ikke fastslå denne personlige tariffstatusen bare fra turen. Den bekreftede overtidsbetalingen etter punkt 13.2 er allerede med; beløpet her viser bare mulig differanse dersom punkt 13.7.3 gjelder for arbeidstakeren.",
                        evidence = specialBands.map { band ->
                            CalculationEvidence(band.start, band.end, band.actualMinutes, "Kort varsel på særskilt dag · 13.7.3 må avklares")
                        },
                        certainty = CalculationCertainty.OPEN,
                        includedInKnownTotal = false,
                        paymentTreatment = PaymentTreatment.OPEN,
                    )
                }
            }

            val passiveNightMinutes = payablePassiveNightBlocks.sumOf(::durationMinutes)
            if (passiveNightMinutes > 0) {
                lines += CalculationLine(
                    id = "travel-passive-night",
                    title = "Nattreise med søvntillatelse · arbeid av passiv karakter",
                    detail = "${minutesLabel(passiveNightMinutes)} arbeidstid → ${passiveTimeLabel(passiveNightMinutes, rateSet)} lønnsekvivalent",
                    amount = moneyAmount(payForMinutes(hourlyRate, passiveNightMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP)),
                    source = "$tariffLabel, punkt 20.3 og 8.9",
                    explanation = "Du har oppgitt at du hadde tillatelse til å sove under nattreisen. Reisetid mellom kl. ${clockLabel(rateSet.travelSleepWindowStart)} og ${clockLabel(rateSet.travelSleepWindowEnd)} beregnes da som arbeid av passiv karakter. Tiden regnes som arbeidstid time for time, mens grunnbetalingen er ${passiveFractionLabel(rateSet).replace("⅓", "1/3")} timelønn per time.",
                    evidence = payablePassiveNightBlocks.map { evidence(it, "Nattreise · søvn tillatt · passiv karakter") },
                    certainty = CalculationCertainty.CONFIRMED,
                    includedInKnownTotal = true,
                    paymentTreatment = PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
                )

                val passiveEveningEvidence = payablePassiveNightBlocks.flatMap { eligibleEveningNightSegments(it, nightWatch = false, rateSet = rateSet) }
                val passiveEveningMinutes = passiveEveningEvidence.sumOf { it.minutes }
                if (passiveEveningMinutes > 0) {
                    val rate = TariffMath.eveningNightRate(hourlyRate, rateSet)
                    lines += CalculationLine(
                        id = "travel-passive-evening-night",
                        title = "Kveld- og nattillegg under passiv nattreise",
                        detail = "${minutesLabel(passiveEveningMinutes)} passiv reise × ${passiveFractionLabel(rateSet)} × ${moneyRate(rate)}",
                        amount = moneyAmount(payForMinutes(rate, passiveEveningMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP)),
                        source = "$tariffLabel, punkt 8.9, 12.1.1 og 20.3",
                        explanation = "Arbeid av passiv karakter får kveld- og nattillegg i forholdet ${passiveRatioLabel(rateSet)}. For reiseperioden brukes de ordinære tidsgrensene for kveld/natt; dette er ikke en nattevakt med forlengelse av tillegget til kl. ${clockLabel(rateSet.nightWatchSupplementEnd)}.",
                        evidence = passiveEveningEvidence.map { it.copy(note = "${it.note} · passiv reise · tillegget betales ${passiveRatioLabel(rateSet)}") },
                        certainty = CalculationCertainty.CONFIRMED,
                    )
                }

                val passiveWeekendEvidence = payablePassiveNightBlocks.flatMap { weekendSegmentsExcludingHoliday(it, weeklyBasis) }
                val passiveWeekendMinutes = passiveWeekendEvidence.sumOf { it.minutes }
                if (passiveWeekendMinutes > 0) {
                    val rate = TariffMath.weekendRate(hourlyRate, weekendProfile, rateSet)
                    lines += CalculationLine(
                        id = "travel-passive-weekend",
                        title = "Lørdags- og søndagstillegg under passiv nattreise",
                        detail = "${minutesLabel(passiveWeekendMinutes)} passiv reise × ${passiveFractionLabel(rateSet)} × ${moneyRate(rate)}",
                        amount = moneyAmount(payForMinutes(rate, passiveWeekendMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP)),
                        source = "$tariffLabel, punkt 8.9, 12.2.2 og 20.3",
                        explanation = "Lørdags- og søndagstillegg under arbeid av passiv karakter betales i forholdet ${passiveRatioLabel(rateSet)}. Timer som samtidig ligger i en helge- eller høytidsperiode med høyere tillegg tas ikke med her.",
                        evidence = passiveWeekendEvidence.map { it.copy(note = "${it.note} · passiv reise · tillegget betales ${passiveRatioLabel(rateSet)}") },
                        certainty = CalculationCertainty.CONFIRMED,
                    )
                }

                val passiveHolidayEvidence = payablePassiveNightBlocks.flatMap { holidaySupplementSegments(it, weeklyBasis) }
                val passiveHolidayMinutes = passiveHolidayEvidence.sumOf { it.minutes }
                if (passiveHolidayMinutes > 0) {
                    val rate = TariffMath.holidaySupplementRate(hourlyRate, rateSet)
                    lines += CalculationLine(
                        id = "travel-passive-holiday",
                        title = "Helge- og høytidstillegg under passiv nattreise",
                        detail = "${minutesLabel(passiveHolidayMinutes)} passiv reise × ${passiveFractionLabel(rateSet)} × ${moneyRate(rate)}",
                        amount = moneyAmount(payForMinutes(rate, passiveHolidayMinutes).divide(BigDecimal(rateSet.passiveWorkDivisor), 8, RoundingMode.HALF_UP)),
                        source = "$tariffLabel, punkt 8.9, 12.2.3 og 20.3",
                        explanation = "Helge- og høytidstillegg under arbeid av passiv karakter betales i forholdet ${passiveRatioLabel(rateSet)}.",
                        evidence = passiveHolidayEvidence.map { it.copy(note = "${it.note} · passiv reise · høytidstillegget betales ${passiveRatioLabel(rateSet)}") },
                        certainty = CalculationCertainty.CONFIRMED,
                    )
                }
            }

            unresolvedSleepNightPayableMinutes = payableUnresolvedNightBlocks.sumOf(::durationMinutes)
            if (unresolvedSleepNightPayableMinutes > 0) {
                lines += CalculationLine(
                    id = "travel-night-sleep-open",
                    title = "Søvntillatelse under nattreisen må avklares",
                    detail = "${minutesLabel(unresolvedSleepNightPayableMinutes)} nattreise · ikke beregnet",
                    amount = moneyAmount(BigDecimal.ZERO),
                    source = "$tariffLabel, punkt 20.3",
                    explanation = "Reisetid mellom kl. ${clockLabel(rateSet.travelSleepWindowStart)} og ${clockLabel(rateSet.travelSleepWindowEnd)} skal beregnes som arbeid av passiv karakter når arbeidstakeren har tillatelse til å sove. Oppgi derfor om du hadde slik tillatelse. Inntil dette er avklart legges nattdelen ikke til betalingsgrunnlaget.",
                    evidence = payableUnresolvedNightBlocks.map { evidence(it, "Nattreise · søvntillatelse ikke avklart") },
                    certainty = CalculationCertainty.OPEN,
                    includedInKnownTotal = false,
                    paymentTreatment = PaymentTreatment.OPEN,
                )
            }
        }

        if (uncertainTravelBlocks.isNotEmpty()) {
            val minutes = uncertainTravelBlocks.sumOf(::durationMinutes)
            lines += CalculationLine(
                id = "travel-responsibility-open",
                title = "Ansvar under reisen må avklares",
                detail = "${minutesLabel(minutes)} registrert · ikke beregnet",
                amount = moneyAmount(BigDecimal.ZERO),
                source = "$tariffLabel, punkt 20.3",
                explanation = "Du har registrert en reise der det er uklart hvem som hadde tilsynsansvaret. Det må avklares før appen kan behandle tiden som reise med eller uten tilsynsansvar.",
                evidence = uncertainTravelBlocks.map { evidence(it, "Ansvar under reisen er ikke avklart") },
                certainty = CalculationCertainty.OPEN,
                includedInKnownTotal = false,
                paymentTreatment = PaymentTreatment.OPEN,
            )
        }

        // A4.1 rule priority: chapter-12 supplements are ordinary-service supplements.
        // They are not stacked on the same minutes that this model compensates under point 20.2.

        val applicableUnresolvedRuleIds = buildSet {
            if (unresolvedSleepNightPayableMinutes > 0) add("D25_20_3_SLEEP_PERMISSION")
            if (unresolvedTravelNoticePayableMinutes > 0) add("D25_18_4_NOTICE")
            if (shortNoticeSpecial133PotentialAmount.signum() > 0) add("D25_18_4_X13_7_3")
            if (exactStayThresholdOpen) add("D25_20_6_EXACT_THRESHOLD")
        }

        val rosterUncoveredEvidence = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
            rosterUncoveredEvidence(blocks, roster, tripStart, tripEnd)
        } else {
            emptyList()
        }
        val rosterUncoveredMinutes = rosterUncoveredEvidence.sumOf { it.minutes }
        val dayAudits = buildDayAudits(dates, weeklyBasis, lines)
        val knownAmount = moneyAmount(lines.filter { it.includedInKnownTotal }.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) })
        val paymentBasisAmount = moneyAmount(lines.filter { it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS }.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) })
        val alreadyCoveredByNormalRosterAmount = moneyAmount(lines.filter { it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER }.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) })
        val excludedKnownRuleAmount = moneyAmount(lines.filterNot { it.includedInKnownTotal }.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) })
        val rosterMinutes = roster.flatMap { (date, value) ->
            RosterEntryCodec.decode(value).mapNotNull { shift ->
                val interval = TurnusOverlapEngine.intervalFor(date, shift) ?: return@mapNotNull null
                intersection(interval.first, interval.second, tripStart, tripEnd)
            }
        }.sumOf { (start, end) -> ChronoUnit.MINUTES.between(start, end) }

        return PreliminaryCalculation(
            hourlyRate = hourlyRate,
            rosterMinutes = rosterMinutes,
            rosterUncoveredMinutes = rosterUncoveredMinutes,
            rosterUncoveredEvidence = rosterUncoveredEvidence,
            activeMinutes = activeMinutes,
            activeInsideRosterMinutes = activeInside,
            activeOutsideRosterMinutes = activeOutside,
            payableActiveWorkMinutes = payableActiveWorkMinutes,
            payableTravelWithResponsibilityMinutes = payableTravelWithResponsibilityMinutes,
            restingNightMinutes = restingMinutes,
            restingNightOutsideRosterMinutes = restingOutside,
            eveningNightMinutes = eveningMinutes,
            weekendMinutes = weekendMinutes,
            holidayMinutes = holidayMinutes,
            stayAllowanceDays = allowanceDays,
            lines = lines,
            dayAudits = dayAudits,
            knownAmount = knownAmount,
            paymentBasisAmount = paymentBasisAmount,
            alreadyCoveredByNormalRosterAmount = alreadyCoveredByNormalRosterAmount,
            excludedKnownRuleAmount = excludedKnownRuleAmount,
            applicableUnresolvedRuleIds = applicableUnresolvedRuleIds,
        )
    }

    /**
     * Non-monetary worktime/roster audit shared by live single- and
     * multi-context presentation. It deliberately uses the same projection and
     * roster-overlap primitives as the calculation core, but does not inspect
     * salary or tariff rates.
     */
    fun buildWorktimeAudit(
        fundingMode: FundingMode,
        blocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): CalculationWorktimeAudit {
        require(tripEnd.isAfter(tripStart))
        val activeBlocks = normalizedActiveBlocks(blocks)
        val restingBlocks = blocks.filter { it.kind == TimeKind.RESTING_NIGHT_WATCH }
        val activeMinutes = activeBlocks.sumOf(::durationMinutes)
        val restingMinutes = restingBlocks.sumOf(::durationMinutes)
        val activeInside = activeBlocks.sumOf { overlapWithRoster(it, roster) }.coerceAtMost(activeMinutes)
        val restingInside = restingBlocks.sumOf { overlapWithRoster(it, roster) }.coerceAtMost(restingMinutes)
        val uncovered = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
            rosterUncoveredEvidence(blocks, roster, tripStart, tripEnd)
        } else {
            emptyList()
        }
        val rosterMinutes = roster.flatMap { (date, value) ->
            RosterEntryCodec.decode(value).mapNotNull { shift ->
                val interval = TurnusOverlapEngine.intervalFor(date, shift) ?: return@mapNotNull null
                intersection(interval.first, interval.second, tripStart, tripEnd)
            }
        }.sumOf { (start, end) -> ChronoUnit.MINUTES.between(start, end) }

        return CalculationWorktimeAudit(
            rosterMinutes = rosterMinutes,
            rosterUncoveredMinutes = uncovered.sumOf { it.minutes },
            rosterUncoveredEvidence = uncovered,
            activeMinutes = activeMinutes,
            activeInsideRosterMinutes = activeInside,
            activeOutsideRosterMinutes = activeMinutes - activeInside,
            restingNightMinutes = restingMinutes,
            restingNightOutsideRosterMinutes = restingMinutes - restingInside,
        )
    }

    fun rosterUncoveredEvidence(
        blocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
    ): List<CalculationEvidence> = rosterUncoveredEvidence(blocks, roster, null, null)

    fun rosterUncoveredEvidence(
        blocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        tripStart: LocalDateTime?,
        tripEnd: LocalDateTime?,
    ): List<CalculationEvidence> {
        val registeredIntervals = blocks
            .filterNot { it.kind == TimeKind.ACTIVE_EVENT_ON_RESTING }
            .map { it.start to it.end }
        return roster.toSortedMap().flatMap { (date, value) ->
            RosterEntryCodec.decode(value).flatMap shiftLoop@ { shift ->
                val rawInterval = TurnusOverlapEngine.intervalFor(date, shift) ?: return@shiftLoop emptyList()
                val interval = if (tripStart != null && tripEnd != null) {
                    intersection(rawInterval.first, rawInterval.second, tripStart, tripEnd) ?: return@shiftLoop emptyList()
                } else {
                    rawInterval
                }
                subtractIntervals(interval.first, interval.second, registeredIntervals).map { (start, end) ->
                    CalculationEvidence(
                        start = start,
                        end = end,
                        minutes = ChronoUnit.MINUTES.between(start, end),
                        note = "Turnustid uten registrert arbeidsperiode på turen",
                    )
                }
            }
        }
    }

    fun preservePlanForDateRange(
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        currentPlans: Map<LocalDate, List<PlannedBlock>>,
        comparisonMode: RosterComparisonMode,
    ): Map<LocalDate, List<PlannedBlock>> {
        // A4.0A: grunnturnus er kun sammenligningsgrunnlag. Den skal aldri automatisk bli
        // faktisk arbeid på turen. Behold det brukeren har registrert på overlappende datoer,
        // og start nye datoer tomme uansett beregningsmodell.
        @Suppress("UNUSED_VARIABLE") val compatibilityInputs = roster to comparisonMode
        return dates.associateWith { date -> currentPlans[date].orEmpty() }
    }

    fun adjustPlanForComparisonChange(
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        currentPlans: Map<LocalDate, List<PlannedBlock>>,
        from: RosterComparisonMode,
        to: RosterComparisonMode,
    ): Map<LocalDate, List<PlannedBlock>> {
        // Beregningsmåten kan endre hvordan periodene sammenlignes med turnus, men den skal
        // ikke skrive om det brukeren faktisk har registrert som arbeid/reise på turen.
        @Suppress("UNUSED_VARIABLE") val compatibilityInputs = Triple(roster, from, to)
        return dates.associateWith { date -> currentPlans[date].orEmpty() }
    }

    fun plannedBlocksForShiftWithinTrip(
        date: LocalDate,
        shift: ShiftDefinition?,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): List<PlannedBlock> {
        if (shift == null || shift.category == ShiftCategory.OFF) return emptyList()
        val interval = TurnusOverlapEngine.intervalFor(date, shift) ?: return emptyList()
        val visible = intersection(interval.first, interval.second, tripStart, tripEnd) ?: return emptyList()
        val kind = if (shift.category == ShiftCategory.NIGHT) TimeKind.ACTIVE_NIGHT_WATCH else TimeKind.ACTIVE_WORK
        return listOf(
            PlannedBlock(
                kind = kind,
                start = visible.first.toLocalTime(),
                end = visible.second.toLocalTime(),
            ),
        )
    }

    fun overlayTravelOnPlan(
        dates: List<LocalDate>,
        basePlans: Map<LocalDate, List<PlannedBlock>>,
        travelPlans: Map<LocalDate, List<PlannedBlock>>,
    ): Map<LocalDate, List<PlannedBlock>> {
        val baseWithoutOldTravel = basePlans.mapValues { (_, blocks) -> blocks.filterNot { it.kind.isTravelKind() } }
        val baseBlocks = projectRange(dates, baseWithoutOldTravel)
        val travelBlocks = projectRange(dates, travelPlans)
        val replaceableKinds = setOf(TimeKind.ACTIVE_WORK, TimeKind.ACTIVE_NIGHT_WATCH)

        val remainingBase = baseBlocks.flatMap { block ->
            if (block.kind !in replaceableKinds) return@flatMap listOf(block)
            subtractWorkBlock(block, travelBlocks)
        }
        val all = (remainingBase + travelBlocks).sortedBy { it.start }
        val result = dates.associateWith { mutableListOf<PlannedBlock>() }.toMutableMap()
        all.forEach { block ->
            val date = block.start.toLocalDate()
            if (date !in result) return@forEach
            result.getValue(date).add(
                PlannedBlock(
                    kind = block.kind,
                    start = block.start.toLocalTime(),
                    end = block.end.toLocalTime(),
                    travelNoticeStatus = block.travelNoticeStatus,
                ),
            )
        }
        return result.mapValues { (_, blocks) -> blocks.sortedBy { it.start } }
    }

    private fun subtractWorkBlock(block: WorkBlock, overlays: List<WorkBlock>): List<WorkBlock> {
        val covered = overlays.mapNotNull { overlay ->
            intersection(block.start, block.end, overlay.start, overlay.end)
        }
        return subtractIntervals(block.start, block.end, covered).map { (start, end) ->
            WorkBlock(start, end, block.kind, block.travelNoticeStatus)
        }
    }

    fun isDayTrip(tripStart: LocalDateTime, tripEnd: LocalDateTime): Boolean =
        tripEnd.isAfter(tripStart) && tripStart.toLocalDate() == tripEnd.toLocalDate()

    fun chapter20Applies(tripStart: LocalDateTime, tripEnd: LocalDateTime): Boolean =
        tripEnd.isAfter(tripStart) && !isDayTrip(tripStart, tripEnd)

    fun controlFindings(
        blocks: List<WorkBlock>,
        unresolvedRuleCount: Int,
        roster: Map<LocalDate, String> = emptyMap(),
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): List<ControlFinding> {
        val findings = mutableListOf<ControlFinding>()
        if (blocks.isEmpty()) {
            return listOf(ControlFinding(FindingSeverity.OK, "Ingen arbeidstid registrert", "Legg inn turarbeid for å kjøre arbeidstidskontrollen."))
        }
        val directWorkBlocks = blocks.filter {
            !it.kind.isTravelWithoutResponsibility() && it.kind != TimeKind.TRAVEL_UNCERTAIN
        }
        // In normal-roster mode the stored ground roster is the app's available evidence of
        // ordinary working time. Point 18.4 counts travel in that time fully as worktime.
        val travelInOrdinaryWorkTime = blocks
            .filter { it.kind.isTravelWithoutResponsibility() }
            .flatMap { block ->
                rosterIntersections(block, roster).map { overlap ->
                    WorkBlock(overlap.start, overlap.end, block.kind, block.travelNoticeStatus)
                }
            }
        // Point 20.3 is explicit: night travel with permission to sleep is passive work,
        // and passive work counts as worktime time-for-time even when it lies outside roster.
        val passiveNightTravel = blocks
            .filter { it.kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED }
            .flatMap { block -> travelNightBlocks(block, rateSet) }
        val work = mergePeriods(directWorkBlocks + travelInOrdinaryWorkTime + passiveNightTravel)
        val totalMinutes = work.sumOf { ChronoUnit.MINUTES.between(it.first, it.second) }
        if (totalMinutes > 48 * 60) {
            findings += ControlFinding(
                FindingSeverity.REVIEW,
                "Mer enn 48 timer i den viste perioden",
                "Du har registrert ${hoursLabel(totalMinutes)} som arbeid i perioden. Kontroller hvilken arbeidstidsordning som gjelder for turen.",
            )
        } else {
            findings += ControlFinding(FindingSeverity.OK, "Samlet arbeidstid", "${hoursLabel(totalMinutes)} i den viste perioden.")
        }
        work.zipWithNext().forEach { (previous, next) ->
            val rest = ChronoUnit.MINUTES.between(previous.second, next.first)
            if (rest in 0 until 11 * 60) {
                findings += ControlFinding(
                    if (rest < 8 * 60) FindingSeverity.CRITICAL else FindingSeverity.REVIEW,
                    "Kort hvile mellom arbeidsperioder",
                    "Du har bare ${hoursLabel(rest)} sammenhengende fri. Den første arbeidsperioden slutter ${controlDateTime(previous.second)}, og den neste begynner ${controlDateTime(next.first)}. Kontroller hvilken arbeidstidsordning som gjelder, og om det kreves kompenserende hvile.",
                )
            }
        }
        work.filter { ChronoUnit.MINUTES.between(it.first, it.second) > 13 * 60 }.forEach { period ->
            val minutes = ChronoUnit.MINUTES.between(period.first, period.second)
            findings += ControlFinding(
                FindingSeverity.REVIEW,
                "Lang sammenhengende arbeidsperiode",
                "Arbeidsperioden varer ${hoursLabel(minutes)}, fra ${controlDateTime(period.first)} til ${controlDateTime(period.second)}. Kontroller at dette er tillatt etter arbeidstidsordningen som gjelder for turen.",
            )
        }
        if (blocks.any { it.kind == TimeKind.TRAVEL_UNCERTAIN }) {
            findings += ControlFinding(
                FindingSeverity.OPEN,
                "Avklar ansvaret under reisen",
                "Minst én reiseperiode er registrert som usikker. Avklar hvem som hadde ansvar for å følge opp beboeren før beregningen brukes som betalingsgrunnlag.",
            )
        }
        val ordinaryTravelRelevantForNotice = blocks
            .filter { it.kind.isTravelWithoutResponsibility() }
            .flatMap { block ->
                val ordinaryParts = when (block.kind) {
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> travelNonNightBlocks(block, rateSet)
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP -> listOf(block)
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY -> travelNonNightBlocks(block, rateSet)
                    else -> emptyList()
                }
                if (roster.isEmpty()) ordinaryParts else ordinaryParts.flatMap { outsideWorkBlocks(it, roster) }
            }
        if (blocks.any { it.kind.isTravelWithoutResponsibility() }) {
            findings += ControlFinding(
                FindingSeverity.REVIEW,
                "Reise uten tilsynsansvar er behandlet etter reisetidsreglene",
                "Punkt 18.4 brukes for ordinær reisetid. Den delen som faller i grunnturnusen regnes fullt ut som arbeidstid i kontrollen. Reisetid utenfor grunnturnusen godtgjøres med ordinær timelønn når ikke særregelen om passiv nattreise i punkt 20.3 gjelder. Varseltidspunktet brukes bare for den ordinære reisetiden som faktisk ligger utenfor ordinær arbeidstid.",
            )
        }
        if (ordinaryTravelRelevantForNotice.any { it.travelNoticeStatus == TravelNoticeStatus.NOT_CLARIFIED }) {
            findings += ControlFinding(
                FindingSeverity.OPEN,
                "Når du fikk vite om reisen må avklares",
                "Minst én relevant reise uten tilsynsansvar mangler svar på om reisen var kjent senest dagen i forveien. Ordinær reisetidsbetaling kan beregnes, men punkt 18.4 kan gi overtidstillegg for inntil ${durationWords(rateSet.shortNoticeMaxMinutes)} ved kort varsel.",
            )
        }
        if (ordinaryTravelRelevantForNotice.any { it.travelNoticeStatus == TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY }) {
            findings += ControlFinding(
                FindingSeverity.REVIEW,
                "Kort varsel om reisen er registrert",
                "Punkt 18.4 gir overtidsbetaling for inntil ${durationWords(rateSet.shortNoticeMaxMinutes)} reisetid som kreves utført utenfor ordinær arbeidstid når reisen ikke var kjent senest dagen i forveien. Beregningen viser overtidsdelen separat fra ordinær reisetidsbetaling.",
            )
        }
        val unresolvedNightTravelMinutes = blocks
            .filter { it.kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY }
            .flatMap { block -> travelNightBlocks(block, rateSet) }
            .sumOf(::durationMinutes)
        if (unresolvedNightTravelMinutes > 0) {
            findings += ControlFinding(
                FindingSeverity.OPEN,
                "Søvntillatelse under nattreisen må avklares",
                "Du har registrert ${minutesLabel(unresolvedNightTravelMinutes)} reise uten tilsynsansvar mellom kl. ${clockLabel(rateSet.travelSleepWindowStart)} og ${clockLabel(rateSet.travelSleepWindowEnd)} uten å angi om du hadde tillatelse til å sove. Punkt 20.3 sier at nattreisen skal beregnes som arbeid av passiv karakter når slik tillatelse forelå. Velg Ja, Nei eller Ikke avklart på reiseperioden.",
            )
        }
        val passiveNightTravelMinutes = passiveNightTravel.sumOf(::durationMinutes)
        if (passiveNightTravelMinutes > 0) {
            findings += ControlFinding(
                FindingSeverity.REVIEW,
                "Passiv nattreise teller som arbeidstid",
                "Du har oppgitt søvntillatelse for ${minutesLabel(passiveNightTravelMinutes)} reise mellom kl. ${clockLabel(rateSet.travelSleepWindowStart)} og ${clockLabel(rateSet.travelSleepWindowEnd)}. Punkt 20.3 regner denne tiden som arbeid av passiv karakter: arbeidstid time for time og grunnbetaling i forholdet ${passiveRatioLabel(rateSet)}.",
            )
        }
        if (unresolvedRuleCount > 0) {
            findings += ControlFinding(
                FindingSeverity.OPEN,
                if (unresolvedRuleCount == 1) "1 regel må fortsatt avklares" else "$unresolvedRuleCount regler må fortsatt avklares",
                "De endrer ikke tiden du har registrert, men kan påvirke det endelige beregnede beløpet.",
            )
        }
        return findings
    }

    fun outsideTripRangeBlocks(
        blocks: List<WorkBlock>,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): List<WorkBlock> = blocks.filter { block ->
        block.start.isBefore(tripStart) || block.end.isAfter(tripEnd)
    }

    fun hasUnintendedOverlap(blocks: List<WorkBlock>): Boolean {
        val sorted = blocks.sortedBy { it.start }
        for (i in sorted.indices) {
            val first = sorted[i]
            for (j in i + 1 until sorted.size) {
                val second = sorted[j]
                if (!second.start.isBefore(first.end)) break
                if (overlapIsIntentional(first.kind, second.kind)) continue
                if (intersection(first.start, first.end, second.start, second.end) != null) return true
            }
        }
        return false
    }

    fun stayAllowanceDays(
        start: LocalDateTime,
        end: LocalDateTime,
        rateSet: TariffRateSet = FerieturTariffRates.current,
    ): Int {
        require(end.isAfter(start))
        val minutes = Duration.between(start, end).toMinutes()
        val fullDays = minutes / (24 * 60)
        val remainder = minutes % (24 * 60)
        return (fullDays + if (remainder > rateSet.stayAllowanceRemainderThresholdMinutes) 1 else 0).toInt()
    }

    fun stayAllowanceRemainderMinutes(start: LocalDateTime, end: LocalDateTime): Long {
        require(end.isAfter(start))
        return Duration.between(start, end).toMinutes() % (24 * 60)
    }

    fun buildDayAudits(
        dates: List<LocalDate>,
        weeklyBasis: WeeklyBasis,
        lines: List<CalculationLine>,
    ): List<DayCalculationAudit> {
        data class MutableContribution(
            val line: CalculationLine,
            var minutes: Long = 0,
            var amount: BigDecimal = BigDecimal.ZERO,
            val evidence: MutableList<CalculationEvidence> = mutableListOf(),
        )

        val dateSet = dates.toSet()
        val perDay = linkedMapOf<LocalDate, LinkedHashMap<String, MutableContribution>>()
        dates.forEach { perDay[it] = linkedMapOf() }

        lines.filterNot { it.id == "stay-allowance" }.forEach { line ->
            val auditEvidence = if (line.id == "active-on-resting") {
                line.evidence.filter { it.note.contains("betalt etter avrunding") }.ifEmpty { line.evidence.take(1) }
            } else {
                line.evidence
            }
            if (auditEvidence.isEmpty()) return@forEach

            val segments = if (line.id == "active-on-resting") {
                auditEvidence.map { evidence ->
                    Triple(evidence.start.toLocalDate(), evidence, evidence.minutes)
                }
            } else {
                auditEvidence.flatMap { evidence -> splitEvidenceByDay(evidence) }
            }.filter { it.first in dateSet && it.third > 0 }

            val totalMinutes = segments.sumOf { it.third }
            if (totalMinutes <= 0) return@forEach
            var allocated = BigDecimal.ZERO
            segments.forEachIndexed { index, (date, evidence, minutes) ->
                val amount = if (line.amount.compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal.ZERO
                } else if (index == segments.lastIndex) {
                    line.amount.subtract(allocated)
                } else {
                    moneyAmount(line.amount.multiply(BigDecimal(minutes)).divide(BigDecimal(totalMinutes), 8, RoundingMode.HALF_UP)).also { allocated = allocated.add(it) }
                }
                val map = perDay.getValue(date)
                val contribution = map.getOrPut(line.id) { MutableContribution(line) }
                contribution.minutes += minutes
                contribution.amount = contribution.amount.add(amount)
                contribution.evidence += evidence
            }
        }

        return dates.map { date ->
            val contributions = perDay.getValue(date).values.map { value ->
                DayCalculationContribution(
                    lineId = value.line.id,
                    title = value.line.title,
                    minutes = value.minutes,
                    amount = value.amount,
                    source = value.line.source,
                    certainty = value.line.certainty,
                    includedInKnownTotal = value.line.includedInKnownTotal,
                    paymentTreatment = value.line.paymentTreatment,
                    evidence = value.evidence.toList(),
                )
            }
            DayCalculationAudit(
                date = date,
                holidayLabels = OsloHolidayCalendar.holidayLabelsForDate(date, weeklyBasis),
                contributions = contributions,
                knownSubtotal = moneyAmount(contributions.filter { it.includedInKnownTotal }.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }),
                paymentSubtotal = moneyAmount(contributions.filter { it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS }.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }),
                alreadyCoveredSubtotal = moneyAmount(contributions.filter { it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER }.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }),
                openSubtotal = moneyAmount(contributions.filterNot { it.includedInKnownTotal }.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }),
            )
        }
    }

    private fun splitEvidenceByDay(evidence: CalculationEvidence): List<Triple<LocalDate, CalculationEvidence, Long>> {
        val results = mutableListOf<Triple<LocalDate, CalculationEvidence, Long>>()
        var cursor = evidence.start
        while (cursor.isBefore(evidence.end)) {
            val nextMidnight = cursor.toLocalDate().plusDays(1).atStartOfDay()
            val end = if (evidence.end.isBefore(nextMidnight)) evidence.end else nextMidnight
            val minutes = ChronoUnit.MINUTES.between(cursor, end)
            if (minutes > 0) {
                results += Triple(
                    cursor.toLocalDate(),
                    CalculationEvidence(cursor, end, minutes, evidence.note),
                    minutes,
                )
            }
            cursor = end
        }
        return results
    }

    private fun normalizedActiveBlocks(blocks: List<WorkBlock>): List<WorkBlock> {
        val primary = blocks.filter { it.kind == TimeKind.ACTIVE_WORK || it.kind == TimeKind.ACTIVE_NIGHT_WATCH }
        val travel = blocks.filter { it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY }
        val uncoveredTravel = travel.flatMap { block -> subtractWorkBlock(block, primary) }
        return (primary + uncoveredTravel).sortedBy { it.start }
    }

    private fun payableTravelBlocks(
        blocks: List<WorkBlock>,
        fundingMode: FundingMode,
        roster: Map<LocalDate, String>,
    ): List<WorkBlock> = when (fundingMode) {
        FundingMode.TURNUS_PLUS_EXTERNAL -> blocks.flatMap { outsideWorkBlocks(it, roster) }
        FundingMode.VACATION_SEPARATE, FundingMode.MUNICIPAL_ALL, FundingMode.CUSTOM -> blocks
    }

    private fun takeFirstMinutes(blocks: List<WorkBlock>, limitMinutes: Long): List<WorkBlock> {
        var remaining = limitMinutes.coerceAtLeast(0L)
        val result = mutableListOf<WorkBlock>()
        blocks.sortedBy { it.start }.forEach { block ->
            if (remaining <= 0L) return@forEach
            val duration = durationMinutes(block)
            val taken = minOf(duration, remaining)
            if (taken > 0L) result += block.copy(end = block.start.plusMinutes(taken))
            remaining -= taken
        }
        return result
    }

    private fun overtimeSupplementBands(
        blocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        rateSet: TariffRateSet,
    ): List<OvertimeSupplementBand> {
        val bands = mutableListOf<OvertimeSupplementBand>()
        blocks.sortedBy { it.start }.forEach { block ->
            if (!block.end.isAfter(block.start)) return@forEach
            var cursor = block.start
            var groupStart = cursor
            var groupFraction = overtimeSupplementFractionAt(cursor, roster, rateSet)
            var groupSpecial133 = OsloHolidayCalendar.isOvertime133Date(cursor.toLocalDate())
            while (cursor.isBefore(block.end)) {
                val next = minOf(cursor.plusMinutes(1), block.end)
                if (next.isBefore(block.end)) {
                    val nextFraction = overtimeSupplementFractionAt(next, roster, rateSet)
                    val nextSpecial133 = OsloHolidayCalendar.isOvertime133Date(next.toLocalDate())
                    if (nextFraction != groupFraction || nextSpecial133 != groupSpecial133) {
                        bands += overtimeBand(groupStart, next, groupFraction, groupSpecial133, rateSet)
                        groupStart = next
                        groupFraction = nextFraction
                        groupSpecial133 = nextSpecial133
                    }
                }
                cursor = next
            }
            if (block.end.isAfter(groupStart)) bands += overtimeBand(groupStart, block.end, groupFraction, groupSpecial133, rateSet)
        }
        return bands
    }

    private fun overtimeBand(
        start: LocalDateTime,
        end: LocalDateTime,
        fraction: BigDecimal,
        special133: Boolean,
        rateSet: TariffRateSet,
    ): OvertimeSupplementBand {
        val actual = ChronoUnit.MINUTES.between(start, end)
        val step = rateSet.overtimeRoundingStepMinutes
        val paid = ((actual + step - 1L) / step) * step
        val percent = fraction.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString().replace('.', ',')
        return OvertimeSupplementBand(
            start = start,
            end = end,
            actualMinutes = actual,
            paidMinutes = paid,
            supplementFraction = fraction,
            label = "$percent % overtidstillegg",
            special133Potential = special133,
        )
    }

    private fun overtimeSupplementFractionAt(
        moment: LocalDateTime,
        roster: Map<LocalDate, String>,
        rateSet: TariffRateSet,
    ): BigDecimal {
        val date = moment.toLocalDate()
        val time = moment.toLocalTime()
        if (isRegisteredWeeklyOffDay(date, roster)) return rateSet.overtimeHighFraction
        if (date.dayOfWeek.value == 7 || OsloHolidayCalendar.isPublicHoliday(date)) return rateSet.overtimeHighFraction
        if (time.isBefore(rateSet.overtimeHighEnd) || !time.isBefore(rateSet.overtimeHighStart)) return rateSet.overtimeHighFraction
        if (OsloHolidayCalendar.isDayBeforeSundayOrPublicHoliday(date)) {
            val ordinaryEnd = ordinaryWorkEndOnDate(date, roster)
            if (ordinaryEnd != null && !moment.isBefore(ordinaryEnd)) return rateSet.overtimeHighFraction
        }
        return rateSet.overtimeStandardFraction
    }

    private fun isRegisteredWeeklyOffDay(date: LocalDate, roster: Map<LocalDate, String>): Boolean =
        RosterEntryCodec.decode(roster[date]).any { it.category == ShiftCategory.OFF && it.weeklyOff }

    private fun ordinaryWorkEndOnDate(date: LocalDate, roster: Map<LocalDate, String>): LocalDateTime? {
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        return roster.flatMap { (shiftDate, value) ->
            RosterEntryCodec.decode(value).mapNotNull { shift ->
                val interval = TurnusOverlapEngine.intervalFor(shiftDate, shift) ?: return@mapNotNull null
                intersection(interval.first, interval.second, dayStart, dayEnd)?.second
            }
        }.maxOrNull()
    }

    private fun travelNightBlocks(block: WorkBlock, rateSet: TariffRateSet): List<WorkBlock> =
        travelBetween23And07Evidence(block, rateSet).map { WorkBlock(it.start, it.end, block.kind, block.travelNoticeStatus) }

    private fun travelNonNightBlocks(block: WorkBlock, rateSet: TariffRateSet): List<WorkBlock> {
        val night = travelNightBlocks(block, rateSet).map { it.start to it.end }
        return subtractIntervals(block.start, block.end, night).map { (start, end) -> WorkBlock(start, end, block.kind, block.travelNoticeStatus) }
    }

    private fun outsideWorkBlocks(block: WorkBlock, roster: Map<LocalDate, String>): List<WorkBlock> {
        val covered = rosterIntersections(block, roster).map { it.start to it.end }
        return subtractIntervals(block.start, block.end, covered).map { (start, end) -> WorkBlock(start, end, block.kind, block.travelNoticeStatus) }
    }

    private fun activeEventsPerRestingWatch(
        restingBlocks: List<WorkBlock>,
        eventBlocks: List<WorkBlock>,
        rateSet: TariffRateSet,
    ): List<ActiveEventGroup> = restingBlocks.mapNotNull { watch ->
        val intersections = eventBlocks.mapNotNull { event ->
            intersection(watch.start, watch.end, event.start, event.end)?.let { (start, end) ->
                CalculationEvidence(
                    start = start,
                    end = end,
                    minutes = ChronoUnit.MINUTES.between(start, end),
                    note = "Aktivt arbeid under hvilende nattevakt",
                )
            }
        }
        if (intersections.isEmpty()) return@mapNotNull null
        val actual = intersections.sumOf { it.minutes }
        val rounded = TariffMath.roundActiveNightMinutes(actual.toInt(), rateSet).toLong()
        val summary = CalculationEvidence(
            start = watch.start,
            end = watch.end,
            minutes = rounded,
            note = "${minutesLabel(actual)} aktivt registrert på vakten → ${minutesLabel(rounded)} betalt etter avrunding",
        )
        ActiveEventGroup(actual, rounded, listOf(summary) + intersections)
    }

    private fun passiveTimeLabel(minutes: Long, rateSet: TariffRateSet): String {
        val divisor = rateSet.passiveWorkDivisor.toLong()
        return if (minutes % divisor == 0L) {
            minutesLabel(minutes / divisor)
        } else {
            "${minutesLabel(minutes)} × ${passiveFractionLabel(rateSet)}"
        }
    }

    private fun passiveRatioLabel(rateSet: TariffRateSet): String =
        "1:${rateSet.passiveWorkDivisor}"

    private fun passiveFractionLabel(rateSet: TariffRateSet): String =
        if (rateSet.passiveWorkDivisor == 3) "⅓" else "1/${rateSet.passiveWorkDivisor}"

    private fun passiveShareText(rateSet: TariffRateSet): String =
        if (rateSet.passiveWorkDivisor == 3) "en tredel" else passiveFractionLabel(rateSet)

    private fun percentLabel(fraction: BigDecimal): String =
        fraction.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString().replace('.', ',')

    private fun decimalLabel(value: BigDecimal, scale: Int? = null): String {
        val normalized = if (scale == null) value.stripTrailingZeros() else value.setScale(scale, RoundingMode.HALF_UP)
        return normalized.toPlainString().replace('.', ',')
    }

    private fun clockLabel(value: LocalTime): String =
        value.format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun durationWords(minutes: Long): String = when (minutes) {
        120L -> "to timer"
        360L -> "seks timer"
        else -> minutesLabel(minutes)
    }

    private fun roundingUnitDefinite(minutes: Long): String =
        if (minutes == 30L) "halve time" else "$minutes minutter"

    private fun roundingUnitIndefinite(minutes: Long): String =
        if (minutes == 30L) "halvtime" else "$minutes-minuttersperiode"

    private fun mixedFractionLabel(numerator: BigDecimal, denominator: BigDecimal): String {
        val n = numerator.stripTrailingZeros().toBigIntegerExact()
        val d = denominator.stripTrailingZeros().toBigIntegerExact()
        val whole = n / d
        val remainder = n % d
        return when {
            remainder.signum() == 0 -> whole.toString()
            whole.signum() == 0 -> "$remainder/$d"
            else -> "$whole $remainder/$d"
        }
    }

    private fun specialOvertimePercentLabel(rateSet: TariffRateSet): String =
        rateSet.specialOvertimePercentageLabel

    private fun eveningNightEvidence(
        fundingMode: FundingMode,
        activeBlocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        rateSet: TariffRateSet,
    ): List<CalculationEvidence> {
        val candidateBlocks = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) rosterServiceBlocks(roster, tripStart, tripEnd) else activeBlocks
        return candidateBlocks.flatMap { block -> eligibleEveningNightSegments(block, block.kind == TimeKind.ACTIVE_NIGHT_WATCH, rateSet) }
    }

    private fun weekendEvidence(
        fundingMode: FundingMode,
        activeBlocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        weeklyBasis: WeeklyBasis,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): List<CalculationEvidence> {
        val candidateBlocks = ordinaryServiceBlocks(fundingMode, activeBlocks, roster, tripStart, tripEnd)
        return candidateBlocks.flatMap { block -> weekendSegmentsExcludingHoliday(block, weeklyBasis) }
    }

    private fun holidayEvidence(
        fundingMode: FundingMode,
        activeBlocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        weeklyBasis: WeeklyBasis,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): List<CalculationEvidence> = ordinaryServiceBlocks(fundingMode, activeBlocks, roster, tripStart, tripEnd)
        .flatMap { block -> holidaySupplementSegments(block, weeklyBasis) }

    private fun ordinaryServiceBlocks(
        fundingMode: FundingMode,
        activeBlocks: List<WorkBlock>,
        roster: Map<LocalDate, String>,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): List<WorkBlock> = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) rosterServiceBlocks(roster, tripStart, tripEnd) else activeBlocks

    private fun rosterServiceBlocks(
        roster: Map<LocalDate, String>,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): List<WorkBlock> = roster.flatMap { (date, value) ->
        RosterEntryCodec.decode(value).mapNotNull { shift ->
            val interval = TurnusOverlapEngine.intervalFor(date, shift) ?: return@mapNotNull null
            val clipped = intersection(interval.first, interval.second, tripStart, tripEnd) ?: return@mapNotNull null
            val kind = if (shift.category == ShiftCategory.NIGHT) TimeKind.ACTIVE_NIGHT_WATCH else TimeKind.ACTIVE_WORK
            WorkBlock(clipped.first, clipped.second, kind)
        }
    }.sortedBy { it.start }

    private fun eligibleEveningNightSegments(block: WorkBlock, nightWatch: Boolean, rateSet: TariffRateSet): List<CalculationEvidence> {
        if (nightWatch) {
            val latest = LocalDateTime.of(block.end.toLocalDate(), rateSet.nightWatchSupplementEnd)
            val end = if (block.end.isAfter(latest)) latest else block.end
            return if (end.isAfter(block.start)) listOf(
                CalculationEvidence(block.start, end, ChronoUnit.MINUTES.between(block.start, end), "Nattevakt · ${percentLabel(rateSet.eveningNightFraction)} % til vaktens slutt, senest ${clockLabel(rateSet.nightWatchSupplementEnd)}"),
            ) else emptyList()
        }
        val results = mutableListOf<CalculationEvidence>()
        var date = block.start.toLocalDate().minusDays(1)
        val lastDate = block.end.toLocalDate()
        while (!date.isAfter(lastDate)) {
            val eveningStart = LocalDateTime.of(date, rateSet.eveningStart)
            val eveningEnd = LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT)
            intersection(block.start, block.end, eveningStart, eveningEnd)?.let { (start, end) ->
                results += CalculationEvidence(start, end, ChronoUnit.MINUTES.between(start, end), "Kveld kl. ${clockLabel(rateSet.eveningStart)}–24:00")
            }
            val nightStart = LocalDateTime.of(date, LocalTime.MIDNIGHT)
            val nightEnd = LocalDateTime.of(date, rateSet.nightEnd)
            intersection(block.start, block.end, nightStart, nightEnd)?.let { (start, end) ->
                results += CalculationEvidence(start, end, ChronoUnit.MINUTES.between(start, end), "Natt kl. 00:00–${clockLabel(rateSet.nightEnd)}")
            }
            date = date.plusDays(1)
        }
        return results.distinctBy { Triple(it.start, it.end, it.note) }
    }

    private fun travelBetween23And07Evidence(block: WorkBlock, rateSet: TariffRateSet): List<CalculationEvidence> {
        val results = mutableListOf<CalculationEvidence>()
        var date = block.start.toLocalDate().minusDays(1)
        val lastDate = block.end.toLocalDate()
        while (!date.isAfter(lastDate)) {
            val nightStart = LocalDateTime.of(date, rateSet.travelSleepWindowStart)
            val nightEnd = LocalDateTime.of(date.plusDays(1), rateSet.travelSleepWindowEnd)
            intersection(block.start, block.end, nightStart, nightEnd)?.let { (start, end) ->
                results += CalculationEvidence(
                    start = start,
                    end = end,
                    minutes = ChronoUnit.MINUTES.between(start, end),
                    note = "Reisetid kl. ${clockLabel(rateSet.travelSleepWindowStart)}–${clockLabel(rateSet.travelSleepWindowEnd)} · søvnregel må kontrolleres",
                )
            }
            date = date.plusDays(1)
        }
        return results.distinctBy { Pair(it.start, it.end) }
    }

    private fun weekendSegments(block: WorkBlock): List<CalculationEvidence> {
        val results = mutableListOf<CalculationEvidence>()
        var date = block.start.toLocalDate().minusDays(1)
        val lastDate = block.end.toLocalDate()
        while (!date.isAfter(lastDate)) {
            if (date.dayOfWeek.value == 6) {
                val weekendStart = date.atStartOfDay()
                val weekendEnd = date.plusDays(2).atStartOfDay()
                intersection(block.start, block.end, weekendStart, weekendEnd)?.let { (start, end) ->
                    results += CalculationEvidence(start, end, ChronoUnit.MINUTES.between(start, end), "Lørdag/søndag")
                }
            }
            date = date.plusDays(1)
        }
        return results.distinctBy { Pair(it.start, it.end) }
    }

    private fun weekendSegmentsExcludingHoliday(block: WorkBlock, weeklyBasis: WeeklyBasis): List<CalculationEvidence> {
        val holidayCovered = OsloHolidayCalendar.holidaySupplementSegments(block, weeklyBasis).map { it.start to it.end }
        return weekendSegments(block).flatMap { weekend ->
            subtractIntervals(weekend.start, weekend.end, holidayCovered).map { (start, end) ->
                CalculationEvidence(start, end, ChronoUnit.MINUTES.between(start, end), "Lørdag/søndag")
            }
        }
    }

    private fun holidaySupplementSegments(block: WorkBlock, weeklyBasis: WeeklyBasis): List<CalculationEvidence> =
        OsloHolidayCalendar.holidaySupplementSegments(block, weeklyBasis).map { segment ->
            CalculationEvidence(
                start = segment.start,
                end = segment.end,
                minutes = ChronoUnit.MINUTES.between(segment.start, segment.end),
                note = "Helge-/høytidsperiode · ${segment.title}",
            )
        }

    private fun holidayOvertime133Segments(block: WorkBlock): List<CalculationEvidence> =
        OsloHolidayCalendar.overtime133Segments(block).map { segment ->
            CalculationEvidence(
                start = segment.start,
                end = segment.end,
                minutes = ChronoUnit.MINUTES.between(segment.start, segment.end),
                note = "Særskilt høytidsdag · ${segment.title}",
            )
        }

    private fun rosterIntersections(block: WorkBlock, roster: Map<LocalDate, String>): List<RosterIntersection> =
        roster.flatMap { (date, value) ->
            RosterEntryCodec.decode(value).mapNotNull { shift ->
                val interval = TurnusOverlapEngine.intervalFor(date, shift) ?: return@mapNotNull null
                intersection(block.start, block.end, interval.first, interval.second)?.let { (start, end) ->
                    RosterIntersection(start, end, shift)
                }
            }
        }.sortedBy { it.start }

    private fun outsideEvidence(block: WorkBlock, roster: Map<LocalDate, String>, note: String): List<CalculationEvidence> {
        val covered = rosterIntersections(block, roster).map { it.start to it.end }
        val outside = subtractIntervals(block.start, block.end, covered)
        return outside.map { (start, end) -> CalculationEvidence(start, end, ChronoUnit.MINUTES.between(start, end), note) }
    }

    private fun subtractIntervals(
        start: LocalDateTime,
        end: LocalDateTime,
        covered: List<Pair<LocalDateTime, LocalDateTime>>,
    ): List<Pair<LocalDateTime, LocalDateTime>> {
        if (covered.isEmpty()) return listOf(start to end)
        val merged = mergePairs(covered)
        val result = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        var cursor = start
        merged.forEach { (coveredStart, coveredEnd) ->
            if (coveredEnd <= cursor || coveredStart >= end) return@forEach
            val clippedStart = if (coveredStart.isBefore(start)) start else coveredStart
            val clippedEnd = if (coveredEnd.isAfter(end)) end else coveredEnd
            if (clippedStart.isAfter(cursor)) result += cursor to clippedStart
            if (clippedEnd.isAfter(cursor)) cursor = clippedEnd
        }
        if (cursor.isBefore(end)) result += cursor to end
        return result
    }

    private fun mergePairs(periods: List<Pair<LocalDateTime, LocalDateTime>>): List<Pair<LocalDateTime, LocalDateTime>> {
        if (periods.isEmpty()) return emptyList()
        val sorted = periods.sortedBy { it.first }
        val merged = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        var currentStart = sorted.first().first
        var currentEnd = sorted.first().second
        sorted.drop(1).forEach { (start, end) ->
            if (!start.isAfter(currentEnd)) {
                if (end.isAfter(currentEnd)) currentEnd = end
            } else {
                merged += currentStart to currentEnd
                currentStart = start
                currentEnd = end
            }
        }
        merged += currentStart to currentEnd
        return merged
    }

    private fun mergePeriods(blocks: List<WorkBlock>): List<Pair<LocalDateTime, LocalDateTime>> =
        mergePairs(blocks.map { it.start to it.end })

    fun overlapIsIntentional(first: TimeKind, second: TimeKind): Boolean {
        val restingEvent = (first == TimeKind.RESTING_NIGHT_WATCH && second == TimeKind.ACTIVE_EVENT_ON_RESTING) ||
            (second == TimeKind.RESTING_NIGHT_WATCH && first == TimeKind.ACTIVE_EVENT_ON_RESTING)
        val travelAnnotatesActive = (first == TimeKind.TRAVEL_WITH_RESPONSIBILITY && second in setOf(TimeKind.ACTIVE_WORK, TimeKind.ACTIVE_NIGHT_WATCH)) ||
            (second == TimeKind.TRAVEL_WITH_RESPONSIBILITY && first in setOf(TimeKind.ACTIVE_WORK, TimeKind.ACTIVE_NIGHT_WATCH))
        return restingEvent || travelAnnotatesActive
    }

    private fun overlappingInput(blocks: List<WorkBlock>): List<String> {
        val sorted = blocks.sortedBy { it.start }
        val warnings = mutableListOf<String>()
        for (i in sorted.indices) {
            val first = sorted[i]
            for (j in i + 1 until sorted.size) {
                val second = sorted[j]
                if (!second.start.isBefore(first.end)) break
                if (overlapIsIntentional(first.kind, second.kind)) continue
                if (intersection(first.start, first.end, second.start, second.end) == null) continue
                warnings += "${first.start}–${first.end} og ${second.start}–${second.end} dekker noe av den samme tiden. Endre periodene hvis dette ikke er med vilje."
            }
        }
        return warnings
    }

    private fun intersection(
        aStart: LocalDateTime,
        aEnd: LocalDateTime,
        bStart: LocalDateTime,
        bEnd: LocalDateTime,
    ): Pair<LocalDateTime, LocalDateTime>? {
        val start = if (aStart.isAfter(bStart)) aStart else bStart
        val end = if (aEnd.isBefore(bEnd)) aEnd else bEnd
        return if (end.isAfter(start)) start to end else null
    }

    private fun controlDateTime(value: LocalDateTime): String {
        val date = value.toLocalDate().format(DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.forLanguageTag("nb-NO")))
        val time = value.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        return "${date.replaceFirstChar { it.lowercase(Locale.forLanguageTag("nb-NO")) }} kl. $time"
    }

    private fun evidence(block: WorkBlock, note: String): CalculationEvidence =
        CalculationEvidence(block.start, block.end, durationMinutes(block), note)

    private fun durationMinutes(block: WorkBlock): Long = ChronoUnit.MINUTES.between(block.start, block.end).coerceAtLeast(0)

    private fun payForMinutes(hourlyRate: BigDecimal, minutes: Long): BigDecimal =
        hourlyRate.multiply(BigDecimal(minutes)).divide(BigDecimal(60), 8, RoundingMode.HALF_UP)

    private fun moneyAmount(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    private fun moneyRate(value: BigDecimal): String = "${moneyAmount(value).toPlainString().replace('.', ',')} kr"

    private fun minutesLabel(minutes: Long): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return if (remainder == 0L) "$hours t" else "$hours t $remainder min"
    }

    private fun hoursLabel(minutes: Long): String = minutesLabel(minutes)
}
