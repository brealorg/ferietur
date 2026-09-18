package app.ferietur.data

import app.ferietur.domain.FERIETUR_RULESET_VERSION
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.SavedTripDraftCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class TripBackupMetadata(
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val appVersionName: String,
    val appVersionCode: Int,
    val rulesetVersion: String,
    val draftSchemaVersion: Int,
)

internal data class TripLibraryBackup(
    val metadata: TripBackupMetadata,
    val drafts: List<SavedTripDraft>,
)

internal data class TripBackupImportResult(
    val importedCount: Int,
    val library: TripLibrarySnapshot,
    val metadata: TripBackupMetadata,
)

internal object TripLibraryBackupCodec {
    const val FORMAT_VERSION = 1
    private const val FORMAT_ID = "FERIETUR_LIBRARY_BACKUP"
    private const val MANIFEST_ENTRY = "manifest.properties"
    private const val MAX_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_DRAFTS = 1000
    private const val MAX_ARCHIVE_ENTRIES = MAX_DRAFTS + 16

    fun write(
        drafts: List<SavedTripDraft>,
        output: OutputStream,
        appVersionName: String,
        appVersionCode: Int,
        createdAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        require(drafts.map(SavedTripDraft::id).distinct().size == drafts.size) {
            "Kan ikke eksportere backup med dupliserte tur-ID-er."
        }
        require(drafts.size <= MAX_DRAFTS) {
            "For mange turer i sikkerhetskopien."
        }

        val manifest = Properties().apply {
            setProperty("format", FORMAT_ID)
            setProperty("formatVersion", FORMAT_VERSION.toString())
            setProperty("createdAtEpochMillis", createdAtEpochMillis.toString())
            setProperty("appVersionName", appVersionName)
            setProperty("appVersionCode", appVersionCode.toString())
            setProperty("rulesetVersion", FERIETUR_RULESET_VERSION)
            setProperty("draftSchemaVersion", SavedTripDraftCodec.SCHEMA_VERSION.toString())
            setProperty("draftCount", drafts.size.toString())
        }

        ZipOutputStream(output.buffered()).use { zip ->
            val manifestBytes = ByteArrayOutputStream().also { bytes ->
                OutputStreamWriter(bytes, StandardCharsets.UTF_8).use { writer ->
                    manifest.store(writer, "Ferietur local backup")
                }
            }.toByteArray()
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestBytes)
            zip.closeEntry()

            drafts.sortedBy(SavedTripDraft::id).forEachIndexed { index, draft ->
                val encoded = StringWriter().also { writer ->
                    SavedTripDraftCodec.write(draft, writer)
                }.toString().toByteArray(StandardCharsets.UTF_8)
                zip.putNextEntry(ZipEntry("drafts/${index.toString().padStart(4, '0')}.properties"))
                zip.write(encoded)
                zip.closeEntry()
            }
            zip.finish()
        }
    }

    fun read(input: InputStream): TripLibraryBackup {
        var manifestBytes: ByteArray? = null
        val draftEntries = mutableListOf<ByteArray>()
        var archiveEntryCount = 0

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                archiveEntryCount += 1
                check(archiveEntryCount <= MAX_ARCHIVE_ENTRIES) {
                    "Sikkerhetskopien inneholder for mange filer."
                }
                if (!entry.isDirectory) {
                    val bytes = zip.readCurrentEntryBounded()
                    when {
                        entry.name == MANIFEST_ENTRY -> {
                            check(manifestBytes == null) {
                                "Sikkerhetskopien inneholder flere manifestfiler."
                            }
                            manifestBytes = bytes
                        }
                        entry.name.startsWith("drafts/") &&
                            entry.name.endsWith(".properties") -> {
                            draftEntries += bytes
                        }
                    }
                }
                zip.closeEntry()
            }
        }

        val manifest = Properties().apply {
            val bytes = manifestBytes ?: error("Filen er ikke en gyldig Ferietur-sikkerhetskopi.")
            InputStreamReader(
                ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8,
            ).use { reader -> load(reader) }
        }

        check(manifest.getProperty("format") == FORMAT_ID) {
            "Filen er ikke en gyldig Ferietur-sikkerhetskopi."
        }
        val formatVersion = manifest.requireInt("formatVersion")
        check(formatVersion == FORMAT_VERSION) {
            "Sikkerhetskopien bruker format $formatVersion, men denne appen støtter format $FORMAT_VERSION."
        }
        val draftSchemaVersion = manifest.requireInt("draftSchemaVersion")
        check(draftSchemaVersion <= SavedTripDraftCodec.SCHEMA_VERSION) {
            "Sikkerhetskopien bruker draft-format $draftSchemaVersion, men denne appen støtter til og med ${SavedTripDraftCodec.SCHEMA_VERSION}."
        }
        val draftCount = manifest.requireInt("draftCount")
        check(draftCount in 0..MAX_DRAFTS) {
            "Sikkerhetskopien oppgir et ugyldig antall turer."
        }
        check(draftEntries.size == draftCount) {
            "Sikkerhetskopien er ufullstendig: forventet $draftCount turer, fant ${draftEntries.size}."
        }

        // Every embedded draft is decoded/migrated before the repository starts writes.
        val drafts = draftEntries.map { bytes ->
            InputStreamReader(
                ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8,
            ).use(SavedTripDraftCodec::read)
        }
        check(drafts.map(SavedTripDraft::id).distinct().size == drafts.size) {
            "Sikkerhetskopien inneholder dupliserte tur-ID-er."
        }

        return TripLibraryBackup(
            metadata = TripBackupMetadata(
                formatVersion = formatVersion,
                createdAtEpochMillis = manifest.requireLong("createdAtEpochMillis"),
                appVersionName = manifest.requireProperty("appVersionName"),
                appVersionCode = manifest.requireInt("appVersionCode"),
                rulesetVersion = manifest.requireProperty("rulesetVersion"),
                draftSchemaVersion = draftSchemaVersion,
            ),
            drafts = drafts,
        )
    }

    private fun ZipInputStream.readCurrentEntryBounded(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            check(total <= MAX_ENTRY_BYTES) {
                "Sikkerhetskopien inneholder en for stor fil."
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun Properties.requireProperty(key: String): String =
        getProperty(key) ?: error("Sikkerhetskopien mangler feltet $key.")

    private fun Properties.requireInt(key: String): Int =
        requireProperty(key).toIntOrNull()
            ?: error("Sikkerhetskopien har ugyldig verdi for $key.")

    private fun Properties.requireLong(key: String): Long =
        requireProperty(key).toLongOrNull()
            ?: error("Sikkerhetskopien har ugyldig verdi for $key.")
}
