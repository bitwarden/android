package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.util.Base64
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Origin
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2AttestationResponse
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2PublicKeyCredential
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialRequest
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPublicKeyAssertionResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPublicKeyAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.MessageDigest

@Suppress("LargeClass")
class Fido2CredentialManagerTest {

    private lateinit var fido2CredentialManager: Fido2CredentialManager

    private val fido2OriginManager = mockk<Fido2OriginManager> {
        coEvery { validateOrigin(any(), any()) } returns Fido2ValidateOriginResult.Success(null)
    }
    private val json = mockk<Json> {
        every {
            decodeFromString<PasskeyAttestationOptions>(any())
        } returns createMockPasskeyAttestationOptions(number = 1)
        every {
            decodeFromString<PasskeyAssertionOptions>(any())
        } returns createMockPasskeyAssertionOptions(number = 1)
        every {
            decodeFromStringOrNull<PasskeyAssertionOptions>(DEFAULT_FIDO2_AUTH_REQUEST_JSON)
        } returns createMockPasskeyAssertionOptions(number = 1)
    }
    private val mockSigningInfo = mockk<SigningInfo> {
        every { apkContentsSigners } returns arrayOf(Signature("0987654321ABCDEF"))
        every { hasMultipleSigners() } returns false
    }
    private val mockMessageDigest = mockk<MessageDigest> {
        every { digest(any()) } returns DEFAULT_APP_SIGNATURE.toByteArray()
    }
    private val mockVaultSdkSource = mockk<VaultSdkSource>()
    private val mockFido2CredentialStore = mockk<Fido2CredentialStore>()

    @BeforeEach
    fun setUp() {
        mockkStatic(MessageDigest::class, Base64::class)
        every { MessageDigest.getInstance(any()) } returns mockMessageDigest

        fido2CredentialManager = Fido2CredentialManagerImpl(
            vaultSdkSource = mockVaultSdkSource,
            fido2CredentialStore = mockFido2CredentialStore,
            fido2OriginManager = fido2OriginManager,
            json = json,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(MessageDigest::class, Base64::class)
        unmockkStatic(
            PublicKeyCredentialAuthenticatorAssertionResponse::toAndroidFido2PublicKeyCredential,
        )
    }

    @Test
    fun `getPasskeyAttestationOptionsOrNull should return passkey options when deserialized`() =
        runTest {
            assertEquals(
                createMockPasskeyAttestationOptions(number = 1),
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = "",
                ),
            )
        }

