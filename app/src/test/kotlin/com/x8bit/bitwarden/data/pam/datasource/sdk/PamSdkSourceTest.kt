package com.x8bit.bitwarden.data.pam.datasource.sdk

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.sdk.AccessRequestsClient
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
import org.junit.jupiter.api.Test

class PamSdkSourceTest {
    private val accessRequestsClient = mockk<AccessRequestsClient>()
    private val leasesClient = mockk<LeasesClient>()
    private val pamClient = mockk<PamClient> {
        every { accessRequests() } returns accessRequestsClient
        every { leases() } returns leasesClient
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
}

private const val USER_ID = "userId"
private const val CIPHER_ID = "cipherId"
