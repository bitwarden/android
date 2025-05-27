package com.bitwarden.authenticatorbridge.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A fake implementation of [LifecycleOwner] and [Lifecycle] for testing purposes.
 */
class FakeLifecycle(
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
