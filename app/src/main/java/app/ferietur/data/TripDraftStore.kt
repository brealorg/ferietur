package app.ferietur.data

import android.content.Context
import androidx.core.util.AtomicFile
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.DecodedSavedTripDraft
import app.ferietur.domain.SavedTripDraftCodec
import app.ferietur.domain.SavedTripDraftMigrator
import app.ferietur.domain.UnsupportedSavedTripSchemaException
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal enum class TripStorageIssueKind {
    RECOVERED,
    CORRUPT,
    UNSUPPORTED_SCHEMA,
    IO_ERROR,
}

internal data class TripStorageIssue(
    val kind: TripStorageIssueKind,
    val draftId: String?,
    val detail: String,
)

internal data class TripLibrarySnapshot(
    val drafts: List<SavedTripDraft>,
    val issues: List<TripStorageIssue>,
)

internal interface TripDraftDataSource {
    fun loadLibrary(): TripLibrarySnapshot
    fun save(draft: SavedTripDraft)
    fun delete(id: String)
}

private sealed interface DraftReadResult {
    data class Success(val draft: SavedTripDraft, val sourceSchemaVersion: Int) : DraftReadResult
    data class Unsupported(val version: Int, val message: String) : DraftReadResult
    data class Corrupt(val message: String) : DraftReadResult
}

