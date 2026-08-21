package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestUpdatesResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsUpdatesResult
import com.x8bit.bitwarden.data.auth.manager.model.CreateAuthRequestResult
import kotlinx.coroutines.flow.Flow

/**
 * A manager class for handling authentication for logging in with remote device.
 */
interface AuthRequestManager {
    /**
     * Creates a new authentication request and then continues to emit updates over time.
     */
    fun createAuthRequestWithUpdates(
        email: String,
        authRequestType: AuthRequestType,
    ): Flow<CreateAuthRequestResult>

    /**
     * Get an auth request by its [fingerprint] and emits updates for that request.
     */
    fun getAuthRequestByFingerprintFlow(fingerprint: String): Flow<AuthRequestUpdatesResult>

    /**
     * Get an auth request by its request ID and emits updates for that request.
     */
    fun getAuthRequestByIdFlow(requestId: String): Flow<AuthRequestUpdatesResult>

    /**
     * Get all auth request and emits updates over time.
     */
    fun getAuthRequestsWithUpdates(): Flow<AuthRequestsUpdatesResult>

    /**
     * Get the [AuthRequest] for each incoming passwordless request for the active user, hydrated
     * with the fingerprint required to approve it.
     *
     * Only requests that can still be acted upon are emitted; those already approved, declined, or
     * expired are not. Requests that cannot be retrieved are omitted rather than emitted as errors.
     */
    fun getPasswordlessAuthRequestFlow(): Flow<AuthRequest>

    /**
     * Get an [AuthRequest] by its request ID.
     */
    suspend fun getAuthRequestIfApproved(requestId: String): Result<AuthRequest>

    /**
     * Get a list of the current user's [AuthRequest]s.
     */
    suspend fun getAuthRequests(): AuthRequestsResult

    /**
     * Approves or declines the request corresponding to this [requestId] based on [publicKey]
     * according to [isApproved].
     */
    suspend fun updateAuthRequest(
        requestId: String,
        masterPasswordHash: String?,
        publicKey: String,
        isApproved: Boolean,
    ): AuthRequestResult
}
