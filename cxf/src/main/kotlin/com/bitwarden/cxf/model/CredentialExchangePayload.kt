package com.bitwarden.cxf.model

/**
 * Represents the result of parsing a CXF payload.
 */
sealed class CredentialExchangePayload {
    /**
     * Indicates that the payload is importable.
     */
    data class Importable(
        val accountsJson: String,
    ) : CredentialExchangePayload()

    /**
     * Indicates that the payload contains no importable items.
     */
    data object NoItems : CredentialExchangePayload()

    /**
     * An error occurred while parsing the payload.
     */
    data class Error(
        val throwable: Throwable,
    ) : CredentialExchangePayload()
}
