package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of retrieving the device matching this app's unique identifier.
 */
sealed class GetDeviceResult {
    /**
     * Contains the [DeviceInfo] for the current device.
     */
    data class Success(val device: DeviceInfo) : GetDeviceResult()

    /**
     * There was an error retrieving the device.
     */
    data object Error : GetDeviceResult()
}
