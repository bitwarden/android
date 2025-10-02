package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import android.util.Base64
import com.bitwarden.fido.AuthenticatorAttestationResponse
import com.bitwarden.fido.ClientExtensionResults
import com.bitwarden.fido.CredPropsResult
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAttestationResponse
import com.bitwarden.fido.SelectedCredential
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialView
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PublicKeyCredentialAuthenticatorAttestationResponseExtensionsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns ""
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `authenticatorAttachment should be null when SDK value is null`() {
        val mockSdkResponse = createMockSdkAttestationResponse(number = 1)
        val result = mockSdkResponse.toAndroidAttestationResponse()
        assertNull(result.authenticatorAttachment)
    }

    @Test
    fun `authenticatorAttachment should be populated when SDK value is non-null`() {
        val mockSdkResponse = createMockSdkAttestationResponse(
            number = 1,
            authenticatorAttachment = "mockAuthenticatorAttachment",
        )
        val result = mockSdkResponse.toAndroidAttestationResponse()
        assertNotNull(result.authenticatorAttachment)
    }

    @Test
    fun `clientExtensionResults should be populated when SDK value is null`() {
        val mockSdkResponse = createMockSdkAttestationResponse(number = 1)
        val result = mockSdkResponse.toAndroidAttestationResponse()
        assertNotNull(result.clientExtensionResults)
    }

    @Test
    fun `residentKey should be populated when SDK value is non-null`() {
        val mockSdkResponse = createMockSdkAttestationResponse(
            number = 1,
            credProps = CredPropsResult(
                rk = true,
                authenticatorDisplayName = null,
            ),
        )
        val result = mockSdkResponse.toAndroidAttestationResponse()
        assert(result.clientExtensionResults.credentialProperties?.residentKey ?: false)
    }
}

private fun createMockSdkAttestationResponse(
    number: Int,
    authenticatorAttachment: String? = null,
    credProps: CredPropsResult? = null,
) = PublicKeyCredentialAuthenticatorAttestationResponse(
    id = "mockId-$number",
    rawId = byteArrayOf(0),
    ty = "mockTy-$number",
    authenticatorAttachment = authenticatorAttachment,
    clientExtensionResults = ClientExtensionResults(credProps),
    response = AuthenticatorAttestationResponse(
        clientDataJson = byteArrayOf(0),
        authenticatorData = byteArrayOf(0),
        publicKey = byteArrayOf(0),
        publicKeyAlgorithm = Long.MIN_VALUE,
        attestationObject = byteArrayOf(0),
        transports = listOf("internal"),
    ),
    selectedCredential = SelectedCredential(
        cipher = createMockCipherView(number = 1),
        credential = createMockFido2CredentialView(number = 1),
    ),
)
