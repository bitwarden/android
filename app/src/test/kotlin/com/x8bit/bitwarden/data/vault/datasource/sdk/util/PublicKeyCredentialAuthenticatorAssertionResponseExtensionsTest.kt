package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import android.util.Base64
import com.bitwarden.fido.AuthenticatorAssertionResponse
import com.bitwarden.fido.ClientExtensionResults
import com.bitwarden.fido.CredPropsResult
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.bitwarden.fido.SelectedCredential
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialView
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PublicKeyCredentialAuthenticatorAssertionResponseExtensionsTest {

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
        val mockSdkResponse = createMockSdkAssertionResponse(number = 1)
        val result = mockSdkResponse.toAndroidFido2PublicKeyCredential()
        assertNull(result.authenticatorAttachment)
    }

    @Test
    fun `authenticatorAttachment should be populated when SDK value is non-null`() {
        val mockSdkResponse = createMockSdkAssertionResponse(
            number = 1,
            authenticatorAttachment = "mockAuthenticatorAttachment",
        )
        val result = mockSdkResponse.toAndroidFido2PublicKeyCredential()
        assertNotNull(result.authenticatorAttachment)
    }

    @Test
    fun `credentialProperties should be null when SDK value is null`() {
        val mockSdkResponse = createMockSdkAssertionResponse(number = 1)
        val result = mockSdkResponse.toAndroidFido2PublicKeyCredential()
        assertNull(result.clientExtensionResults.credentialProperties)
    }

    @Test
    fun `credentialProperties should be populated when SDK value is non-null`() {
        val mockSdkResponse = createMockSdkAssertionResponse(
            number = 1,
            credProps = CredPropsResult(
                rk = true,
            ),
        )
        val result = mockSdkResponse.toAndroidFido2PublicKeyCredential()
        assertNotNull(result.clientExtensionResults.credentialProperties)
    }

    @Test
    fun `residentKey defaults to true when SDK value is null`() {
        val mockSdkResponse = createMockSdkAssertionResponse(
            number = 1,
            credProps = CredPropsResult(
                rk = null,
            ),
        )
        val result = mockSdkResponse.toAndroidFido2PublicKeyCredential()
        assertTrue(result.clientExtensionResults.credentialProperties?.residentKey!!)
    }
}

private fun createMockSdkAssertionResponse(
    number: Int,
    authenticatorAttachment: String? = null,
    credProps: CredPropsResult? = null,
) = PublicKeyCredentialAuthenticatorAssertionResponse(
    id = "mockId-$number",
    rawId = byteArrayOf(0),
    ty = "mockTy-$number",
    authenticatorAttachment = authenticatorAttachment,
    clientExtensionResults = ClientExtensionResults(credProps = credProps),
    response = AuthenticatorAssertionResponse(
        clientDataJson = byteArrayOf(0),
        authenticatorData = byteArrayOf(0),
        signature = byteArrayOf(0),
        userHandle = byteArrayOf(0),
    ),
    selectedCredential = SelectedCredential(
        cipher = createMockCipherView(number = 1),
        credential = createMockFido2CredentialView(number = 1),
    ),
)
