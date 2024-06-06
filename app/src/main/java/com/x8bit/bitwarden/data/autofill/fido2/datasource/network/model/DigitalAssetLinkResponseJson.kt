package com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models a response from an RP digital asset link request.
 */
@Serializable
data class DigitalAssetLinkResponseJson(
    @SerialName("relation")
    val relation: List<String>,

    @SerialName("target")
    val target: Target,
) {

    /**
     * Represents targets for an asset link statement.
     */
    @Serializable
    data class Target(
        @SerialName("namespace")
        val namespace: String,

        @SerialName("package_name")
        val packageName: String?,

        @SerialName("sha256_cert_fingerprints")
        val sha256CertFingerprints: List<String>?,
    )
}
