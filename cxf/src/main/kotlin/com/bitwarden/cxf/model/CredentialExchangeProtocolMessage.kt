package com.bitwarden.cxf.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the top-level structure of a message used in the credential exchange protocol.
 *
 * This data class is designed to be serialized and deserialized for importing or exporting
 * credentials between applications.
 *
 * @property version The version of the credential exchange protocol being used.
 * @property exporterRpId The relying party identifier (e.g., website domain) of the application
 * that exported the credentials.
 * @property exporterDisplayName The user-friendly display name of the application that exported
 * the credentials.
 * @property payload A base64-encoded string containing the actual credential data, typically
 * serialized and encrypted.
 */
@Serializable
data class CredentialExchangeProtocolMessage(
    @SerialName("version")
    val version: CredentialExchangeVersion,
    @SerialName("exporterRpId")
    val exporterRpId: String,
    @SerialName("exporterDisplayName")
    val exporterDisplayName: String,
    @SerialName("payload")
    val payload: String,
)
