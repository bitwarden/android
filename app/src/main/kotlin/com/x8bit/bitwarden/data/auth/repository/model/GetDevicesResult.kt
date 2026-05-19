package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of retrieving all devices registered to the current user.
 */
sealed class GetDevicesResult {
    /**
     * Contains the list of [DeviceInfo] for the current user's registered devices.
     */
    data class Success(val devices: List<DeviceInfo>) : GetDevicesResult()

    /**
     * There was an error retrieving the devices.
     */
    data object Error : GetDevicesResult()
}
