package com.bitwarden.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Represents the data used to create the kdf settings.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class KdfJson(
    @SerialName("kdfType")
    @JsonNames("KdfType")
    val kdfType: KdfTypeJson,

    @SerialName("iterations")
    @JsonNames("Iterations")
    val iterations: Int,

    @SerialName("memory")
    @JsonNames("Memory")
    val memory: Int?,

    @SerialName("parallelism")
    @JsonNames("Parallelism")
    val parallelism: Int?,
)
