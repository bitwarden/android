package com.x8bit.bitwarden.data.platform.util

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Flat maps a successful [Result] with the given [transform] to another [Result], and leaves
 * failures untouched.
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    this.exceptionOrNull()
        ?.asFailure()
        ?: transform(this.getOrThrow())

/**
 * Returns the given receiver of type [T] as a "success" [Result].
 *
 * Note that this will never double wrap the `Result` and we return the original value if [T] is
 * already an instance of `Result`
 */
fun <T> T.asSuccess(): Result<T> = if (this is Result<*>) {
    @Suppress("UNCHECKED_CAST")
    this as Result<T>
} else {
    Result.success(this)
}

/**
 * Returns the given [Throwable] as a "failure" [Result].
 */
fun Throwable.asFailure(): Result<Nothing> =
    Result.failure(this)

/**
 * Retrieves results from [firstResultProvider] and [secondResultProvider] by first running in
 * parallel and then combining successful results using the given [zipper].
 */
suspend fun <T1, T2, R> zip(
    firstResultProvider: suspend () -> Result<T1>,
    secondResultProvider: suspend () -> Result<T2>,
    zipper: suspend (first: T1, second: T2) -> R,
): Result<R> = coroutineScope {
    val firstResultDeferred = async { firstResultProvider() }
    val secondResultDeferred = async { secondResultProvider() }

    val firstResult = firstResultDeferred.await()
    val secondResult = secondResultDeferred.await()

    val errorOrNull = firstResult.exceptionOrNull()
        ?: secondResult.exceptionOrNull()

    errorOrNull
        ?.asFailure()
        ?: zipper(
            firstResult.getOrThrow(),
            secondResult.getOrThrow(),
        )
            .asSuccess()
}

/**
 * Retrieves results from [firstResultProvider], [secondResultProvider], and [thirdResultProvider]
 * by first running in parallel and then combining successful results using the given [zipper].
 */
suspend fun <T1, T2, T3, R> zip(
    firstResultProvider: suspend () -> Result<T1>,
    secondResultProvider: suspend () -> Result<T2>,
    thirdResultProvider: suspend () -> Result<T3>,
    zipper: suspend (first: T1, second: T2, third: T3) -> R,
): Result<R> = coroutineScope {
    val firstResultDeferred = async { firstResultProvider() }
    val secondResultDeferred = async { secondResultProvider() }
    val thirdResultDeferred = async { thirdResultProvider() }

    val firstResult = firstResultDeferred.await()
    val secondResult = secondResultDeferred.await()
    val thirdResult = thirdResultDeferred.await()

    val errorOrNull = firstResult.exceptionOrNull()
        ?: secondResult.exceptionOrNull()
        ?: thirdResult.exceptionOrNull()

    errorOrNull
        ?.asFailure()
        ?: zipper(
            firstResult.getOrThrow(),
            secondResult.getOrThrow(),
            thirdResult.getOrThrow(),
        )
            .asSuccess()
}
