package com.x8bit.bitwarden.data.pam.datasource.sdk

import com.bitwarden.pam.AccessDecisionRequest
import com.bitwarden.pam.AccessLeaseExtensionRequest
import com.bitwarden.pam.AccessLeaseRevokeRequest
import com.bitwarden.pam.AccessLeaseView
import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.AccessRequestView
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.vault.CipherView

/**
 * Source of Privileged Access Management (PAM) functionality from the Bitwarden SDK.
 *
 * PAM must only ever be reached through the SDK, never through REST endpoints.
 */
@Suppress("TooManyFunctions")
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

    /**
     * Lists the access requests made by the user, including finished ones.
     */
    suspend fun listMyAccessRequests(userId: String): Result<List<AccessRequestView>>

    /**
     * Lists the leases held by the user, including finished ones.
     */
    suspend fun listMyLeases(userId: String): Result<List<AccessLeaseView>>

    /**
     * Starts access for the approved request with the given [requestId], minting a lease.
     */
    suspend fun activateAccessRequest(
        userId: String,
        requestId: String,
    ): Result<AccessLeaseView>

    /**
     * Cancels the pending or approved-but-unstarted request with the given [requestId].
     */
    suspend fun cancelAccessRequest(
        userId: String,
        requestId: String,
    ): Result<Unit>

    /**
     * Ends the active lease with the given [leaseId].
     */
    suspend fun endLease(
        userId: String,
        leaseId: String,
        request: AccessLeaseRevokeRequest,
    ): Result<Unit>

    /**
     * Requests an extension of the active lease with the given [leaseId].
     */
    suspend fun extendLease(
        userId: String,
        leaseId: String,
        request: AccessLeaseExtensionRequest,
    ): Result<AccessRequestView>

    /**
     * Lists the pending access requests the user can decide on as an approver.
     */
    suspend fun listApprovalInbox(userId: String): Result<List<AccessRequestView>>

    /**
     * Lists the access requests the user has decided on (or that were decided by others) as an
     * approver.
     */
    suspend fun listApprovalHistory(userId: String): Result<List<AccessRequestView>>

    /**
     * Records the approver's decision on the request with the given [requestId].
     */
    suspend fun decideAccessRequest(
        userId: String,
        requestId: String,
        request: AccessDecisionRequest,
    ): Result<AccessRequestView>
}
