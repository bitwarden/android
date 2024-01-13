package com.x8bit.bitwarden.data.platform.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppForegroundManagerTest {

    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private val appForegroundManager = AppForegroundManagerImpl(
        processLifecycleOwner = fakeLifecycleOwner,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `appForegroundStateFlow should emit whenever the underlying ProcessLifecycleOwner receives start and stop events`() =
        runTest {
            appForegroundManager.appForegroundStateFlow.test {
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

private class FakeLifecycle(
    private val lifecycleOwner: LifecycleOwner,
) : Lifecycle() {
    private val observers = mutableSetOf<DefaultLifecycleObserver>()

    override var currentState: State = State.INITIALIZED

    override fun addObserver(observer: LifecycleObserver) {
        observers += (observer as DefaultLifecycleObserver)
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers -= (observer as DefaultLifecycleObserver)
    }

    /**
     * Triggers [DefaultLifecycleObserver.onStart] calls for each registered observer.
     */
    fun dispatchOnStart() {
        currentState = State.STARTED
        observers.forEach { observer ->
            observer.onStart(lifecycleOwner)
        }
    }

    /**
     * Triggers [DefaultLifecycleObserver.onStop] calls for each registered observer.
     */
    fun dispatchOnStop() {
        currentState = State.CREATED
        observers.forEach { observer ->
            observer.onStop(lifecycleOwner)
        }
    }
}

private class FakeLifecycleOwner : LifecycleOwner {
    override val lifecycle: FakeLifecycle = FakeLifecycle(this)
}
