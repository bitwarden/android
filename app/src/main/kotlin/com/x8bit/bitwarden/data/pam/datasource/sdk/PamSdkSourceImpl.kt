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
import com.x8bit.bitwarden.data.platform.datasource.sdk.BaseSdkSource
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager

/**
 * Primary implementation of [PamSdkSource] that wraps the SDK's commercial PAM client.
 */
@Suppress("TooManyFunctions")
class PamSdkSourceImpl(
    sdkClientManager: SdkClientManager,
) : BaseSdkSource(sdkClientManager = sdkClientManager),
    PamSdkSource {

    override suspend fun preCheck(
        userId: String,
        cipherId: String,
    ): Result<AccessPreCheckView> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .accessRequests()
            .preCheck(cipherId = cipherId)
    }

    override suspend fun getCipherAccessState(
        userId: String,
        cipherId: String,
    ): Result<CipherAccessStateView> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .accessRequests()
            .cipherAccessState(cipherId = cipherId)
    }

    override suspend fun requestAccess(
        userId: String,
        cipherId: String,
        request: AccessRequestCreateRequest,
    ): Result<AccessRequestResultView> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .accessRequests()
            .request(cipherId = cipherId, request = request)
    }

    override suspend fun getLeasedCipher(
        userId: String,
        cipherId: String,
    ): Result<CipherView?> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .leases()
            .leasedCipher(cipherId = cipherId)
    }

    override suspend fun listMyAccessRequests(
        userId: String,
    ): Result<List<AccessRequestView>> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .accessRequests()
            .listMine()
    }

    override suspend fun listMyLeases(
        userId: String,
    ): Result<List<AccessLeaseView>> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .leases()
            .listMine()
    }

    override suspend fun activateAccessRequest(
        userId: String,
        requestId: String,
    ): Result<AccessLeaseView> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .accessRequests()
            .activate(id = requestId)
    }

    override suspend fun cancelAccessRequest(
        userId: String,
        requestId: String,
    ): Result<Unit> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .accessRequests()
            .cancel(id = requestId)
    }

    override suspend fun endLease(
        userId: String,
        leaseId: String,
        request: AccessLeaseRevokeRequest,
    ): Result<Unit> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .leases()
            .end(leaseId = leaseId, request = request)
    }

    override suspend fun extendLease(
        userId: String,
        leaseId: String,
        request: AccessLeaseExtensionRequest,
    ): Result<AccessRequestView> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .leases()
            .extend(leaseId = leaseId, request = request)
    }

    override suspend fun listApprovalInbox(
        userId: String,
    ): Result<List<AccessRequestView>> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .approvals()
            .listInbox()
    }

    override suspend fun listApprovalHistory(
        userId: String,
    ): Result<List<AccessRequestView>> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .approvals()
            .listHistory()
    }

    override suspend fun decideAccessRequest(
        userId: String,
        requestId: String,
        request: AccessDecisionRequest,
    ): Result<AccessRequestView> = runCatchingWithLogs {
        getClient(userId = userId)
            .commercial()
            .pam()
            .approvals()
            .decide(id = requestId, request = request)
    }
}
