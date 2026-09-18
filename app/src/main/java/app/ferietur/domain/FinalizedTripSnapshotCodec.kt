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
    private const val LEGACY_FORMAT_VERSION = 1
    private const val SINGLE_CONTEXT_FORMAT_VERSION = 2
    private const val MULTI_CONTEXT_PROVENANCE_FORMAT_VERSION = 3
    private const val CALCULATION_PAYLOAD_FORMAT_VERSION = 4
    private const val HOLIDAY_WORK_PLAN_STATUS_FORMAT_VERSION = 5
    private const val PERIOD_RELATION_FORMAT_VERSION = 6
    private const val WORK_PLAN_BASIS_FORMAT_VERSION = 7
    private const val FORMAT_VERSION = WORK_PLAN_BASIS_FORMAT_VERSION
    private const val LEGACY_UNKNOWN_TARIFF_PACKAGE_ID = "legacy-v1-unknown-tariff-package"
    private const val LEGACY_UNKNOWN_RATE_SET_ID = "legacy-v1-unknown-rate-set"
    // Snapshot format v1 did not persist tariff package/rate-set identity.
    // Ruleset 2026.3 + the 2026 Oslo salary table is the historical combination
    // that predates explicit tariff provenance and can be mapped deterministically.
    // Keep this frozen: it must not move when the current ruleset advances.
    private const val LEGACY_DOK25_2026_2028_RULESET_VERSION = "2026.3"

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
            require(
                version == LEGACY_FORMAT_VERSION ||
                    version == SINGLE_CONTEXT_FORMAT_VERSION ||
                    version == MULTI_CONTEXT_PROVENANCE_FORMAT_VERSION ||
                    version == CALCULATION_PAYLOAD_FORMAT_VERSION ||
                    version == HOLIDAY_WORK_PLAN_STATUS_FORMAT_VERSION ||
                    version == PERIOD_RELATION_FORMAT_VERSION ||
                    version == FORMAT_VERSION,
            ) {
                "Unsupported finalized snapshot format: $version"
            }
            input.readSnapshot(version)
        }
    }

    private fun DataOutputStream.writeSnapshot(snapshot: FinalizedTripSnapshot) {
        writeString(snapshot.id)
        writeDateTime(snapshot.createdAt)
        writeString(snapshot.appVersionName)
        writeInt(snapshot.appVersionCode)
        writeString(snapshot.rulesetVersion)
        writeString(snapshot.tariffPackageId)
        writeString(snapshot.tariffRateSetId)
        writeString(snapshot.salaryTableId)
        writeDate(snapshot.salaryTableEffectiveFrom)
        writeString(snapshot.salaryTableSourceLabel)
        writeString(snapshot.title)
        writeDateTime(snapshot.tripStart)
        writeDateTime(snapshot.tripEnd)
        writeList(snapshot.tariffContexts) { writeTariffContext(it) }
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
        writeCalculationPayload(snapshot.calculationPayload)
        writeSettlement(snapshot.settlement)
        writeList(snapshot.findings) { writeFinding(it) }
        writeList(snapshot.unresolvedRules) { writeRule(it) }
        writeString(snapshot.holidayWorkPlanStatus.name)
        writeString(snapshot.workPlanBasis.name)
        writeList(snapshot.employerWorkPlanBlocks) { writeWorkBlock(it) }
    }

    private fun DataInputStream.readSnapshot(formatVersion: Int): FinalizedTripSnapshot {
        val id = readString()
        val createdAt = readDateTime()
        val appVersionName = readString()
        val appVersionCode = readInt()
        val rulesetVersion = readString()
        val storedTariffPackageId = if (formatVersion >= SINGLE_CONTEXT_FORMAT_VERSION) readString() else null
        val storedTariffRateSetId = if (formatVersion >= SINGLE_CONTEXT_FORMAT_VERSION) readString() else null
        val salaryTableId = readString()
        val salaryTableEffectiveFrom = readDate()
        val salaryTableSourceLabel = readString()
        val legacyProvenance = legacyTariffProvenance(rulesetVersion, salaryTableId)
        val tariffPackageId = storedTariffPackageId ?: legacyProvenance.first
        val tariffRateSetId = storedTariffRateSetId ?: legacyProvenance.second
        val title = readString()
        val tripStart = readDateTime()
        val tripEnd = readDateTime()
        val storedTariffContexts =
            if (formatVersion >= MULTI_CONTEXT_PROVENANCE_FORMAT_VERSION) readList { readTariffContext() } else null
        val employerKind: EmployerKind = enumValueOf(readString())
        val payingParty: PayingParty = enumValueOf(readString())
        val rosterComparisonMode: RosterComparisonMode = enumValueOf(readString())
        val salaryStep = readInt()
        val annualSalary = readDecimal()
        val weeklyBasis: WeeklyBasis = enumValueOf(readString())
        val weekendProfile: WeekendProfile = enumValueOf(readString())
        val payslipChecked = readBoolean()
        val rosterGapConfirmed = readBoolean()
        val roster = readList { readRosterRow() }
        val workBlocks = readList { readWorkBlock(formatVersion) }
        val calculationPayload = if (formatVersion >= CALCULATION_PAYLOAD_FORMAT_VERSION) {
            readCalculationPayload()
        } else {
            FinalizedCalculationPayload.Preliminary(readCalculation())
        }
        val settlement = readSettlement()
        val findings = readList { readFinding() }
        val unresolvedRules = readList { readRule() }
        val holidayWorkPlanStatus = if (formatVersion >= HOLIDAY_WORK_PLAN_STATUS_FORMAT_VERSION) {
            enumValueOf<HolidayWorkPlanStatus>(readString())
        } else {
            HolidayWorkPlanStatus.NOT_CLARIFIED
        }
        val workPlanBasis = if (formatVersion >= WORK_PLAN_BASIS_FORMAT_VERSION) {
            enumValueOf<TripWorkPlanBasis>(readString())
        } else {
            TripWorkPlanBasis.NOT_CLARIFIED
        }
        val employerWorkPlanBlocks = if (formatVersion >= WORK_PLAN_BASIS_FORMAT_VERSION) {
            readList { readWorkBlock(formatVersion) }
        } else {
            emptyList()
        }

        val tariffContexts = storedTariffContexts ?: listOf(
            legacySingleContext(
                tripStart = tripStart,
                tripEnd = tripEnd,
                tariffPackageId = tariffPackageId,
                rulesetVersion = rulesetVersion,
                tariffRateSetId = tariffRateSetId,
                salaryTableId = salaryTableId,
                salaryTableEffectiveFrom = salaryTableEffectiveFrom,
                salaryTableSourceLabel = salaryTableSourceLabel,
                annualSalary = annualSalary,
                hourlyRate = requireNotNull(calculationPayload.preliminaryOrNull) {
                    "Legacy finalized snapshot without explicit tariff contexts must contain PreliminaryCalculation."
                }.hourlyRate,
            ),
        )

        return FinalizedTripSnapshot(
            id = id,
            createdAt = createdAt,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            rulesetVersion = rulesetVersion,
            tariffPackageId = tariffPackageId,
            tariffRateSetId = tariffRateSetId,
            salaryTableId = salaryTableId,
            salaryTableEffectiveFrom = salaryTableEffectiveFrom,
            salaryTableSourceLabel = salaryTableSourceLabel,
            tariffContexts = tariffContexts,
            title = title,
            tripStart = tripStart,
            tripEnd = tripEnd,
            employerKind = employerKind,
            payingParty = payingParty,
            rosterComparisonMode = rosterComparisonMode,
            salaryStep = salaryStep,
            annualSalary = annualSalary,
            weeklyBasis = weeklyBasis,
            weekendProfile = weekendProfile,
            payslipChecked = payslipChecked,
            rosterGapConfirmed = rosterGapConfirmed,
            roster = roster,
            workBlocks = workBlocks,
            calculationPayload = calculationPayload,
            settlement = settlement,
            findings = findings,
            unresolvedRules = unresolvedRules,
            holidayWorkPlanStatus = holidayWorkPlanStatus,
            workPlanBasis = workPlanBasis,
            employerWorkPlanBlocks = employerWorkPlanBlocks,
        )
    }

    private fun legacySingleContext(
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        tariffPackageId: String,
        rulesetVersion: String,
        tariffRateSetId: String,
        salaryTableId: String,
        salaryTableEffectiveFrom: LocalDate,
        salaryTableSourceLabel: String,
        annualSalary: BigDecimal,
        hourlyRate: BigDecimal,
    ): FinalizedTariffContextSnapshot {
        val occupied = requireNotNull(TariffEffectiveDateRange.forTrip(tripStart, tripEnd)) {
            "Legacy finalized snapshot has invalid trip interval."
        }
        return FinalizedTariffContextSnapshot(
            start = occupied.start,
            end = occupied.end,
            tariffPackageId = tariffPackageId,
            rulesetVersion = rulesetVersion,
            tariffRateSetId = tariffRateSetId,
            salaryTableId = salaryTableId,
            salaryTableEffectiveFrom = salaryTableEffectiveFrom,
            salaryTableSourceLabel = salaryTableSourceLabel,
            annualSalary = annualSalary,
            hourlyRate = hourlyRate,
        )
    }

    private fun legacyTariffProvenance(rulesetVersion: String, salaryTableId: String): Pair<String, String> =
        if (
            rulesetVersion == LEGACY_DOK25_2026_2028_RULESET_VERSION &&
            salaryTableId == OsloSalaryTable2026.tableId
        ) {
            FerieturTariffs.DOK25_2026_2028_ID to FerieturTariffRates.DOK25_2026_2028_RATE_SET_ID
        } else {
            LEGACY_UNKNOWN_TARIFF_PACKAGE_ID to LEGACY_UNKNOWN_RATE_SET_ID
        }

    private fun DataOutputStream.writeTariffContext(value: FinalizedTariffContextSnapshot) {
        writeDate(value.start)
        writeDate(value.end)
        writeString(value.tariffPackageId)
        writeString(value.rulesetVersion)
        writeString(value.tariffRateSetId)
        writeString(value.salaryTableId)
        writeDate(value.salaryTableEffectiveFrom)
        writeString(value.salaryTableSourceLabel)
        writeDecimal(value.annualSalary)
        writeDecimal(value.hourlyRate)
    }

    private fun DataInputStream.readTariffContext(): FinalizedTariffContextSnapshot =
        FinalizedTariffContextSnapshot(
            start = readDate(),
            end = readDate(),
            tariffPackageId = readString(),
            rulesetVersion = readString(),
            tariffRateSetId = readString(),
            salaryTableId = readString(),
            salaryTableEffectiveFrom = readDate(),
            salaryTableSourceLabel = readString(),
            annualSalary = readDecimal(),
            hourlyRate = readDecimal(),
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
        writeString(block.holidayWorkPlanRelation.name)
        writeString(block.travelDutyStatus.name)
    }

    private fun DataInputStream.readWorkBlock(formatVersion: Int): WorkBlock {
        val start = readDateTime()
        val end = readDateTime()
        val kind: TimeKind = enumValueOf(readString())
        val travelNoticeStatus: TravelNoticeStatus = enumValueOf(readString())
        val holidayWorkPlanRelation =
            if (formatVersion >= PERIOD_RELATION_FORMAT_VERSION) {
                enumValueOf<HolidayWorkPlanRelation>(readString())
            } else {
                HolidayWorkPlanRelation.NOT_CLARIFIED
            }
        val travelDutyStatus =
            if (formatVersion >= PERIOD_RELATION_FORMAT_VERSION) {
                enumValueOf<TravelDutyStatus>(readString())
            } else {
                TravelDutyStatus.NOT_CLARIFIED
            }
        return WorkBlock(
            start = start,
            end = end,
            kind = kind,
            travelNoticeStatus = travelNoticeStatus,
            holidayWorkPlanRelation = holidayWorkPlanRelation,
            travelDutyStatus = travelDutyStatus,
        )
    }

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

    private fun DataOutputStream.writeScopedCalculationLine(value: FinalizedScopedCalculationLineSnapshot) {
        writeString(value.scope.name)
        writeBoolean(value.sliceIndex != null)
        if (value.sliceIndex != null) writeInt(value.sliceIndex)
        writeBoolean(value.watchSourceIndex != null)
        if (value.watchSourceIndex != null) writeInt(value.watchSourceIndex)
        writeCalculationLine(value.line)
    }

    private fun DataInputStream.readScopedCalculationLine(): FinalizedScopedCalculationLineSnapshot {
        val scope: TariffCalculationLineScope = enumValueOf(readString())
        val sliceIndex = if (readBoolean()) readInt() else null
        val watchSourceIndex = if (readBoolean()) readInt() else null
        return FinalizedScopedCalculationLineSnapshot(
            scope = scope,
            line = readCalculationLine(),
            sliceIndex = sliceIndex,
            watchSourceIndex = watchSourceIndex,
        )
    }

    private fun DataOutputStream.writeCalculationPayload(value: FinalizedCalculationPayload) {
        writeString(value.mode.name)
        when (value) {
            is FinalizedCalculationPayload.Preliminary -> writeCalculation(value.calculation)
            is FinalizedCalculationPayload.SegmentedContexts -> {
                writeList(value.lineEntries) { writeScopedCalculationLine(it) }
                writeList(value.applicableUnresolvedRuleIds.sorted()) { writeString(it) }
            }
        }
    }

    private fun DataInputStream.readCalculationPayload(): FinalizedCalculationPayload =
        when (val mode: FinalizedCalculationPayloadMode = enumValueOf(readString())) {
            FinalizedCalculationPayloadMode.PRELIMINARY ->
                FinalizedCalculationPayload.Preliminary(readCalculation())
            FinalizedCalculationPayloadMode.SEGMENTED_CONTEXTS ->
                FinalizedCalculationPayload.SegmentedContexts(
                    lineEntries = readList { readScopedCalculationLine() },
                    applicableUnresolvedRuleIds = readList { readString() }.toSet(),
                )
        }

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
