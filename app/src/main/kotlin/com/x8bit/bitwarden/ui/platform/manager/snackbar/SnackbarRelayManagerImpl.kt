package com.x8bit.bitwarden.ui.platform.manager.snackbar

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.util.emitWhenSubscribedTo
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The default implementation of the [SnackbarRelayManager] interface.
 */
class SnackbarRelayManagerImpl(
    dispatcherManager: DispatcherManager,
) : SnackbarRelayManager {
    private val unconfinedScope = CoroutineScope(context = dispatcherManager.unconfined)
    private val snackbarSharedFlow = SnackbarLastSubscriberMutableSharedFlow()

    override fun sendSnackbarData(data: BitwardenSnackbarData, relay: SnackbarRelay) {
        unconfinedScope.launch {
            snackbarSharedFlow.emitWhenSubscribedTo(
                value = SnackbarDataAndRelay(
                    relay = relay,
                    data = data,
                ),
            )
        }
    }

    override fun getSnackbarDataFlow(
        relay: SnackbarRelay,
        vararg relays: SnackbarRelay,
    ): Flow<BitwardenSnackbarData> =
        merge(
            snackbarSharedFlow.generateFlowFor(relay = relay),
            *relays.map { snackbarSharedFlow.generateFlowFor(relay = it) }.toTypedArray(),
        )
            .map { it.data }
}

/**
 * A wrapper for the [BitwardenSnackbarData] payload and [SnackbarRelay] associated with it.
 */
private data class SnackbarDataAndRelay(
    val relay: SnackbarRelay,
    val data: BitwardenSnackbarData,
)

/**
 * Helper class that ensures that only the last subscriber to a specific relay gets the Snackbar
 * data.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class SnackbarLastSubscriberMutableSharedFlow(
    private val source: MutableSharedFlow<SnackbarDataAndRelay> = MutableSharedFlow(),
) : MutableSharedFlow<SnackbarDataAndRelay> by source {
    private val mutableRelayUuidMap: MutableMap<SnackbarRelay, MutableList<UUID>> = mutableMapOf()

    fun generateFlowFor(
        relay: SnackbarRelay,
    ): Flow<SnackbarDataAndRelay> {
        lateinit var uuid: UUID
        return source
            .onSubscription {
                uuid = UUID.randomUUID().also { getUuidStack(relay = relay).add(element = it) }
            }
            .onCompletion { getUuidStack(relay = relay).remove(element = uuid) }
            .filter { it.relay == relay }
            .filter { getUuidStack(relay = relay).last() == uuid }
    }

    private fun getUuidStack(
        relay: SnackbarRelay,
    ): MutableList<UUID> = mutableRelayUuidMap.getOrPut(key = relay) { mutableListOf() }
}
