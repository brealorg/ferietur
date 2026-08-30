package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate

object OsloSalaryTable2026 {
    const val tableId = "oslo-salary-2026-05-01"
    val effectiveFromDate: LocalDate = LocalDate.of(2026, 5, 1)
    val verifiedThroughDate: LocalDate = LocalDate.of(2027, 4, 30)
    const val effectiveFrom = "01.05.2026"
    const val tariffPackageId = FerieturTariffs.DOK25_2026_2028_ID
    const val sourceLabel = "Lønnstabell Oslo kommune fra 01.05.2026"
    const val sourcePageUrl = "https://www.oslo.kommune.no/jobb-i-oslo-kommune/"

    private val annualSalaryByStep = mapOf(
        1 to 473350,
        2 to 476650,
        3 to 480050,
        4 to 483550,
        5 to 487050,
        6 to 490650,
        7 to 494350,
        8 to 498150,
        9 to 501950,
        10 to 505450,
        11 to 509050,
        12 to 512550,
        13 to 516250,
        14 to 520350,
        15 to 524350,
        16 to 528550,
        17 to 532550,
        18 to 536750,
        19 to 540950,
        20 to 545150,
        21 to 550050,
        22 to 554750,
        23 to 559350,
        24 to 566600,
        25 to 571000,
        26 to 576000,
        27 to 581200,
        28 to 586800,
        29 to 592700,
        30 to 599100,
        31 to 606200,
        32 to 614600,
        33 to 623400,
        34 to 633200,
        35 to 643000,
        36 to 654100,
        37 to 666400,
        38 to 677500,
        39 to 689300,
        40 to 702200,
        41 to 714100,
        42 to 726900,
        43 to 740600,
        44 to 752700,
        45 to 767500,
        46 to 782700,
        47 to 798200,
        48 to 813500,
        49 to 828300,
        50 to 844000,
        51 to 859400,
        52 to 874800,
        53 to 891600,
        54 to 909000,
        55 to 930100,
        56 to 951400,
        57 to 969300,
        58 to 988200,
        59 to 1007700,
        60 to 1028300,
        61 to 1048400,
        62 to 1069400,
        63 to 1091300,
        64 to 1111000,
        65 to 1133400,
        66 to 1155900,
        67 to 1178400,
        68 to 1201800,
        69 to 1225500,
        70 to 1249300,
        71 to 1274100,
        72 to 1299000,
        73 to 1324700,
        74 to 1351000,
        75 to 1377600,
        76 to 1405100,
        77 to 1432400,
        78 to 1461000,
        79 to 1490000,
        80 to 1519400,
    )

    val minStep: Int = annualSalaryByStep.keys.min()
    val maxStep: Int = annualSalaryByStep.keys.max()

    fun annualSalary(step: Int): BigDecimal =
        BigDecimal(requireNotNull(annualSalaryByStep[step]) { "Ukjent lønnstrinn: $step" })

    fun contains(step: Int): Boolean = annualSalaryByStep.containsKey(step)
}
