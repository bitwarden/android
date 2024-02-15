package com.x8bit.bitwarden.ui.platform.manager.nfc

/**
 * Provides basic functionality for starting and stopping discovery of NFC devices.
 */
interface NfcManager {
    /**
     * Starts listening for NFC events.
     *
     * Note: This is a no-op if the device does not support NFC.
     */
    fun start()

    /**
     * Stops listening for NFC events.
     *
     * Note: This is a no-op if the device does not support NFC.
     */
    fun stop()
}
