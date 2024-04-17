package com.bitwarden.authenticator.data.platform.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * An interface for accessing the [CoroutineDispatcher]s.
 */
interface DispatcherManager {
    /**
     * The default [CoroutineDispatcher] for the app.
     */
    val default: CoroutineDispatcher

    /**
     * The [MainCoroutineDispatcher] for the app.
     */
    val main: MainCoroutineDispatcher

    /**
     * The IO [CoroutineDispatcher] for the app.
     */
    val io: CoroutineDispatcher

    /**
     * The unconfined [CoroutineDispatcher] for the app.
     */
    val unconfined: CoroutineDispatcher
}
