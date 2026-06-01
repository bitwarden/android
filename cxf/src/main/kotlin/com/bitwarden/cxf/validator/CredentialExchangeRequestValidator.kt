package com.bitwarden.cxf.validator

import com.bitwarden.cxf.model.ImportCredentialsRequestData

/**
 * Defines the contract for validating a credential exchange request.
 *
 * This interface provides a mechanism to ensure that incoming credential import requests
 * are legitimate and meet the required security criteria before they are processed.
 */
interface CredentialExchangeRequestValidator {
    /**
     * Validates the provided [ImportCredentialsRequestData].
     *
     * @param importCredentialsRequestData The request data to be validated.
     * @return `true` if the request is valid, `false` otherwise.
     */
    fun validate(importCredentialsRequestData: ImportCredentialsRequestData): Boolean
}
