package app.ferietur

import androidx.core.content.pm.PackageInfoCompat
import androidx.test.platform.app.InstrumentationRegistry
import app.ferietur.data.TripDraftStore
import app.ferietur.data.TripStorageIssueKind
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FINAL01 same-certificate RC1 -> 0.5.4 durable-state continuity proof.
 *
 * The pre-upgrade test records a digest sentinel inside app-private storage.
 * The post-upgrade test executes before normal Activity launch, requires exact
 * durable-state byte identity excluding the sentinel itself, verifies the real
 * draft library, then removes the sentinel.
 */
class FinalPromotionStateTest {
    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val sentinel: File
        get() = File(targetContext.filesDir, SENTINEL_NAME)

    @Test
    fun capturePreUpgradeState() {
        assertEquals(50L, installedVersionCode())
        assertEquals("0.5.4-rc1", installedVersionName())

        val library = TripDraftStore(targetContext).loadLibrary()
        assertHealthyLibrary(library.issues.map { it.kind })

        val measured = measureDurableState()
        val props = Properties().apply {
            setProperty("format", "1")
            setProperty("nonce", UUID.randomUUID().toString())
            setProperty("preVersionCode", "50")
            setProperty("preVersionName", "0.5.4-rc1")
            setProperty("digest", measured.digest)
            setProperty("fileCount", measured.fileCount.toString())
            setProperty("totalBytes", measured.totalBytes.toString())
            setProperty("draftCount", library.drafts.size.toString())
        }

        FileOutputStream(sentinel).use { output ->
            props.store(output, "FERIETUR_FINAL01_PROMOTION_STATE")
            output.fd.sync()
        }
        assertTrue(sentinel.isFile)
    }

    @Test
    fun verifyPostUpgradeStateAndCleanup() {
        assertEquals(51L, installedVersionCode())
        assertEquals("0.5.4", installedVersionName())
        assertTrue("FINAL01 pre-upgrade sentinel missing", sentinel.isFile)

        val props = Properties().apply {
            sentinel.inputStream().use { input -> load(input) }
        }
        assertEquals("1", props.getProperty("format"))
        assertEquals("50", props.getProperty("preVersionCode"))
        assertEquals("0.5.4-rc1", props.getProperty("preVersionName"))
        requireNotNull(props.getProperty("nonce"))

        val measured = measureDurableState()
        assertEquals(props.getProperty("digest"), measured.digest)
        assertEquals(props.getProperty("fileCount").toInt(), measured.fileCount)
        assertEquals(props.getProperty("totalBytes").toLong(), measured.totalBytes)

        val library = TripDraftStore(targetContext).loadLibrary()
        assertEquals(props.getProperty("draftCount").toInt(), library.drafts.size)
        assertHealthyLibrary(library.issues.map { it.kind })

        assertTrue("FINAL01 sentinel cleanup failed", sentinel.delete())
        assertFalse(sentinel.exists())
    }

    private fun installedVersionCode(): Long {
        val info = targetContext.packageManager.getPackageInfo(targetContext.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info)
    }

    private fun installedVersionName(): String {
        val info = targetContext.packageManager.getPackageInfo(targetContext.packageName, 0)
        return requireNotNull(info.versionName)
    }

    private fun assertHealthyLibrary(kinds: List<TripStorageIssueKind>) {
        assertFalse(
            kinds.any { kind ->
                kind == TripStorageIssueKind.CORRUPT ||
                    kind == TripStorageIssueKind.UNSUPPORTED_SCHEMA ||
                    kind == TripStorageIssueKind.IO_ERROR
            },
        )
    }

    private fun measureDurableState(): StateMeasurement {
        val dataRoot = requireNotNull(targetContext.filesDir.parentFile)
        val files = DURABLE_ROOTS
            .map { File(dataRoot, it) }
            .filter(File::exists)
            .flatMap { root -> root.walkTopDown().filter(File::isFile).toList() }
            .filterNot { file -> file.absoluteFile == sentinel.absoluteFile }
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

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class StateMeasurement(
        val digest: String,
        val fileCount: Int,
        val totalBytes: Long,
    )

    private companion object {
        const val SENTINEL_NAME = ".ferietur-final01-promotion-state"
        val DURABLE_ROOTS = listOf("files", "shared_prefs", "databases", "no_backup")
    }
}
