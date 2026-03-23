package com.bitwarden.ui.platform.base.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class EventsEffectTest : BaseComposeTest() {

    private val mutableEventFlow: MutableSharedFlow<TestEvent> = bufferedMutableSharedFlow()
    private val viewModel: BaseViewModel<Unit, TestEvent, Unit> = mockk {
        every { eventFlow } returns mutableEventFlow
    }

    @Test
    fun `events are dispatched to handler when lifecycle is RESUMED`() {
        val handledEvents = mutableListOf<TestEvent>()
        val lifecycle = createLifecycle(Lifecycle.State.RESUMED)

        setTestContent {
            EventsEffect(
                viewModel = viewModel,
                lifecycleOwner = lifecycle,
                handler = { handledEvents.add(it) },
            )
        }

        composeTestRule.runOnIdle {
            mutableEventFlow.tryEmit(TestEvent.Regular)
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(TestEvent.Regular), handledEvents)
    }

    @Test
    fun `events are not dispatched to handler when lifecycle is not RESUMED`() {
        val handledEvents = mutableListOf<TestEvent>()
        val lifecycle = createLifecycle(Lifecycle.State.STARTED)

        setTestContent {
            EventsEffect(
                viewModel = viewModel,
                lifecycleOwner = lifecycle,
                handler = { handledEvents.add(it) },
            )
        }

        composeTestRule.runOnIdle {
            mutableEventFlow.tryEmit(TestEvent.Regular)
        }
        composeTestRule.waitForIdle()

        assertEquals(emptyList<TestEvent>(), handledEvents)
    }

    @Test
    fun `BackgroundEvent is dispatched to handler even when lifecycle is not RESUMED`() {
        val handledEvents = mutableListOf<TestEvent>()
        val lifecycle = createLifecycle(Lifecycle.State.STARTED)

        setTestContent {
            EventsEffect(
                viewModel = viewModel,
                lifecycleOwner = lifecycle,
                handler = { handledEvents.add(it) },
            )
        }

        composeTestRule.runOnIdle {
            mutableEventFlow.tryEmit(TestEvent.Background)
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(TestEvent.Background), handledEvents)
    }

    @Test
    fun `DeferredBackgroundEvent is not dispatched until lifecycle transitions to RESUMED`() {
        val handledEvents = mutableListOf<TestEvent>()
        val lifecycle = createLifecycle(Lifecycle.State.STARTED)

        setTestContent {
            EventsEffect(
                viewModel = viewModel,
                lifecycleOwner = lifecycle,
                handler = { handledEvents.add(it) },
            )
        }

        composeTestRule.runOnIdle {
            mutableEventFlow.tryEmit(TestEvent.DeferredBackground)
        }
        composeTestRule.waitForIdle()

        assertEquals(emptyList<TestEvent>(), handledEvents)

        composeTestRule.runOnIdle {
            lifecycle.currentState = Lifecycle.State.RESUMED
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(TestEvent.DeferredBackground), handledEvents)
    }

    @Test
    fun `DeferredBackgroundEvent is dispatched immediately when lifecycle is already RESUMED`() {
        val handledEvents = mutableListOf<TestEvent>()
        val lifecycle = createLifecycle(Lifecycle.State.RESUMED)

        setTestContent {
            EventsEffect(
                viewModel = viewModel,
                lifecycleOwner = lifecycle,
                handler = { handledEvents.add(it) },
            )
        }

        composeTestRule.runOnIdle {
            mutableEventFlow.tryEmit(TestEvent.DeferredBackground)
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(TestEvent.DeferredBackground), handledEvents)
    }

    @Test
    fun `multiple events are dispatched in order when lifecycle is RESUMED`() {
        val handledEvents = mutableListOf<TestEvent>()
        val lifecycle = createLifecycle(Lifecycle.State.RESUMED)

        setTestContent {
            EventsEffect(
                viewModel = viewModel,
                lifecycleOwner = lifecycle,
                handler = { handledEvents.add(it) },
            )
        }

        composeTestRule.runOnIdle {
            mutableEventFlow.tryEmit(TestEvent.Regular)
            mutableEventFlow.tryEmit(TestEvent.Background)
            mutableEventFlow.tryEmit(TestEvent.DeferredBackground)
        }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                TestEvent.Regular,
                TestEvent.Background,
                TestEvent.DeferredBackground,
            ),
            handledEvents,
        )
    }

    private fun createLifecycle(
        state: Lifecycle.State,
    ): LifecycleRegistry = LifecycleRegistry(mockk()).apply { currentState = state }
}

private sealed class TestEvent {
    data object Regular : TestEvent()
    data object Background : TestEvent(), BackgroundEvent
    data object DeferredBackground : TestEvent(), DeferredBackgroundEvent
}