internal class TripDraftStore private constructor(
    private val directory: File,
    private val backupRoot: File,
) : TripDraftDataSource {
    constructor(context: Context) : this(
        directory = File(context.filesDir, "trip-drafts"),
        backupRoot = File(context.filesDir, "trip-draft-backups"),
    )

    internal constructor(
        directory: File,
        backupRoot: File,
        createDirectories: Boolean,
    ) : this(directory, backupRoot) {
        if (createDirectories) {
            directory.mkdirs()
            backupRoot.mkdirs()
        }
    }

    init {
        directory.mkdirs()
        backupRoot.mkdirs()
    }

    override fun loadLibrary(): TripLibrarySnapshot {
        directory.mkdirs()
        backupRoot.mkdirs()
        val drafts = mutableListOf<SavedTripDraft>()
        val issues = mutableListOf<TripStorageIssue>()

        directory
            .listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .sortedBy { it.name }
            .forEach { file ->
                val id = file.nameWithoutExtension
                when (val primary = readAtomic(file)) {
                    is DraftReadResult.Success -> {
                        if (primary.sourceSchemaVersion < SavedTripDraftCodec.SCHEMA_VERSION) {
                            writeAtomically(primary.draft, backupExisting = false)
                            if (
                                SavedTripDraftMigrator.MIGRATION_V6_A39A1_PLAN_REPAIR in
                                primary.draft.migrationHistory
                            ) {
                                issues += TripStorageIssue(
                                    kind = TripStorageIssueKind.RECOVERED,
                                    draftId = id,
                                    detail = "En eldre utviklingsskade i arbeidsplanen ble migrert én gang og lagret i nytt format.",
                                )
                            }
                        }
                        drafts += primary.draft
                    }
                    is DraftReadResult.Unsupported -> {
                        issues += TripStorageIssue(
                            kind = TripStorageIssueKind.UNSUPPORTED_SCHEMA,
                            draftId = id,
                            detail = primary.message,
                        )
                    }
                    is DraftReadResult.Corrupt -> {
                        val recovered = latestValidBackup(id)
                        if (recovered != null) {
                            writeAtomically(recovered, backupExisting = false)
                            drafts += recovered
                            issues += TripStorageIssue(
                                kind = TripStorageIssueKind.RECOVERED,
                                draftId = id,
                                detail = "En korrupt lagret tur ble gjenopprettet fra siste gyldige sikkerhetskopi.",
                            )
                        } else {
                            issues += TripStorageIssue(
                                kind = TripStorageIssueKind.CORRUPT,
                                draftId = id,
                                detail = primary.message,
                            )
                        }
                    }
                }
            }

        return TripLibrarySnapshot(
            drafts = drafts.sortedByDescending { it.updatedAtEpochMillis },
            issues = issues,
        )
    }

    override fun save(draft: SavedTripDraft) {
        directory.mkdirs()
        backupRoot.mkdirs()
        writeAtomically(draft, backupExisting = true)
    }

    override fun delete(id: String) {
        AtomicFile(fileFor(id)).delete()
        backupDirectoryFor(id).deleteRecursively()
    }

    internal fun primaryFileForTest(id: String): File = fileFor(id)
    internal fun backupDirectoryForTest(id: String): File = backupDirectoryFor(id)

    private fun writeAtomically(
        draft: SavedTripDraft,
        backupExisting: Boolean,
    ) {
        val destination = fileFor(draft.id)
        val atomic = AtomicFile(destination)

        if (backupExisting && destination.exists()) {
            val current = readAtomic(destination)
            if (current is DraftReadResult.Success) {
                backupDraft(current.draft)
            }
        }

        var stream: FileOutputStream? = null
        try {
            val output = atomic.startWrite()
            stream = output
            val writer = BufferedWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
            SavedTripDraftCodec.write(draft, writer)
            writer.flush()
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun backupDraft(draft: SavedTripDraft) {
        val backupDirectory = backupDirectoryFor(draft.id).apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        var backup = File(backupDirectory, "$timestamp.properties")
        var suffix = 1
        while (backup.exists()) {
            backup = File(backupDirectory, "$timestamp-$suffix.properties")
            suffix += 1
        }

        backup.outputStream().bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            SavedTripDraftCodec.write(draft, writer)
        }
        pruneBackups(backupDirectory)
    }

    private fun latestValidBackup(id: String): SavedTripDraft? =
        backupDirectoryFor(id)
            .listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .firstNotNullOfOrNull { backup ->
                when (val result = readPlain(backup)) {
                    is DraftReadResult.Success -> result.draft
                    is DraftReadResult.Unsupported,
                    is DraftReadResult.Corrupt -> null
                }
            }

    private fun pruneBackups(backupDirectory: File) {
        backupDirectory
            .listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .drop(MAX_BACKUPS_PER_TRIP)
            .forEach(File::delete)
    }

    private fun readAtomic(file: File): DraftReadResult =
        runCatching {
            AtomicFile(file).openRead()
                .bufferedReader(StandardCharsets.UTF_8)
                .use(SavedTripDraftCodec::readDecoded)
        }.fold(
            onSuccess = { decoded ->
                DraftReadResult.Success(
                    draft = decoded.draft,
                    sourceSchemaVersion = decoded.sourceSchemaVersion,
                )
            },
            onFailure = ::classifyReadFailure,
        )

    private fun readPlain(file: File): DraftReadResult =
        runCatching {
            file.inputStream()
                .bufferedReader(StandardCharsets.UTF_8)
                .use(SavedTripDraftCodec::readDecoded)
        }.fold(
            onSuccess = { decoded ->
                DraftReadResult.Success(
                    draft = decoded.draft,
                    sourceSchemaVersion = decoded.sourceSchemaVersion,
                )
            },
            onFailure = ::classifyReadFailure,
        )

    private fun classifyReadFailure(error: Throwable): DraftReadResult =
        when (error) {
            is UnsupportedSavedTripSchemaException ->
                DraftReadResult.Unsupported(
                    version = error.schemaVersion,
                    message = error.message ?: "Ustøttet lagringsformat.",
                )
            else ->
                DraftReadResult.Corrupt(
                    message = error.message ?: error::class.java.simpleName,
                )
        }

    private fun fileFor(id: String): File = File(directory, "$id.properties")

    private fun backupDirectoryFor(id: String): File = File(backupRoot, id)

    private companion object {
        const val MAX_BACKUPS_PER_TRIP = 12
    }
}
