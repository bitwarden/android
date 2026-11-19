package com.x8bit.bitwarden.data.pam.datasource.sdk

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.pam.AccessDecisionRequest
import com.bitwarden.pam.AccessDecisionVerdict
import com.bitwarden.pam.AccessLeaseExtensionRequest
import com.bitwarden.pam.AccessLeaseRevokeRequest
import com.bitwarden.pam.AccessLeaseView
import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.AccessRequestView
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.sdk.AccessRequestsClient
import com.bitwarden.sdk.ApprovalsClient
import com.bitwarden.sdk.Client
import com.bitwarden.sdk.CommercialClient
import com.bitwarden.sdk.LeasesClient
import com.bitwarden.sdk.PamClient
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PamSdkSourceTest {
    private val accessRequestsClient = mockk<AccessRequestsClient>()
    private val leasesClient = mockk<LeasesClient>()
    private val approvalsClient = mockk<ApprovalsClient>()
    private val pamClient = mockk<PamClient> {
        every { accessRequests() } returns accessRequestsClient
        every { leases() } returns leasesClient
        every { approvals() } returns approvalsClient
    }
    private val commercialClient = mockk<CommercialClient> {
        every { pam() } returns pamClient
    }
    private val client = mockk<Client> {
        every { commercial() } returns commercialClient
    }
    private val sdkClientManager = mockk<SdkClientManager> {
        coEvery { getOrCreateClient(userId = USER_ID) } returns client
    }
    private val pamSdkSource: PamSdkSource = PamSdkSourceImpl(
        sdkClientManager = sdkClientManager,
    )

    @Test
    fun `preCheck should call the SDK and return the result`() = runTest {
        val expected = mockk<AccessPreCheckView>()
        coEvery { accessRequestsClient.preCheck(cipherId = CIPHER_ID) } returns expected

        val result = pamSdkSource.preCheck(userId = USER_ID, cipherId = CIPHER_ID)

        assertEquals(expected.asSuccess(), result)
    }

    @Test
    fun `preCheck should return a failure when the SDK throws`() = runTest {
        val error = IllegalStateException("fail")
        coEvery { accessRequestsClient.preCheck(cipherId = CIPHER_ID) } throws error

        val result = pamSdkSource.preCheck(userId = USER_ID, cipherId = CIPHER_ID)

        assertEquals(error.asFailure(), result)
    }

    @Test
    fun `getCipherAccessState should call the SDK and return the result`() = runTest {
        val expected = mockk<CipherAccessStateView>()
        coEvery { accessRequestsClient.cipherAccessState(cipherId = CIPHER_ID) } returns expected

        val result = pamSdkSource.getCipherAccessState(userId = USER_ID, cipherId = CIPHER_ID)

        assertEquals(expected.asSuccess(), result)
    }

    @Test
    fun `requestAccess should call the SDK with the request and return the result`() = runTest {
        val request = AccessRequestCreateRequest(
            durationSeconds = 60u,
            start = null,
            end = null,
            reason = null,
        )
        val expected = mockk<AccessRequestResultView>()
        coEvery {
            accessRequestsClient.request(cipherId = CIPHER_ID, request = request)
        } returns expected

        val result = pamSdkSource.requestAccess(
            userId = USER_ID,
            cipherId = CIPHER_ID,
            request = request,
        )

        assertEquals(expected.asSuccess(), result)
        coVerify(exactly = 1) {
            accessRequestsClient.request(cipherId = CIPHER_ID, request = request)
        }
    }

    @Test
    fun `getLeasedCipher should call the SDK and return the cipher`() = runTest {
        val expected = mockk<CipherView>()
        coEvery { leasesClient.leasedCipher(cipherId = CIPHER_ID) } returns expected

        val result = pamSdkSource.getLeasedCipher(userId = USER_ID, cipherId = CIPHER_ID)

        assertEquals(expected.asSuccess(), result)
    }

    @Test
    fun `getLeasedCipher should return null while the cipher is gated`() = runTest {
        coEvery { leasesClient.leasedCipher(cipherId = CIPHER_ID) } returns null

        val result = pamSdkSource.getLeasedCipher(userId = USER_ID, cipherId = CIPHER_ID)

        assertEquals(Result.success(null), result)
    }

    @Test
    fun `listMyAccessRequests should call the SDK and return the requests`() = runTest {
        val expected = listOf(mockk<AccessRequestView>())
        coEvery { accessRequestsClient.listMine() } returns expected

        assertEquals(expected.asSuccess(), pamSdkSource.listMyAccessRequests(userId = USER_ID))
    }

    @Test
    fun `listMyAccessRequests should return a failure when the SDK throws`() = runTest {
        val error = IllegalStateException("fail")
        coEvery { accessRequestsClient.listMine() } throws error

        assertEquals(
            error.asFailure(),
            pamSdkSource.listMyAccessRequests(userId = USER_ID),
        )
    }

    @Test
    fun `listMyLeases should call the SDK and return the leases`() = runTest {
        val expected = listOf(mockk<AccessLeaseView>())
        coEvery { leasesClient.listMine() } returns expected

        assertEquals(expected.asSuccess(), pamSdkSource.listMyLeases(userId = USER_ID))
    }

    @Test
    fun `activateAccessRequest should call the SDK and return the lease`() = runTest {
        val expected = mockk<AccessLeaseView>()
        coEvery { accessRequestsClient.activate(id = REQUEST_ID) } returns expected

        val result = pamSdkSource.activateAccessRequest(userId = USER_ID, requestId = REQUEST_ID)

        assertEquals(expected.asSuccess(), result)
    }

    @Test
    fun `cancelAccessRequest should call the SDK`() = runTest {
        coEvery { accessRequestsClient.cancel(id = REQUEST_ID) } returns Unit

        val result = pamSdkSource.cancelAccessRequest(userId = USER_ID, requestId = REQUEST_ID)

        assertEquals(Unit.asSuccess(), result)
        coVerify(exactly = 1) { accessRequestsClient.cancel(id = REQUEST_ID) }
    }

    @Test
    fun `cancelAccessRequest should return a failure when the SDK throws`() = runTest {
        coEvery { accessRequestsClient.cancel(id = REQUEST_ID) } throws IllegalStateException()

        val result = pamSdkSource.cancelAccessRequest(userId = USER_ID, requestId = REQUEST_ID)

        assertTrue(result.isFailure)
    }

    @Test
    fun `endLease should call the SDK`() = runTest {
        val request = AccessLeaseRevokeRequest(reason = null)
        coEvery { leasesClient.end(leaseId = LEASE_ID, request = request) } returns Unit

        val result = pamSdkSource.endLease(userId = USER_ID, leaseId = LEASE_ID, request = request)

        assertEquals(Unit.asSuccess(), result)
        coVerify(exactly = 1) { leasesClient.end(leaseId = LEASE_ID, request = request) }
    }

    @Test
    fun `extendLease should call the SDK and return the extension request`() = runTest {
        val request = AccessLeaseExtensionRequest(durationSeconds = 600u, reason = "More time")
        val expected = mockk<AccessRequestView>()
        coEvery { leasesClient.extend(leaseId = LEASE_ID, request = request) } returns expected

        val result = pamSdkSource.extendLease(
            userId = USER_ID,
            leaseId = LEASE_ID,
            request = request,
        )

        assertEquals(expected.asSuccess(), result)
    }

    @Test
    fun `listApprovalInbox should call the SDK and return the requests`() = runTest {
        val expected = listOf(mockk<AccessRequestView>())
        coEvery { approvalsClient.listInbox() } returns expected

        assertEquals(expected.asSuccess(), pamSdkSource.listApprovalInbox(userId = USER_ID))
    }

    @Test
    fun `listApprovalInbox should return a failure when the SDK throws`() = runTest {
        val error = IllegalStateException("fail")
        coEvery { approvalsClient.listInbox() } throws error

        assertEquals(error.asFailure(), pamSdkSource.listApprovalInbox(userId = USER_ID))
    }

    @Test
    fun `listApprovalHistory should call the SDK and return the requests`() = runTest {
        val expected = listOf(mockk<AccessRequestView>())
        coEvery { approvalsClient.listHistory() } returns expected

        assertEquals(expected.asSuccess(), pamSdkSource.listApprovalHistory(userId = USER_ID))
    }

    @Test
    fun `listApprovalHistory should return a failure when the SDK throws`() = runTest {
        val error = IllegalStateException("fail")
        coEvery { approvalsClient.listHistory() } throws error

        assertEquals(error.asFailure(), pamSdkSource.listApprovalHistory(userId = USER_ID))
    }

    @Test
    fun `decideAccessRequest should call the SDK and return the decided request`() = runTest {
        val request = AccessDecisionRequest(verdict = AccessDecisionVerdict.DENY, comment = "No")
        val expected = mockk<AccessRequestView>()
        coEvery { approvalsClient.decide(id = REQUEST_ID, request = request) } returns expected

        val result = pamSdkSource.decideAccessRequest(
            userId = USER_ID,
            requestId = REQUEST_ID,
            request = request,
        )

        assertEquals(expected.asSuccess(), result)
    }

    @Test
    fun `decideAccessRequest should return a failure when the SDK throws`() = runTest {
        val error = IllegalStateException("fail")
        val request = AccessDecisionRequest(verdict = AccessDecisionVerdict.APPROVE, comment = null)
        coEvery { approvalsClient.decide(id = REQUEST_ID, request = request) } throws error

        val result = pamSdkSource.decideAccessRequest(
            userId = USER_ID,
            requestId = REQUEST_ID,
            request = request,
        )

        assertEquals(error.asFailure(), result)
    }
}

private const val USER_ID = "userId"
private const val CIPHER_ID = "cipherId"
private const val REQUEST_ID = "requestId"
private const val LEASE_ID = "leaseId"
