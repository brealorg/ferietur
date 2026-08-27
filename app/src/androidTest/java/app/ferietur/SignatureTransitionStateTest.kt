package app.ferietur

import androidx.test.platform.app.InstrumentationRegistry
import app.ferietur.data.TripDraftStore
import app.ferietur.data.TripStorageIssueKind
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * SIGN01A2 signed-device state continuity proof.
 *
 * This test must run before any Activity launch after the final release APK is
 * installed. It compares the durable app-private file bytes to the host-side
 * digest captured before the debug-certificate uninstall, then asks the real
 * TripDraftStore to load the restored library.
 */
class SignatureTransitionStateTest {
    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun restoredDurableStateMatchesPreTransitionDigest() {
        val args = InstrumentationRegistry.getArguments()
        val expectedDigest = requireNotNull(args.getString("expectedStateDigest"))
        val expectedCount = requireNotNull(args.getString("expectedStateCount")).toInt()
        val expectedBytes = requireNotNull(args.getString("expectedStateBytes")).toLong()
        val expectedDraftCount = requireNotNull(args.getString("expectedDraftCount")).toInt()

        val measured = measureDurableState()
        assertEquals(expectedDigest.lowercase(), measured.digest)
        assertEquals(expectedCount, measured.fileCount)
        assertEquals(expectedBytes, measured.totalBytes)

        val library = TripDraftStore(targetContext).loadLibrary()
        assertEquals(expectedDraftCount, library.drafts.size)
        assertFalse(
            library.issues.any { issue ->
                issue.kind == TripStorageIssueKind.CORRUPT ||
                    issue.kind == TripStorageIssueKind.UNSUPPORTED_SCHEMA ||
                    issue.kind == TripStorageIssueKind.IO_ERROR
            },
        )
    }

    private fun measureDurableState(): StateMeasurement {
        val dataRoot = requireNotNull(targetContext.filesDir.parentFile)
        val roots = DURABLE_ROOTS.map { File(dataRoot, it) }.filter(File::exists)
        val files = roots
            .flatMap { root -> root.walkTopDown().filter(File::isFile).toList() }
            .sortedBy { file -> file.relativeTo(dataRoot).invariantSeparatorsPath }

        val aggregate = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        files.forEach { file ->
            val relative = file.relativeTo(dataRoot).invariantSeparatorsPath
            val bytes = file.readBytes()
            val contentDigest = bytes.sha256Hex()
            totalBytes += bytes.size
            aggregate.update("$relative\u0000${bytes.size}\u0000$contentDigest\n".toByteArray(Charsets.UTF_8))
        }

        return StateMeasurement(
            digest = aggregate.digest().toHex(),
            fileCount = files.size,
            totalBytes = totalBytes,
        )
    }

    private fun ByteArray.sha256Hex(): String =
        MessageDigest.getInstance("SHA-256").digest(this).toHex()

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class StateMeasurement(
        val digest: String,
        val fileCount: Int,
        val totalBytes: Long,
    )

    private companion object {
        val DURABLE_ROOTS = listOf("files", "shared_prefs", "databases", "no_backup")
    }
}
