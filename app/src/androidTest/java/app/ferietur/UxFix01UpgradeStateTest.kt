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

/** Exact durable-state proof for an in-place same-certificate UXFIX01 install. */
class UxFix01UpgradeStateTest {
    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val sentinel: File
        get() = File(targetContext.filesDir, SENTINEL_NAME)

    @Test
    fun capturePreInstallState() {
        val versionCode = installedVersionCode()
        val versionName = installedVersionName()
        assertTrue("Unexpected pre-UXFIX versionCode $versionCode", versionCode == 51L || versionCode == 52L)
        assertTrue("Unexpected pre-UXFIX versionName $versionName", versionName == "0.5.4" || versionName == "0.5.5")

        val library = TripDraftStore(targetContext).loadLibrary()
        assertHealthyLibrary(library.issues.map { it.kind })

        val measured = measureDurableState()
        val props = Properties().apply {
            setProperty("format", "1")
            setProperty("nonce", UUID.randomUUID().toString())
            setProperty("preVersionCode", versionCode.toString())
            setProperty("preVersionName", versionName)
            setProperty("digest", measured.digest)
            setProperty("fileCount", measured.fileCount.toString())
            setProperty("totalBytes", measured.totalBytes.toString())
            setProperty("draftCount", library.drafts.size.toString())
        }

        FileOutputStream(sentinel).use { output ->
            props.store(output, "FERIETUR_UXFIX01_UPGRADE_STATE")
            output.fd.sync()
        }
        assertTrue(sentinel.isFile)
    }

    @Test
    fun verifyPostInstallStateAndCleanup() {
        assertEquals(52L, installedVersionCode())
        assertEquals("0.5.5", installedVersionName())
        assertTrue("UXFIX01 pre-install sentinel missing", sentinel.isFile)

        val props = Properties().apply {
            sentinel.inputStream().use { input -> load(input) }
        }
        assertEquals("1", props.getProperty("format"))
        requireNotNull(props.getProperty("nonce"))

        val measured = measureDurableState()
        assertEquals(props.getProperty("digest"), measured.digest)
        assertEquals(props.getProperty("fileCount").toInt(), measured.fileCount)
        assertEquals(props.getProperty("totalBytes").toLong(), measured.totalBytes)

        val library = TripDraftStore(targetContext).loadLibrary()
        assertEquals(props.getProperty("draftCount").toInt(), library.drafts.size)
        assertHealthyLibrary(library.issues.map { it.kind })

        assertTrue("UXFIX01 sentinel cleanup failed", sentinel.delete())
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

        return StateMeasurement(aggregate.digest().toHex(), files.size, totalBytes)
    }

    private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private data class StateMeasurement(val digest: String, val fileCount: Int, val totalBytes: Long)

    private companion object {
        const val SENTINEL_NAME = ".ferietur-uxfix01-upgrade-state"
        val DURABLE_ROOTS = listOf("files", "shared_prefs", "databases", "no_backup")
    }
}
