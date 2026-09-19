package app.ferietur.domain

import java.io.Reader
import java.io.Writer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Properties

/**
 * Local, editable trip state. This is deliberately separate from FinalizedTripSnapshot:
 * a draft may be resumed and changed, while a finalized snapshot documents one frozen calculation.
 */
data class SavedTripDraft(
    val id: String,
    val updatedAtEpochMillis: Long,
    val screen: String,
    val title: String,
    val employerKind: EmployerKind,
    val payingParty: PayingParty,
    val rosterComparisonMode: RosterComparisonMode,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val salaryStep: Int,
    val weeklyBasis: WeeklyBasis,
    val weekendProfile: WeekendProfile,
    val payslipChecked: Boolean,
    val rosterGapConfirmed: Boolean,
    val roster: Map<LocalDate, String>,
    val plans: Map<LocalDate, List<PlannedBlock>>,
    val outboundArrival: LocalDateTime,
    val returnDeparture: LocalDateTime,
    val outboundTravelKind: TimeKind?,
    val returnTravelKind: TimeKind?,
    val settlementMode: String,
    val settlementAmountText: String,
    val settlementReason: String,
    val finalizedSnapshot: FinalizedTripSnapshot? = null,
    val finalizationHistory: List<FinalizedTripSnapshot> = emptyList(),
    val migrationHistory: Set<String> = emptySet(),
    val holidayWorkPlanStatus: HolidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED,
    val workPlanBasis: TripWorkPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED,
    val holidayPlans: Map<LocalDate, List<PlannedBlock>> = emptyMap(),
)

data class DecodedSavedTripDraft(
    val draft: SavedTripDraft,
    val sourceSchemaVersion: Int,
)

class UnsupportedSavedTripSchemaException(
    val schemaVersion: Int,
    val maxSupportedSchemaVersion: Int,
) : IllegalArgumentException(
    "Unsupported saved trip schema: $schemaVersion (max supported: $maxSupportedSchemaVersion)",
)

/**
 * SECURITY04: trip IDs become file and directory names in TripDraftStore and part of the
 * exported PDF file name. IDs read from external `.ferietur` backups are untrusted, so only a
 * conservative character set is accepted. App-generated UUIDs always satisfy this rule.
 */
object SafeStorageId {
    const val MAX_LENGTH = 64
    private val pattern = Regex("^[A-Za-z0-9_-]{1,$MAX_LENGTH}$")

    fun isValid(value: String): Boolean = pattern.matches(value)

    fun requireValid(value: String, label: String = "ID"): String {
        require(isValid(value)) {
            "Ugyldig $label: bare bokstavene A–Z, tall, bindestrek og understrek er tillatt (maks $MAX_LENGTH tegn)."
        }
        return value
    }
}

object SavedTripDraftCodec {
    const val SCHEMA_VERSION = 10

