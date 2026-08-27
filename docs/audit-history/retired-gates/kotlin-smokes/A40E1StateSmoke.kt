import app.ferietur.domain.TimeKind
import app.ferietur.ui.nightTravelSleepStatus
import app.ferietur.ui.periodTypeSelection
import app.ferietur.ui.travelWithoutResponsibilitySelection

fun main() {
    check(travelWithoutResponsibilitySelection(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED) == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED)
    check(travelWithoutResponsibilitySelection(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP) == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP)
    check(travelWithoutResponsibilitySelection(TimeKind.TRAVEL_WITH_RESPONSIBILITY) == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY)
    check(nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, true) == "Søvntillatelse: Ja")
    check(nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP, true) == "Søvntillatelse: Nei")
    check(nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, true) == "Søvntillatelse: Ikke avklart")
    check(nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, false) == null)
    check(periodTypeSelection(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, TimeKind.TRAVEL_UNCERTAIN) == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED)
    println("A40E1_STATE_SMOKE=PASS")
}
