package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the data used to create the kdf settings.
 */
@Serializable
data class KdfJson(
    @SerialName("kdfType")
    val kdfType: KdfTypeJson,

    @SerialName("iterations")
    val iterations: Int,

    @SerialName("memory")
    val memory: Int?,

    @SerialName("parallelism")
    val parallelism: Int?,
)