    fun write(draft: SavedTripDraft, writer: Writer) {
        val properties = Properties().apply {
            setProperty("schemaVersion", SCHEMA_VERSION.toString())
            setProperty("id", SafeStorageId.requireValid(draft.id, "tur-ID"))
            setProperty("updatedAtEpochMillis", draft.updatedAtEpochMillis.toString())
            setProperty("screen", draft.screen)
            setProperty("title", draft.title)
            setProperty("employerKind", draft.employerKind.name)
            setProperty("payingParty", draft.payingParty.name)
            setProperty("rosterComparisonMode", draft.rosterComparisonMode.name)
            // Kept as a compatibility breadcrumb for troubleshooting older local drafts.
            setProperty("fundingMode", draft.rosterComparisonMode.toFundingMode().name)
            setProperty("startDate", draft.startDate.toString())
            setProperty("endDate", draft.endDate.toString())
            setProperty("startTime", draft.startTime.toString())
            setProperty("endTime", draft.endTime.toString())
            setProperty("salaryStep", draft.salaryStep.toString())
            setProperty("weeklyBasis", draft.weeklyBasis.name)
            setProperty("weekendProfile", draft.weekendProfile.name)
            setProperty("payslipChecked", draft.payslipChecked.toString())
            setProperty("rosterGapConfirmed", draft.rosterGapConfirmed.toString())
            setProperty("outboundArrival", draft.outboundArrival.toString())
            setProperty("returnDeparture", draft.returnDeparture.toString())
            draft.outboundTravelKind?.let { setProperty("outboundTravelKind", it.name) }
            draft.returnTravelKind?.let { setProperty("returnTravelKind", it.name) }
            setProperty("settlementMode", draft.settlementMode)
            setProperty("settlementAmountText", draft.settlementAmountText)
            setProperty("settlementReason", draft.settlementReason)
            draft.finalizedSnapshot?.let {
                setProperty("finalized.current", FinalizedTripSnapshotCodec.encode(it))
            }
            setProperty("finalized.history.count", draft.finalizationHistory.size.toString())
            draft.finalizationHistory.forEachIndexed { index, snapshot ->
                setProperty("finalized.history.$index", FinalizedTripSnapshotCodec.encode(snapshot))
            }
            setProperty("migrationHistory", draft.migrationHistory.sorted().joinToString(","))
            setProperty("holidayWorkPlanStatus", draft.holidayWorkPlanStatus.name)
            setProperty("workPlanBasis", draft.workPlanBasis.name)

            draft.roster.toSortedMap().forEach { (date, code) ->
                setProperty("roster.$date", code)
            }
            draft.plans.toSortedMap().forEach { (date, blocks) ->
                blocks.forEachIndexed { index, block ->
                    setProperty(
                        "plan.$date.$index",
                        listOf(
                            block.kind.name,
                            block.start.toString(),
                            block.end.toString(),
                            block.travelNoticeStatus.name,
                            block.holidayWorkPlanRelation.name,
                            block.travelDutyStatus.name,
                        ).joinToString("|"),
                    )
                }
            }
            draft.holidayPlans.toSortedMap().forEach { (date, blocks) ->
                blocks.forEachIndexed { index, block ->
                    setProperty(
                        "holidayPlan.$date.$index",
                        listOf(
                            block.kind.name,
                            block.start.toString(),
                            block.end.toString(),
                            block.travelNoticeStatus.name,
                            block.holidayWorkPlanRelation.name,
                            block.travelDutyStatus.name,
                        ).joinToString("|"),
                    )
                }
            }
        }
        properties.store(writer, "FERIETUR01 local draft")
    }

    fun read(reader: Reader): SavedTripDraft =
        readDecoded(reader).draft

