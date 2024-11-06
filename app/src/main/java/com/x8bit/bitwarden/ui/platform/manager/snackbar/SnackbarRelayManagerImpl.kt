package com.x8bit.bitwarden.ui.platform.manager.snackbar

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion

/**
 * The default implementation of the [SnackbarRelayManager] interface.
 */
class SnackbarRelayManagerImpl : SnackbarRelayManager {
    private val mutableSnackbarRelayMap =
        mutableMapOf<SnackbarRelay, MutableSharedFlow<BitwardenSnackbarData?>>()

    override fun sendSnackbarData(data: BitwardenSnackbarData, relay: SnackbarRelay) {
        getSnackbarDataFlowInternal(relay).tryEmit(data)
    }

    override fun getSnackbarDataFlow(relay: SnackbarRelay): Flow<BitwardenSnackbarData> =
        getSnackbarDataFlowInternal(relay)
            .onCompletion {
                // when the subscription is ended, remove the relay from the map.
                mutableSnackbarRelayMap.remove(relay)
            }
            .filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun clearRelayBuffer(relay: SnackbarRelay) {
        getSnackbarDataFlowInternal(relay).resetReplayCache()
    }

    private fun getSnackbarDataFlowInternal(
        relay: SnackbarRelay,
    ): MutableSharedFlow<BitwardenSnackbarData?> =
        mutableSnackbarRelayMap.getOrPut(relay) {
            bufferedMutableSharedFlow(replay = 1)
        }
}
