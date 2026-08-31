package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffResolverDelegationTest {

    private val directAdapter =
        TariffCatalogResolverAdapter(
            FerieturGlobalRuntimeCatalogView,
        )

    @Test
    fun representativeSingleContextResolutionMatchesGlobalAdapter() {
        val ranges = listOf(
            LocalDate.of(2026, 5, 1) to
                LocalDate.of(2026, 5, 1),

            LocalDate.of(2026, 8, 10) to
                LocalDate.of(2026, 8, 16),

            LocalDate.of(2027, 4, 30) to
                LocalDate.of(2027, 4, 30),

            LocalDate.of(2027, 5, 1) to
                LocalDate.of(2027, 5, 1),
        )

        ranges.forEach { (start, end) ->
            assertEquals(
                directAdapter.resolveRange(
                    start,
                    end,
                ),
                FerieturTariffResolver.resolveRange(
                    start,
                    end,
                ),
            )

            assertEquals(
                directAdapter.supportsRange(
                    start,
                    end,
                ),
                FerieturTariffResolver.supportsRange(
                    start,
                    end,
                ),
            )
        }
    }

    @Test
    fun segmentedPlanningMatchesGlobalAdapter() {
        val ranges = listOf(
            LocalDate.of(2026, 5, 1) to
                LocalDate.of(2026, 5, 2),

            LocalDate.of(2027, 4, 29) to
                LocalDate.of(2027, 4, 30),

            LocalDate.of(2027, 4, 30) to
                LocalDate.of(2027, 5, 1),
        )

        ranges.forEach { (start, end) ->
            assertEquals(
                directAdapter.planSegments(
                    start,
                    end,
                ),
                FerieturTariffResolver.planSegments(
                    start,
                    end,
                ),
            )

            assertEquals(
                directAdapter.supportsSegmentedRange(
                    start,
                    end,
                ),
                FerieturTariffResolver.supportsSegmentedRange(
                    start,
                    end,
                ),
            )
        }
    }

    @Test
    fun halfOpenMidnightBoundaryBehaviorIsPreserved() {
        val start =
            LocalDateTime.of(
                2027,
                4,
                30,
                23,
                0,
            )

        val exactMidnight =
            LocalDateTime.of(
                2027,
                5,
                1,
                0,
                0,
            )

        val oneMinuteAfter =
            LocalDateTime.of(
                2027,
                5,
                1,
                0,
                1,
            )

        val expectedExact =
            directAdapter.planSegments(
                start,
                exactMidnight,
            )

        val productionExact =
            FerieturTariffResolver.planSegments(
                start,
                exactMidnight,
            )

        assertEquals(
            expectedExact,
            productionExact,
        )

        assertTrue(
            productionExact is
                TariffSegmentationResult.Success,
        )

        val expectedAfter =
            directAdapter.planSegments(
                start,
                oneMinuteAfter,
            )

        val productionAfter =
            FerieturTariffResolver.planSegments(
                start,
                oneMinuteAfter,
            )

        assertEquals(
            expectedAfter,
            productionAfter,
        )

        assertTrue(
            productionAfter is
                TariffSegmentationResult.Failure,
        )
    }

    @Test
    fun futureSalaryBoundaryRemainsUnsupportedInProduction() {
        val boundary =
            LocalDate.of(
                2027,
                5,
                1,
            )

        assertNull(
            FerieturTariffResolver.resolveRange(
                boundary,
                boundary,
            ),
        )

        assertFalse(
            FerieturTariffResolver.supportsRange(
                boundary,
                boundary,
            ),
        )

        assertNull(
            OsloSalaryTables.descriptorForDate(
                boundary,
            ),
        )
    }

    @Test
    fun requireSupportedRangeRemainsFailClosed() {
        val supported =
            FerieturTariffResolver.requireSupportedRange(
                LocalDate.of(2027, 4, 30),
                LocalDate.of(2027, 4, 30),
            )

        assertEquals(
            OsloSalaryTable2026.tableId,
            supported.salaryTable.id,
        )

        val failure =
            assertThrows(
                IllegalArgumentException::class.java,
            ) {
                FerieturTariffResolver
                    .requireSupportedRange(
                        LocalDate.of(2027, 5, 1),
                        LocalDate.of(2027, 5, 1),
                    )
            }

        assertTrue(
            failure.message
                ?.contains(
                    "Ingen komplett verifisert kombinasjon",
                ) == true,
        )
    }
}
