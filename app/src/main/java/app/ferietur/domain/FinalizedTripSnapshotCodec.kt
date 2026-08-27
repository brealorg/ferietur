package app.ferietur.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64

object FinalizedTripSnapshotCodec {
    private const val FORMAT_VERSION = 1

    fun encode(snapshot: FinalizedTripSnapshot): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(FORMAT_VERSION)
            out.writeSnapshot(snapshot)
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(encoded: String): FinalizedTripSnapshot {
        val bytes = Base64.getDecoder().decode(encoded)
        return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val version = input.readInt()
            require(version == FORMAT_VERSION) {
                "Unsupported finalized snapshot format: $version"
            }
            input.readSnapshot()
        }
    }

    private fun DataOutputStream.writeSnapshot(snapshot: FinalizedTripSnapshot) {
        writeString(snapshot.id)
        writeDateTime(snapshot.createdAt)
        writeString(snapshot.appVersionName)
        writeInt(snapshot.appVersionCode)
        writeString(snapshot.rulesetVersion)
        writeString(snapshot.salaryTableId)
        writeDate(snapshot.salaryTableEffectiveFrom)
        writeString(snapshot.salaryTableSourceLabel)
        writeString(snapshot.title)
        writeDateTime(snapshot.tripStart)
        writeDateTime(snapshot.tripEnd)
        writeString(snapshot.employerKind.name)
        writeString(snapshot.payingParty.name)
        writeString(snapshot.rosterComparisonMode.name)
        writeInt(snapshot.salaryStep)
        writeDecimal(snapshot.annualSalary)
        writeString(snapshot.weeklyBasis.name)
        writeString(snapshot.weekendProfile.name)
        writeBoolean(snapshot.payslipChecked)
        writeBoolean(snapshot.rosterGapConfirmed)
        writeList(snapshot.roster) { writeRosterRow(it) }
        writeList(snapshot.workBlocks) { writeWorkBlock(it) }
        writeCalculation(snapshot.calculation)
        writeSettlement(snapshot.settlement)
        writeList(snapshot.findings) { writeFinding(it) }
        writeList(snapshot.unresolvedRules) { writeRule(it) }
    }

    private fun DataInputStream.readSnapshot(): FinalizedTripSnapshot =
        FinalizedTripSnapshot(
            id = readString(),
            createdAt = readDateTime(),
            appVersionName = readString(),
            appVersionCode = readInt(),
            rulesetVersion = readString(),
            salaryTableId = readString(),
            salaryTableEffectiveFrom = readDate(),
            salaryTableSourceLabel = readString(),
            title = readString(),
            tripStart = readDateTime(),
            tripEnd = readDateTime(),
            employerKind = enumValueOf(readString()),
            payingParty = enumValueOf(readString()),
            rosterComparisonMode = enumValueOf(readString()),
            salaryStep = readInt(),
            annualSalary = readDecimal(),
            weeklyBasis = enumValueOf(readString()),
            weekendProfile = enumValueOf(readString()),
            payslipChecked = readBoolean(),
            rosterGapConfirmed = readBoolean(),
            roster = readList { readRosterRow() },
            workBlocks = readList { readWorkBlock() },
            calculation = readCalculation(),
            settlement = readSettlement(),
            findings = readList { readFinding() },
            unresolvedRules = readList { readRule() },
        )

    private fun DataOutputStream.writeRosterRow(row: RosterSnapshotRow) {
        writeDate(row.date)
        writeString(row.code)
        writeString(row.label)
        writeNullableDateTime(row.start)
        writeNullableDateTime(row.end)
    }

    private fun DataInputStream.readRosterRow(): RosterSnapshotRow =
        RosterSnapshotRow(
            date = readDate(),
            code = readString(),
            label = readString(),
            start = readNullableDateTime(),
            end = readNullableDateTime(),
        )

    private fun DataOutputStream.writeWorkBlock(block: WorkBlock) {
        writeDateTime(block.start)
        writeDateTime(block.end)
        writeString(block.kind.name)
        writeString(block.travelNoticeStatus.name)
    }

    private fun DataInputStream.readWorkBlock(): WorkBlock =
        WorkBlock(
            start = readDateTime(),
            end = readDateTime(),
            kind = enumValueOf(readString()),
            travelNoticeStatus = enumValueOf(readString()),
        )

    private fun DataOutputStream.writeSettlement(value: SettlementSnapshot) {
        writeDecimal(value.calculatedAmount)
        writeDecimal(value.proposedAmount)
        writeBoolean(value.usesFullCalculation)
        writeString(value.reason)
    }

    private fun DataInputStream.readSettlement(): SettlementSnapshot =
        SettlementSnapshot(
            calculatedAmount = readDecimal(),
            proposedAmount = readDecimal(),
            usesFullCalculation = readBoolean(),
            reason = readString(),
        )

    private fun DataOutputStream.writeEvidence(value: CalculationEvidence) {
        writeDateTime(value.start)
        writeDateTime(value.end)
        writeLong(value.minutes)
        writeString(value.note)
    }

    private fun DataInputStream.readEvidence(): CalculationEvidence =
        CalculationEvidence(
            start = readDateTime(),
            end = readDateTime(),
            minutes = readLong(),
            note = readString(),
        )

    private fun DataOutputStream.writeCalculationLine(value: CalculationLine) {
        writeString(value.id)
        writeString(value.title)
        writeString(value.detail)
        writeDecimal(value.amount)
        writeString(value.source)
        writeString(value.explanation)
        writeList(value.evidence) { writeEvidence(it) }
        writeString(value.certainty.name)
        writeBoolean(value.includedInKnownTotal)
        writeString(value.paymentTreatment.name)
    }

    private fun DataInputStream.readCalculationLine(): CalculationLine =
        CalculationLine(
            id = readString(),
            title = readString(),
            detail = readString(),
            amount = readDecimal(),
            source = readString(),
            explanation = readString(),
            evidence = readList { readEvidence() },
            certainty = enumValueOf(readString()),
            includedInKnownTotal = readBoolean(),
            paymentTreatment = enumValueOf(readString()),
        )

    private fun DataOutputStream.writeContribution(value: DayCalculationContribution) {
        writeString(value.lineId)
        writeString(value.title)
        writeLong(value.minutes)
        writeDecimal(value.amount)
        writeString(value.source)
        writeString(value.certainty.name)
        writeBoolean(value.includedInKnownTotal)
        writeString(value.paymentTreatment.name)
        writeList(value.evidence) { writeEvidence(it) }
    }

    private fun DataInputStream.readContribution(): DayCalculationContribution =
        DayCalculationContribution(
            lineId = readString(),
            title = readString(),
            minutes = readLong(),
            amount = readDecimal(),
            source = readString(),
            certainty = enumValueOf(readString()),
            includedInKnownTotal = readBoolean(),
            paymentTreatment = enumValueOf(readString()),
            evidence = readList { readEvidence() },
        )

    private fun DataOutputStream.writeDayAudit(value: DayCalculationAudit) {
        writeDate(value.date)
        writeList(value.holidayLabels) { writeString(it) }
        writeList(value.contributions) { writeContribution(it) }
        writeDecimal(value.knownSubtotal)
        writeDecimal(value.paymentSubtotal)
        writeDecimal(value.alreadyCoveredSubtotal)
        writeDecimal(value.openSubtotal)
    }

    private fun DataInputStream.readDayAudit(): DayCalculationAudit =
        DayCalculationAudit(
            date = readDate(),
            holidayLabels = readList { readString() },
            contributions = readList { readContribution() },
            knownSubtotal = readDecimal(),
            paymentSubtotal = readDecimal(),
            alreadyCoveredSubtotal = readDecimal(),
            openSubtotal = readDecimal(),
        )

    private fun DataOutputStream.writeCalculation(value: PreliminaryCalculation) {
        writeDecimal(value.hourlyRate)
        writeLong(value.rosterMinutes)
        writeLong(value.rosterUncoveredMinutes)
        writeList(value.rosterUncoveredEvidence) { writeEvidence(it) }
        writeLong(value.activeMinutes)
        writeLong(value.activeInsideRosterMinutes)
        writeLong(value.activeOutsideRosterMinutes)
        writeLong(value.payableActiveWorkMinutes)
        writeLong(value.payableTravelWithResponsibilityMinutes)
        writeLong(value.restingNightMinutes)
        writeLong(value.restingNightOutsideRosterMinutes)
        writeLong(value.eveningNightMinutes)
        writeLong(value.weekendMinutes)
        writeLong(value.holidayMinutes)
        writeInt(value.stayAllowanceDays)
        writeList(value.lines) { writeCalculationLine(it) }
        writeList(value.dayAudits) { writeDayAudit(it) }
        writeDecimal(value.knownAmount)
        writeDecimal(value.paymentBasisAmount)
        writeDecimal(value.alreadyCoveredByNormalRosterAmount)
        writeDecimal(value.excludedKnownRuleAmount)
        val ids = value.applicableUnresolvedRuleIds.sorted()
        writeList(ids) { writeString(it) }
    }

    private fun DataInputStream.readCalculation(): PreliminaryCalculation =
        PreliminaryCalculation(
            hourlyRate = readDecimal(),
            rosterMinutes = readLong(),
            rosterUncoveredMinutes = readLong(),
            rosterUncoveredEvidence = readList { readEvidence() },
            activeMinutes = readLong(),
            activeInsideRosterMinutes = readLong(),
            activeOutsideRosterMinutes = readLong(),
            payableActiveWorkMinutes = readLong(),
            payableTravelWithResponsibilityMinutes = readLong(),
            restingNightMinutes = readLong(),
            restingNightOutsideRosterMinutes = readLong(),
            eveningNightMinutes = readLong(),
            weekendMinutes = readLong(),
            holidayMinutes = readLong(),
            stayAllowanceDays = readInt(),
            lines = readList { readCalculationLine() },
            dayAudits = readList { readDayAudit() },
            knownAmount = readDecimal(),
            paymentBasisAmount = readDecimal(),
            alreadyCoveredByNormalRosterAmount = readDecimal(),
            excludedKnownRuleAmount = readDecimal(),
            applicableUnresolvedRuleIds = readList { readString() }.toSet(),
        )

    private fun DataOutputStream.writeFinding(value: ControlFinding) {
        writeString(value.severity.name)
        writeString(value.title)
        writeString(value.detail)
    }

    private fun DataInputStream.readFinding(): ControlFinding =
        ControlFinding(
            severity = enumValueOf(readString()),
            title = readString(),
            detail = readString(),
        )

    private fun DataOutputStream.writeRule(value: DomainRule) {
        writeString(value.id)
        writeString(value.title)
        writeString(value.source)
        writeString(value.status.name)
    }

    private fun DataInputStream.readRule(): DomainRule =
        DomainRule(
            id = readString(),
            title = readString(),
            source = readString(),
            status = enumValueOf(readString()),
        )

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size >= 0) { "Negative string length in finalized snapshot." }
        val bytes = ByteArray(size)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun DataOutputStream.writeDecimal(value: BigDecimal) =
        writeString(value.toPlainString())

    private fun DataInputStream.readDecimal(): BigDecimal =
        BigDecimal(readString())

    private fun DataOutputStream.writeDate(value: LocalDate) =
        writeString(value.toString())

    private fun DataInputStream.readDate(): LocalDate =
        LocalDate.parse(readString())

    private fun DataOutputStream.writeDateTime(value: LocalDateTime) =
        writeString(value.toString())

    private fun DataInputStream.readDateTime(): LocalDateTime =
        LocalDateTime.parse(readString())

    private fun DataOutputStream.writeNullableDateTime(value: LocalDateTime?) {
        writeBoolean(value != null)
        if (value != null) writeDateTime(value)
    }

    private fun DataInputStream.readNullableDateTime(): LocalDateTime? =
        if (readBoolean()) readDateTime() else null

    private fun <T> DataOutputStream.writeList(
        values: List<T>,
        writeItem: DataOutputStream.(T) -> Unit,
    ) {
        writeInt(values.size)
        values.forEach { writeItem(it) }
    }

    private fun <T> DataInputStream.readList(
        readItem: DataInputStream.() -> T,
    ): List<T> {
        val count = readInt()
        require(count >= 0) { "Negative list length in finalized snapshot." }
        return List(count) { readItem() }
    }
}
