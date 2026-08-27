package app.ferietur.data

import app.ferietur.domain.EmployerKind
import app.ferietur.domain.PayingParty
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRepositoryTest {
    @Test
    fun repositoryRunsStorageOperationsOnInjectedIoDispatcher() {
        val source = RecordingSource()
        val ioThread = AtomicReference<Thread>()
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "ferietur-io-test").also(ioThread::set)
        }
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            val repository = TripRepository(source, dispatcher)
            runBlocking {
                repository.loadLibrary()
                repository.save(draft("one"))
                repository.delete("one")
            }

            val expectedThread = requireNotNull(ioThread.get())
            assertEquals(5, source.threads.size)
            assertTrue(source.threads.all { it === expectedThread })
        } finally {
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    private class RecordingSource : TripDraftDataSource {
        val threads = mutableListOf<Thread>()
        private var drafts = emptyList<SavedTripDraft>()

        override fun loadLibrary(): TripLibrarySnapshot {
            threads += Thread.currentThread()
            return TripLibrarySnapshot(drafts, emptyList())
        }

        override fun save(draft: SavedTripDraft) {
            threads += Thread.currentThread()
            drafts = listOf(draft)
        }

        override fun delete(id: String) {
            threads += Thread.currentThread()
            drafts = drafts.filterNot { it.id == id }
        }
    }

    private fun draft(id: String): SavedTripDraft {
        val start = LocalDate.of(2026, 8, 25)
        return SavedTripDraft(
            id = id,
            updatedAtEpochMillis = 1L,
            screen = "TRIP",
            title = "Test",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = start,
            endDate = start.plusDays(6),
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = emptyMap(),
            outboundArrival = LocalDateTime.of(start, LocalTime.of(11, 0)),
            returnDeparture = LocalDateTime.of(start.plusDays(6), LocalTime.of(16, 0)),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
        )
    }
}
