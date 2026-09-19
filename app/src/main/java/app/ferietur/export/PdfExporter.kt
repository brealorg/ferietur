package app.ferietur.export

import app.ferietur.domain.TripWorkPlanBasis
import app.ferietur.domain.HolidayWorkPlanStatus
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import app.ferietur.domain.CalculationCertainty
import app.ferietur.domain.CalculationEvidence
import app.ferietur.domain.CalculationLine
import app.ferietur.domain.ControlFinding
import app.ferietur.domain.DomainRule
import app.ferietur.domain.EmployerKind
import app.ferietur.domain.FinalizedTripSnapshot
import app.ferietur.domain.FinalizedCalculationPresentationLine
import app.ferietur.domain.FinalizedCalculationPresentations
import app.ferietur.domain.FerieturTariffRates
import app.ferietur.domain.TariffRateSet
import app.ferietur.domain.TariffCalculationLineScope
import app.ferietur.domain.FindingSeverity
import app.ferietur.domain.PayingParty
import app.ferietur.domain.PaymentTreatment
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SafeStorageId
import app.ferietur.domain.TimeKind
import app.ferietur.domain.TravelNoticeStatus
import app.ferietur.domain.WorkBlock
import app.ferietur.domain.isTravelWithoutResponsibility
import app.ferietur.domain.WeeklyBasis
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NORWEGIAN_LOCALE = Locale.forLanguageTag("nb-NO")
internal const val PAYMENT_SCENARIO_DISCLAIMER = "Betalingsscenarioet brukes i betalingsforslaget og dokumentasjonen. Det endrer ikke selve beregningen og fastsetter ikke hvem som rettslig skal bære kostnaden."

object PdfExporter {

    enum class Variant { SHORT, FULL }

    private const val EXPORT_DIRECTORY_NAME = "exports"
    private const val EXPORT_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L

    internal data class PdfWorktimeSegment(
        val kind: TimeKind,
        val start: java.time.LocalDateTime,
        val end: java.time.LocalDateTime,
    ) {
        val minutes: Long
            get() = java.time.temporal.ChronoUnit.MINUTES.between(start, end)
    }

    internal data class PdfWorktimePeriod(
        val start: java.time.LocalDateTime,
        val end: java.time.LocalDateTime,
        val segments: List<PdfWorktimeSegment>,
    ) {
        val minutes: Long
            get() = java.time.temporal.ChronoUnit.MINUTES.between(start, end)
    }

    /**
     * PDF-only projection of the primary registered work periods.
     *
     * This does not decide whether a work-time arrangement is lawful and does
     * not alter TripPlanEngine.controlFindings(). It is used only when the PDF
     * can represent the already-produced long-period findings without semantic
     * loss. Unsupported/special travel cases fall back to the existing textual
     * finding presentation.
     */
    internal fun longWorktimePeriodsForPdf(
        blocks: List<WorkBlock>,
    ): List<PdfWorktimePeriod> {
        val relevant = blocks
            .filter { block ->
                block.kind == TimeKind.ACTIVE_WORK ||
                    block.kind == TimeKind.ACTIVE_NIGHT_WATCH ||
                    block.kind == TimeKind.RESTING_NIGHT_WATCH ||
                    block.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY
            }
            .sortedWith(
                compareBy<WorkBlock> { it.start }
                    .thenBy { it.end },
            )

        if (relevant.isEmpty()) return emptyList()

        val groups = mutableListOf<MutableList<WorkBlock>>()
        var current = mutableListOf(relevant.first())
        var currentEnd = relevant.first().end

        relevant.drop(1).forEach { block ->
            if (block.start.isAfter(currentEnd)) {
                groups += current
                current = mutableListOf(block)
                currentEnd = block.end
            } else {
                current += block
                if (block.end.isAfter(currentEnd)) {
                    currentEnd = block.end
                }
            }
        }
        groups += current

        return groups.mapNotNull { group ->
            val start = group.minOf { it.start }
            val end = group.maxOf { it.end }
            val periodMinutes =
                java.time.temporal.ChronoUnit.MINUTES.between(start, end)

            if (periodMinutes <= 13L * 60L) {
                return@mapNotNull null
            }

            val segments = mutableListOf<PdfWorktimeSegment>()

            group.sortedBy { it.start }.forEach { block ->
                val previous = segments.lastOrNull()
                if (
                    previous != null &&
                    previous.kind == block.kind &&
                    previous.end == block.start
                ) {
                    segments[segments.lastIndex] =
                        previous.copy(end = block.end)
                } else {
                    segments += PdfWorktimeSegment(
                        kind = block.kind,
                        start = block.start,
                        end = block.end,
                    )
                }
            }

            PdfWorktimePeriod(
                start = start,
                end = end,
                segments = segments,
            )
        }
    }

    internal fun isCompactWorktimePeriodForPdf(
        period: PdfWorktimePeriod,
    ): Boolean = period.segments.size == 1

    internal fun createBlocking(context: Context, snapshot: FinalizedTripSnapshot, variant: Variant): File {
        val dir = File(context.cacheDir, EXPORT_DIRECTORY_NAME).apply { mkdirs() }
        pruneStaleExports(dir)
        val suffix = if (variant == Variant.FULL) "fullt-grunnlag" else "oppsummering"
        // SECURITY04: the snapshot ID can originate from an imported backup and is part of a file name.
        SafeStorageId.requireValid(snapshot.id, "beregnings-ID")
        val file = File(dir, "ferietur-${snapshot.tripStart.toLocalDate()}-${snapshot.id}-$suffix.pdf")
        val rateSet = FerieturTariffRates.requireById(snapshot.tariffRateSetId)
        val document = PdfDocument()
        try {
            val writer = PdfWriter(document)

            if (variant == Variant.SHORT) {
                writeExecutiveSummary(writer, snapshot, rateSet, includeFooter = true)
            } else {
                writeExecutiveSummary(writer, snapshot, rateSet, includeFooter = false)
                writer.pageBreak()
                writeRosterAndPlan(writer, snapshot)
                writeCalculationDetails(writer, snapshot, rateSet)
                writeDayAudit(writer, snapshot)
                writeGroupedControl(writer, snapshot, rateSet, includeDetails = true)
                writeSources(writer, snapshot, rateSet)
            }

            writer.finish()
            FileOutputStream(file).use(document::writeTo)
        } catch (error: Throwable) {
            // Never leave a half-written PDF with salary data behind in the share directory.
            file.delete()
            throw error
        } finally {
            document.close()
        }
        return file
    }

    /**
     * Exported PDFs contain salary data and only need to live long enough to be shared.
     * Older exports are removed whenever a new one is created.
     */
    internal fun pruneStaleExports(
        directory: File,
        nowEpochMillis: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = EXPORT_MAX_AGE_MILLIS,
    ) {
        directory
            .listFiles { file -> file.isFile && file.extension == "pdf" }
            .orEmpty()
            .filter { nowEpochMillis - it.lastModified() > maxAgeMillis }
            .forEach { it.delete() }
    }

    fun sharePrepared(context: Context, filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            // ClipData makes the read grant follow the URI through the chooser to the target app.
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Del beregningsgrunnlag"))
    }

