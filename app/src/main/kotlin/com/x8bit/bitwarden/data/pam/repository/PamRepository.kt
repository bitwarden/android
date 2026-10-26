package com.x8bit.bitwarden.data.pam.repository

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamItemAccess
import com.x8bit.bitwarden.data.pam.repository.model.PamMyAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.RequestPamAccessResult
import kotlinx.coroutines.flow.SharedFlow

/**
 * Provides an API for observing and requesting privileged access (PAM) to gated vault items.
 */
@Suppress("TooManyFunctions")
interface PamRepository {

    /**
     * Emits whenever the user's access changed because of an action taken in the app (access was
     * started, ended, extended or cancelled), so screens showing a gated item can refresh and
     * mask or reveal it.
     */
    val accessChangedFlow: SharedFlow<Unit>

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

    /**
     * Returns the requests made by the active user, newest first. Also brings the scheduled
     * lease-ending reminders in line with the leases the server reports.
     */
    suspend fun getMyAccessRequests(): Result<List<PamMyAccessRequest>>

    /**
     * Returns the number of the active user's own access requests that are still current:
     * pending, approved and ready to start, or active. These are the requests the "My access"
     * screen lists under its current tab.
     */
    suspend fun getCurrentAccessRequestCount(): Result<Int>

    /**
     * Starts access for the approved request with the given [requestId].
     */
    suspend fun startAccess(requestId: String): Result<Unit>

    /**
     * Cancels the pending or approved-unstarted request with the given [requestId].
     */
    suspend fun cancelAccessRequest(requestId: String): Result<Unit>

    /**
     * Ends the active lease with the given [leaseId].
     */
    suspend fun endAccess(leaseId: String): Result<Unit>

    /**
     * Requests an extension of the active lease with the given [leaseId] by [durationSeconds],
     * giving the required [reason].
     */
    suspend fun extendAccess(
        leaseId: String,
        durationSeconds: Long,
        reason: String,
    ): Result<Unit>
}
