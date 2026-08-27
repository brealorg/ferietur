package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Legacy/internal calculation switch kept while the calculation engine is being split from
 * payment/employer semantics. New UI and persistence use [RosterComparisonMode].
 */
enum class FundingMode {
    TURNUS_PLUS_EXTERNAL,
    VACATION_SEPARATE,
    MUNICIPAL_ALL,
    CUSTOM,
}

enum class EmployerKind {
    UNSPECIFIED,
    OSLO_KOMMUNE,
    PRIVATE_OR_OTHER_UNRESOLVED,
}

enum class PayingParty {
    UNSPECIFIED,
    OSLO_KOMMUNE,
    RESIDENT_OR_GUARDIAN,
    OTHER,
}

enum class RosterComparisonMode {
    USE_NORMAL_ROSTER,
    DO_NOT_USE_NORMAL_ROSTER,
}

fun RosterComparisonMode.toFundingMode(): FundingMode = when (this) {
    RosterComparisonMode.USE_NORMAL_ROSTER -> FundingMode.TURNUS_PLUS_EXTERNAL
    RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER -> FundingMode.VACATION_SEPARATE
}

fun FundingMode.toRosterComparisonMode(): RosterComparisonMode = when (this) {
    FundingMode.TURNUS_PLUS_EXTERNAL -> RosterComparisonMode.USE_NORMAL_ROSTER
    FundingMode.VACATION_SEPARATE,
    FundingMode.MUNICIPAL_ALL,
    FundingMode.CUSTOM -> RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER
}

fun EmployerKind.ruleBasisLabel(): String = when (this) {
    EmployerKind.OSLO_KOMMUNE -> "Oslo kommune – Dok. 25 2026–28, kapittel 20"
    EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED -> "Ikke endelig avklart"
    EmployerKind.UNSPECIFIED -> "Avklares ut fra hvordan turen organiseres"
}

fun EmployerKind.isRuleBasisConfirmed(): Boolean = this == EmployerKind.OSLO_KOMMUNE

enum class TravelNoticeStatus {
    /** Reisen var kjent senest dagen i forveien. */
    KNOWN_BY_PREVIOUS_DAY,
    /** Reisen var ikke kjent senest dagen i forveien. */
    NOT_KNOWN_BY_PREVIOUS_DAY,
    /** Det er ikke avklart når arbeidstakeren fikk vite om reisen. */
    NOT_CLARIFIED,
}

enum class TimeKind {
    ACTIVE_WORK,
    ACTIVE_NIGHT_WATCH,
    RESTING_NIGHT_WATCH,
    TRAVEL_WITH_RESPONSIBILITY,
    /** Reise uten tilsynsansvar der søvntillatelse for eventuell nattdel ikke er avklart. */
    TRAVEL_WITHOUT_RESPONSIBILITY,
    /** Reise uten tilsynsansvar der arbeidstakeren ikke hadde tillatelse til å sove kl. 23–07. */
    TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
    /** Reise uten tilsynsansvar der arbeidstakeren hadde tillatelse til å sove kl. 23–07. */
    TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
    TRAVEL_UNCERTAIN,
    ACTIVE_EVENT_ON_RESTING,
}

fun TimeKind.isTravelWithoutResponsibility(): Boolean = this == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY ||
    this == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP ||
    this == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED

fun TimeKind.isTravelKind(): Boolean = this == TimeKind.TRAVEL_WITH_RESPONSIBILITY ||
    isTravelWithoutResponsibility() ||
    this == TimeKind.TRAVEL_UNCERTAIN

data class ShiftInstance(
    val date: LocalDate,
    val shiftCode: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
)

data class WorkBlock(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val kind: TimeKind,
    val travelNoticeStatus: TravelNoticeStatus = TravelNoticeStatus.NOT_CLARIFIED,
)

data class PlannedBlock(
    val kind: TimeKind,
    val start: LocalTime,
    val end: LocalTime,
    val travelNoticeStatus: TravelNoticeStatus = TravelNoticeStatus.NOT_CLARIFIED,
) {
    fun toWorkBlock(date: LocalDate): WorkBlock {
        val startDateTime = LocalDateTime.of(date, start)
        val endDate = if (end.isAfter(start)) date else date.plusDays(1)
        return WorkBlock(startDateTime, LocalDateTime.of(endDate, end), kind, travelNoticeStatus)
    }
}

data class TripDraft(
    val title: String,
    val employerKind: EmployerKind,
    val payingParty: PayingParty,
    val rosterComparisonMode: RosterComparisonMode,
    val from: LocalDate,
    val to: LocalDate,
)
