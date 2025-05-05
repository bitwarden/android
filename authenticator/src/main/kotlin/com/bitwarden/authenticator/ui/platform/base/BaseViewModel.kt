package com.bitwarden.authenticator.ui.platform.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * A base [ViewModel] that helps enforce the unidirectional data flow pattern and associated
 * responsibilities of a typical ViewModel:
 *
 * - Maintaining and emitting a current state (of type [S]) with the given `initialState`.
 * - Emitting one-shot events as needed (of type [E]). These should be rare and are typically
 *   reserved for things such as non-state based navigation.
 * - Receiving actions (of type [A]) that may induce changes in the current state, trigger an
 *   event emission, or both.
 */
abstract class BaseViewModel<S, E, A>(
    initialState: S,
) : ViewModel() {
    protected val mutableStateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
    private val eventChannel: Channel<E> = Channel(capacity = Channel.UNLIMITED)
    private val internalActionChannel: Channel<A> = Channel(capacity = Channel.UNLIMITED)

    /**
     * A helper that returns the current state of the view model.
     */
    protected val state: S get() = mutableStateFlow.value

    /**
     * A [StateFlow] representing state updates.
     */
    val stateFlow: StateFlow<S> = mutableStateFlow.asStateFlow()

    /**
     * A [Flow] of one-shot events. These may be received and consumed by only a single consumer.
     * Any additional consumers will receive no events.
     */
    val eventFlow: Flow<E> = eventChannel.receiveAsFlow()

    /**
     * A [SendChannel] for sending actions to the ViewModel for processing.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val actionChannel: SendChannel<A> = internalActionChannel

    init {
        viewModelScope.launch {
            internalActionChannel
                .consumeAsFlow()
                .collect { action ->
                    handleAction(action)
                }
        }
    }

    /**
     * Handles the given [action] in a synchronous manner.
     *
     * Any changes to internal state that first require asynchronous work should post a follow-up
     * action that may be used to then update the state synchronously.
     */
    protected abstract fun handleAction(action: A): Unit

    /**
     * Convenience method for sending an action to the [actionChannel].
     */
    fun trySendAction(action: A) {
        actionChannel.trySend(action)
    }

    /**
     * Helper method for sending an internal action.
     */
    protected suspend fun sendAction(action: A) {
        actionChannel.send(action)
    }

    /**
     * Helper method for sending an event.
     */
    protected fun sendEvent(event: E) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
