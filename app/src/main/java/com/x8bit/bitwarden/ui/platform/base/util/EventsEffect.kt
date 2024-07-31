package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Convenience method for observing event flow from [BaseViewModel].
 */
@Composable
fun <E> EventsEffect(
    viewModel: BaseViewModel<*, E, *>,
    lifecycleOwner: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    handler: suspend (E) -> Unit,
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.eventFlow
            .filter {
                it is BackgroundEvent ||
                    lifecycleOwner.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
            .onEach { handler.invoke(it) }
            .launchIn(this)
    }
}
