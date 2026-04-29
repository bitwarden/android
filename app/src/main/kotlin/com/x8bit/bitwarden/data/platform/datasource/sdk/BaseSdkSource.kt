package com.x8bit.bitwarden.data.platform.datasource.sdk

import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import timber.log.Timber

/**
 * Base class for simplifying sdk interactions.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseSdkSource(
    protected val sdkClientManager: SdkClientManager,
) {
    /**
     * Helper function to retrieve the [Client] associated with the given [userId].
     */
    protected suspend fun getClient(
        userId: String,
    ): Client = sdkClientManager.getOrCreateClient(userId = userId)

    /**
     * Helper function to retrieve a new [Client] and use it in the given [block].
     */
    protected suspend fun <T> useClient(
        userId: String? = null,
        accessToken: String? = null,
        block: suspend Client.() -> T,
    ): T = sdkClientManager.singleUseClient(
        userId = userId,
        accessToken = accessToken,
        block = block,
    )

    /**
     * Invokes the [block] with `this` value as its receiver and returns its result if it was
     * successful and catches any exception that was thrown from the `block` and wrapping it as a
     * failure.
     */
    protected inline fun <T, R> T.runCatchingWithLogs(
        block: T.() -> R,
    ): Result<R> = runCatching(block = block)
        .onFailure { Timber.w(it) }
}
