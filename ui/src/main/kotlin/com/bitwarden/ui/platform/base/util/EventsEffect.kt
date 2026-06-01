package com.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Convenience method for observing event flow from [BaseViewModel].
 *
 * By default, events will only be consumed when the associated screen is resumed, to avoid bugs
 * like duplicate navigation calls. To override this behavior, a given event type can implement
 * [BackgroundEvent] which will allow the event to pass through regardless of the lifecycle state.
 * Additionally, an event can implement [DeferredBackgroundEvent] which will not be filtered out
 * based on the lifecycle but will be processed when the screen resumes.
 */
@Composable
fun <E> EventsEffect(
    viewModel: BaseViewModel<*, E, *>,
    lifecycleOwner: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    handler: (E) -> Unit,
) {
    LaunchedEffect(key1 = Unit) {
        viewModel
            .eventFlow
            .filter { event ->
                event is BackgroundEvent ||
                    lifecycleOwner.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
            .onEach { event ->
                if (event is DeferredBackgroundEvent) {
                    // Defer processing this event until the screen is resumed. This is launched
                    // in its own coroutine scope to allow other events to flow unimpeded.
                    launch {
                        lifecycleOwner
                            .currentStateFlow
                            .first { it.isAtLeast(Lifecycle.State.RESUMED) }
                        handler(event)
                    }
                } else {
                    handler(event)
                }
            }
            .launchIn(this)
    }
}
