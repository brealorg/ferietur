package app.ferietur.domain

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SavedTripDraftSchemaFailureTest {
    @Test
    fun unsupportedSchemaHasTypedFailure() {
        val error = assertThrows(UnsupportedSavedTripSchemaException::class.java) {
            SavedTripDraftCodec.read(
                StringReader(
                    """
                    schemaVersion=999
                    id=future
                    """.trimIndent(),
                ),
            )
        }

        assertEquals(999, error.schemaVersion)
        assertEquals(SavedTripDraftCodec.SCHEMA_VERSION, error.maxSupportedSchemaVersion)
    }
}
