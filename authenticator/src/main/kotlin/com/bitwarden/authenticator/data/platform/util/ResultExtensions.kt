package com.bitwarden.authenticator.data.platform.util

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
 */
fun <T> T.asSuccess(): Result<T> =
    Result.success(this)

/**
 * Returns the given [Throwable] as a "failure" [Result].
 */
fun Throwable.asFailure(): Result<Nothing> =
    Result.failure(this)
