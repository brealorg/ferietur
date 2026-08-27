package app.ferietur.data

import android.content.Context
import app.ferietur.domain.SavedTripDraft
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
}