    /**
     * A3.9C: PDF-en starter med det mottakeren faktisk trenger å vite:
     * hvilket beløp som skal vurderes, hvem som betaler og hva beløpet består av.
     * Juridisk sporbarhet og kontrollinformasjon kommer etterpå, i kompakt form.
     */
    private fun writeExecutiveSummary(w: PdfWriter, s: FinalizedTripSnapshot, rateSet: TariffRateSet, includeFooter: Boolean) {
        val calculation = s.presentation
        w.documentLabel(if (includeFooter) "KORT OPPSUMMERING" else "FULLT BEREGNINGSGRUNNLAG")
        w.h1(s.title.ifBlank { "Ferietur" })
        w.p("${dateTime(s.tripStart)} - ${dateTime(s.tripEnd)}")

        val amountLabel = when {
            s.employerKind != EmployerKind.OSLO_KOMMUNE -> "Foreløpig regnegrunnlag"
            s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER && s.unresolvedRules.isNotEmpty() -> "Foreløpig betalingsgrunnlag i tillegg til grunnturnusen"
            s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER -> "Betalingsgrunnlag i tillegg til grunnturnusen"
            s.unresolvedRules.isNotEmpty() -> "Foreløpig beregnet lønn og godtgjøring"
            else -> "Beregnet lønn og godtgjøring"
        }
        val proposed = if (s.settlement.usesFullCalculation) calculation.paymentBasisAmount else s.settlement.proposedAmount
        val openLines = calculation.lines.filter { it.paymentTreatment == PaymentTreatment.OPEN && (it.amount > BigDecimal.ZERO || it.certainty == CalculationCertainty.OPEN) }
        val possibleExtra = openLines.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) }
        val sideLabel: String
        val sideValue: String
        when {
            !s.settlement.usesFullCalculation -> {
                sideLabel = "Foreslått betaling"
                sideValue = money(proposed)
            }
            s.unresolvedRules.isNotEmpty() -> {
                sideLabel = "${s.unresolvedRules.size} ${if (s.unresolvedRules.size == 1) "regel må" else "regler må"} avklares"
                sideValue = if (possibleExtra > BigDecimal.ZERO) "Mulig tillegg: ${money(possibleExtra)}" else "Beløpet kan bli høyere"
            }
            else -> {
                sideLabel = "Betalingsforslag"
                sideValue = "Samme som beregnet grunnlag"
            }
        }
        w.summaryAmount(amountLabel, calculation.paymentBasisAmount, sideLabel, sideValue)

        if (!s.settlement.usesFullCalculation) {
            val difference = calculation.paymentBasisAmount.subtract(s.settlement.proposedAmount)
            w.summaryDifference("Forskjell fra beregnet betalingsgrunnlag", difference)
            if (s.settlement.reason.isNotBlank()) w.smallText("Begrunnelse for annet beløp: ${s.settlement.reason}")
        }

        w.summaryMetaRow("Arbeidsgiver", employerLabel(s.employerKind), "Betalingsscenario", payingPartyLabel(s.payingParty))
        w.summaryMetaSingle("Regler appen bruker", s.ruleBasis)
        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            w.summaryMetaSingle("Planbasis", workPlanBasisLabelForPdf(s.workPlanBasis))
            if (s.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
                w.summaryMetaSingle(
                    "Arbeidsgivers plan",
                    holidayWorkPlanStatusLabelForPdf(s.holidayWorkPlanStatus),
                )
            }
        }
        w.smallText(PAYMENT_SCENARIO_DISCLAIMER)

        when {
            s.rosterComparisonMode != RosterComparisonMode.USE_NORMAL_ROSTER ->
                w.compactNote("Hvordan turen er regnet", separateTripCalculationNote())

            s.workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES -> {
                val coveredSupplement = calculation.alreadyCoveredByNormalRosterAmount
                val coveredText = buildString {
                    append("I denne beregningen er det lagt til grunn at Oslo kommune utbetaler ordinær lønn og turnustillegg etter grunnturnusen. Den delen av grunnturnusen som overlapper turen utgjør ${minutes(calculation.rosterMinutes)}.")
                    if (coveredSupplement > BigDecimal.ZERO) {
                        append(" Beregnede turnustillegg fra grunnturnusen: ${money(coveredSupplement)}.")
                    }
                    append(" Dette er ikke med i betalingsgrunnlaget over.")
                }
                w.compactNote("Grunnturnus som sammenligningsgrunnlag", coveredText)
            }

            s.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN ->
                w.compactNote(
                    "Arbeidsgivers plan som sammenligningsgrunnlag",
                    "Arbeidsgiver har fastsatt en egen arbeidsplan for turen. Registrert arbeid sammenlignes med denne planen når arbeid utover avtalt tid klassifiseres. Grunnturnusen dokumenteres fortsatt som ordinær turnus, men er ikke punkt 20.2-baseline i denne ferdigstillingen.",
                )

            else ->
                w.compactNote(
                    "Planbasis i eldre ferdigstilling",
                    "Denne ferdigstillingen ble opprettet før Ferietur lagret eksplisitt om grunnturnusen eller en arbeidsgiverfastsatt turplan var sammenligningsgrunnlaget. Appen gjetter derfor ikke planbasis.",
                )
        }

        w.h2("Slik er beløpet satt sammen")
        calculation.lineEntries
            .filter { it.line.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS && it.line.amount != BigDecimal.ZERO }
            .forEach { entry ->
                w.moneyRow(
                    presentationLineTitle(s, entry),
                    plainFormula(entry.line),
                    entry.line.amount,
                )
            }

        val reviewFindings = s.findings.filter { it.severity == FindingSeverity.REVIEW || it.severity == FindingSeverity.CRITICAL }
        val workingInterpretationRules =
            app.ferietur.domain.FerieturRules
                .applicableWorkingInterpretationRules(
                    calculation.lines,
                )

        w.h2("Status")
        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            w.statusRow(
                "Planbasis",
                workPlanBasisLabelForPdf(s.workPlanBasis),
                if (s.workPlanBasis == TripWorkPlanBasis.NOT_CLARIFIED) PdfTone.WARNING else PdfTone.OK,
            )
            if (s.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
                w.statusRow(
                    "Arbeidsgivers plan",
                    holidayWorkPlanStatusLabelForPdf(s.holidayWorkPlanStatus),
                    if (s.holidayWorkPlanStatus == HolidayWorkPlanStatus.NOT_CLARIFIED) PdfTone.WARNING else PdfTone.OK,
                )
            }
        }
        w.statusRow("Lønnsopplysninger", if (s.payslipChecked) "Kontrollert mot lønnsslipp" else "Må kontrolleres", if (s.payslipChecked) PdfTone.OK else PdfTone.WARNING)
        if (calculation.rosterUncoveredMinutes > 0L) {
            w.statusRow("Turnussammenligning", "${minutes(calculation.rosterUncoveredMinutes)} uten registrert arbeidsperiode · kontrollert", PdfTone.INFO)
        }
        w.statusRow(
            "Beregning",
            if (s.unresolvedRules.isEmpty()) "Ingen åpne regler som treffer denne turen" else "${s.unresolvedRules.size} ${if (s.unresolvedRules.size == 1) "regel" else "regler"} må avklares",
            if (s.unresolvedRules.isEmpty()) PdfTone.OK else PdfTone.WARNING,
        )
        if (workingInterpretationRules.isNotEmpty()) {
            w.statusRow(
                "Fortolkning",
                "${workingInterpretationRules.size} ${if (workingInterpretationRules.size == 1) "arbeidsfortolkning er" else "arbeidsfortolkninger er"} brukt i beløpet · fortsatt til avklaring",
                PdfTone.INFO,
            )
        }
        w.statusRow(
            "Arbeidstid",
            if (reviewFindings.isEmpty()) "Ingen forhold markert" else "${reviewFindings.size} forhold bør vurderes",
            if (reviewFindings.isEmpty()) PdfTone.OK else PdfTone.INFO,
        )

        if (s.employerKind != EmployerKind.OSLO_KOMMUNE) {
            w.warningLine("Arbeidsgiverforholdet er ikke bekreftet som Oslo kommune. Dokumentet er derfor et foreløpig regnegrunnlag og viser ikke samlet arbeidsgiverkostnad.")
        }
        if (s.payingParty == PayingParty.UNSPECIFIED) {
            w.statusRow("Betalingsscenario", "Ikke avklart ennå · gyldig status", PdfTone.INFO)
        }
        if (s.unresolvedRules.isNotEmpty()) {
            val extra = if (possibleExtra > BigDecimal.ZERO) {
                " Mulig tillegg som ikke er lagt til: ${money(possibleExtra)}."
            } else {
                " Beløpet kan derfor bli høyere når regelen er avklart."
            }
            w.warningLine(sentenceWithFollowUp(
                "Betalingsforslaget er ikke endelig: ${s.unresolvedRules.joinToString("; ") { rule ->
                    presentationRuleRateSet(s, rule.id)?.let { resolved -> plainRuleTitle(rule, resolved) } ?: rule.title
                }}",
                extra,
            ))
        }

        if (includeFooter) {
            w.rule()
            w.smallText(shortFooterDescription(s.rosterComparisonMode))
            val tariffFooter = if (s.hasMultipleTariffContexts) {
                "${s.tariffContexts.size} tariff-/lønnskontekster"
            } else {
                rateSet.weekendRate(s.weekendProfile).label
            }
            w.footerMeta("Lønnstrinn ${s.salaryStep} · ${weeklyBasisLabel(s.weeklyBasis)} full arbeidsuke · $tariffFooter")
            w.footerMeta("Beregning-ID ${s.id} · opprettet ${dateTime(s.createdAt)}")
        }
    }

    private fun writeRosterAndPlan(w: PdfWriter, s: FinalizedTripSnapshot) {
        val calculation = s.presentation
        w.h1("Arbeidsgrunnlaget")
        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER && s.roster.isNotEmpty()) {
            w.h2("Grunnturnus")
            w.p(
                when (s.workPlanBasis) {
                    TripWorkPlanBasis.NORMAL_ROSTER_APPLIES ->
                        "Dette er turnusen som var lagret da beregningen ble ferdigstilt. Den brukes som avtalt sammenligningsgrunnlag for arbeid på turen."
                    TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN ->
                        "Dette er ordinær grunnturnus som var lagret da beregningen ble ferdigstilt. Arbeidsgivers egen plan for turen er sammenligningsgrunnlaget for arbeid utover avtalt tid."
                    TripWorkPlanBasis.NOT_CLARIFIED ->
                        "Dette er grunnturnusen som var lagret i den eldre ferdigstillingen. Eksakt planbasis ble ikke lagret, og Ferietur gjetter derfor ikke hvilken plan som var sammenligningsgrunnlaget."
                },
            )
            s.roster.forEach { row ->
                val time = if (row.start == null || row.end == null) {
                    "Fri"
                } else {
                    "${clock(row.start)}-${clock(row.end)}${if (row.end.toLocalDate() != row.start.toLocalDate()) " neste dag" else ""}"
                }
                w.compactRow(date(row.date), "${row.code} ${row.label}", time)
            }
            if (s.workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES) {
                val restingInside = (calculation.restingNightMinutes - calculation.restingNightOutsideRosterMinutes).coerceAtLeast(0)
                w.summaryLine("Grunnturnustid som overlapper turen", minutes(calculation.rosterMinutes))
                w.summaryLine("Registrert aktivt arbeid/reise innen turnusen", minutes(calculation.activeInsideRosterMinutes))
                if (restingInside > 0) w.summaryLine("Registrert hvilende nattevakt innen turnusen", minutes(restingInside))
                if (calculation.rosterUncoveredMinutes > 0) {
                    w.summaryLine("Turnustid uten registrert arbeidsperiode på turen", "${minutes(calculation.rosterUncoveredMinutes)} · kontrollert")
                    calculation.rosterUncoveredEvidence.forEach { evidence ->
                        val period = "${date(evidence.start.toLocalDate())} kl. ${clock(evidence.start)}-${clock(evidence.end)}"
                        w.compactRow(period, "Ingen registrert arbeidsperiode", "Kontrollert")
                    }
                }
                if (calculation.alreadyCoveredByNormalRosterAmount > BigDecimal.ZERO) {
                    w.summaryLine("Turnustillegg fra grunnturnusen - ikke med i betalingsgrunnlaget", money(calculation.alreadyCoveredByNormalRosterAmount))
                }
            }
            w.space(5)
        } else {
            w.h2("Grunnturnus")
            w.p("Grunnturnusen er ikke brukt som sammenligningsgrunnlag i denne beregningen.")
        }

        if (
            s.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN &&
            s.employerWorkPlanBlocks.isNotEmpty()
        ) {
            w.h2("Arbeidsgivers arbeidsplan")
            w.p("Dette er planen arbeidsgiver hadde fastsatt for turen da beregningen ble ferdigstilt.")
            s.employerWorkPlanBlocks.groupBy { it.start.toLocalDate() }.toSortedMap().forEach { (day, blocks) ->
                w.keepTogether(24f + blocks.size * 20f)
                w.dayHeader(date(day))
                blocks.sortedBy { it.start }.forEach { block ->
                    val period = "${clock(block.start)}-${clock(block.end)}${if (block.end.toLocalDate() != block.start.toLocalDate()) " neste dag" else ""}"
                    w.compactRow(period, workBlockKind(block), "")
                }
            }
            w.space(5)
        }

        w.h2("Arbeid på turen")
        w.p("Dette er det registrerte arbeidet som beregningen bygger på.")
        s.workBlocks.groupBy { it.start.toLocalDate() }.toSortedMap().forEach { (day, blocks) ->
            w.keepTogether(24f + blocks.size * 20f)
            w.dayHeader(date(day))
            blocks.sortedBy { it.start }.forEach { block ->
                val period = "${clock(block.start)}-${clock(block.end)}${if (block.end.toLocalDate() != block.start.toLocalDate()) " neste dag" else ""}"
                w.compactRow(period, workBlockKind(block), "")
            }
        }
    }

    private fun writeCalculationDetails(w: PdfWriter, s: FinalizedTripSnapshot, rateSet: TariffRateSet) {
        val calculation = s.presentation
        w.h1("Hvorfor blir beløpet slik?")
        w.p("Hver post under viser hva som er beregnet, hvordan beløpet er regnet og hvilken regel som er brukt.")

        calculation.lineEntries
            .filter { it.line.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS }
            .forEach { entry -> writeDetailedLine(w, s, entry, rateSet, alreadyCovered = false) }

        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            when (s.workPlanBasis) {
                TripWorkPlanBasis.NORMAL_ROSTER_APPLIES -> {
                    w.h2("Grunnturnus - forutsetning i beregningen")
                    w.p("Grunnturnusen brukes her som appens sammenligningsgrunnlag for hva som er forutsatt dekket gjennom ordinær lønn og turnustillegg. Dette er en modellforutsetning, ikke en gjengivelse av ordlyden i Dok. 25 punkt 20.2. Punkt 20.2 omtaler egen arbeidsplan før reisen, gjennomsnittsberegning og kompensasjon for arbeidstid ut over ordinær arbeidstid etter kapittel 8.")
                    w.summaryLine("Grunnturnustid som overlapper turen", minutes(calculation.rosterMinutes))
                    if (calculation.alreadyCoveredByNormalRosterAmount > BigDecimal.ZERO) {
                        w.summaryLine("Turnustillegg beregnet fra grunnturnusen", money(calculation.alreadyCoveredByNormalRosterAmount))
                    }
                }
                TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN -> {
                    w.h2("Arbeidsgivers plan - forutsetning i beregningen")
                    w.p("Arbeidsgiver har fastsatt en egen arbeidsplan for turen. Ferietur bruker den frosne planen som sammenligningsgrunnlag for arbeid utover avtalt tid. Grunnturnusen dokumenteres separat som ordinær turnus.")
                    w.summaryLine("Planstatus", holidayWorkPlanStatusLabelForPdf(s.holidayWorkPlanStatus))
                }
                TripWorkPlanBasis.NOT_CLARIFIED -> {
                    w.h2("Planbasis - eldre ferdigstilling")
                    w.p("Denne ferdigstillingen inneholder ikke eksplisitt planbasis. Ferietur gjetter derfor ikke om grunnturnusen eller en egen arbeidsgiverplan var sammenligningsgrunnlaget.")
                }
            }
        }

        val openLines = calculation.lineEntries
            .filter { it.line.paymentTreatment == PaymentTreatment.OPEN }
            .filter { shouldRenderDetailedLine(it.line) }
        if (openLines.isNotEmpty()) {
            val first = openLines.first()
            w.keepH2WithFirstDetailBlock(
                heading = "Beløp eller regler som må avklares",
                title = presentationLineTitle(s, first),
                formula = plainFormula(first.line),
                explanation = presentationLineExplanation(s, first, rateSet),
                source = first.line.source,
            )
            w.h2("Beløp eller regler som må avklares")
            openLines.forEach { entry -> writeDetailedLine(w, s, entry, rateSet, alreadyCovered = false) }
        }
    }

    private fun shouldRenderDetailedLine(line: CalculationLine): Boolean =
        !(line.amount == BigDecimal.ZERO && line.paymentTreatment == PaymentTreatment.OPEN && line.certainty != CalculationCertainty.OPEN)

    private fun writeDetailedLine(
        w: PdfWriter,
        snapshot: FinalizedTripSnapshot,
        entry: FinalizedCalculationPresentationLine,
        fallbackRateSet: TariffRateSet,
        alreadyCovered: Boolean,
    ) {
        val line = entry.line
        if (!shouldRenderDetailedLine(line)) return
        val amountLabel = when {
            line.paymentTreatment == PaymentTreatment.OPEN && line.amount > BigDecimal.ZERO -> "Mulig ${money(line.amount)}"
            line.paymentTreatment == PaymentTreatment.OPEN -> "Må avklares"
            alreadyCovered -> "${money(line.amount)} · grunnturnus · ikke med"
            else -> money(line.amount)
        }
        w.detailBlock(
            title = presentationLineTitle(snapshot, entry),
            amount = amountLabel,
            formula = plainFormula(line),
            explanation = presentationLineExplanation(snapshot, entry, fallbackRateSet),
            source = line.source,
            warning = line.paymentTreatment == PaymentTreatment.OPEN,
        )
    }

    private fun writeDayAudit(w: PdfWriter, s: FinalizedTripSnapshot) {
        val calculation = s.presentation
        w.h1("Dag for dag")
        w.p("Her kan du kontrollere hva hver kalenderdag bidrar med. Summen til høyre er beløpet som kommer i tillegg den dagen. Grunnturnusen er dokumentert i arbeidsgrunnlaget og gjentas ikke her.")
        calculation.dayAudits.forEach { day ->
            val holidayText = day.holidayLabels.takeIf { it.isNotEmpty() }?.let { "Høytidsperiode: ${it.joinToString()}" }
            val visible = day.contributions.filter { it.paymentTreatment != PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER }
            val rows = visible.map { c ->
                val minutesText = if (c.minutes > 0) minutes(c.minutes) else ""
                val value = when {
                    c.paymentTreatment == PaymentTreatment.OPEN && c.amount > BigDecimal.ZERO -> "Mulig ${money(c.amount)}"
                    c.paymentTreatment == PaymentTreatment.OPEN -> "Må avklares"
                    else -> money(c.amount)
                }
                Triple(plainContributionTitle(c.lineId, c.title), minutesText, value)
            }
            val emptyText = if (visible.isEmpty()) "Ingen egne betalingsposter denne dagen." else null
            val warningText = day.openSubtotal.takeIf { it > BigDecimal.ZERO }?.let { "Mulig tillegg som ikke er med ennå: ${money(it)}" }
            w.keepDayAuditTogether(holidayText, rows, emptyText, warningText)
            w.dayTotalHeader(date(day.date), day.paymentSubtotal)
            holidayText?.let(w::smallText)
            emptyText?.let(w::smallText)
            rows.forEach { (title, minutesText, value) -> w.compactRow(title, minutesText, value) }
            warningText?.let(w::warningLine)
        }

        val stayAllowance = calculation.lines.firstOrNull {
            it.id == "stay-allowance" && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS
        }?.amount ?: BigDecimal.ZERO.setScale(2)
        val distributed = calculation.dayAudits.fold(BigDecimal.ZERO) { acc, day -> acc.add(day.paymentSubtotal) }.setScale(2)
        w.keepTogether(66f)
        w.rule()
        w.summaryLine("Sum fordelt på kalenderdager", money(distributed), strong = true)
        if (stayAllowance > BigDecimal.ZERO) {
            w.summaryLine("Døgngodtgjøring - gjelder hele reisen", money(stayAllowance))
        }
        w.summaryLine("Totalt betalingsgrunnlag", money(calculation.paymentBasisAmount), strong = true)
        w.smallText("Dagsbeløp fordeles til øre slik at de summerer tilbake til hovedpostene. Små avrundingsforskjeller kan derfor forekomme på enkeltdager.")
    }

    private fun writeGroupedControl(
        w: PdfWriter,
        s: FinalizedTripSnapshot,
        rateSet: TariffRateSet,
        includeDetails: Boolean,
    ) {
        if (includeDetails) {
            // PILOT01-003: give the human-facing work-time audit its own
            // document surface instead of continuing a dense bullet list.
            w.pageBreak()
        }

        w.h1("Arbeidstid som bør vurderes")
        w.p(
            "Tallene under viser sammenhengende arbeidstid og tid mellom " +
                "arbeidsperioder. Når en arbeidsperiode består av flere " +
                "registrerte tidstyper, vises de hver for seg.",
        )
        w.compactNote(
            "Hvilende nattevakt og arbeidstid",
            "Hvilende nattevakt regnes som arbeidstid når arbeidstiden " +
                "kontrolleres, selv om betalingen beregnes annerledes.",
        )

        val review = s.findings.filter {
            it.severity == FindingSeverity.REVIEW ||
                it.severity == FindingSeverity.CRITICAL
        }

        if (review.isEmpty()) {
            w.statusRow(
                "Arbeidstid",
                "Ingen forhold markert",
                PdfTone.OK,
            )
        } else {
            val groups = review.groupBy { it.title }

            groups.forEach { (title, findings) ->
                w.controlSummaryRow(
                    plainFindingTitle(title),
                    findingMetric(title, findings),
                    findings.size,
                )
            }

            if (includeDetails) {
                val shortRest =
                    groups["Kort hvile mellom arbeidsperioder"].orEmpty()
                val longPeriods =
                    groups["Lang sammenhengende arbeidsperiode"].orEmpty()

                if (shortRest.isNotEmpty()) {
                    w.space(5)
                    w.worktimeSectionTitle(
                        "Kort tid mellom arbeidsperioder",
                        PdfTone.WARNING,
                    )
                    w.worktimeRestCard(
                        shortRest.map(::compactFindingDetail),
                    )
                }

                if (longPeriods.isNotEmpty()) {
                    w.space(7)
                    w.worktimeSectionTitle(
                        "Lange arbeidsperioder",
                        PdfTone.INFO,
                    )

                    val visualPeriods =
                        longWorktimePeriodsForPdf(s.workBlocks)

                    if (visualPeriods.size == longPeriods.size) {
                        w.worktimePeriodGrid(visualPeriods)
                    } else {
                        // Fail closed for presentation: special travel/roster
                        // combinations retain the already-qualified textual
                        // audit rather than being visualized incorrectly.
                        val details =
                            longPeriods.map(::compactFindingDetail)
                        w.keepControlGroupTogether(
                            "Lange arbeidsperioder",
                            details,
                        )
                        details.forEach(w::compactControlDetail)
                    }
                }

                val otherGroups = groups.filterKeys { title ->
                    title != "Kort hvile mellom arbeidsperioder" &&
                        title != "Lang sammenhengende arbeidsperiode"
                }

                if (otherGroups.isNotEmpty()) {
                    w.space(6)
                    w.subheading("Andre forhold")
                    otherGroups.forEach { (title, findings) ->
                        val heading = plainFindingTitle(title)
                        val details =
                            findings.map(::compactFindingDetail)

                        w.keepControlGroupTogether(
                            heading,
                            details,
                        )
                        w.smallText(heading)
                        details.forEach(w::compactControlDetail)
                        w.space(2)
                    }
                }
            }
        }

        w.footerMeta(
            "Kontrollgrunnlag: arbeidsmiljøloven kapittel 10 og Dok. 25 " +
                "punkt 20.2. Den konkrete arbeidstidsordningen kan avhenge " +
                "av gjennomsnittsberegning og lokale avtaler.",
        )
        w.space(4)
        val workingInterpretationRules =
            app.ferietur.domain.FerieturRules
                .applicableWorkingInterpretationRules(
                    s.presentation.lines,
                )

        w.h2("Regler som fortsatt må avklares")
        if (s.unresolvedRules.isEmpty()) {
            w.statusRow("Beregning", "Ingen kjente åpne regler som treffer denne turen", PdfTone.OK)
        } else {
            s.unresolvedRules.forEach { rule ->
                val possible = possibleAmountForRule(s, rule.id)
                val ruleRateSet = presentationRuleRateSet(s, rule.id)
                val explanation = buildString {
                    append(
                        if (ruleRateSet != null) {
                            plainRuleExplanation(rule, ruleRateSet)
                        } else {
                            multiContextRuleExplanation(rule)
                        },
                    )
                    if (possible > BigDecimal.ZERO) append(" Med dagens registrerte timer og satser er mulig tillegg ${money(possible)}. Beløpet er ikke inkludert i betalingsgrunnlaget.")
                }
                val title = if (ruleRateSet != null) plainRuleTitle(rule, ruleRateSet) else rule.title
                w.openRule(title, explanation, rule.source)
            }
        }

        if (workingInterpretationRules.isNotEmpty()) {
            w.space(4)
            w.h2("Arbeidsfortolkninger som fortsatt avklares")

            workingInterpretationRules.forEach { rule ->
                val affectedAmount =
                    when (rule.id) {
                        "D25_8_9_X20" ->
                            s.presentation.lines
                                .filter { line ->
                                    line.id in setOf(
                                        "resting-evening-night",
                                        "resting-weekend",
                                        "resting-holiday",
                                        "travel-passive-evening-night",
                                        "travel-passive-weekend",
                                        "travel-passive-holiday",
                                    )
                                }
                                .fold(BigDecimal.ZERO) { total, line ->
                                    total.add(line.amount)
                                }

                        else -> BigDecimal.ZERO
                    }

                val explanation =
                    when (rule.id) {
                        "D25_8_9_X20" ->
                            "Ferietur bruker som arbeidsfortolkning at tillegg etter kapittel 12 under arbeid av passiv karakter beregnes i forholdet som følger av punkt 8.9. Spørsmålet om denne anvendelsen ved arbeid etter punkt 20.3 og 20.4 behandles ikke som endelig avklart i appen. De berørte postene utgjør ${money(affectedAmount)} i denne beregningen og er allerede inkludert i betalingsgrunnlaget."

                        else ->
                            "Ferietur bruker denne fortolkningen i det beregnede grunnlaget, men presenterer den ikke som endelig avklart."
                    }

                w.openRule(
                    rule.title,
                    explanation,
                    rule.source,
                )
            }
        }
    }

    private fun writeSources(w: PdfWriter, s: FinalizedTripSnapshot, rateSet: TariffRateSet) {
        w.h2("Grunnlaget som er brukt")
        w.p("Opplysningene under er fryst sammen med beregningen, slik at den kan kontrolleres senere.")
        w.summaryLine("Arbeidsgiver", employerLabel(s.employerKind))
        w.summaryLine("Betalingsscenario", payingPartyLabel(s.payingParty))
        w.smallText(PAYMENT_SCENARIO_DISCLAIMER)
        w.summaryLine("Regler appen bruker", s.ruleBasis)
        // TIME01: make the wall-clock assumption visible to whoever checks the document.
        w.summaryLine("Klokkeslett", "Registrert som norsk tid. Tidssoner og sommertidsskifte justeres ikke automatisk.")
        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            w.summaryLine("Planbasis", workPlanBasisLabelForPdf(s.workPlanBasis))
            if (s.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
                w.summaryLine(
                    "Arbeidsgivers planstatus",
                    holidayWorkPlanStatusLabelForPdf(s.holidayWorkPlanStatus),
                )
            }
        }

        if (s.tariffContexts.size == 1) {
            val context = s.tariffContexts.single()
            // Preserve the qualified single-context PDF ordering exactly.
            val salaryTableName =
                context.salaryTableSourceLabel
                    .removePrefix("Lønnstabell ")
                    .substringBeforeLast(" fra ")
            w.summaryLine(
                "Lønnstabell",
                "$salaryTableName · fra ${date(context.salaryTableEffectiveFrom)}",
            )
            w.summaryLine("Lønnstrinn", s.salaryStep.toString())
            w.summaryLine("Årslønn", money(context.annualSalary))
            w.summaryLine("Full arbeidsuke", weeklyBasisLabel(s.weeklyBasis))
            w.summaryLine("Lørdags- og søndagssats", rateSet.weekendRate(s.weekendProfile).label)
            w.summaryLine("Kontrollert mot lønnsslipp", if (s.payslipChecked) "Ja" else "Nei")
        } else {
            w.summaryLine("Lønnstrinn", s.salaryStep.toString())
            w.summaryLine("Full arbeidsuke", weeklyBasisLabel(s.weeklyBasis))
            w.summaryLine("Kontrollert mot lønnsslipp", if (s.payslipChecked) "Ja" else "Nei")
            w.h2("Tariff- og lønnskontekster")
            w.p("Turen krysser en virkningsdato. Hver periode under er beregnet med sitt eget fryste sats- og lønnsgrunnlag.")
            s.tariffContexts.forEachIndexed { index, context ->
                val contextRateSet = FerieturTariffRates.forId(context.tariffRateSetId)
                val period = if (context.start == context.end) date(context.start) else "${date(context.start)}–${date(context.end)}"
                w.compactRow(
                    "Periode ${index + 1}: $period",
                    context.salaryTableSourceLabel,
                    "${money(context.annualSalary)} · ${money(context.hourlyRate)}/t",
                )
                contextRateSet?.let { resolved ->
                    w.smallText("Lørdags-/søndagssats: ${resolved.weekendRate(s.weekendProfile).label}")
                }
                w.footerMeta(
                    "Tariffpakke-ID: ${context.tariffPackageId} · satssett-ID: ${context.tariffRateSetId} · " +
                        "lønnstabell-ID: ${context.salaryTableId}",
                )
            }
        }

        w.rule()
        w.footerMeta("Regelversjon: FERIETUR01 ${s.rulesetVersion} · appversjon: ${s.appVersionName} · build ${s.appVersionCode}")
        if (s.tariffContexts.size == 1) {
            w.footerMeta("Tariffpakke-ID: ${s.tariffPackageId} · satssett-ID: ${s.tariffRateSetId}")
            w.footerMeta("Lønnstabell-ID: ${s.salaryTableId} · gyldig fra ${date(s.salaryTableEffectiveFrom)}")
        } else {
            w.footerMeta("Tariffkontekster: ${s.tariffContexts.size} · se periodene over for sats- og lønnstabell-ID-er")
        }
        w.finalFooterMeta("Opprettet: ${dateTime(s.createdAt)} · beregning-ID: ${s.id}")
    }

    internal fun workPlanBasisLabelForPdf(value: TripWorkPlanBasis): String = when (value) {
        TripWorkPlanBasis.NORMAL_ROSTER_APPLIES -> "Vanlig grunnturnus gjelder"
        TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN -> "Arbeidsgiver har fastsatt egen plan"
        TripWorkPlanBasis.NOT_CLARIFIED -> "Ikke lagret i eldre ferdigstilling"
    }

    internal fun holidayWorkPlanStatusLabelForPdf(value: HolidayWorkPlanStatus): String = when (value) {
        HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED -> "Godkjent · minst 14 dagers varsel"
        HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE -> "Ikke godkjent / kortere varsel"
        HolidayWorkPlanStatus.NOT_CLARIFIED -> "Ikke avklart"
    }

    private fun employerLabel(value: EmployerKind): String = when (value) {
        EmployerKind.OSLO_KOMMUNE -> "Oslo kommune"
        EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED -> "Privat/annet - må avklares"
        EmployerKind.UNSPECIFIED -> "Ikke avklart ennå"
    }

    private fun payingPartyLabel(value: PayingParty): String = when (value) {
        PayingParty.OSLO_KOMMUNE -> "Oslo kommune"
        PayingParty.RESIDENT_OR_GUARDIAN -> "Beboer/verge"
        PayingParty.OTHER -> "Annet"
        PayingParty.UNSPECIFIED -> "Ikke avklart ennå"
    }

    private fun presentationLineTitle(
        snapshot: FinalizedTripSnapshot,
        entry: FinalizedCalculationPresentationLine,
    ): String {
        val base = plainLineTitle(entry.line)
        if (!snapshot.hasMultipleTariffContexts || entry.scope != TariffCalculationLineScope.SEGMENT_LOCAL) {
            return base
        }
        val workPeriod = evidenceDateLabelForPdf(entry.line.evidence) ?: return base
        return "$base · $workPeriod"
    }

    /**
     * Arbeidsdato kommer fra det fryste beregningsevidenset.
     * Tariff-/lønnskontekstens datoer beskriver prisgrunnlaget, ikke
     * nødvendigvis når arbeid faktisk ble utført.
     *
     * Evidensintervaller er halvåpne: et intervall som slutter nøyaktig
     * kl. 00:00 opptar ikke den påfølgende kalenderdagen.
     */
    internal fun evidenceDateLabelForPdf(
        evidence: List<CalculationEvidence>,
    ): String? {
        val occupiedRanges = evidence.mapNotNull { item ->
            if (!item.end.isAfter(item.start)) return@mapNotNull null
            item.start.toLocalDate() to item.end.minusNanos(1).toLocalDate()
        }

        if (occupiedRanges.isEmpty()) return null

        val first = occupiedRanges.minOf { it.first }
        val last = occupiedRanges.maxOf { it.second }

        return if (first == last) {
            date(first)
        } else {
            "${date(first)}–${date(last)}"
        }
    }

    private fun presentationLineExplanation(
        snapshot: FinalizedTripSnapshot,
        entry: FinalizedCalculationPresentationLine,
        fallbackRateSet: TariffRateSet,
    ): String {
        val resolved = snapshot.presentation.rateSetForLine(entry)
        val base = when {
            resolved != null -> plainLineExplanation(entry.line, resolved)
            snapshot.hasMultipleTariffContexts && entry.line.explanation.isNotBlank() -> entry.line.explanation
            else -> plainLineExplanation(entry.line, fallbackRateSet)
        }
            return if (
            snapshot.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER &&
            snapshot.workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES &&
            entry.line.id == "active" &&
            entry.line.title.contains("utenfor grunnturnusen")
        ) {
            base.replace(
                "Hvilken arbeidsplan og eventuell gjennomsnittsberegning som gjelder for ferieoppholdet må avklares med arbeidsgiver.",
                "I denne ferdigstillingen er vanlig grunnturnus registrert som gjeldende planbasis. Eventuell gjennomsnittsberegning eller annen arbeidstidsordning må vurderes særskilt med arbeidsgiver.",
            )
        } else {
            base
        }
}

    private fun presentationRuleRateSet(snapshot: FinalizedTripSnapshot, ruleId: String): TariffRateSet? {
        if (!snapshot.hasMultipleTariffContexts) return FerieturTariffRates.forId(snapshot.tariffRateSetId)
        val lineIds: Set<String> = when (ruleId) {
            "D25_18_4_NOTICE" -> setOf("travel-notice-open")
            "D25_18_4_X13_7_3" -> setOf("travel-short-notice-133-open")
            "D25_20_3_SLEEP_PERMISSION" -> setOf("travel-night-sleep-open")
            "D25_20_6_EXACT_THRESHOLD" -> setOf("stay-allowance-exact-threshold-open")
            else -> emptySet()
        }
        val relevant = snapshot.presentation.lineEntries.filter { it.line.id in lineIds }
        val rateSetIds = relevant.mapNotNull { entry ->
            snapshot.presentation.contextForLine(entry)?.tariffRateSetId
        }.distinct()
        if (rateSetIds.size == 1) return FerieturTariffRates.forId(rateSetIds.single())

        // Whole-trip rules may intentionally have no single presentation slice.
        // A4A4 already requires their numeric policy to be identical before a
        // segmented monetary result is allowed, so the shared control rate set
        // is safe when one exists.
        return FinalizedCalculationPresentations.sharedControlRateSet(snapshot.tariffContexts)
    }

    private fun multiContextRuleExplanation(rule: DomainRule): String = when (rule.id) {
        "D25_20_3_SLEEP_PERMISSION" ->
            "Søvntillatelse må avklares for de registrerte nattreiseperiodene. Se beregningspostene for " +
                "tidsvindu og satsgrunnlag i hver tariffkontekst."
        "D25_18_4_NOTICE" ->
            "Varseltidspunktet for reisen må avklares. Se beregningspostene for hvilket satsgrunnlag som " +
                "gjelder i hver tariffkontekst."
        "D25_18_4_X13_7_3" ->
            "Den særskilte overtidsprosenten må avklares mot arbeidstakerens tariffstatus. Ferietur bruker " +
                "ikke én felles prosenttekst når turen har flere tariffkontekster."
        "D25_20_6_EXACT_THRESHOLD" ->
            "Nøyaktig resttid på terskelen for døgngodtgjøring er fortsatt et åpent tolkningspunkt. " +
                "Mulig beløp vises separat og er ikke lagt inn i betalingsgrunnlaget."
        else -> "Denne regelen må avklares før beregningen kan regnes som komplett."
    }

    private fun plainLineTitle(line: CalculationLine): String = when (line.id) {
        "active" -> line.title
        "resting-night" -> "Hvilende nattevakt"
        "resting-evening-night" -> "Kveld- og nattillegg på hvilende nattevakt"
        "resting-weekend" -> "Lørdags- og søndagstillegg på hvilende nattevakt"
        "resting-holiday" -> "Høytidstillegg på hvilende nattevakt"
        "evening-night" -> "Kveld- og nattillegg"
        "weekend" -> "Lørdags- og søndagstillegg"
        "holiday" -> "Helge- og høytidstillegg"
        "stay-allowance" -> "Døgngodtgjøring for ferieopphold"
        "active-on-resting" -> "Aktivt arbeid under hvilende nattevakt"
        "travel-without-responsibility" -> "Reisetid uten tilsynsansvar"
        "travel-notice-open" -> "Når du fikk vite om reisen må avklares"
        "travel-short-notice-overtime" -> "Overtidstillegg ved kort varsel om reisen"
        "travel-short-notice-133-open" -> "Særskilt overtidsprosent på helge-/høytidsdag må avklares"
        "travel-passive-night" -> "Passiv nattreise med søvntillatelse"
        "travel-passive-evening-night" -> "Kveld- og nattillegg under passiv nattreise"
        "travel-passive-weekend" -> "Lørdags- og søndagstillegg under passiv nattreise"
        "travel-passive-holiday" -> "Helge- og høytidstillegg under passiv nattreise"
        "travel-night-sleep-open" -> "Søvntillatelse under nattreisen må avklares"
        "travel-responsibility-open" -> "Ansvar under reisen er ikke avklart"
        else -> line.title
    }

    private fun plainContributionTitle(lineId: String, fallback: String): String = when (lineId) {
        "active" -> fallback
        "resting-night" -> "Hvilende nattevakt"
        "resting-evening-night" -> "Kveld/natt på hvilende vakt"
        "resting-weekend" -> "Helgetillegg på hvilende vakt"
        "resting-holiday" -> "Høytidstillegg på hvilende vakt"
        "evening-night" -> "Kveld- og nattillegg"
        "weekend" -> "Lørdags- og søndagstillegg"
        "holiday" -> "Helge- og høytidstillegg"
        "active-on-resting" -> "Aktivt arbeid under hvilende vakt"
        "travel-without-responsibility" -> "Reise uten tilsynsansvar"
        "travel-notice-open" -> "Varseltidspunkt for reisen"
        "travel-short-notice-overtime" -> "Overtidstillegg ved kort varsel"
        "travel-short-notice-133-open" -> "Mulig særskilt overtidsprosent"
        "travel-passive-night" -> "Passiv nattreise"
        "travel-passive-evening-night" -> "Kveld/natt på passiv nattreise"
        "travel-passive-weekend" -> "Helgetillegg på passiv nattreise"
        "travel-passive-holiday" -> "Høytidstillegg på passiv nattreise"
        "travel-night-sleep-open" -> "Søvntillatelse under nattreise"
        else -> fallback
    }

    internal fun plainFormula(line: CalculationLine): String {
        val normalized = line.detail
            .replace("lønnsekvivalent", "tid det beregnes lønn av")
            .replace("×", "x")
            .replace("⅓", "1/3")
        return normalized.replace(
            Regex("""([\d\s,.]+) kr · (\d+) % · min\. ([\d\s,.]+) kr/t$"""),
        ) { match ->
            " ${match.groupValues[1].trim()} kr/t (${match.groupValues[2]} % av timelønn, minst ${match.groupValues[3].trim()} kr/t)"
        }
    }

    internal fun plainLineExplanation(line: CalculationLine, rateSet: TariffRateSet = FerieturTariffRates.current): String = when (line.id) {
        "active" -> if (line.title.contains("utenfor grunnturnusen")) {
            buildString {
                append("I denne beregningsmodellen brukes grunnturnusen som sammenligningsgrunnlag for arbeid som er forutsatt dekket gjennom ordinær lønn. Timer modellen klassifiserer som arbeid i tillegg til grunnturnusen beregnes her med timelønn pluss ${pdfPercent(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent. Dok. 25 punkt 20.2 fastsetter at arbeidstid ut over ordinær arbeidstid etter kapittel 8 kompenseres med timelønn pluss ${pdfPercent(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent.")
                if (line.source.contains("20.3")) append(" Reise med ansvar for beboeren er med i disse timene fordi reisetid med aktivt tilsyn regnes som arbeidstid etter punkt 20.3.")
                append(" Hvilken arbeidsplan og eventuell gjennomsnittsberegning som gjelder for ferieoppholdet må avklares med arbeidsgiver. På de samme minuttene som modellen behandler etter punkt 20.2, legger appen ikke til kveld-/nattillegg eller lørdags-/søndagstillegg fra kapittel 12. Punkt 12.1.1 og 12.2.2 gjelder ordinær tjeneste og utelukker overtid. På særskilte høytidsdager bruker appen punkt 20.2 som den spesifikke ferieoppholdsregelen; punkt 13.1 sier at kapittel 13 gjelder dersom ikke annet er fastsatt i tariffavtalen.")
            }
        } else {
            line.explanation
        }
        "resting-night" -> "Hele vakten teller som arbeidstid. Betalingen beregnes i forholdet ${pdfPassiveRatio(rateSet)}."
        "resting-evening-night" -> "Kveld- og nattillegget beregnes også for ${pdfPassiveShare(rateSet)} av den hvilende tiden."
        "resting-weekend" -> "Lørdags- og søndagstillegget på en hvilende vakt beregnes for ${pdfPassiveShare(rateSet)} av den hvilende tiden. Timer med høyere høytidstillegg tas ikke med her."
        "resting-holiday" -> "Når en hvilende vakt ligger i en høytidsperiode, beregnes høytidstillegget også for ${pdfPassiveShare(rateSet)} av tiden."
        "evening-night" -> "For relevant turnus er tillegget ${pdfPercent(rateSet.eveningNightFraction)} prosent for ordinært arbeid mellom kl. ${pdfClock(rateSet.eveningStart)} og ${pdfClock(rateSet.nightEnd)}. For nattevakt kan tillegget fortsette til vakten slutter, senest kl. ${pdfClock(rateSet.nightWatchSupplementEnd)}."
        "weekend" -> "Dette er tillegget for ordinært arbeid fra lørdag kl. 00:00 til søndag kl. 24:00. Appen bruker satsen som er kontrollert mot lønnsslippen."
        "holiday" -> "Dette er tillegget for ordinært arbeid i helge- og høytidsperiodene som Dok. 25 lister opp. Satsen i regnestykket er selve tillegget per time."
        "stay-allowance" -> "Ved ferieopphold som omfattes av kapittel 20 gis ${pdfDecimal(rateSet.stayAllowancePerDay)} kroner per døgn i tillegg til lønnen. Et påbegynt døgn teller når resttiden er mer enn ${pdfDurationWords(rateSet.stayAllowanceRemainderThresholdMinutes)}."
        "active-on-resting" -> "Aktiv tid under en hvilende nattevakt summeres per vakt og rundes til nærmeste ${pdfRoundingUnitDefinite(rateSet.activeNightRoundingStepMinutes.toLong())}. Den avrundede tiden betales med timelønn pluss ${pdfPercent(rateSet.chapter20ActiveMultiplier.subtract(BigDecimal.ONE))} prosent."
        "travel-without-responsibility" -> "Reisetid uten tilsynsansvar beregnes etter punkt 18.4. Utenfor ordinær arbeidstid godtgjøres den ordinære reisetiden med ordinær timelønn. Varseltidspunktet registreres på reiseperioden. Hvis reisen ikke var kjent senest dagen i forveien, står ordinær reisetidsbetaling fortsatt på denne linjen, mens overtidsdelen for inntil ${pdfDurationWords(rateSet.shortNoticeMaxMinutes)} vises separat."
        "travel-notice-open" -> "Ordinær reisetidsbetaling er allerede med. Det må avklares om reisen var kjent senest dagen i forveien, fordi punkt 18.4 kan gi overtidsbetaling for inntil ${pdfDurationWords(rateSet.shortNoticeMaxMinutes)} reisetid som kreves utført utenfor ordinær arbeidstid. Beløpet på den åpne posten er et mulig tillegg og er ikke inkludert i betalingsgrunnlaget."
        "travel-short-notice-overtime" -> "Reisen er registrert som ikke kjent senest dagen i forveien. Punkt 18.4 gir da overtidsbetaling for inntil ${pdfDurationWords(rateSet.shortNoticeMaxMinutes)} av reisetiden utenfor ordinær arbeidstid. Den ordinære timelønnen står på reisetidslinjen; denne posten er bare overtidsdelen i tillegg. Beregningsmodellen bruker de første inntil ${pdfDurationWords(rateSet.shortNoticeMaxMinutes)} med kortvarslet ordinær reisetid i turen og avrunder overtidsdelen til påbegynt ${pdfRoundingUnitIndefinite(rateSet.overtimeRoundingStepMinutes)} etter punkt 13.3."
        "travel-short-notice-133-open" -> "Punkt 13.7.3 kan gi ${pdfSpecialOvertimePercent(rateSet)} prosent overtidstillegg på særskilt opplistede dager for arbeidstakere som har ordinær tjeneste på søn- og helgedager. Appen kan ikke fastslå denne personlige tariffstatusen bare fra turen. Bekreftet overtidsbetaling etter de øvrige overtidsreglene er allerede med; beløpet her viser bare mulig differanse dersom punkt 13.7.3 gjelder."
        "travel-passive-night" -> "For nattreise mellom kl. ${pdfClock(rateSet.travelSleepWindowStart)} og ${pdfClock(rateSet.travelSleepWindowEnd)} der arbeidstakeren hadde tillatelse til å sove, regnes tiden som arbeid av passiv karakter. Tiden teller som arbeidstid time for time, mens grunnbetalingen er ${pdfPassiveFraction(rateSet)} timelønn per time."
        "travel-passive-evening-night" -> "Kveld- og nattillegg under passiv nattreise betales i forholdet ${pdfPassiveRatio(rateSet)}. For reise brukes ordinære tidsgrenser for kveld/natt; perioden behandles ikke som nattevakt."
        "travel-passive-weekend" -> "Lørdags- og søndagstillegg under passiv nattreise betales i forholdet ${pdfPassiveRatio(rateSet)}. Timer med høyere helge- og høytidstillegg tas ikke med her."
        "travel-passive-holiday" -> "Helge- og høytidstillegg under passiv nattreise betales i forholdet ${pdfPassiveRatio(rateSet)}."
        "travel-night-sleep-open" -> "Søvntillatelsen er ikke avklart for nattreisen mellom kl. ${pdfClock(rateSet.travelSleepWindowStart)} og ${pdfClock(rateSet.travelSleepWindowEnd)}. Nattdelen er derfor ikke lagt til betalingsgrunnlaget."
        "travel-responsibility-open" -> "Det er ikke avklart om du hadde ansvar for beboeren under reisen. Derfor er reisetiden ikke ferdig klassifisert i beregningen."
        else -> line.explanation
    }

    private fun plainFindingTitle(title: String): String = when (title) {
        "Mer enn 48 timer i den viste perioden" -> "Mye arbeid i perioden"
        "Mer enn 48 timer i en sju-dagersperiode" -> "Høy arbeidstid på sju dager"
        "Kort hvile mellom arbeidsperioder" -> "Kort tid mellom arbeidsperioder"
        "Lang sammenhengende arbeidsperiode" -> "Lange arbeidsperioder"
        else -> title
    }

    private fun plainFindingSummary(title: String, findings: List<ControlFinding>): String = when (title) {
        "Mer enn 48 timer i den viste perioden" -> "Den registrerte arbeidstiden er høy. Kontroller hvilken arbeidstidsordning som gjelder for turen."
        "Mer enn 48 timer i en sju-dagersperiode" ->
            "Du har registrert mer enn 48 timer arbeid i løpet av sju dager. Dette bør sjekkes nærmere."
        "Kort hvile mellom arbeidsperioder" -> "${findings.size} perioder har kort sammenhengende fri mellom arbeidsperiodene."
        "Lang sammenhengende arbeidsperiode" -> "${findings.size} arbeidsperioder er så lange at de bør vurderes særskilt."
        else -> findings.firstOrNull()?.detail ?: "Bør vurderes."
    }

    private fun findingMetric(title: String, findings: List<ControlFinding>): String {
        fun firstDuration(pattern: Regex): Long? = findings.mapNotNull { finding ->
            pattern.find(finding.detail)?.let { match ->
                val hours = match.groupValues.getOrNull(1)?.toLongOrNull() ?: 0L
                val minutes = match.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
                hours * 60 + minutes
            }
        }.let { values ->
            when (title) {
                "Kort hvile mellom arbeidsperioder" -> values.minOrNull()
                else -> values.maxOrNull()
            }
        }
        val minutes = when (title) {
            "Mer enn 48 timer i den viste perioden" ->
                firstDuration(Regex("Du har registrert (\\d+) t(?: (\\d+) min)?"))
            "Mer enn 48 timer i en sju-dagersperiode" ->
                firstDuration(Regex("Du har registrert (\\d+) t(?: (\\d+) min)? arbeidstid"))
            "Kort hvile mellom arbeidsperioder" ->
                firstDuration(Regex("Du har bare (\\d+) t(?: (\\d+) min)? sammenhengende fri"))
            "Lang sammenhengende arbeidsperiode" ->
                firstDuration(Regex("Arbeidsperioden varer (\\d+) t(?: (\\d+) min)?"))
            else -> null
        }
        return when (title) {
            "Mer enn 48 timer i den viste perioden" ->
                minutes?.let(::minutes)?.let { "$it registrert" } ?: "Bør vurderes"
            "Mer enn 48 timer i en sju-dagersperiode" ->
                minutes?.let(::minutes)?.let { "$it på 7 dager" } ?: "Bør vurderes"
            "Kort hvile mellom arbeidsperioder" ->
                minutes?.let(::minutes)?.let { "kortest $it" } ?: "Bør vurderes"
            "Lang sammenhengende arbeidsperiode" ->
                minutes?.let(::minutes)?.let { "lengst $it" } ?: "Bør vurderes"
            else -> "Bør vurderes"
        }
    }

    private fun compactFindingDetail(finding: ControlFinding): String = finding.detail
        .substringBefore("Kontroller hvilken arbeidstidsordning")
        .substringBefore("Kontroller at dette er tillatt")
        .replace("Du har bare ", "")
        .replace(" sammenhengende fri. Den første arbeidsperioden slutter ", " fri · ")
        .replace(", og den neste begynner ", " → ")
        .replace("Arbeidsperioden varer ", "")
        .replace(", fra ", " · ")
        .replace(" til ", " → ")
        .trim()
        .trimEnd('.', ' ')

    private fun plainRuleTitle(rule: DomainRule, rateSet: TariffRateSet = FerieturTariffRates.current): String = when (rule.id) {
        "D25_18_4_NOTICE" -> "Var reisen kjent senest dagen i forveien?"
        "D25_18_4_X13_7_3" -> "Gjelder ${pdfSpecialOvertimePercent(rateSet)} prosent overtid på den særskilte dagen?"
        "D25_20_3_SLEEP_PERMISSION" -> "Hadde arbeidstakeren tillatelse til å sove under nattreisen?"
        else -> rule.title
    }

    private fun plainRuleExplanation(rule: DomainRule, rateSet: TariffRateSet): String = when (rule.id) {
        "D25_18_4_NOTICE" -> "Punkt 18.4 sier at dersom arbeidstakeren ikke fikk vite om reisen senest dagen i forveien, betales inntil ${pdfDurationWords(rateSet.shortNoticeMaxMinutes)} av reisetiden som overtid når den kreves utført utenfor ordinær arbeidstid. Ordinær reisetidsbetaling er allerede med; varseltidspunktet må avklares for å vite om overtidsdelen også skal med."
        "D25_18_4_X13_7_3" -> "Punkt 13.7.3 gir ${pdfSpecialOvertimePercent(rateSet)} prosent overtidstillegg på særskilt opplistede dager for arbeidstakere som har ordinær tjeneste på søn- og helgedager. Appen kan ikke fastslå denne personlige tariffstatusen bare fra turen."
        "D25_20_3_SLEEP_PERMISSION" -> "Punkt 20.3 sier at reisetid mellom kl. ${pdfClock(rateSet.travelSleepWindowStart)} og ${pdfClock(rateSet.travelSleepWindowEnd)} beregnes som arbeid av passiv karakter når arbeidstakeren har tillatelse til å sove. Nattdelen holdes derfor utenfor betalingsgrunnlaget til dette er avklart."
        else -> "Denne regelen må avklares før beregningen kan regnes som komplett."
    }

    private fun pdfPercent(fraction: BigDecimal): String =
        fraction.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString().replace('.', ',')

    private fun pdfDecimal(value: BigDecimal): String =
        value.stripTrailingZeros().toPlainString().replace('.', ',')

    private fun pdfClock(value: java.time.LocalTime): String =
        value.format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun pdfDurationWords(value: Long): String = when (value) {
        120L -> "to timer"
        360L -> "seks timer"
        else -> minutes(value)
    }

    private fun pdfPassiveRatio(rateSet: TariffRateSet): String =
        "1:${rateSet.passiveWorkDivisor}"

    private fun pdfPassiveFraction(rateSet: TariffRateSet): String =
        "1/${rateSet.passiveWorkDivisor}"

    private fun pdfPassiveShare(rateSet: TariffRateSet): String =
        if (rateSet.passiveWorkDivisor == 3) "én tredel" else pdfPassiveFraction(rateSet)

    private fun pdfRoundingUnitDefinite(minutes: Long): String =
        if (minutes == 30L) "halve time" else "$minutes minutter"

    private fun pdfRoundingUnitIndefinite(minutes: Long): String =
        if (minutes == 30L) "halvtime" else "$minutes-minuttersperiode"

    private fun pdfSpecialOvertimePercent(rateSet: TariffRateSet): String =
        rateSet.specialOvertimePercentageLabel

    private fun possibleAmountForRule(s: FinalizedTripSnapshot, ruleId: String): BigDecimal {
        val calculation = s.presentation
        val lineIds: Set<String> = when (ruleId) {
            "D25_18_4_NOTICE" -> setOf("travel-notice-open")
            "D25_18_4_X13_7_3" -> setOf("travel-short-notice-133-open")
            else -> emptySet()
        }
        return calculation.lines
            .filter { it.id in lineIds && it.paymentTreatment == PaymentTreatment.OPEN }
            .fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) }
            .setScale(2)
    }


    internal fun separateTripCalculationNote(): String =
        "Grunnturnusen er ikke brukt som sammenligningsgrunnlag. Hele den registrerte arbeidsplanen på turen behandles som eget beregningsgrunnlag."

    internal fun shortFooterDescription(mode: RosterComparisonMode): String =
        if (mode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            "Kortversjonen viser betalingsgrunnlaget og hovedkontrollene. Fullt beregningsgrunnlag inneholder grunnturnus, hele arbeidsplanen, dag-for-dag-kontroll, begrunnelser og kilder."
        } else {
            "Kortversjonen viser betalingsgrunnlaget og hovedkontrollene. Fullt beregningsgrunnlag inneholder hele arbeidsplanen, dag-for-dag-kontroll, begrunnelser og kilder."
        }

    internal fun sentenceWithFollowUp(text: String, followUp: String): String {
        val base = text.trimEnd()
        val punctuation = if (base.lastOrNull() in setOf('.', '?', '!')) "" else "."
        val tail = followUp.trim()
        return if (tail.isEmpty()) "$base$punctuation" else "$base$punctuation $tail"
    }

    private fun date(value: LocalDate): String = value.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", NORWEGIAN_LOCALE))
    private fun clock(value: LocalDateTime): String = value.format(DateTimeFormatter.ofPattern("HH:mm"))
    private fun dateTime(value: LocalDateTime): String = "${date(value.toLocalDate())} kl. ${clock(value)}"
    private fun minutes(value: Long): String = "${value / 60} t${if (value % 60L != 0L) " ${value % 60} min" else ""}"
    private fun oneThirdMinutes(value: Long): String = minutes(value / 3)
    private fun money(value: BigDecimal): String = NumberFormat.getCurrencyInstance(NORWEGIAN_LOCALE).format(value)

    private fun weeklyBasisLabel(basis: WeeklyBasis): String = when (basis) {
        WeeklyBasis.HOURS_37_5 -> "37,5 timer"
        WeeklyBasis.HOURS_35_5 -> "35,5 timer"
        WeeklyBasis.DOK25_8_2_2 -> "Tredelt turnus / punkt 8.2.2"
        WeeklyBasis.HOURS_33_6 -> "33,6 timer"
    }

    private fun workBlockKind(block: WorkBlock): String {
        val base = kind(block.kind)
        if (!block.kind.isTravelWithoutResponsibility()) return base
        val notice = when (block.travelNoticeStatus) {
            TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY -> "varsel kjent senest dagen før"
            TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY -> "kort varsel"
            TravelNoticeStatus.NOT_CLARIFIED -> "varseltidspunkt ikke avklart"
        }
        return "$base · $notice"
    }

    private fun kind(kind: TimeKind): String = when (kind) {
        TimeKind.ACTIVE_WORK -> "Aktivt arbeid"
        TimeKind.ACTIVE_NIGHT_WATCH -> "Våken nattevakt"
        TimeKind.RESTING_NIGHT_WATCH -> "Hvilende nattevakt"
        TimeKind.TRAVEL_WITH_RESPONSIBILITY -> "Reise med ansvar for beboeren"
        TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY -> "Reise uten tilsynsansvar - søvntillatelse må avklares ved nattreise"
        TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP -> "Reise uten tilsynsansvar - ingen søvntillatelse ved nattreise"
        TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> "Reise uten tilsynsansvar - søvn tillatt ved nattreise"
        TimeKind.TRAVEL_UNCERTAIN -> "Reise - ansvar må avklares"
        TimeKind.ACTIVE_EVENT_ON_RESTING -> "Aktivt arbeid under hvilende nattevakt"
    }
}

