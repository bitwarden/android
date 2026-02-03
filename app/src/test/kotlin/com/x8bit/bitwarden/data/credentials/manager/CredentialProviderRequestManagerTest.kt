package com.x8bit.bitwarden.data.credentials.manager

import com.x8bit.bitwarden.data.credentials.manager.model.CredentialProviderRequest
import com.x8bit.bitwarden.data.credentials.model.createMockCreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.createMockGetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.createMockProviderGetPasswordCredentialRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CredentialProviderRequestManagerTest {

    private lateinit var manager: CredentialProviderRequestManagerImpl

    @BeforeEach
    fun setup() {
        manager = CredentialProviderRequestManagerImpl()
    }

    @Test
    fun `getPendingCredentialRequest returns null when no request is pending`() {
        assertNull(manager.getPendingCredentialRequest())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setPendingCredentialRequest followed by getPendingCredentialRequest returns the request`() {
        val createRequest = createMockCreateCredentialRequest(number = 1)
        val credentialRequest = CredentialProviderRequest.CreateCredential(createRequest)

        manager.setPendingCredentialRequest(credentialRequest)
        val result = manager.getPendingCredentialRequest()

        assertEquals(credentialRequest, result)
    }

    @Test
    fun `getPendingCredentialRequest clears the pending request`() {
        val createRequest = createMockCreateCredentialRequest(number = 1)
        val credentialRequest = CredentialProviderRequest.CreateCredential(createRequest)

        manager.setPendingCredentialRequest(credentialRequest)
        manager.getPendingCredentialRequest()
        val secondResult = manager.getPendingCredentialRequest()

        assertNull(secondResult)
    }

    @Test
    fun `setPendingCredentialRequest overwrites previous request`() {
        val createRequest = createMockCreateCredentialRequest(number = 1)
        val assertionRequest = createMockFido2CredentialAssertionRequest(number = 1)
        val firstCredentialRequest = CredentialProviderRequest.CreateCredential(createRequest)
        val secondCredentialRequest = CredentialProviderRequest.Fido2Assertion(assertionRequest)

        manager.setPendingCredentialRequest(firstCredentialRequest)
        manager.setPendingCredentialRequest(secondCredentialRequest)
        val result = manager.getPendingCredentialRequest()

        assertEquals(secondCredentialRequest, result)
    }

    @Test
    fun `supports all CredentialProviderRequest types`() {
        val createRequest = createMockCreateCredentialRequest(number = 1)
        val fido2Request = createMockFido2CredentialAssertionRequest(number = 1)
        val passwordRequest = createMockProviderGetPasswordCredentialRequest(number = 1)
        val getCredentialsRequest = createMockGetCredentialsRequest(number = 1)

        // Test CreateCredential
        manager.setPendingCredentialRequest(
            CredentialProviderRequest.CreateCredential(createRequest),
        )
        assertEquals(
            CredentialProviderRequest.CreateCredential(createRequest),
            manager.getPendingCredentialRequest(),
        )

        // Test Fido2Assertion
        manager.setPendingCredentialRequest(
            CredentialProviderRequest.Fido2Assertion(fido2Request),
        )
        assertEquals(
            CredentialProviderRequest.Fido2Assertion(fido2Request),
            manager.getPendingCredentialRequest(),
        )

        // Test GetPassword
        manager.setPendingCredentialRequest(
            CredentialProviderRequest.GetPassword(passwordRequest),
        )
        assertEquals(
            CredentialProviderRequest.GetPassword(passwordRequest),
            manager.getPendingCredentialRequest(),
        )

        // Test GetCredentials
        manager.setPendingCredentialRequest(
            CredentialProviderRequest.GetCredentials(getCredentialsRequest),
        )
        assertEquals(
            CredentialProviderRequest.GetCredentials(getCredentialsRequest),
            manager.getPendingCredentialRequest(),
        )
    }
}
