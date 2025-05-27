package com.x8bit.bitwarden.ui.platform.manager.snackbar

import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import kotlinx.coroutines.flow.Flow

/**
 * Manager responsible for relaying snackbar data between a producer and consumer who may
 * communicate with reference to a specific [SnackbarRelay].
 */
interface SnackbarRelayManager {
    /**
     * Called from a producer to send snackbar data to a consumer, the producer must
     * specify the [relay] to send the data to.
     */
    fun sendSnackbarData(data: BitwardenSnackbarData, relay: SnackbarRelay)

    /**
     * Called from a consumer to receive snackbar data from a producer, the consumer must specify
     * the [relay] to receive the data from.
     */
    fun getSnackbarDataFlow(relay: SnackbarRelay): Flow<BitwardenSnackbarData>

    /**
     * Clears the buffer for the given [relay].
     */
    fun clearRelayBuffer(relay: SnackbarRelay)
}
