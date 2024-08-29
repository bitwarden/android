package com.bitwarden.authenticator.data.platform.base

import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * A faked implementation of [DispatcherManager] that uses [UnconfinedTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeDispatcherManager(
    override val default: CoroutineDispatcher = UnconfinedTestDispatcher(),
    override val io: CoroutineDispatcher = UnconfinedTestDispatcher(),
    override val unconfined: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : DispatcherManager {
    override val main: MainCoroutineDispatcher = Dispatchers.Main

    /**
     * Updates the main dispatcher to use the provided [dispatcher]. Used in conjunction with
     * [resetMain].
     */
    fun setMain(dispatcher: CoroutineDispatcher) {
        Dispatchers.setMain(dispatcher)
    }

    /**
     * Restores the main dispatcher to it's default state. Used in conjunction with [setMain].
     */
    fun resetMain() {
        Dispatchers.resetMain()
    }
}
