package com.x8bit.bitwarden.testharness.data.model

/**
 * Result of a credential operation test.
 */
sealed class CredentialTestResult {
    /**
     * Credential operation completed successfully.
     *
     * @property message Human-readable success message.
     * @property data JSON representation of the credential response (if available).
     */
    data class Success(
        val message: String,
        val data: String? = null,
    ) : CredentialTestResult()

    /**
     * Credential operation failed with an error.
     *
     * @property message Human-readable error message.
     * @property exception The underlying exception that caused the failure.
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : CredentialTestResult()

    /**
     * User cancelled the credential operation.
     */
    data object Cancelled : CredentialTestResult()
}
