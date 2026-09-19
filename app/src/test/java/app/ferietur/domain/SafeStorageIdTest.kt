package app.ferietur.domain

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeStorageIdTest {
    @Test
    fun appGeneratedUuidsAndLegacyTestIdsAreAccepted() {
        assertTrue(SafeStorageId.isValid(UUID.randomUUID().toString()))
        assertTrue(SafeStorageId.isValid("legacy-v5"))
        assertTrue(SafeStorageId.isValid("trip_1"))
        assertEquals("abc", SafeStorageId.requireValid("abc"))
    }

    @Test
    fun pathSeparatorsDotsAndOversizedIdsAreRejected() {
        listOf("", ".", "..", "../x", "a/b", "a\\b", "a.b", "a b", "æøå", "x".repeat(65)).forEach { value ->
            assertFalse("Skulle vært avvist: '$value'", SafeStorageId.isValid(value))
            assertTrue(runCatching { SafeStorageId.requireValid(value) }.isFailure)
        }
    }

    @Test
    fun maximumLengthIsInclusive() {
        assertTrue(SafeStorageId.isValid("x".repeat(SafeStorageId.MAX_LENGTH)))
    }
}
