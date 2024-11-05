package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.util.FakeLifecycleOwner
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppStateManagerTest {

    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private val appStateManager = AppStateManagerImpl(
        processLifecycleOwner = fakeLifecycleOwner,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `appForegroundStateFlow should emit whenever the underlying ProcessLifecycleOwner receives start and stop events`() =
        runTest {
            appStateManager.appForegroundStateFlow.test {
                // Initial state is BACKGROUNDED
                assertEquals(
                    AppForegroundState.BACKGROUNDED,
                    awaitItem(),
                )

                fakeLifecycleOwner.lifecycle.dispatchOnStart()

                assertEquals(
                    AppForegroundState.FOREGROUNDED,
                    awaitItem(),
                )

                fakeLifecycleOwner.lifecycle.dispatchOnStop()

                assertEquals(
                    AppForegroundState.BACKGROUNDED,
                    awaitItem(),
                )
            }
        }
}
