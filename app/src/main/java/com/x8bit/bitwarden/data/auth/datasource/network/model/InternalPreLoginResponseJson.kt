package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for pre login. This internal model is only used for as a surrogate serializer.
 *
 * See [PreLoginResponseJson] for exposed model.
 */
@Serializable
data class InternalPreLoginResponseJson(
    @SerialName("kdf")
    val kdfType: KdfTypeJson,

    @SerialName("kdfIterations")
    val kdfIterations: UInt,

    @SerialName("kdfMemory")
    val kdfMemory: UInt? = null,

    @SerialName("kdfParallelism")
    val kdfParallelism: UInt? = null,
)
