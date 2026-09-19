package app.ferietur.export

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfExportRetentionTest {
    @Test
    fun staleExportsAreRemovedWhileFreshExportsAndOtherFilesAreKept() {
        val directory = Files.createTempDirectory("ferietur-exports").toFile()
        try {
            val now = 10L * 24L * 60L * 60L * 1000L
            val oneHour = 60L * 60L * 1000L
            val stale = directory.resolve("ferietur-old.pdf").apply {
                writeText("old")
                setLastModified(now - 25L * oneHour)
            }
            val fresh = directory.resolve("ferietur-new.pdf").apply {
                writeText("new")
                setLastModified(now - oneHour)
            }
            val unrelated = directory.resolve("notes.txt").apply {
                writeText("keep")
                setLastModified(now - 100L * oneHour)
            }

            PdfExporter.pruneStaleExports(
                directory = directory,
                nowEpochMillis = now,
                maxAgeMillis = 24L * oneHour,
            )

            assertFalse(stale.exists())
            assertTrue(fresh.exists())
            assertTrue(unrelated.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun missingExportDirectoryIsIgnored() {
        val directory = Files.createTempDirectory("ferietur-exports").toFile()
        directory.deleteRecursively()
        PdfExporter.pruneStaleExports(directory)
    }
}
