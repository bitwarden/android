package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the V2 upgrade token, allowing vault unlock after V1 → V2 upgrade.
 */
@Serializable
data class V2UpgradeTokenJson(
    @SerialName("wrappedUserKey1")
    val wrappedUserKey1: String,

    @SerialName("wrappedUserKey2")
    val wrappedUserKey2: String,
)
