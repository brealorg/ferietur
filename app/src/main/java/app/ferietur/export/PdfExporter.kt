package app.ferietur.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import app.ferietur.domain.CalculationCertainty
import app.ferietur.domain.CalculationLine
import app.ferietur.domain.ControlFinding
import app.ferietur.domain.DomainRule
import app.ferietur.domain.EmployerKind
import app.ferietur.domain.FinalizedTripSnapshot
import app.ferietur.domain.FerieturTariffRates
import app.ferietur.domain.TariffRateSet
import app.ferietur.domain.FindingSeverity
import app.ferietur.domain.PayingParty
import app.ferietur.domain.PaymentTreatment
import app.ferietur.domain.RosterComparisonMode
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

    internal fun createBlocking(context: Context, snapshot: FinalizedTripSnapshot, variant: Variant): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val suffix = if (variant == Variant.FULL) "fullt-grunnlag" else "oppsummering"
        val file = File(dir, "ferietur-${snapshot.tripStart.toLocalDate()}-${snapshot.id}-$suffix.pdf")
        val document = PdfDocument()
        val writer = PdfWriter(document)
        val rateSet = FerieturTariffRates.requireById(snapshot.tariffRateSetId)

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
        document.close()
        return file
    }

    fun sharePrepared(context: Context, filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
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
        val proposed = if (s.settlement.usesFullCalculation) s.calculation.paymentBasisAmount else s.settlement.proposedAmount
        val openLines = s.calculation.lines.filter { it.paymentTreatment == PaymentTreatment.OPEN && (it.amount > BigDecimal.ZERO || it.certainty == CalculationCertainty.OPEN) }
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
        w.summaryAmount(amountLabel, s.calculation.paymentBasisAmount, sideLabel, sideValue)

        if (!s.settlement.usesFullCalculation) {
            val difference = s.calculation.paymentBasisAmount.subtract(s.settlement.proposedAmount)
            w.summaryDifference("Forskjell fra beregnet betalingsgrunnlag", difference)
            if (s.settlement.reason.isNotBlank()) w.smallText("Begrunnelse for annet beløp: ${s.settlement.reason}")
        }

        w.summaryMetaRow("Arbeidsgiver", employerLabel(s.employerKind), "Betalingsscenario", payingPartyLabel(s.payingParty))
        w.summaryMetaSingle("Regler appen bruker", s.ruleBasis)
        w.smallText(PAYMENT_SCENARIO_DISCLAIMER)

        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            val coveredSupplement = s.calculation.alreadyCoveredByNormalRosterAmount
            val coveredText = buildString {
                append("I denne beregningen er det lagt til grunn at Oslo kommune utbetaler ordinær lønn og turnustillegg etter grunnturnusen. Den delen av grunnturnusen som overlapper turen utgjør ${minutes(s.calculation.rosterMinutes)}.")
                if (coveredSupplement > BigDecimal.ZERO) {
                    append(" Beregnede turnustillegg fra grunnturnusen: ${money(coveredSupplement)}.")
                }
                append(" Dette er ikke med i betalingsgrunnlaget over.")
            }
            w.compactNote("Grunnturnus som sammenligningsgrunnlag", coveredText)
        } else {
            w.compactNote("Hvordan turen er regnet", separateTripCalculationNote())
        }

        w.h2("Slik er beløpet satt sammen")
        s.calculation.lines
            .filter { it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS && it.amount != BigDecimal.ZERO }
            .forEach { line -> w.moneyRow(plainLineTitle(line), plainFormula(line), line.amount) }

        val reviewFindings = s.findings.filter { it.severity == FindingSeverity.REVIEW || it.severity == FindingSeverity.CRITICAL }
        w.h2("Status")
        w.statusRow("Lønnsopplysninger", if (s.payslipChecked) "Kontrollert mot lønnsslipp" else "Må kontrolleres", if (s.payslipChecked) PdfTone.OK else PdfTone.WARNING)
        if (s.calculation.rosterUncoveredMinutes > 0L) {
            w.statusRow("Turnussammenligning", "${minutes(s.calculation.rosterUncoveredMinutes)} uten registrert arbeidsperiode · kontrollert", PdfTone.INFO)
        }
        w.statusRow(
            "Beregning",
            if (s.unresolvedRules.isEmpty()) "Ingen åpne regler som treffer denne turen" else "${s.unresolvedRules.size} ${if (s.unresolvedRules.size == 1) "regel" else "regler"} må avklares",
            if (s.unresolvedRules.isEmpty()) PdfTone.OK else PdfTone.WARNING,
        )
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
                "Betalingsforslaget er ikke endelig: ${s.unresolvedRules.joinToString("; ") { plainRuleTitle(it, rateSet) }}",
                extra,
            ))
        }

        if (includeFooter) {
            w.rule()
            w.smallText(shortFooterDescription(s.rosterComparisonMode))
            w.footerMeta("Lønnstrinn ${s.salaryStep} · ${weeklyBasisLabel(s.weeklyBasis)} full arbeidsuke · ${rateSet.weekendRate(s.weekendProfile).label}")
            w.footerMeta("Beregning-ID ${s.id} · opprettet ${dateTime(s.createdAt)}")
        }
    }

    private fun writeRosterAndPlan(w: PdfWriter, s: FinalizedTripSnapshot) {
        w.h1("Arbeidsgrunnlaget")
        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER && s.roster.isNotEmpty()) {
            w.h2("Grunnturnus")
            w.p("Dette er turnusen som var lagret da beregningen ble ferdigstilt. Den brukes for å skille arbeid som allerede er dekket fra arbeid som kommer i tillegg.")
            s.roster.forEach { row ->
                val time = if (row.start == null || row.end == null) {
                    "Fri"
                } else {
                    "${clock(row.start)}-${clock(row.end)}${if (row.end.toLocalDate() != row.start.toLocalDate()) " neste dag" else ""}"
                }
                w.compactRow(date(row.date), "${row.code} ${row.label}", time)
            }
            val restingInside = (s.calculation.restingNightMinutes - s.calculation.restingNightOutsideRosterMinutes).coerceAtLeast(0)
            w.summaryLine("Grunnturnustid som overlapper turen", minutes(s.calculation.rosterMinutes))
            w.summaryLine("Registrert aktivt arbeid/reise innen turnusen", minutes(s.calculation.activeInsideRosterMinutes))
            if (restingInside > 0) w.summaryLine("Registrert hvilende nattevakt innen turnusen", minutes(restingInside))
            if (s.calculation.rosterUncoveredMinutes > 0) {
                w.summaryLine("Turnustid uten registrert arbeidsperiode på turen", "${minutes(s.calculation.rosterUncoveredMinutes)} · kontrollert")
                s.calculation.rosterUncoveredEvidence.forEach { evidence ->
                    val period = "${date(evidence.start.toLocalDate())} kl. ${clock(evidence.start)}-${clock(evidence.end)}"
                    w.compactRow(period, "Ingen registrert arbeidsperiode", "Kontrollert")
                }
            }
            if (s.calculation.alreadyCoveredByNormalRosterAmount > BigDecimal.ZERO) {
                w.summaryLine("Turnustillegg fra grunnturnusen - ikke med i betalingsgrunnlaget", money(s.calculation.alreadyCoveredByNormalRosterAmount))
            }
            w.space(5)
        } else {
            w.h2("Grunnturnus")
            w.p("Grunnturnusen er ikke brukt som sammenligningsgrunnlag i denne beregningen.")
        }

        w.h2("Arbeidsplan på turen")
        w.p("Dette er den registrerte arbeidsplanen som beregningen bygger på.")
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
        w.h1("Hvorfor blir beløpet slik?")
        w.p("Hver post under viser hva som er beregnet, hvordan beløpet er regnet og hvilken regel som er brukt.")

        s.calculation.lines
            .filter { it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS }
            .forEach { line -> writeDetailedLine(w, line, rateSet, alreadyCovered = false) }

        if (s.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            w.h2("Grunnturnus - forutsetning i beregningen")
            w.p("Grunnturnusen brukes her som appens sammenligningsgrunnlag for hva som er forutsatt dekket gjennom ordinær lønn og turnustillegg. Dette er en modellforutsetning, ikke en gjengivelse av ordlyden i Dok. 25 punkt 20.2. Punkt 20.2 omtaler egen arbeidsplan før reisen, gjennomsnittsberegning og kompensasjon for arbeidstid ut over ordinær arbeidstid etter kapittel 8. Hvilken arbeidstidsordning som faktisk gjelder for ferieoppholdet må avklares med arbeidsgiver. Turnusen vises for kontroll og legges ikke til betalingsgrunnlaget.")
            w.summaryLine("Grunnturnustid som overlapper turen", minutes(s.calculation.rosterMinutes))
            if (s.calculation.alreadyCoveredByNormalRosterAmount > BigDecimal.ZERO) {
                w.summaryLine("Turnustillegg beregnet fra grunnturnusen", money(s.calculation.alreadyCoveredByNormalRosterAmount))
            }
        }

        val openLines = s.calculation.lines
            .filter { it.paymentTreatment == PaymentTreatment.OPEN }
            .filter(::shouldRenderDetailedLine)
        if (openLines.isNotEmpty()) {
            val first = openLines.first()
            w.keepH2WithFirstDetailBlock(
                heading = "Beløp eller regler som må avklares",
                title = plainLineTitle(first),
                formula = plainFormula(first),
                explanation = plainLineExplanation(first, rateSet),
                source = first.source,
            )
            w.h2("Beløp eller regler som må avklares")
            openLines.forEach { line -> writeDetailedLine(w, line, rateSet, alreadyCovered = false) }
        }
    }

    private fun shouldRenderDetailedLine(line: CalculationLine): Boolean =
        !(line.amount == BigDecimal.ZERO && line.paymentTreatment == PaymentTreatment.OPEN && line.certainty != CalculationCertainty.OPEN)

    private fun writeDetailedLine(w: PdfWriter, line: CalculationLine, rateSet: TariffRateSet, alreadyCovered: Boolean) {
        if (!shouldRenderDetailedLine(line)) return
        val amountLabel = when {
            line.paymentTreatment == PaymentTreatment.OPEN && line.amount > BigDecimal.ZERO -> "Mulig ${money(line.amount)}"
            line.paymentTreatment == PaymentTreatment.OPEN -> "Må avklares"
            alreadyCovered -> "${money(line.amount)} · grunnturnus · ikke med"
            else -> money(line.amount)
        }
        w.detailBlock(
            title = plainLineTitle(line),
            amount = amountLabel,
            formula = plainFormula(line),
            explanation = plainLineExplanation(line, rateSet),
            source = line.source,
            warning = line.paymentTreatment == PaymentTreatment.OPEN,
        )
    }

    private fun writeDayAudit(w: PdfWriter, s: FinalizedTripSnapshot) {
        w.h1("Dag for dag")
        w.p("Her kan du kontrollere hva hver kalenderdag bidrar med. Summen til høyre er beløpet som kommer i tillegg den dagen. Grunnturnusen er dokumentert i arbeidsgrunnlaget og gjentas ikke her.")
        s.calculation.dayAudits.forEach { day ->
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

        val stayAllowance = s.calculation.lines.firstOrNull {
            it.id == "stay-allowance" && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS
        }?.amount ?: BigDecimal.ZERO.setScale(2)
        val distributed = s.calculation.dayAudits.fold(BigDecimal.ZERO) { acc, day -> acc.add(day.paymentSubtotal) }.setScale(2)
        w.keepTogether(66f)
        w.rule()
        w.summaryLine("Sum fordelt på kalenderdager", money(distributed), strong = true)
        if (stayAllowance > BigDecimal.ZERO) {
            w.summaryLine("Døgngodtgjøring - gjelder hele reisen", money(stayAllowance))
        }
        w.summaryLine("Totalt betalingsgrunnlag", money(s.calculation.paymentBasisAmount), strong = true)
        w.smallText("Dagsbeløp fordeles til øre slik at de summerer tilbake til hovedpostene. Små avrundingsforskjeller kan derfor forekomme på enkeltdager.")
    }

    private fun writeGroupedControl(w: PdfWriter, s: FinalizedTripSnapshot, rateSet: TariffRateSet, includeDetails: Boolean) {
        w.h1("Arbeidstid som bør vurderes")
        w.p("Appen viser forhold i arbeidsplanen som bør kontrolleres mot arbeidstidsordningen som gjelder. Den avgjør ikke om arbeidsordningen er lovlig.")
        w.compactNote("Hvilende nattevakt og arbeidstid", "Hvilende nattevakt regnes som arbeidstid når arbeidstiden kontrolleres, selv om betalingen beregnes annerledes.")
        val review = s.findings.filter { it.severity == FindingSeverity.REVIEW || it.severity == FindingSeverity.CRITICAL }
        if (review.isEmpty()) {
            w.statusRow("Arbeidstid", "Ingen forhold markert", PdfTone.OK)
        } else {
            val groups = review.groupBy { it.title }
            groups.forEach { (title, findings) ->
                w.controlSummaryRow(plainFindingTitle(title), findingMetric(title, findings), findings.size)
            }
            if (includeDetails) {
                w.subheading("Detaljer")
                groups.forEach { (title, findings) ->
                    val heading = plainFindingTitle(title)
                    val details = findings.map(::compactFindingDetail)
                    w.keepControlGroupTogether(heading, details)
                    w.smallText(heading)
                    details.forEach(w::compactControlDetail)
                    w.space(2)
                }
            }
        }

        w.footerMeta("Kontrollgrunnlag: arbeidsmiljøloven kapittel 10 og Dok. 25 punkt 20.2. Den konkrete arbeidstidsordningen kan avhenge av gjennomsnittsberegning og lokale avtaler.")
        w.space(4)
        w.h2("Regler som fortsatt må avklares")
        if (s.unresolvedRules.isEmpty()) {
            w.statusRow("Beregning", "Ingen kjente åpne regler som treffer denne turen", PdfTone.OK)
        } else {
            s.unresolvedRules.forEach { rule ->
                val possible = possibleAmountForRule(s, rule.id)
                val explanation = buildString {
                    append(plainRuleExplanation(rule, rateSet))
                    if (possible > BigDecimal.ZERO) append(" Med dagens registrerte timer og satser er mulig tillegg ${money(possible)}. Beløpet er ikke inkludert i betalingsgrunnlaget.")
                }
                w.openRule(plainRuleTitle(rule, rateSet), explanation, rule.source)
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
        w.summaryLine("Lønnstabell", "${s.salaryTableSourceLabel} · fra ${date(s.salaryTableEffectiveFrom)}")
        w.summaryLine("Lønnstrinn", s.salaryStep.toString())
        w.summaryLine("Årslønn", money(s.annualSalary))
        w.summaryLine("Full arbeidsuke", weeklyBasisLabel(s.weeklyBasis))
        w.summaryLine("Lørdags- og søndagssats", rateSet.weekendRate(s.weekendProfile).label)
        w.summaryLine("Kontrollert mot lønnsslipp", if (s.payslipChecked) "Ja" else "Nei")
        w.rule()
        w.footerMeta("Regelversjon: FERIETUR01 ${s.rulesetVersion} · appversjon: ${s.appVersionName} · build ${s.appVersionCode}")
        w.footerMeta("Tariffpakke-ID: ${s.tariffPackageId} · satssett-ID: ${s.tariffRateSetId}")
        w.footerMeta("Lønnstabell-ID: ${s.salaryTableId} · gyldig fra ${date(s.salaryTableEffectiveFrom)}")
        w.footerMeta("Opprettet: ${dateTime(s.createdAt)} · beregning-ID: ${s.id}")
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
        "Kort hvile mellom arbeidsperioder" -> "Kort tid mellom arbeidsperioder"
        "Lang sammenhengende arbeidsperiode" -> "Lange arbeidsperioder"
        else -> title
    }

    private fun plainFindingSummary(title: String, findings: List<ControlFinding>): String = when (title) {
        "Mer enn 48 timer i den viste perioden" -> "Den registrerte arbeidstiden er høy. Kontroller hvilken arbeidstidsordning som gjelder for turen."
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
            "Mer enn 48 timer i den viste perioden" -> firstDuration(Regex("Du har registrert (\\d+) t(?: (\\d+) min)?"))
            "Kort hvile mellom arbeidsperioder" -> firstDuration(Regex("Du har bare (\\d+) t(?: (\\d+) min)? sammenhengende fri"))
            "Lang sammenhengende arbeidsperiode" -> firstDuration(Regex("Arbeidsperioden varer (\\d+) t(?: (\\d+) min)?"))
            else -> null
        }
        return when (title) {
            "Mer enn 48 timer i den viste perioden" -> minutes?.let(::minutes)?.let { "$it registrert" } ?: "Bør vurderes"
            "Kort hvile mellom arbeidsperioder" -> minutes?.let(::minutes)?.let { "kortest $it" } ?: "Bør vurderes"
            "Lang sammenhengende arbeidsperiode" -> minutes?.let(::minutes)?.let { "lengst $it" } ?: "Bør vurderes"
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
        val lineIds: Set<String> = when (ruleId) {
            "D25_18_4_NOTICE" -> setOf("travel-notice-open")
            "D25_18_4_X13_7_3" -> setOf("travel-short-notice-133-open")
            else -> emptySet()
        }
        return s.calculation.lines
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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35, 35, 35) }

    init { newPage() }

    fun finish() { page?.let(document::finishPage); page = null }
    fun pageBreak() { page?.let(document::finishPage); page = null; newPage() }
    fun space(px: Int) { ensure(px.toFloat()); y += px }
    fun documentLabel(text: String) = text(text, 8.3f, true, 5f, Color.rgb(92, 98, 108))
    fun h1(text: String) { ensure(58f); text(text, 19f, true, 9f, Color.rgb(26, 31, 40)) }
    fun h2(text: String) { ensure(40f); text(text, 12.5f, true, 5f, Color.rgb(26, 31, 40)) }
    fun subheading(text: String) = text(text, 9.7f, true, 3f, Color.rgb(72, 78, 88))
    fun p(text: String) = text(text, 9.6f, false, 4f, Color.rgb(45, 48, 54))
    fun smallText(text: String) = text(text, 8.6f, false, 3f, Color.rgb(77, 82, 91))
    fun footerMeta(text: String) = text(text, 7.8f, false, 2f, Color.rgb(102, 107, 116))
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
