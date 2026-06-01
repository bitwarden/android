package com.bitwarden.testharness.data.manager

import com.bitwarden.testharness.data.model.CredentialTestResult

/**
 * Manager for testing credential operations through the Android CredentialManager API.
 *
 * This manager wraps the CredentialManager API to test credential provider implementations.
 */
interface CredentialTestManager {
    /**
     * Test password creation through CredentialManager.
     *
     * @param username The username/identifier for the password.
     * @param password The password value.
     * @param origin Optional origin/domain for the credential (e.g., "https://example.com").
     * @return Result indicating success, error, or cancellation.
     */
    suspend fun createPassword(
        username: String,
        password: String,
        origin: String?,
    ): CredentialTestResult

    /**
     * Test password retrieval through CredentialManager.
     *
     * @return Result indicating success with credential data, error, or cancellation.
     */
    suspend fun getPassword(): CredentialTestResult

    /**
     * Test passkey creation through CredentialManager.
     *
     * @param username The username/identifier for the passkey.
     * @param rpId The Relying Party ID (domain).
     * @param origin Optional origin/domain for privileged app simulation
     * (e.g., "https://example.com"). Used to simulate browser or password manager apps making
     * requests on behalf of websites. Requires CREDENTIAL_MANAGER_SET_ORIGIN permission.
     * @return Result indicating success, error, or cancellation.
     */
    suspend fun createPasskey(
        username: String,
        rpId: String,
        origin: String? = null,
    ): CredentialTestResult

    /**
     * Test passkey authentication through CredentialManager.
     *
     * @param rpId The Relying Party ID (domain) for the passkey request.
     * @param origin Optional origin/domain for privileged app simulation
     * (e.g., "https://example.com"). Used to simulate browser or password manager apps making
     * requests on behalf of websites. Requires CREDENTIAL_MANAGER_SET_ORIGIN permission.
     * @return Result indicating success with credential data, error, or cancellation.
     */
    suspend fun getPasskey(
        rpId: String,
        origin: String? = null,
    ): CredentialTestResult

    /**
     * Test combined password and passkey retrieval through CredentialManager.
     *
     * This method includes both GetPasswordOption and GetPublicKeyCredentialOption in a single
     * GetCredentialRequest, allowing the user to choose between saved passwords and passkeys
     * from the system credential picker.
     *
     * @param rpId The Relying Party ID (domain) for the passkey request.
     * @param origin Optional origin/domain for privileged app simulation
     * (e.g., "https://example.com"). Used to simulate browser or password manager apps making
     * requests on behalf of websites. Requires CREDENTIAL_MANAGER_SET_ORIGIN permission.
     * @return Result indicating success with credential data (password or passkey), error, or
     * cancellation.
     */
    suspend fun getPasswordOrPasskey(
        rpId: String,
        origin: String? = null,
    ): CredentialTestResult
}
