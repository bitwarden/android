package com.x8bit.bitwarden.data.pam.datasource.sdk

import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.platform.datasource.sdk.BaseSdkSource
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager

/**
 * Primary implementation of [PamSdkSource] that wraps the SDK's commercial PAM client.
 */
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
}
