package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for pre login.
 */
@Serializable
data class PreLoginResponseJson(
    // TODO parse this property as an enum (BIT-329)
    @SerialName("kdf")
    val kdf: Int,
    @SerialName("kdfIterations")
    val kdfIterations: UInt,
    @SerialName("kdfMemory")
    val kdfMemory: Int? = null,
    @SerialName("kdfParallelism")
    val kdfParallelism: Int? = null,
)
