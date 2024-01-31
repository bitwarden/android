package com.x8bit.bitwarden.data.auth.datasource.network.model

/**
 * Hold the information necessary to add authorization with device to a login request.
 */
data class DeviceDataModel(
    val accessCode: String,
    val masterPasswordHash: String?,
    val asymmetricalKey: String,
    val privateKey: String,
)
