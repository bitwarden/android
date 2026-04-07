package com.bitwarden.data.manager.appstate

import android.app.Activity
import android.app.Application
import app.cash.turbine.test
import com.bitwarden.core.data.util.FakeLifecycleOwner
import com.bitwarden.data.autofill.util.createdForAutofill
import com.bitwarden.data.manager.appstate.model.AppCreationState
import com.bitwarden.data.manager.appstate.model.AppForegroundState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppStateManagerTest {

    private val activityLifecycleCallbacks = slot<Application.ActivityLifecycleCallbacks>()
    private val application = mockk<Application> {
        every { registerActivityLifecycleCallbacks(capture(activityLifecycleCallbacks)) } just runs
    }
    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private val appStateManager = AppStateManagerImpl(
        application = application,
        processLifecycleOwner = fakeLifecycleOwner,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Activity::createdForAutofill)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Activity::createdForAutofill)
    }

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

    @Suppress("MaxLineLength")
    @Test
    fun `appCreatedStateFlow should emit whenever the underlying activities are all destroyed or a creation event occurs`() =
        runTest {
            val activity = mockk<Activity> {
                every { isChangingConfigurations } returns false
                every { createdForAutofill } returns false
            }
            appStateManager.appCreatedStateFlow.test {
                // Initial state is DESTROYED
                assertEquals(AppCreationState.Destroyed, awaitItem())

                activityLifecycleCallbacks.captured.onActivityCreated(activity, null)
                assertEquals(AppCreationState.Created(isAutoFill = false), awaitItem())

                activityLifecycleCallbacks.captured.onActivityCreated(activity, null)
                expectNoEvents()

                activityLifecycleCallbacks.captured.onActivityDestroyed(activity)
                expectNoEvents()

                activityLifecycleCallbacks.captured.onActivityDestroyed(activity)
                assertEquals(AppCreationState.Destroyed, awaitItem())
            }
        }
}