private enum class PdfTone { NEUTRAL, INFO, OK, WARNING }

private class PdfWriter(private val document: PdfDocument) {
    private val width = 595
    private val height = 842
    private val left = 40f
    private val right = 555f
    private val bottom = 804f
    private var pageNo = 0
    private var page: PdfDocument.Page? = null
    private var y = 0f
    private var finalFooter: String? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35, 35, 35) }

    init { newPage() }

    fun finish() {
        page?.let { current ->
            finalFooter?.let(::drawFinalFooter)
            document.finishPage(current)
        }
        page = null
    }
    fun pageBreak() { page?.let(document::finishPage); page = null; newPage() }
    fun space(px: Int) { ensure(px.toFloat()); y += px }
    fun documentLabel(text: String) = text(text, 8.3f, true, 5f, Color.rgb(92, 98, 108))
    fun h1(text: String) { ensure(58f); text(text, 19f, true, 9f, Color.rgb(26, 31, 40)) }
    fun h2(text: String) { ensure(40f); text(text, 12.5f, true, 5f, Color.rgb(26, 31, 40)) }
    fun subheading(text: String) = text(text, 9.7f, true, 3f, Color.rgb(72, 78, 88))
    fun p(text: String) = text(text, 9.6f, false, 4f, Color.rgb(45, 48, 54))
    fun smallText(text: String) = text(text, 8.6f, false, 3f, Color.rgb(77, 82, 91))
    fun footerMeta(text: String) = text(text, 7.8f, false, 2f, Color.rgb(102, 107, 116))
    fun finalFooterMeta(text: String) { finalFooter = text }
    fun bullet(text: String) = text("- $text", 8.7f, false, 1.5f, Color.rgb(58, 63, 72))

    fun summaryAmount(label: String, amount: BigDecimal, sideLabel: String, sideValue: String) {
        ensure(76f)
        drawTextAt(label, left, y + 10f, 10f, true, Color.rgb(62, 76, 103))
        drawTextAt(NumberFormat.getCurrencyInstance(NORWEGIAN_LOCALE).format(amount), left, y + 39f, 26f, true, Color.rgb(31, 44, 70))
        val sideLabelLines = wrapped(sideLabel, 8.8f, 190f, false)
        val sideValueLines = wrapped(sideValue, 10.4f, 190f, true)
        sideLabelLines.forEachIndexed { i, line -> drawRightText(line, right, y + 10f + i * 11f, 8.8f, false, Color.rgb(90, 94, 102)) }
        val valueStart = y + 27f + (sideLabelLines.size - 1).coerceAtLeast(0) * 11f
        sideValueLines.forEachIndexed { i, line -> drawRightText(line, right, valueStart + i * 12.5f, 10.4f, true, Color.rgb(42, 47, 57)) }
        y += 54f
        rule()
    }

    fun summaryDifference(label: String, amount: BigDecimal) {
        summaryLine(label, NumberFormat.getCurrencyInstance(NORWEGIAN_LOCALE).format(amount), strong = true)
    }

    fun summaryMetaRow(label1: String, value1: String, label2: String, value2: String) {
        val half = (right - left) / 2f
        val valueOffset = 96f
        val valueWidth = half - valueOffset - 8f
        val a = wrapped(value1, 9.1f, valueWidth, true)
        val b = wrapped(value2, 9.1f, valueWidth, true)
        val rows = maxOf(a.size, b.size, 1)
        val h = rows * 12.5f + 8f
        ensure(h)
        drawTextAt(label1, left, y + 9f, 8.2f, false, Color.rgb(100, 104, 112))
        drawTextAt(label2, left + half, y + 9f, 8.2f, false, Color.rgb(100, 104, 112))
        a.forEachIndexed { i, line -> drawTextAt(line, left + valueOffset, y + 9f + i * 12.5f, 9.1f, true, Color.rgb(45, 48, 54)) }
        b.forEachIndexed { i, line -> drawTextAt(line, left + half + valueOffset, y + 9f + i * 12.5f, 9.1f, true, Color.rgb(45, 48, 54)) }
        y += h
    }

    fun summaryMetaSingle(label: String, value: String) {
        val lines = wrapped(value, 8.9f, right - left - 120f, false)
        ensure(lines.size * 12f + 7f)
        drawTextAt(label, left, y + 8f, 8.2f, false, Color.rgb(100, 104, 112))
        lines.forEachIndexed { i, line -> drawTextAt(line, left + 118f, y + 8f + i * 12f, 8.9f, false, Color.rgb(55, 58, 64)) }
        y += lines.size * 12f + 7f
    }

    fun compactNote(title: String, body: String) {
        val titleLines = wrapped(title, 9f, 180f, true)
        val bodyLines = wrapped(body, 8.8f, right - left - 200f, false)
        val rows = maxOf(titleLines.size, bodyLines.size)
        val h = rows * 12.4f + 9f
        ensure(h)
        titleLines.forEachIndexed { i, line -> drawTextAt(line, left, y + 9f + i * 12.4f, 9f, true, Color.rgb(48, 54, 64)) }
        bodyLines.forEachIndexed { i, line -> drawTextAt(line, left + 196f, y + 9f + i * 12.4f, 8.8f, false, Color.rgb(72, 77, 86)) }
        y += h
        rule(light = true)
    }

    fun moneyRow(title: String, formula: String, amount: BigDecimal, muted: Boolean = false) {
        val amountText = NumberFormat.getCurrencyInstance(NORWEGIAN_LOCALE).format(amount)
        val titleColor = if (muted) Color.rgb(88, 92, 100) else Color.rgb(38, 42, 49)
        val detailColor = Color.rgb(103, 107, 115)
        val titleLines = wrapped(title, 9.6f, 335f, true)
        val formulaLines = if (formula.isBlank()) emptyList() else wrapped(formula, 8.1f, 355f, false)
        val h = titleLines.size * 12.3f + formulaLines.size * 10.7f + 7f
        ensure(h)
        var yy = y + 9f
        titleLines.forEach { line -> drawTextAt(line, left, yy, 9.6f, true, titleColor); yy += 12.3f }
        drawRightText(amountText, right, y + 9f, 9.8f, true, titleColor)
        formulaLines.forEach { line -> drawTextAt(line, left, yy, 8.1f, false, detailColor); yy += 10.7f }
        y += h
    }

    fun statusRow(label: String, value: String, tone: PdfTone) {
        val toneColor = when (tone) {
            PdfTone.OK -> Color.rgb(42, 118, 72)
            PdfTone.WARNING -> Color.rgb(164, 105, 21)
            PdfTone.INFO -> Color.rgb(74, 95, 138)
            PdfTone.NEUTRAL -> Color.rgb(90, 94, 102)
        }
        val lines = wrapped(value, 8.9f, right - left - 178f, false)
        val h = maxOf(20f, lines.size * 12f + 5f)
        ensure(h)
        paint.color = toneColor
        page!!.canvas.drawCircle(left + 4f, y + 8f, 3f, paint)
        drawTextAt(label, left + 14f, y + 10f, 8.7f, true, Color.rgb(58, 63, 72))
        lines.forEachIndexed { i, line -> drawTextAt(line, left + 176f, y + 10f + i * 12f, 8.9f, false, Color.rgb(55, 58, 64)) }
        y += h
    }

    fun warningLine(text: String) {
        val lines = wrapped(text, 8.7f, right - left - 18f, false)
        val h = warningLineHeight(text)
        ensure(h)
        paint.color = Color.rgb(164, 105, 21)
        page!!.canvas.drawRect(left, y + 2f, left + 3f, y + lines.size * 11.8f + 4f, paint)
        lines.forEachIndexed { i, line -> drawTextAt(line, left + 12f, y + 10f + i * 11.8f, 8.7f, false, Color.rgb(92, 67, 28)) }
        y += h
    }

    fun summaryLine(label: String, value: String, strong: Boolean = false) {
        val labelLines = wrapped(label, 8.9f, 300f, strong)
        val valueLines = wrapped(value, 8.9f, right - left - 316f, strong)
        val rows = maxOf(labelLines.size, valueLines.size)
        val h = rows * 12.2f + 4f
        ensure(h)
        for (i in 0 until rows) {
            if (i < labelLines.size) drawTextAt(labelLines[i], left, y + 9f + i * 12.2f, 8.9f, strong, Color.rgb(65, 69, 77))
            if (i < valueLines.size) drawTextAt(valueLines[i], left + 310f, y + 9f + i * 12.2f, 8.9f, strong, Color.rgb(45, 48, 54))
        }
        y += h
    }

    fun compactRow(first: String, second: String, third: String) {
        val firstWidth = 180f
        val thirdWidth = 110f
        val secondWidth = right - left - firstWidth - thirdWidth - 16f
        val a = wrapped(first, 8.5f, firstWidth, false)
        val b = wrapped(second, 8.5f, secondWidth, false)
        val c = wrapped(third, 8.5f, thirdWidth, true)
        val rows = maxOf(a.size, b.size, c.size)
        val lineHeight = 11.6f
        val h = compactRowHeight(first, second, third)
        ensure(h)
        for (i in 0 until rows) {
            if (i < a.size) drawTextAt(a[i], left, y + 8f + i * lineHeight, 8.5f, false, Color.rgb(78, 82, 90))
            if (i < b.size) drawTextAt(b[i], left + firstWidth + 6f, y + 8f + i * lineHeight, 8.5f, false, Color.rgb(48, 52, 59))
            if (i < c.size) drawTextAt(c[i], right - thirdWidth, y + 8f + i * lineHeight, 8.5f, true, Color.rgb(48, 52, 59))
        }
        y += h
    }

    fun dayHeader(text: String) {
        space(2)
        rule(light = true)
        text(text, 10.3f, true, 3f, Color.rgb(35, 42, 55))
    }

    fun dayTotalHeader(text: String, amount: BigDecimal) {
        ensure(48f)
        rule(light = true)
        drawTextAt(text, left, y + 11f, 10.3f, true, Color.rgb(35, 42, 55))
        drawRightText(NumberFormat.getCurrencyInstance(NORWEGIAN_LOCALE).format(amount), right, y + 11f, 10.3f, true, Color.rgb(35, 42, 55))
        y += 22f
    }

    fun keepH2WithFirstDetailBlock(heading: String, title: String, formula: String, explanation: String, source: String) {
        val headingReserve = maxOf(40f, textHeight(heading, 12.5f, true, 5f))
        val detailHeight = detailBlockHeight(title, formula, explanation, source)
        // Reserve the section heading plus its first content block so the heading
        // can never be stranded at the bottom of a page.
        ensure(headingReserve + detailHeight + 4f)
    }

    fun detailBlock(title: String, amount: String, formula: String, explanation: String, source: String, warning: Boolean) {
        val titleLines = wrapped(title, 10.4f, 340f, true)
        val formulaLines = if (formula.isBlank()) emptyList() else wrapped(formula, 8.5f, right - left, false)
        val explanationLines = wrapped(explanation, 8.8f, right - left, false)
        val sourceLines = if (source.isBlank()) emptyList() else wrapped("Kilde: $source", 7.8f, right - left, false)
        val h = detailBlockHeight(title, formula, explanation, source)
        ensure(h + 4f)
        if (warning) {
            paint.color = Color.rgb(164, 105, 21)
            page!!.canvas.drawRect(left, y + 1f, left + 3f, y + h - 4f, paint)
        }
        var yy = y + 10f
        titleLines.forEach { line -> drawTextAt(line, left + if (warning) 10f else 0f, yy, 10.4f, true, Color.rgb(38, 42, 49)); yy += 13f }
        drawRightText(amount, right, y + 10f, 10.2f, true, if (warning) Color.rgb(130, 84, 18) else Color.rgb(38, 42, 49))
        formulaLines.forEach { line -> drawTextAt(line, left + if (warning) 10f else 0f, yy, 8.5f, false, Color.rgb(88, 92, 100)); yy += 11.2f }
        yy += 2f
        explanationLines.forEach { line -> drawTextAt(line, left + if (warning) 10f else 0f, yy, 8.8f, false, Color.rgb(57, 61, 69)); yy += 11.7f }
        if (sourceLines.isNotEmpty()) yy += 2f
        sourceLines.forEach { line -> drawTextAt(line, left + if (warning) 10f else 0f, yy, 7.8f, false, Color.rgb(99, 103, 111)); yy += 10.2f }
        y += h
        rule(light = true)
    }

    fun worktimeSectionTitle(
        title: String,
        tone: PdfTone,
    ) {
        ensure(24f)

        val toneColor = when (tone) {
            PdfTone.WARNING -> Color.rgb(205, 137, 27)
            PdfTone.INFO -> Color.rgb(74, 95, 138)
            PdfTone.OK -> Color.rgb(42, 118, 72)
            PdfTone.NEUTRAL -> Color.rgb(90, 94, 102)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = toneColor
        page!!.canvas.drawCircle(
            left + 6f,
            y + 9f,
            5f,
            paint,
        )
        paint.style = Paint.Style.FILL

        drawTextAt(
            title,
            left + 18f,
            y + 12f,
            10.4f,
            true,
            Color.rgb(35, 42, 55),
        )

        y += 23f
    }

    fun worktimeRestCard(
        details: List<String>,
    ) {
        if (details.isEmpty()) return

        val textWidth = right - left - 45f
        val rowHeights = details.map { detail ->
            maxOf(
                16f,
                wrapped(
                    detail,
                    7.8f,
                    textWidth,
                    false,
                ).size * 10.2f + 5f,
            )
        }

        val cardHeight =
            10f + rowHeights.sum()

        ensure(cardHeight + 5f)

        drawRoundedPanel(
            x = left,
            top = y,
            width = right - left,
            height = cardHeight,
            radius = 8f,
            fill = Color.rgb(252, 252, 253),
            stroke = Color.rgb(224, 228, 234),
        )

        var yy = y + 7f

        details.forEachIndexed { index, detail ->
            val rowHeight = rowHeights[index]
            val lines = wrapped(
                detail,
                7.8f,
                textWidth,
                false,
            )

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(218, 148, 30)
            page!!.canvas.drawCircle(
                left + 13f,
                yy + 7f,
                5f,
                paint,
            )

            drawCenteredText(
                "!",
                left + 13f,
                yy + 9.5f,
                7f,
                true,
                Color.WHITE,
            )

            lines.forEachIndexed { lineIndex, line ->
                drawTextAt(
                    line,
                    left + 28f,
                    yy + 8.5f + lineIndex * 10.2f,
                    7.8f,
                    false,
                    Color.rgb(55, 60, 69),
                )
            }

            yy += rowHeight

            if (index != details.lastIndex) {
                paint.color = Color.rgb(232, 234, 238)
                paint.strokeWidth = 0.7f
                page!!.canvas.drawLine(
                    left + 10f,
                    yy,
                    right - 10f,
                    yy,
                    paint,
                )
            }
        }

        y += cardHeight + 4f
    }

    fun worktimePeriodGrid(
        periods: List<PdfExporter.PdfWorktimePeriod>,
    ) {
        if (periods.isEmpty()) return

        val compactPeriods =
            periods.filter(
                PdfExporter::isCompactWorktimePeriodForPdf,
            )

        val visualPeriods =
            periods.filterNot(
                PdfExporter::isCompactWorktimePeriodForPdf,
            )

        if (compactPeriods.isNotEmpty()) {
            worktimeCompactPeriods(compactPeriods)

            if (visualPeriods.isNotEmpty()) {
                space(5)
            }
        }

        if (visualPeriods.isEmpty()) return

        val gap = 10f
        val cardWidth =
            (right - left - gap) / 2f

        visualPeriods.chunked(2).forEach { row ->
            val heights = row.map { period ->
                worktimePeriodCardHeight(
                    period,
                    cardWidth,
                )
            }

            val rowHeight =
                heights.maxOrNull() ?: 0f

            ensure(rowHeight + 8f)

            row.forEachIndexed { index, period ->
                val x =
                    left + index * (cardWidth + gap)

                drawWorktimePeriodCard(
                    period = period,
                    x = x,
                    top = y,
                    width = cardWidth,
                    height = rowHeight,
                )
            }

            y += rowHeight + 8f
        }
    }

    private fun worktimeCompactPeriods(
        periods: List<PdfExporter.PdfWorktimePeriod>,
    ) {
        if (periods.isEmpty()) return

        val rowHeight = 20f
        val panelHeight =
            8f + periods.size * rowHeight

        ensure(panelHeight + 4f)

        drawRoundedPanel(
            x = left,
            top = y,
            width = right - left,
            height = panelHeight,
            radius = 7f,
            fill = Color.rgb(250, 251, 252),
            stroke = Color.rgb(222, 226, 232),
        )

        var yy = y + 8f

        periods.forEachIndexed { index, period ->
            val segment = period.segments.single()
            val centerY = yy + 7f

            paint.style = Paint.Style.FILL
            paint.color =
                worktimeSegmentColor(segment.kind)

            page!!.canvas.drawRoundRect(
                RectF(
                    left + 10f,
                    centerY - 4f,
                    left + 18f,
                    centerY + 4f,
                ),
                2f,
                2f,
                paint,
            )

            drawTextAt(
                worktimeDurationLabel(period.minutes),
                left + 26f,
                centerY + 2.5f,
                8.1f,
                true,
                Color.rgb(42, 47, 57),
            )

            drawTextAt(
                worktimePeriodRange(period),
                left + 72f,
                centerY + 2.5f,
                7.5f,
                false,
                Color.rgb(67, 72, 81),
            )

            drawRightText(
                worktimeSegmentLabel(segment.kind),
                right - 10f,
                centerY + 2.5f,
                7.7f,
                true,
                Color.rgb(55, 60, 69),
            )

            yy += rowHeight

            if (index != periods.lastIndex) {
                paint.color = Color.rgb(232, 234, 238)
                paint.strokeWidth = 0.7f

                page!!.canvas.drawLine(
                    left + 10f,
                    yy,
                    right - 10f,
                    yy,
                    paint,
                )
            }
        }

        y += panelHeight + 3f
    }

    private fun worktimePeriodCardHeight(
        period: PdfExporter.PdfWorktimePeriod,
        width: Float,
    ): Float {
        val subtitleLines = wrapped(
            worktimePeriodRange(period),
            6.9f,
            width - 18f,
            false,
        ).size

        val note = worktimePeriodNote(period)
        val noteLines = if (note == null) {
            0
        } else {
            wrapped(
                note,
                7f,
                width - 30f,
                false,
            ).size
        }

        return 57f +
            subtitleLines * 8.9f +
            period.segments.size * 14.2f +
            noteLines * 9f
    }

    private fun drawWorktimePeriodCard(
        period: PdfExporter.PdfWorktimePeriod,
        x: Float,
        top: Float,
        width: Float,
        height: Float,
    ) {
        drawRoundedPanel(
            x = x,
            top = top,
            width = width,
            height = height,
            radius = 8f,
            fill = Color.rgb(249, 250, 252),
            stroke = Color.rgb(210, 218, 231),
        )

        val duration =
            worktimeDurationLabel(period.minutes)

        drawTextAt(
            "$duration sammenhengende arbeidstid",
            x + 9f,
            top + 14f,
            9.1f,
            true,
            Color.rgb(35, 42, 55),
        )

        val subtitle = worktimePeriodRange(period)
        val subtitleLines = wrapped(
            subtitle,
            6.9f,
            width - 18f,
            false,
        )

        subtitleLines.forEachIndexed { index, line ->
            drawTextAt(
                line,
                x + 9f,
                top + 25f + index * 8.9f,
                6.9f,
                false,
                Color.rgb(78, 83, 93),
            )
        }

        var yy =
            top + 31f +
                subtitleLines.size * 8.9f

        val barLeft = x + 9f
        val barRight = x + width - 9f
        val barWidth = barRight - barLeft

        drawTextAt(
            worktimeClock(period.start),
            barLeft,
            yy,
            6.7f,
            false,
            Color.rgb(70, 76, 87),
        )

        var elapsed = 0L

        period.segments.dropLast(1).forEach { segment ->
            elapsed += segment.minutes

            val centerX =
                barLeft +
                    barWidth *
                    (
                        elapsed.toFloat() /
                            period.minutes.toFloat()
                        )

            drawCenteredText(
                worktimeClock(segment.end),
                centerX,
                yy,
                6.7f,
                false,
                Color.rgb(70, 76, 87),
            )
        }

        drawRightText(
            worktimeClock(period.end),
            barRight,
            yy,
            6.7f,
            false,
            Color.rgb(70, 76, 87),
        )

        val barTop = yy + 5f
        val barHeight = 11f
        var segmentX = barLeft

        period.segments.forEach { segment ->
            val segmentWidth =
                barWidth *
                    (
                        segment.minutes.toFloat() /
                            period.minutes.toFloat()
                        )

            paint.style = Paint.Style.FILL
            paint.color =
                worktimeSegmentColor(segment.kind)

            page!!.canvas.drawRoundRect(
                RectF(
                    segmentX,
                    barTop,
                    segmentX + segmentWidth,
                    barTop + barHeight,
                ),
                3.5f,
                3.5f,
                paint,
            )

            segmentX += segmentWidth
        }

        yy = barTop + barHeight + 10f

        period.segments.forEach { segment ->
            val color =
                worktimeSegmentColor(segment.kind)

            paint.style = Paint.Style.FILL
            paint.color = color

            page!!.canvas.drawRoundRect(
                RectF(
                    x + 9f,
                    yy - 7f,
                    x + 17f,
                    yy + 1f,
                ),
                2f,
                2f,
                paint,
            )

            val label =
                "${worktimeClock(segment.start)}-" +
                    "${worktimeClock(segment.end)} · " +
                    worktimeSegmentLabel(segment.kind)

            drawTextAt(
                label,
                x + 23f,
                yy,
                7f,
                false,
                Color.rgb(55, 60, 69),
            )

            drawRightText(
                worktimeDurationLabel(segment.minutes),
                x + width - 9f,
                yy,
                7.1f,
                true,
                Color.rgb(45, 50, 59),
            )

            yy += 14.2f
        }

        worktimePeriodNote(period)?.let { note ->
            val lines = wrapped(
                note,
                7f,
                width - 30f,
                false,
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.rgb(76, 101, 151)

            page!!.canvas.drawCircle(
                x + 13f,
                yy - 2.5f,
                4.5f,
                paint,
            )

            paint.style = Paint.Style.FILL

            drawCenteredText(
                "i",
                x + 13f,
                yy,
                6.5f,
                true,
                Color.rgb(76, 101, 151),
            )

            lines.forEachIndexed { index, line ->
                drawTextAt(
                    line,
                    x + 23f,
                    yy + index * 9f,
                    7f,
                    false,
                    Color.rgb(76, 101, 151),
                )
            }
        }
    }

    private fun worktimePeriodRange(
        period: PdfExporter.PdfWorktimePeriod,
    ): String =
        "${worktimeCompactDateTime(period.start)}  →  " +
            worktimeCompactDateTime(period.end)

    private fun worktimeCompactDateTime(
        value: java.time.LocalDateTime,
    ): String {
        val day =
            value.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.SHORT,
                NORWEGIAN_LOCALE,
            ).let { label ->
                if (label.endsWith(".")) label else "$label."
            }

        val month =
            value.month.getDisplayName(
                java.time.format.TextStyle.SHORT,
                NORWEGIAN_LOCALE,
            ).let { label ->
                if (label.endsWith(".")) label else "$label."
            }

        return "$day ${value.dayOfMonth}. $month " +
            worktimeClock(value)
    }

    private fun worktimeClock(
        value: java.time.LocalDateTime,
    ): String =
        "%02d:%02d".format(
            NORWEGIAN_LOCALE,
            value.hour,
            value.minute,
        )

    private fun worktimeDurationLabel(
        minutes: Long,
    ): String {
        val hours = minutes / 60L
        val remainder = minutes % 60L

        return when {
            remainder == 0L -> "$hours t"
            hours == 0L -> "$remainder min"
            else -> "$hours t $remainder min"
        }
    }

    private fun worktimeSegmentLabel(
        kind: TimeKind,
    ): String = when (kind) {
        TimeKind.RESTING_NIGHT_WATCH ->
            "Hvilende nattevakt"

        TimeKind.ACTIVE_WORK ->
            "Aktivt arbeid"

        TimeKind.ACTIVE_NIGHT_WATCH ->
            "Aktiv nattevakt"

        TimeKind.TRAVEL_WITH_RESPONSIBILITY ->
            "Reise med ansvar"

        else ->
            "Arbeidstid"
    }

    private fun worktimeSegmentColor(
        kind: TimeKind,
    ): Int = when (kind) {
        TimeKind.RESTING_NIGHT_WATCH ->
            Color.rgb(65, 82, 126)

        TimeKind.ACTIVE_WORK,
        TimeKind.ACTIVE_NIGHT_WATCH,
        ->
            Color.rgb(78, 132, 106)

        TimeKind.TRAVEL_WITH_RESPONSIBILITY ->
            Color.rgb(218, 145, 42)

        else ->
            Color.rgb(110, 116, 126)
    }

    private fun worktimePeriodNote(
        period: PdfExporter.PdfWorktimePeriod,
    ): String? = when {
        period.segments.any {
            it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY
        } ->
            "Perioden består av flere registrerte tidstyper."

        period.segments.any {
            it.kind == TimeKind.RESTING_NIGHT_WATCH
        } ->
            "Dette betyr ikke " +
                "${worktimeDurationLabel(period.minutes)} aktivt arbeid."

        period.segments.size > 1 ->
            "Perioden består av flere registrerte tidstyper."

        else ->
            null
    }

    private fun drawRoundedPanel(
        x: Float,
        top: Float,
        width: Float,
        height: Float,
        radius: Float,
        fill: Int,
        stroke: Int,
    ) {
        val rect = RectF(
            x,
            top,
            x + width,
            top + height,
        )

        paint.style = Paint.Style.FILL
        paint.color = fill
        page!!.canvas.drawRoundRect(
            rect,
            radius,
            radius,
            paint,
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = stroke
        page!!.canvas.drawRoundRect(
            rect,
            radius,
            radius,
            paint,
        )

        paint.style = Paint.Style.FILL
    }

    private fun drawCenteredText(
        text: String,
        centerX: Float,
        baseline: Float,
        size: Float,
        bold: Boolean,
        color: Int,
    ) {
        paint.textSize = size
        paint.typeface =
            if (bold) {
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD,
                )
            } else {
                Typeface.DEFAULT
            }
        paint.color = color

        page!!.canvas.drawText(
            text,
            centerX - paint.measureText(text) / 2f,
            baseline,
            paint,
        )
    }

    fun controlSummaryRow(title: String, metric: String, count: Int) {
        val countText = if (count == 1) "1 forhold" else "$count forhold"
        val titleLines = wrapped(title, 9.2f, 265f, true)
        val metricLines = wrapped(metric, 8.8f, 125f, false)
        val h = maxOf(titleLines.size, metricLines.size, 1) * 12.2f + 6f
        ensure(h)
        titleLines.forEachIndexed { i, line -> drawTextAt(line, left, y + 9f + i * 12.2f, 9.2f, true, Color.rgb(46, 50, 58)) }
        metricLines.forEachIndexed { i, line -> drawTextAt(line, left + 285f, y + 9f + i * 12.2f, 8.8f, false, Color.rgb(67, 72, 81)) }
        drawRightText(countText, right, y + 9f, 8.7f, true, Color.rgb(84, 89, 98))
        y += h
        rule(light = true)
    }

    fun compactControlDetail(text: String) {
        val lines = wrapped(text, 8.2f, right - left - 12f, false)
        val h = compactControlDetailHeight(text)
        ensure(h)
        lines.forEachIndexed { i, line -> drawTextAt(if (i == 0) "- $line" else line, left + if (i == 0) 0f else 10f, y + 8f + i * 10.7f, 8.2f, false, Color.rgb(72, 77, 86)) }
        y += h
    }

    fun keepDayAuditTogether(holidayText: String?, rows: List<Triple<String, String, String>>, emptyText: String?, warningText: String?) {
        var required = 28f
        if (holidayText != null) required += textHeight(holidayText, 8.6f, false, 3f)
        if (emptyText != null) required += textHeight(emptyText, 8.6f, false, 3f)
        rows.forEach { (first, second, third) -> required += compactRowHeight(first, second, third) }
        if (warningText != null) required += warningLineHeight(warningText)
        ensure(required)
    }

    fun keepControlGroupTogether(title: String, details: List<String>) {
        var required = textHeight(title, 8.6f, false, 3f) + 2f
        details.forEach { required += compactControlDetailHeight(it) }
        ensure(required)
    }

    fun keepTogether(required: Float) = ensure(required)

    fun findingRow(title: String, count: Int, summary: String) {
        val countText = if (count == 1) "1 tilfelle" else "$count tilfeller"
        val titleLines = wrapped(title, 9.5f, 300f, true)
        val summaryLines = wrapped(summary, 8.6f, right - left - 18f, false)
        val h = titleLines.size * 12.2f + summaryLines.size * 11.3f + 11f
        ensure(h)
        var yy = y + 9f
        titleLines.forEach { line -> drawTextAt(line, left, yy, 9.5f, true, Color.rgb(46, 50, 58)); yy += 12.2f }
        drawRightText(countText, right, y + 9f, 8.7f, true, Color.rgb(84, 89, 98))
        summaryLines.forEach { line -> drawTextAt(line, left, yy, 8.6f, false, Color.rgb(67, 72, 81)); yy += 11.3f }
        y += h
    }

    fun openRule(title: String, explanation: String, source: String) {
        warningLine(title)
        smallText(explanation)
        footerMeta("Kilde: $source")
        space(3)
    }

    fun rule(light: Boolean = false) {
        ensure(6f)
        paint.color = if (light) Color.rgb(229, 231, 235) else Color.rgb(205, 209, 216)
        paint.strokeWidth = 1f
        page!!.canvas.drawLine(left, y + 2f, right, y + 2f, paint)
        y += 6f
    }

    private fun detailBlockHeight(title: String, formula: String, explanation: String, source: String): Float {
        val titleLines = wrapped(title, 10.4f, 340f, true)
        val formulaLines = if (formula.isBlank()) emptyList() else wrapped(formula, 8.5f, right - left, false)
        val explanationLines = wrapped(explanation, 8.8f, right - left, false)
        val sourceLines = if (source.isBlank()) emptyList() else wrapped("Kilde: $source", 7.8f, right - left, false)
        return titleLines.size * 13f + formulaLines.size * 11.2f + explanationLines.size * 11.7f + sourceLines.size * 10.2f + 20f
    }

    private fun textHeight(raw: String, size: Float, bold: Boolean, after: Float): Float =
        wrapped(raw, size, right - left, bold).size * size * 1.34f + after

    private fun warningLineHeight(text: String): Float =
        wrapped(text, 8.7f, right - left - 18f, false).size * 11.8f + 9f

    private fun compactRowHeight(first: String, second: String, third: String): Float {
        val firstWidth = 180f
        val thirdWidth = 110f
        val secondWidth = right - left - firstWidth - thirdWidth - 16f
        val rows = maxOf(
            wrapped(first, 8.5f, firstWidth, false).size,
            wrapped(second, 8.5f, secondWidth, false).size,
            wrapped(third, 8.5f, thirdWidth, true).size,
        )
        return rows * 11.6f + 5f
    }

    private fun compactControlDetailHeight(text: String): Float =
        wrapped(text, 8.2f, right - left - 12f, false).size * 10.7f + 3f

    private fun text(raw: String, size: Float, bold: Boolean, after: Float, color: Int) {
        val lines = wrapped(raw, size, right - left, bold)
        val lineHeight = size * 1.34f
        ensure(lines.size * lineHeight + after)
        lines.forEach { line ->
            drawTextAt(line, left, y + size, size, bold, color)
            y += lineHeight
        }
        y += after
    }

    private fun wrapped(raw: String, size: Float, maxWidth: Float, bold: Boolean): List<String> {
        val safe = raw.replace('–', '-').replace('×', 'x').replace("⅓", "1/3")
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        if (safe.isBlank()) return listOf("")
        val out = mutableListOf<String>()
        safe.split('\n').forEach { paragraph ->
            var remaining = paragraph.trim()
            if (remaining.isEmpty()) { out += ""; return@forEach }
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                if (count >= remaining.length) { out += remaining; break }
                val preferred = remaining.substring(0, count).lastIndexOf(' ').takeIf { it > 0 } ?: count
                out += remaining.substring(0, preferred).trim()
                remaining = remaining.substring(preferred).trimStart()
            }
        }
        return out
    }

    private fun drawTextAt(text: String, x: Float, baseline: Float, size: Float, bold: Boolean, color: Int) {
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        paint.color = color
        page!!.canvas.drawText(text, x, baseline, paint)
    }

    private fun drawRightText(text: String, rightX: Float, baseline: Float, size: Float, bold: Boolean, color: Int) {
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        paint.color = color
        val x = rightX - paint.measureText(text)
        page!!.canvas.drawText(text, x, baseline, paint)
    }

    private fun drawFinalFooter(text: String) {
        paint.color = Color.rgb(218, 221, 226)
        paint.strokeWidth = 0.8f
        page!!.canvas.drawLine(left, bottom + 5f, right, bottom + 5f, paint)

        drawTextAt(
            text,
            left,
            height - 16f,
            7.2f,
            false,
            Color.rgb(102, 107, 116),
        )
    }

    private fun ensure(required: Float) {
        if (y + required <= bottom) return
        page?.let(document::finishPage)
        page = null
        newPage()
    }

    private fun newPage() {
        pageNo += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
        y = 40f
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(103, 108, 117)
        page!!.canvas.drawText("FERIETUR01", left, 23f, paint)
        val pageText = "Side $pageNo"
        page!!.canvas.drawText(pageText, right - paint.measureText(pageText), 23f, paint)
    }
}
