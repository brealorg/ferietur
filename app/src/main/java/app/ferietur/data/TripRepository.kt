package app.ferietur.data

import android.content.Context
import app.ferietur.domain.SavedTripDraft
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class TripRepository(
    private val store: TripDraftDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    constructor(context: Context) : this(
        store = TripDraftStore(context.applicationContext),
        ioDispatcher = Dispatchers.IO,
    )

    private val mutex = Mutex()

    suspend fun loadLibrary(): TripLibrarySnapshot =
        withContext(ioDispatcher) {
            mutex.withLock {
                store.loadLibrary()
            }
        }

    suspend fun save(draft: SavedTripDraft): TripLibrarySnapshot =
        withContext(ioDispatcher) {
            mutex.withLock {
                store.save(draft)
                store.loadLibrary()
            }
        }

    suspend fun delete(id: String): TripLibrarySnapshot =
        withContext(ioDispatcher) {
            mutex.withLock {
                store.delete(id)
                store.loadLibrary()
            }
        }

    suspend fun exportBackup(
        output: OutputStream,
        appVersionName: String,
        appVersionCode: Int,
    ): Int =
        withContext(ioDispatcher) {
            mutex.withLock {
                val library = store.loadLibrary()
                val blockingIssues = library.issues.filter {
                    it.kind == TripStorageIssueKind.CORRUPT ||
                        it.kind == TripStorageIssueKind.UNSUPPORTED_SCHEMA ||
                        it.kind == TripStorageIssueKind.IO_ERROR
                }
                check(blockingIssues.isEmpty()) {
                    "Kan ikke eksportere før lokale lagringsfeil er rettet."
                }
                TripLibraryBackupCodec.write(
                    drafts = library.drafts,
                    output = output,
                    appVersionName = appVersionName,
                    appVersionCode = appVersionCode,
                )
                library.drafts.size
            }
        }

    suspend fun importBackup(input: InputStream): TripBackupImportResult =
        withContext(ioDispatcher) {
            mutex.withLock {
                // Decode and validate the complete external file before mutating local storage.
                val backup = TripLibraryBackupCodec.read(input)
                backup.drafts.forEach(store::save)
                TripBackupImportResult(
                    importedCount = backup.drafts.size,
                    library = store.loadLibrary(),
                    metadata = backup.metadata,
                )
            }
        }
}