    @Test
    fun `getPasskeyAttestationOptionsOrNull should return null when deserialization fails`() =
        runTest {
            every {
                json.decodeFromString<PasskeyAttestationOptions>(any())
            } throws SerializationException()
            assertNull(
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = "",
                ),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getPasskeyAttestationOptionsOrNull should return null when IllegalArgumentException is thrown`() {
        every {
            json.decodeFromString<PasskeyAttestationOptions>(any())
        } throws IllegalArgumentException()

        assertNull(fido2CredentialManager.getPasskeyAttestationOptionsOrNull(requestJson = ""))
    }

    @Test
    fun `getPasskeyAssertionOptionsOrNull should return options when deserialized`() = runTest {
        assertEquals(
            createMockPasskeyAssertionOptions(number = 1),
            fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                requestJson = "",
            ),
        )
    }

    @Test
    fun `getPasskeyAssertionOptionsOrNull should return null when deserialization fails`() =
        runTest {
            every {
                json.decodeFromString<PasskeyAssertionOptions>(any())
            } throws SerializationException()
            assertNull(
                fido2CredentialManager.getPasskeyAssertionOptionsOrNull(
                    requestJson = "",
                ),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getPasskeyAssertionOptionsOrNull should return null when IllegalArgumentException is thrown`() {
        every {
            json.decodeFromString<PasskeyAssertionOptions>(any())
        } throws IllegalArgumentException()

        assertNull(fido2CredentialManager.getPasskeyAssertionOptionsOrNull(requestJson = ""))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should construct ClientData DefaultWithCustomHash when callingAppInfo origin is populated`() =
        runTest {
            val mockFido2CreateCredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            val mockCipherView = createMockCipherView(1)
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every { json.encodeToString<Fido2AttestationResponse>(any(), any()) } returns ""
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers {
                mockRegistrationResponse
                    .asSuccess()
            }

            fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CreateCredentialRequest,
                selectedCipherView = mockCipherView,
            )

            assertTrue(
                requestCaptureSlot.captured.clientData is ClientData.DefaultWithCustomHash,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should construct ClientData DefaultWithExtraData when callingAppInfo origin is null`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
                every { hasMultipleSigners() } returns false
            }
            val mockFido2Request = createMockFido2CredentialRequest(
                number = 1,
                signingInfo = mockSigningInfo,
            )
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every { json.encodeToString<Fido2AttestationResponse>(any(), any()) } returns ""
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers {
                mockRegistrationResponse
                    .asSuccess()
            }

            fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2Request,
                selectedCipherView = createMockCipherView(1),
            )
        }

    @Test
    fun `registerFido2Credential should wrap request in webauthn json object`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
                every { hasMultipleSigners() } returns false
            }
            val mockFido2CreateCredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            val mockCipherView = createMockCipherView(1)
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every { json.encodeToString<Fido2AttestationResponse>(any(), any()) } returns ""
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers {
                mockRegistrationResponse
                    .asSuccess()
            }

            fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CreateCredentialRequest,
                selectedCipherView = mockCipherView,
            )

            assertEquals(
                """{"publicKey": ${mockFido2CreateCredentialRequest.requestJson}}""",
                requestCaptureSlot.captured.requestJson,
            )
        }

    @Test
    fun `registerFido2Credential should register FIDO 2 credential to active user ID`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
                every { hasMultipleSigners() } returns false
            }
            val mockFido2CreateCredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            val mockCipherView = createMockCipherView(1)
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every { json.encodeToString<Fido2AttestationResponse>(any(), any()) } returns ""
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers {
                mockRegistrationResponse
                    .asSuccess()
            }

            fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CreateCredentialRequest,
                selectedCipherView = mockCipherView,
            )

            assertEquals(
                "mockUserId",
                requestCaptureSlot.captured.userId,
            )

            assertNotEquals(
                "mockUserId",
                mockFido2CreateCredentialRequest.userId,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should return Error when getAppSigningSignatureFingerprint is null`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { hasMultipleSigners() } returns true
            }
            val mockFido2CredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )

            val result = fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            assertEquals(
                Fido2RegisterCredentialResult.Error,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should return Error when getSignatureFingerprintAsHexString is null`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { hasMultipleSigners() } returns true
            }
            val mockFido2CredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                signingInfo = mockSigningInfo,
            )

            val result = fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            assertEquals(
                Fido2RegisterCredentialResult.Error,
                result,
            )
        }

    @Test
    fun `registerFido2Credential should return Error when toHostOrPathOrNull is null`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
                every { hasMultipleSigners() } returns false
            }
            val mockFido2CredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                origin = "illegal empty spaces",
                signingInfo = mockSigningInfo,
            )

            val result = fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            assertEquals(
                Fido2RegisterCredentialResult.Error,
                result,
            )
        }

    @Test
    fun `registerFido2Credential should return Error when deserialization fails`() =
        runTest {
            val mockSigningInfo = mockk<SigningInfo> {
                every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
                every { hasMultipleSigners() } returns false
            }
            val mockFido2CredentialRequest = createMockFido2CredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every {
                json.encodeToString<Fido2AttestationResponse>(
                    any(),
                    any(),
                )
            } throws IllegalArgumentException()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
            } coAnswers {
                mockRegistrationResponse
                    .asSuccess()
            }

            val result = fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            assertEquals(
                Fido2RegisterCredentialResult.Error,
                result,
            )
        }

    @Test
    fun `registerFido2Credential should return Error when origin is null`() = runTest {
        val mockAssertionRequest = createMockFido2CredentialRequest(
            number = 1,
            origin = null,
            signingInfo = mockSigningInfo,
        )
        val mockSelectedCipher = createMockCipherView(number = 1)

        every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
        every {
            json.decodeFromString<PasskeyAttestationOptions>(any())
        } throws SerializationException()

        val result = fido2CredentialManager.registerFido2Credential(
            userId = "activeUserId",
            fido2CreateCredentialRequest = mockAssertionRequest,
            selectedCipherView = mockSelectedCipher,
        )

        coVerify(exactly = 0) {
            mockVaultSdkSource.registerFido2Credential(
                request = any(),
                fido2CredentialStore = any(),
            )
        }

        assertEquals(
            Fido2RegisterCredentialResult.Error,
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasAuthenticationAttemptsRemaining returns true when authenticationAttempts is less than 5`() {
        assertTrue(fido2CredentialManager.hasAuthenticationAttemptsRemaining())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hasAuthenticationAttemptsRemaining returns false when authenticationAttempts is greater than 5`() {
        fido2CredentialManager.authenticationAttempts = 6
        assertFalse(fido2CredentialManager.hasAuthenticationAttemptsRemaining())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFido2Credential should construct ClientData DefaultWithCustomHash when clientDataHash is not null`() =
        runTest {
            every { Base64.encodeToString(any(), any()) } returns ""
            val mockCipherView = createMockCipherView(number = 1)
            val mockRequest = createMockFido2AssertionRequest(
                mockClientDataHash = byteArrayOf(),
                mockSigningInfo = mockSigningInfo,
            )
            val requestCaptureSlot = slot<AuthenticateFido2CredentialRequest>()
            val mockSdkResponse =
                mockk<PublicKeyCredentialAuthenticatorAssertionResponse>(relaxed = true)
            coEvery {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
            } returns mockSdkResponse.asSuccess()

            fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = mockRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            coVerify {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            }
            assertEquals(
                AuthenticateFido2CredentialRequest(
                    userId = "activeUserId",
                    origin = DEFAULT_ORIGIN,
                    requestJson = """{"publicKey": ${mockRequest.requestJson}}""",
                    clientData = ClientData.DefaultWithCustomHash(mockRequest.clientDataHash!!),
                    selectedCipherView = mockCipherView,
                    isUserVerificationSupported = true,
                ),
                requestCaptureSlot.captured,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFido2Credential should construct ClientData DefaultWithExtraData when clientDataHash is null`() =
        runTest {
            every {
                mockSigningInfo.apkContentsSigners
            } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
            every {
                mockSigningInfo.hasMultipleSigners()
            } returns false
            val mockCipherView = createMockCipherView(number = 1)
            val mockRequest = createMockFido2AssertionRequest(
                mockSigningInfo = mockSigningInfo,
            )
            val requestCaptureSlot = slot<AuthenticateFido2CredentialRequest>()
            val mockSdkResponse =
                mockk<PublicKeyCredentialAuthenticatorAssertionResponse>(relaxed = true)
            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            coEvery {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
            } returns mockSdkResponse.asSuccess()

            fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = mockRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            coVerify {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            }
            assertEquals(
                AuthenticateFido2CredentialRequest(
                    userId = "activeUserId",
                    origin = Origin.Android(
                        UnverifiedAssetLink(
                            packageName = DEFAULT_PACKAGE_NAME,
                            sha256CertFingerprint = DEFAULT_CERT_FINGERPRINT,
                            host = DEFAULT_HOST,
                            assetLinkUrl = mockRequest.origin!!,
                        ),
                    ),
                    requestJson = """{"publicKey": ${mockRequest.requestJson}}""",
                    clientData = ClientData.DefaultWithExtraData(
                        androidPackageName = "android:apk-key-hash:$DEFAULT_APP_SIGNATURE",
                    ),
                    selectedCipherView = mockCipherView,
                    isUserVerificationSupported = true,
                ),
                requestCaptureSlot.captured,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFido2Credential should construct correct assertion request when calling app is unprivileged`() =
        runTest {
            val mockAssertionRequest = createMockFido2AssertionRequest(
                mockOrigin = null,
                mockClientDataHash = null,
                mockSigningInfo = mockSigningInfo,
            )
            val mockAssertionOptions = createMockPasskeyAssertionOptions(number = 1)
            val mockSelectedCipher = createMockCipherView(number = 1)
            val requestCaptureSlot = slot<AuthenticateFido2CredentialRequest>()

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            coEvery {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
            } returns createMockPublicKeyAssertionResponse(number = 1).asSuccess()

            fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = mockAssertionRequest,
                selectedCipherView = mockSelectedCipher,
            )

            coVerify {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            }

            assertEquals(
                Origin.Android(
                    UnverifiedAssetLink(
                        DEFAULT_PACKAGE_NAME,
                        DEFAULT_CERT_FINGERPRINT,
                        mockAssertionOptions.relyingPartyId!!,
                        "https://${mockAssertionOptions.relyingPartyId}",
                    ),
                ),
                requestCaptureSlot.captured.origin,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFido2Credential should return Error when origin is null`() = runTest {
        val mockAssertionRequest = createMockFido2AssertionRequest(
            mockOrigin = null,
            mockClientDataHash = null,
            mockSigningInfo = mockSigningInfo,
        )

        val mockSelectedCipher = createMockCipherView(number = 1)

        every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
        every {
            json.decodeFromString<PasskeyAssertionOptions>(any())
        } throws SerializationException()

        val result = fido2CredentialManager.authenticateFido2Credential(
            userId = "activeUserId",
            request = mockAssertionRequest,
            selectedCipherView = mockSelectedCipher,
        )

        coVerify(exactly = 0) {
            mockVaultSdkSource.authenticateFido2Credential(
                request = any(),
                fido2CredentialStore = any(),
            )
        }

        assertEquals(
            Fido2CredentialAssertionResult.Error,
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFidoCredential should convert SDK response to AndroidFido2PublicKeyCredential, deserialize the response to JSON, and return Success with response JSON`() =
        runTest {
            mockkStatic(
                PublicKeyCredentialAuthenticatorAssertionResponse::toAndroidFido2PublicKeyCredential,
            )
            every { Base64.encodeToString(any(), any()) } returns ""
            val mockRequest = createMockFido2AssertionRequest(mockSigningInfo = mockSigningInfo)
            val mockPublicKeyCredential = mockk<Fido2PublicKeyCredential>()
            val mockSdkResponse =
                mockk<PublicKeyCredentialAuthenticatorAssertionResponse>(relaxed = true) {
                    every { toAndroidFido2PublicKeyCredential() } returns mockPublicKeyCredential
                }
            coEvery {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
            } returns mockSdkResponse.asSuccess()
            every { json.encodeToString(mockPublicKeyCredential) } returns "mockResponseJson"

            val authResult = fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = mockRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            coVerify {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
                mockSdkResponse.toAndroidFido2PublicKeyCredential()
                json.encodeToString(mockPublicKeyCredential)
            }

            assertEquals(
                Fido2CredentialAssertionResult.Success("mockResponseJson"),
                authResult,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `authenticateFido2Credential should return Error when response cannot be serialized`() =
        runTest {
            mockkStatic(PublicKeyCredentialAuthenticatorAssertionResponse::toAndroidFido2PublicKeyCredential)
            every { Base64.encodeToString(any(), any()) } returns ""
            val mockRequest = createMockFido2AssertionRequest(mockSigningInfo = mockSigningInfo)
            val requestCaptureSlot = slot<AuthenticateFido2CredentialRequest>()
            val mockPublicKeyCredential = mockk<Fido2PublicKeyCredential>()
            val mockSdkResponse =
                mockk<PublicKeyCredentialAuthenticatorAssertionResponse>(relaxed = true) {
                    every { toAndroidFido2PublicKeyCredential() } returns mockPublicKeyCredential
                }
            coEvery {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } returns mockSdkResponse.asSuccess()
            every { json.encodeToString(mockPublicKeyCredential) } throws SerializationException()

            val authResult = fido2CredentialManager.authenticateFido2Credential(
                userId = "activeUserId",
                request = mockRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            coVerify {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
                mockSdkResponse.toAndroidFido2PublicKeyCredential()
                json.encodeToString(mockPublicKeyCredential)
            }

            assertEquals(
                Fido2CredentialAssertionResult.Error,
                authResult,
            )
        }

    @Test
    fun `authenticateFido2Credential should return Error when relyingPartyId is null`() = runTest {
        val mockAssertionRequest = createMockFido2AssertionRequest(
            mockSigningInfo = mockSigningInfo,
        )
        val mockSelectedCipherView = createMockCipherView(number = 1)

        every {
            json.decodeFromStringOrNull<PasskeyAssertionOptions>(DEFAULT_FIDO2_AUTH_REQUEST_JSON)
        } returns createMockPasskeyAssertionOptions(number = 1, relyingPartyId = null)

        val result = fido2CredentialManager.authenticateFido2Credential(
            userId = "activeUserId",
            request = mockAssertionRequest,
            selectedCipherView = mockSelectedCipherView,
        )

        coVerify(exactly = 0) {
            mockVaultSdkSource.authenticateFido2Credential(
                any(),
                any(),
            )
        }

        assertEquals(
            Fido2CredentialAssertionResult.Error,
            result,
        )
    }

    @Test
    fun `authenticateFido2Credential should return Error when validateOrigin is Error`() = runTest {
        val mockAssertionRequest = createMockFido2AssertionRequest(
            mockOrigin = null,
            mockSigningInfo = mockSigningInfo,
        )
        val mockSelectedCipherView = createMockCipherView(number = 1)

        coEvery {
            fido2OriginManager.validateOrigin(any(), any())
        } returns Fido2ValidateOriginResult.Error.Unknown

        val result = fido2CredentialManager.authenticateFido2Credential(
            userId = "activeUserId",
            request = mockAssertionRequest,
            selectedCipherView = mockSelectedCipherView,
        )

        assertEquals(
            Fido2CredentialAssertionResult.Error,
            result,
        )

        coVerify(exactly = 0) {
            mockVaultSdkSource.authenticateFido2Credential(any(), any())
        }
    }
}

private const val DEFAULT_PACKAGE_NAME = "com.x8bit.bitwarden"
private const val DEFAULT_APP_SIGNATURE = "0987654321ABCDEF"
private const val DEFAULT_CERT_FINGERPRINT = "30:39:38:37:36:35:34:33:32:31:41:42:43:44:45:46"
private const val DEFAULT_HOST = "bitwarden.com"
private val DEFAULT_ORIGIN = Origin.Android(
    UnverifiedAssetLink(
        packageName = DEFAULT_PACKAGE_NAME,
        sha256CertFingerprint = DEFAULT_CERT_FINGERPRINT,
        host = DEFAULT_HOST,
        assetLinkUrl = "bitwarden.com",
    ),
)
private const val DEFAULT_ALLOW_LIST = """
{
  "apps": [
    {
      "type": "android",
      "info": {
        "package_name": "com.x8bit.bitwarden",
        "signatures": [
          {
            "build": "release",
            "cert_fingerprint_sha256": "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83"
          },
          {
            "build": "userdebug",
            "cert_fingerprint_sha256": "19:75:B2:F1:71:77:BC:89:A5:DF:F3:1F:9E:64:A6:CA:E2:81:A5:3D:C1:D1:D5:9B:1D:14:7F:E1:C8:2A:FA:00"
          }
        ]
      }
    }
  ]
}
"""
private const val DEFAULT_FIDO2_AUTH_REQUEST_JSON = """
{
  "allowCredentials": [
    {
      "id": "mockCredentialId-1",
      "transports": [
        "internal"
      ],
      "type": "public-key"
    },
    {
      "id": "mockCredentialId-2",
      "transports": [
        "internal"
      ],
      "type": "public-key"
    }
  ],
  "challenge": "mockChallenge",
  "rpId": "bitwarden.com",
  "userVerification": "preferred"
}
"""

private fun createMockFido2AssertionRequest(
    mockOrigin: String? = "bitwarden.com",
    mockClientDataHash: ByteArray? = null,
    mockSigningInfo: SigningInfo,
) = mockk<Fido2CredentialAssertionRequest> {
    every { origin } returns mockOrigin
    every { requestJson } returns DEFAULT_FIDO2_AUTH_REQUEST_JSON
    every { clientDataHash } returns mockClientDataHash
    every { callingAppInfo } returns mockk {
        every { origin } returns mockOrigin
        every { packageName } returns DEFAULT_PACKAGE_NAME
        every { getOrigin(DEFAULT_ALLOW_LIST) } returns mockOrigin
        every { signingInfo } returns mockSigningInfo
        every { isOriginPopulated() } returns mockOrigin.isNullOrEmpty().not()
    }
}
