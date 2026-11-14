package com.x8bit.bitwarden.data.pam.repository

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamItemAccess
import com.x8bit.bitwarden.data.pam.repository.model.RequestPamAccessResult

/**
 * Provides an API for observing and requesting privileged access (PAM) to gated vault items.
 */
interface PamRepository {

    /**
     * Returns the [PamItemAccess] for the cipher with the given [cipherId], combining the SDK's
     * access-request pre-check and the cipher's access state.
     */
    suspend fun getItemAccess(cipherId: String): Result<PamItemAccess>

    /**
     * Submits an access [request] for the cipher with the given [cipherId].
     */
    suspend fun requestAccess(
        cipherId: String,
        request: PamAccessRequest,
    ): RequestPamAccessResult

    /**
     * Returns the fully decrypted cipher while the user holds an active lease for it, or `null`
     * while the cipher is still gated.
     */
    suspend fun getLeasedCipher(cipherId: String): Result<CipherView?>
}