    fun readDecoded(reader: Reader): DecodedSavedTripDraft {
        val properties = Properties().apply { load(reader) }
        val version = properties.requireProperty("schemaVersion").toInt()
        if (version !in 1..SCHEMA_VERSION) {
            throw UnsupportedSavedTripSchemaException(
                schemaVersion = version,
                maxSupportedSchemaVersion = SCHEMA_VERSION,
            )
        }

        val roster = linkedMapOf<LocalDate, String>()
        val plansByDate = linkedMapOf<LocalDate, MutableList<Pair<Int, PlannedBlock>>>()
        val holidayPlansByDate = linkedMapOf<LocalDate, MutableList<Pair<Int, PlannedBlock>>>()

        properties.stringPropertyNames().forEach { key ->
            when {
                key.startsWith("roster.") -> {
                    val date = LocalDate.parse(key.removePrefix("roster."))
                    roster[date] = properties.requireProperty(key)
                }
                key.startsWith("holidayPlan.") -> {
                    val suffix = key.removePrefix("holidayPlan.")
                    val lastDot = suffix.lastIndexOf('.')
                    require(lastDot > 0) { "Malformed saved holiday plan key: $key" }
                    val date = LocalDate.parse(suffix.substring(0, lastDot))
                    val index = suffix.substring(lastDot + 1).toInt()
                    val parts = properties.requireProperty(key).split('|')
                    require(parts.size in 3..6) { "Malformed saved holiday plan value: $key" }
                    val kind = TimeKind.valueOf(parts[0])
                    val notice = if (parts.size >= 4) {
                        enumValueOrDefault(parts[3], TravelNoticeStatus.NOT_CLARIFIED)
                    } else {
                        TravelNoticeStatus.NOT_CLARIFIED
                    }
                    val workPlanRelation = if (parts.size >= 5) {
                        enumValueOrDefault(parts[4], HolidayWorkPlanRelation.NOT_CLARIFIED)
                    } else {
                        HolidayWorkPlanRelation.NOT_CLARIFIED
                    }
                    val travelDutyStatus = if (parts.size >= 6) {
                        enumValueOrDefault(parts[5], TravelDutyStatus.NOT_CLARIFIED)
                    } else {
                        TravelDutyStatus.NOT_CLARIFIED
                    }
                    val block = PlannedBlock(
                        kind = kind,
                        start = LocalTime.parse(parts[1]),
                        end = LocalTime.parse(parts[2]),
                        travelNoticeStatus = notice,
                        holidayWorkPlanRelation = workPlanRelation,
                        travelDutyStatus = travelDutyStatus,
                    )
                    holidayPlansByDate.getOrPut(date) { mutableListOf() }.add(index to block)
                }
                key.startsWith("plan.") -> {
                    val suffix = key.removePrefix("plan.")
                    val lastDot = suffix.lastIndexOf('.')
                    require(lastDot > 0) { "Malformed saved plan key: $key" }
                    val date = LocalDate.parse(suffix.substring(0, lastDot))
                    val index = suffix.substring(lastDot + 1).toInt()
                    val parts = properties.requireProperty(key).split('|')
                    require(parts.size in 3..6) { "Malformed saved plan value: $key" }
                    val kind = TimeKind.valueOf(parts[0])
                    val notice = if (parts.size >= 4) {
                        enumValueOrDefault(parts[3], TravelNoticeStatus.NOT_CLARIFIED)
                    } else {
                        TravelNoticeStatus.NOT_CLARIFIED
                    }
                    val workPlanRelation = if (parts.size >= 5) {
                        enumValueOrDefault(parts[4], HolidayWorkPlanRelation.NOT_CLARIFIED)
                    } else {
                        HolidayWorkPlanRelation.NOT_CLARIFIED
                    }
                    val travelDutyStatus = if (parts.size >= 6) {
                        enumValueOrDefault(parts[5], TravelDutyStatus.NOT_CLARIFIED)
                    } else {
                        TravelDutyStatus.NOT_CLARIFIED
                    }
                    val block = PlannedBlock(
                        kind = kind,
                        start = LocalTime.parse(parts[1]),
                        end = LocalTime.parse(parts[2]),
                        travelNoticeStatus = notice,
                        holidayWorkPlanRelation = workPlanRelation,
                        travelDutyStatus = travelDutyStatus,
                    )
                    plansByDate.getOrPut(date) { mutableListOf() }.add(index to block)
                }
            }
        }

        val legacyFundingMode = enumValueOrDefault(
            properties.getProperty("fundingMode"),
            FundingMode.TURNUS_PLUS_EXTERNAL,
        )
        val rosterComparisonMode = if (version >= 2) {
            enumValueOrDefault(
                properties.getProperty("rosterComparisonMode"),
                legacyFundingMode.toRosterComparisonMode(),
            )
        } else {
            legacyFundingMode.toRosterComparisonMode()
        }
        // Schema v1 mixed comparison/payment/employment semantics. Do not infer employer or payer
        // from that ambiguous choice; preserve the trip and ask the user to choose explicitly.
        val employerKind = if (version >= 2) {
            enumValueOrDefault(properties.getProperty("employerKind"), EmployerKind.UNSPECIFIED)
        } else {
            EmployerKind.UNSPECIFIED
        }
        val payingParty = if (version >= 2) {
            enumValueOrDefault(properties.getProperty("payingParty"), PayingParty.UNSPECIFIED)
        } else {
            PayingParty.UNSPECIFIED
        }

        val decoded = SavedTripDraft(
            id = SafeStorageId.requireValid(properties.requireProperty("id"), "tur-ID"),
            updatedAtEpochMillis = properties.requireProperty("updatedAtEpochMillis").toLong(),
            screen = properties.getProperty("screen", "TRIP"),
            title = properties.getProperty("title", "Ferietur"),
            employerKind = employerKind,
            payingParty = payingParty,
            rosterComparisonMode = rosterComparisonMode,
            startDate = LocalDate.parse(properties.requireProperty("startDate")),
            endDate = LocalDate.parse(properties.requireProperty("endDate")),
            startTime = LocalTime.parse(properties.requireProperty("startTime")),
            endTime = LocalTime.parse(properties.requireProperty("endTime")),
            salaryStep = properties.getProperty("salaryStep", "32").toInt(),
            weeklyBasis = enumValueOrDefault(properties.getProperty("weeklyBasis"), WeeklyBasis.HOURS_35_5),
            weekendProfile = enumValueOrDefault(properties.getProperty("weekendProfile"), WeekendProfile.STANDARD),
            payslipChecked = properties.getProperty("payslipChecked", "false").toBoolean(),
            rosterGapConfirmed = if (version >= 3) properties.getProperty("rosterGapConfirmed", "false").toBoolean() else false,
            roster = roster,
            plans = plansByDate.mapValues { (_, indexed) -> indexed.sortedBy { it.first }.map { it.second } },
            outboundArrival = LocalDateTime.parse(properties.requireProperty("outboundArrival")),
            returnDeparture = LocalDateTime.parse(properties.requireProperty("returnDeparture")),
            outboundTravelKind = enumValueOrNull<TimeKind>(properties.getProperty("outboundTravelKind")),
            returnTravelKind = enumValueOrNull<TimeKind>(properties.getProperty("returnTravelKind")),
            settlementMode = properties.getProperty("settlementMode", "FULL_CALCULATION"),
            settlementAmountText = properties.getProperty("settlementAmountText", ""),
            settlementReason = properties.getProperty("settlementReason", ""),
            finalizedSnapshot = if (version >= 6) {
                properties.getProperty("finalized.current")?.let(FinalizedTripSnapshotCodec::decode)
            } else {
                null
            },
            finalizationHistory = if (version >= 6) {
                val count = properties.getProperty("finalized.history.count", "0").toInt()
                List(count) { index ->
                    FinalizedTripSnapshotCodec.decode(
                        properties.requireProperty("finalized.history.$index"),
                    )
                }
            } else {
                emptyList()
            },
            migrationHistory = if (version >= 6) {
                properties.getProperty("migrationHistory")
                    .orEmpty()
                    .split(',')
                    .filter(String::isNotBlank)
                    .toSet()
            } else {
                emptySet()
            },
            holidayWorkPlanStatus = if (version >= 7) {
                enumValueOrDefault(
                    properties.getProperty("holidayWorkPlanStatus"),
                    HolidayWorkPlanStatus.NOT_CLARIFIED,
                )
            } else {
                HolidayWorkPlanStatus.NOT_CLARIFIED
            },
            workPlanBasis = if (version >= 10) {
                enumValueOrDefault(
                    properties.getProperty("workPlanBasis"),
                    TripWorkPlanBasis.NOT_CLARIFIED,
                )
            } else {
                TripWorkPlanBasis.NOT_CLARIFIED
            },
            holidayPlans = if (version >= 9) {
                holidayPlansByDate.mapValues { (_, indexed) ->
                    indexed.sortedBy { it.first }.map { it.second }
                }
            } else {
                emptyMap()
            },
        )
        val migrated = SavedTripDraftMigrator.migrate(
            sourceSchemaVersion = version,
            draft = decoded,
        )
        return DecodedSavedTripDraft(
            draft = migrated,
            sourceSchemaVersion = version,
        )
    }

    private fun Properties.requireProperty(key: String): String =
        getProperty(key) ?: error("Missing saved trip property: $key")

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
