package app.ferietur.ui

import kotlinx.coroutines.CancellationException

/**
 * Coroutine-safe variant of [runCatching].
 *
 * Plain `runCatching` also captures [CancellationException]. Inside a coroutine that turns a normal
 * cancellation (screen left, ViewModel cleared) into an ordinary failure and lets the cancelled
 * coroutine keep running its error handling. This variant rethrows cancellation and only wraps
 * real failures.
 */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
