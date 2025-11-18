package com.bitwarden.testharness.data.model

/**
 * Result of a credential operation test.
 */
sealed class CredentialTestResult {
    /**
     * Credential operation completed successfully.
     *
     * @property data Structured credential response data (e.g., username, origin, JSON response).
     */
    data class Success(
        val data: String? = null,
    ) : CredentialTestResult()

    /**
     * Credential operation failed with an error.
     *
     * @property exception The underlying exception that caused the failure.
     */
    data class Error(
        val exception: Throwable? = null,
    ) : CredentialTestResult()

    /**
     * User cancelled the credential operation.
     */
    data object Cancelled : CredentialTestResult()
}
