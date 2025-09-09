package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to create the kdf settings.
 */
@Serializable
data class KdfJsonRequest(
    @SerialName("KdfType")
    val kdfType: KdfTypeJson,

    @SerialName("Iterations")
    val iterations: Int,

    @SerialName("Memory")
    val memory: Int?,

    @SerialName("Parallelism")
    val parallelism: Int?,
)
