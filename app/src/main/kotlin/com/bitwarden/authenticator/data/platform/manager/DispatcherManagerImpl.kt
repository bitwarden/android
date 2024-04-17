package com.bitwarden.authenticator.data.platform.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * Primary implementation of [DispatcherManager].
 */
class DispatcherManagerImpl : DispatcherManager {
    override val default: CoroutineDispatcher = Dispatchers.Default

    override val main: MainCoroutineDispatcher = Dispatchers.Main

    override val io: CoroutineDispatcher = Dispatchers.IO

    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
