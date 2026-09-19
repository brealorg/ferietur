package app.ferietur.ui

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CoroutineResultsTest {
    @Test
    fun successAndOrdinaryFailuresAreWrapped() {
        assertEquals(42, runCatchingCancellable { 42 }.getOrNull())

        val failure = runCatchingCancellable<Int> { error("boom") }
        assertTrue(failure.isFailure)
        assertEquals("boom", failure.exceptionOrNull()?.message)
    }

    @Test
    fun cancellationIsRethrownInsteadOfBeingReportedAsFailure() {
        try {
            runCatchingCancellable<Unit> { throw CancellationException("cancelled") }
            fail("CancellationException skulle ha blitt kastet videre")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }
}
