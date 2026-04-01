package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for the list of devices registered to the current user.
 *
 * @property devices The list of devices.
 */
@Serializable
data class DevicesResponseJson(
    @SerialName("data") val devices: List<DeviceResponseJson>,
)
