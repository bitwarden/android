package com.x8bit.bitwarden.data.pam.datasource.sdk

import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.vault.CipherView

/**
 * Source of Privileged Access Management (PAM) functionality from the Bitwarden SDK.
 *
 * PAM must only ever be reached through the SDK, never through REST endpoints.
 */
interface PamSdkSource {

    /**
     * Runs the access-request pre-check for the given [cipherId]. The result tells the caller
     * which request form to present (automatic or human approval) and whether the single lease
     * slot is currently free.
     */
    suspend fun preCheck(
        userId: String,
        cipherId: String,
    ): Result<AccessPreCheckView>

    /**
     * Returns the current access state (active lease, pending or approved request) of the cipher
     * with the given [cipherId].
     */
    suspend fun getCipherAccessState(
        userId: String,
        cipherId: String,
    ): Result<CipherAccessStateView>

    /**
     * Submits an access request for the cipher with the given [cipherId].
     */
    suspend fun requestAccess(
        userId: String,
        cipherId: String,
        request: AccessRequestCreateRequest,
    ): Result<AccessRequestResultView>

    /**
     * Fetches and decrypts the cipher with the given [cipherId] using the active lease. The result
     * is `null` while the cipher is still gated (no active lease).
     */
    suspend fun getLeasedCipher(
        userId: String,
        cipherId: String,
    ): Result<CipherView?>
}
