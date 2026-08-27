import app.ferietur.domain.SolhaugenShiftCatalog
import app.ferietur.domain.TariffMath
import app.ferietur.domain.TurnusOverlapEngine
import java.time.LocalDate
import java.time.LocalTime

fun main() {
    val lv = requireNotNull(SolhaugenShiftCatalog.byCode("LV"))
    check(lv.durationMinutes() == 720L)
    check(SolhaugenShiftCatalog.tripRelevant.none { it.code == "K6" })

    val overlap = TurnusOverlapEngine.compare(
        LocalDate.of(2026, 8, 17),
        LocalTime.of(7, 0),
        LocalTime.of(23, 0),
        lv,
    )
    check(overlap.insideTurnusMinutes == 720L)
    check(overlap.outsideTurnusMinutes == 240L)

    val passive = TariffMath.passiveNight(480)
    check(passive.workTimeMinutes == 480)
    check(passive.payEquivalentMinutes.toPlainString() == "160.00000000")
    check(TariffMath.roundActiveNightMinutes(14) == 0)
    check(TariffMath.roundActiveNightMinutes(15) == 30)

    println("FERIETUR01_DOMAIN_SMOKE=PASS")
}
