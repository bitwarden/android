package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of checking whether this is a known device.
 */
sealed class KnownDeviceResult {
    /**
     * Contains a [Boolean] indicating whether this is a known device.
     */
    data class Success(val isKnownDevice: Boolean) : KnownDeviceResult()

    /**
     * There was an error determining if this is a known device.
     */
    data object Error : KnownDeviceResult()
}
