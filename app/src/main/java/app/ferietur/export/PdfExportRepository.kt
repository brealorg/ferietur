package app.ferietur.export

import android.content.Context
import app.ferietur.domain.FinalizedTripSnapshot
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PdfExportRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext

    suspend fun create(
        snapshot: FinalizedTripSnapshot,
        variant: PdfExporter.Variant,
    ): File = withContext(ioDispatcher) {
        PdfExporter.createBlocking(
            context = appContext,
            snapshot = snapshot,
            variant = variant,
        )
    }
}
