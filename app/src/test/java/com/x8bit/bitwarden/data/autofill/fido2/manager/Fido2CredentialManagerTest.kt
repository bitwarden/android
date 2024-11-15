package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.Context
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.graphics.drawable.Icon
import android.util.Base64
import androidx.core.graphics.drawable.IconCompat
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Origin
import com.bitwarden.fido.PublicKeyCredentialAuthenticatorAssertionResponse
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2AttestationResponse
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2PublicKeyCredential
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.platform.util.validatePrivilegedApp
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPublicKeyAssertionResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPublicKeyAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2Credential
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.ui.autofill.fido2.util.createFido2IconCompatFromIconDataOrDefault
import com.x8bit.bitwarden.ui.autofill.fido2.util.createFido2IconCompatFromResource
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAssertionOptions
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.createMockPasskeyAttestationOptions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val mutableCipherStateFlow = MutableStateFlow<DataState<List<CipherView>>>(
        DataState.Loaded(listOf(DEFAULT_CIPHER_VIEW)),
    )
    private val mockContext = mockk<Context> {
        every { getString(any()) } returns "mockString"
    }
    private val mockIntentManager = mockk<IntentManager>()
    private val mockVaultRepository = mockk<VaultRepository> {
        every { ciphersStateFlow } returns mutableCipherStateFlow
    }
    private val mockSettingsRepository = mockk<SettingsRepository> {
        every { isIconLoadingDisabled } returns true
    }
    private val mockEnvironmentRepository = mockk<EnvironmentRepository> {
        every { environment } returns Environment.Us
    }
    private val mockDispatcherManager = FakeDispatcherManager()
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
        every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
        every { hasMultipleSigners() } returns false
    }
    private val mockMessageDigest = mockk<MessageDigest> {
        every { digest(any()) } returns DEFAULT_APP_SIGNATURE.toByteArray()
    }
    private val mockVaultSdkSource = mockk<VaultSdkSource>()
    private val mockFido2CredentialStore = mockk<Fido2CredentialStore>()
    private val mockFido2OriginManager = mockk<Fido2OriginManager> {
        coEvery { validateOrigin(any(), any()) } returns Fido2ValidateOriginResult.Success
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(
            MessageDigest::class,
            Base64::class,
            IconCompat::class,
        )
        mockkConstructor(CallingAppInfo::class)

        every { MessageDigest.getInstance(any()) } returns mockMessageDigest
        every {
            anyConstructed<CallingAppInfo>().validatePrivilegedApp(DEFAULT_ALLOW_LIST)
        } returns Fido2ValidateOriginResult.Success
        every { anyConstructed<CallingAppInfo>().packageName } returns DEFAULT_PACKAGE_NAME
        every {
            anyConstructed<CallingAppInfo>().getOrigin(DEFAULT_ALLOW_LIST)
        } returns DEFAULT_PACKAGE_NAME

        fido2CredentialManager = Fido2CredentialManagerImpl(
            context = mockContext,
            vaultSdkSource = mockVaultSdkSource,
            fido2CredentialStore = mockFido2CredentialStore,
            vaultRepository = mockVaultRepository,
            settingsRepository = mockSettingsRepository,
            environmentRepository = mockEnvironmentRepository,
            intentManager = mockIntentManager,
            dispatcherManager = mockDispatcherManager,
            fido2OriginManager = mockFido2OriginManager,
            json = json,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(MessageDigest::class, Base64::class)
        unmockkStatic(
            PublicKeyCredentialAuthenticatorAssertionResponse::toAndroidFido2PublicKeyCredential,
        )
        unmockkConstructor(
            CallingAppInfo::class,
            PublicKeyCredentialEntry.Builder::class,
        )
    }

    @Test
    fun `getPasskeyAttestationOptionsOrNull should return passkey options when deserialized`() =
        runTest {
            val mockAttestationOptions = createMockPasskeyAttestationOptions(number = 1)
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns mockAttestationOptions

            assertEquals(
                mockAttestationOptions,
                fido2CredentialManager.getPasskeyAttestationOptionsOrNull(
                    requestJson = "",
                ),
            )
        }

    @Test
    fun `getPasskeyAttestationOptionsOrNull should return null when deserialization fails`() =
        runTest {
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } throws SerializationException()

            assertNull(fido2CredentialManager.getPasskeyAttestationOptionsOrNull(requestJson = ""))
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getPasskeyAttestationOptionsOrNull should return null when IllegalArgumentException is thrown`() {
        every {
            json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
        } throws IllegalArgumentException()

        assertNull(fido2CredentialManager.getPasskeyAttestationOptionsOrNull(requestJson = ""))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should construct ClientData DefaultWithCustomHash when callingAppInfo origin is populated`() =
        runTest {
            val mockCipherView = createMockCipherView(1)
            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)

            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers { createMockPublicKeyAttestationResponse(number = 1).asSuccess() }

            fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(
                    number = 1,
                    origin = "origin",
                    signingInfo = mockSigningInfo,
                ),
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
            val mockFido2Request = createMockFido2CreateCredentialRequest(
                number = 1,
                signingInfo = mockk<SigningInfo> {
                    every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
                    every { hasMultipleSigners() } returns false
                },
            )
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers { mockRegistrationResponse.asSuccess() }

            fido2CredentialManager.registerFido2Credential(
                userId = "mockUserId",
                fido2CreateCredentialRequest = mockFido2Request,
                selectedCipherView = createMockCipherView(1),
            )

            assertTrue(
                requestCaptureSlot.captured.clientData is ClientData.DefaultWithExtraData,
            )
        }

    @Test
    fun `registerFido2Credential should wrap request in webauthn json object`() =
        runTest {
            val mockFido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            val mockCipherView = createMockCipherView(1)
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers { mockRegistrationResponse.asSuccess() }

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
            val mockFido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            val mockCipherView = createMockCipherView(1)
            val mockRegistrationResponse = createMockPublicKeyAttestationResponse(number = 1)

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every { json.encodeToString<Fido2AttestationResponse>(any(), any()) } returns ""
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)
            val requestCaptureSlot = slot<RegisterFido2CredentialRequest>()
            coEvery {
                mockVaultSdkSource.registerFido2Credential(
                    request = capture(requestCaptureSlot),
                    fido2CredentialStore = any(),
                )
            } coAnswers { mockRegistrationResponse.asSuccess() }

            fido2CredentialManager.registerFido2Credential(
                userId = "activeUserId",
                fido2CreateCredentialRequest = mockFido2CreateCredentialRequest,
                selectedCipherView = mockCipherView,
            )

            assertEquals(
                "activeUserId",
                requestCaptureSlot.captured.userId,
            )

            assertNotEquals(
                "activeUserId",
                mockFido2CreateCredentialRequest.userId,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `registerFido2Credential should return Error when getAppSigningSignatureFingerprint is null`() =
        runTest {
            val mockFido2CredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            every { mockSigningInfo.hasMultipleSigners() } returns true
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)

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
            val mockFido2CredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                signingInfo = mockSigningInfo,
            )
            every { mockSigningInfo.hasMultipleSigners() } returns true
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)

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
            val mockFido2CredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = "illegal empty spaces",
                signingInfo = mockSigningInfo,
            )
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns createMockPasskeyAttestationOptions(number = 1)

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
            val mockFido2CredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = "origin",
                signingInfo = mockSigningInfo,
            )
            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns null

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
    fun `registerFido2Credential should return Error when asset link url is null`() = runTest {
        every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
        every {
            json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
        } returns null

        val result = fido2CredentialManager.registerFido2Credential(
            userId = "mockUserId",
            fido2CreateCredentialRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = null,
                signingInfo = mockSigningInfo,
            ),
            selectedCipherView = createMockCipherView(number = 1),
        )
        assertEquals(
            Fido2RegisterCredentialResult.Error,
            result,
        )
    }

    @Test
    fun `registerFido2Credential should return Error when validateOriginResult is Error`() =
        runTest {
            val mockAssertionRequest = createMockFido2CreateCredentialRequest(
                number = 1,
                origin = null,
                signingInfo = mockSigningInfo,
            )
            val mockOptions = createMockPasskeyAttestationOptions(number = 1)
            every {
                json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
            } returns mockOptions
            coEvery {
                mockFido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.Unknown

            val result = fido2CredentialManager.registerFido2Credential(
                userId = "activeUserId",
                fido2CreateCredentialRequest = mockAssertionRequest,
                selectedCipherView = createMockCipherView(number = 1),
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

            fido2CredentialManager.authenticateFido2Credential(request = mockRequest)

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

            fido2CredentialManager.authenticateFido2Credential(request = mockRequest)

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
            val requestCaptureSlot = slot<AuthenticateFido2CredentialRequest>()

            every { Base64.encodeToString(any(), any()) } returns DEFAULT_APP_SIGNATURE
            coEvery {
                mockVaultSdkSource.authenticateFido2Credential(
                    request = any(),
                    fido2CredentialStore = any(),
                )
            } returns createMockPublicKeyAssertionResponse(number = 1).asSuccess()

            fido2CredentialManager.authenticateFido2Credential(request = mockAssertionRequest)

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
        every {
            json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
        } throws SerializationException()
        coEvery {
            mockVaultSdkSource.authenticateFido2Credential(
                request = any(),
                fido2CredentialStore = any(),
            )
        } returns createMockPublicKeyAssertionResponse(number = 1).asSuccess()

        val result =
            fido2CredentialManager.authenticateFido2Credential(request = mockAssertionRequest)

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

            val authResult =
                fido2CredentialManager.authenticateFido2Credential(request = mockRequest)

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

            val authResult =
                fido2CredentialManager.authenticateFido2Credential(request = mockRequest)

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

        every {
            json.decodeFromStringOrNull<PasskeyAssertionOptions>(DEFAULT_FIDO2_AUTH_REQUEST_JSON)
        } returns createMockPasskeyAssertionOptions(number = 1, relyingPartyId = null)

        val result =
            fido2CredentialManager.authenticateFido2Credential(request = mockAssertionRequest)

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
        coEvery {
            mockFido2OriginManager.validateOrigin(any(), any())
        } returns Fido2ValidateOriginResult.Error.Unknown

        val result = fido2CredentialManager.authenticateFido2Credential(
            request = createMockFido2AssertionRequest(
                mockSigningInfo = mockSigningInfo,
            ),
        )

        assertEquals(
            Fido2CredentialAssertionResult.Error,
            result,
        )

        coVerify(exactly = 0) {
            mockVaultSdkSource.authenticateFido2Credential(any(), any())
        }
    }

    @Test
    fun `authenticateFido2Credential should return Error when cipher list is empty`() =
        runTest {
            mutableCipherStateFlow.value = DataState.Loaded(emptyList())
            assertEquals(
                fido2CredentialManager.authenticateFido2Credential(
                    createMockFido2AssertionRequest(mockSigningInfo = mockSigningInfo),
                ),
                Fido2CredentialAssertionResult.Error,
            )
        }

    @Test
    fun `authenticateFido2Credential should return Error when selected cipher is not in vault`() =
        runTest {
            mutableCipherStateFlow.value = DataState.Loaded(
                listOf(
                    createMockCipherView(number = 2),
                ),
            )

            assertEquals(
                fido2CredentialManager.authenticateFido2Credential(
                    createMockFido2AssertionRequest(mockSigningInfo = mockSigningInfo),
                ),
                Fido2CredentialAssertionResult.Error,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CredentialsForRelyingParty should return Success with correct credential entries`() =
        runTest {
            mockkConstructor(
                PublicKeyCredentialEntry.Builder::class,
                BeginGetPublicKeyCredentialOption::class,
            )
            val mockRequest = createMockFido2GetCredentialsRequest(
                number = 1,
                signingInfo = mockSigningInfo,
                requestJson = DEFAULT_FIDO2_AUTH_REQUEST_JSON,
            )
            val mockAssertionOptions = createMockPasskeyAssertionOptions(number = 1)
            val mockFido2Credential = createMockSdkFido2Credential(
                number = 1,
                rpId = mockAssertionOptions.relyingPartyId!!,
            )
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = listOf(mockFido2Credential),
            )
            val cipherViews = listOf(mockCipherView)
            val mockFido2AutofillView = createMockFido2CredentialAutofillView(
                number = 1,
                cipherId = "mockId-1",
                rpId = mockAssertionOptions.relyingPartyId,
            )
            val mockIcon = mockk<Icon>()
            val mockIconCompat = mockk<IconCompat> {
                every { toIcon(mockContext) } returns mockIcon
            }
            every { Base64.encodeToString(any(), any()) } returns ""
            every {
                json.decodeFromStringOrNull<PasskeyAssertionOptions>(
                    DEFAULT_FIDO2_AUTH_REQUEST_JSON,
                )
            } returns mockAssertionOptions
            coEvery {
                mockFido2CredentialStore.findCredentials(
                    ids = any(),
                    ripId = mockAssertionOptions.relyingPartyId,
                )
            } returns cipherViews
            coEvery {
                mockVaultRepository.getDecryptedFido2CredentialAutofillViews(
                    cipherViewList = cipherViews,
                )
            } returns DecryptFido2CredentialAutofillViewResult.Success(
                fido2CredentialAutofillViews = listOf(mockFido2AutofillView),
            )
            every {
                mockIntentManager.createFido2GetCredentialPendingIntent(
                    action = "com.x8bit.bitwarden.fido2.ACTION_GET_PASSKEY",
                    userId = "mockUserId-1",
                    credentialId = mockFido2AutofillView.credentialId.toString(),
                    cipherId = mockFido2AutofillView.cipherId,
                    requestCode = any(Int::class),
                )
            } returns mockk()
            every {
                IconCompat.createWithResource(mockContext, R.drawable.ic_bw_passkey)
            } returns mockIconCompat
            coEvery {
                mockContext.createFido2IconCompatFromIconDataOrDefault(
                    iconData = IconData.Local(R.drawable.ic_bw_passkey),
                    defaultResourceId = R.drawable.ic_bw_passkey,
                )
            } returns mockIconCompat
            every {
                mockContext.createFido2IconCompatFromResource(
                    resourceId = R.drawable.ic_bw_passkey,
                )
            } returns mockIconCompat
            val mockPublicKeyCredentialEntry = mockk<PublicKeyCredentialEntry>()
            every {
                anyConstructed<PublicKeyCredentialEntry.Builder>().build()
            } returns mockPublicKeyCredentialEntry

            val expected = Fido2GetCredentialsResult.Success(
                userId = mockRequest.userId,
                options = mockRequest.option,
                credentialEntries = listOf(
                    mockPublicKeyCredentialEntry,
                ),
            )
            val result = fido2CredentialManager.getFido2CredentialsForRelyingParty(
                fido2GetCredentialsRequest = mockRequest,
            ) as? Fido2GetCredentialsResult.Success

            assertEquals(
                expected.credentialEntries,
                result?.credentialEntries,
            )

            assertEquals(
                expected.userId,
                result?.userId,
            )

            // Verify contents of options explicitly to avoid comparing object references.
            assertEquals(
                expected.options.requestJson,
                result?.options?.requestJson,
            )
            assertEquals(
                expected.options.id,
                result?.options?.id,
            )
            assertEquals(
                expected.options.type,
                result?.options?.type,
            )
            assertEquals(
                expected.options.clientDataHash,
                result?.options?.clientDataHash,
            )
            assertEquals(
                expected.options.candidateQueryData,
                result?.options?.candidateQueryData,
            )
        }

    @Test
    fun `getFido2CredentialsForRelyingParty should return Error when request options are null`() =
        runTest {
            every { json.decodeFromStringOrNull<PasskeyAssertionOptions>(any()) } returns null
            assertEquals(
                Fido2GetCredentialsResult.Error,
                fido2CredentialManager.getFido2CredentialsForRelyingParty(
                    createMockFido2GetCredentialsRequest(number = 1),
                ),
            )
        }

    @Test
    fun `getFido2CredentialsForRelyingParty should return Error when relyingPartyId is null`() =
        runTest {
            every {
                json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
            } returns createMockPasskeyAssertionOptions(number = 1, relyingPartyId = null)

            assertEquals(
                Fido2GetCredentialsResult.Error,
                fido2CredentialManager.getFido2CredentialsForRelyingParty(
                    createMockFido2GetCredentialsRequest(number = 1),
                ),
            )
        }

    @Test
    fun `getFido2CredentialsForRelyingParty should return Error when validateOrigin is Error`() =
        runTest {
            val mockRequest = createMockFido2GetCredentialsRequest(number = 1, origin = null)
            every {
                json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
            } returns createMockPasskeyAssertionOptions(number = 1)
            coEvery {
                mockFido2OriginManager.validateOrigin(any(), any())
            } returns Fido2ValidateOriginResult.Error.Unknown

            assertEquals(
                Fido2GetCredentialsResult.Error,
                fido2CredentialManager.getFido2CredentialsForRelyingParty(mockRequest),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CredentialsForRelyingParty should return Error when DecryptAutofillViewResult is Error`() =
        runTest {
            val mockRequest = createMockFido2GetCredentialsRequest(
                number = 1,
                signingInfo = mockSigningInfo,
                requestJson = DEFAULT_FIDO2_AUTH_REQUEST_JSON,
            )
            val mockAssertionOptions = createMockPasskeyAssertionOptions(number = 1)
            val mockFido2Credential = createMockSdkFido2Credential(
                number = 1,
                rpId = mockAssertionOptions.relyingPartyId!!,
            )
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = listOf(mockFido2Credential),
            )
            val cipherViews = listOf(mockCipherView)
            every {
                json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
            } returns createMockPasskeyAssertionOptions(number = 1)
            coEvery {
                mockFido2CredentialStore.findCredentials(
                    ids = any(),
                    ripId = mockAssertionOptions.relyingPartyId,
                )
            } returns cipherViews
            coEvery {
                mockVaultRepository.getDecryptedFido2CredentialAutofillViews(cipherViews)
            } returns DecryptFido2CredentialAutofillViewResult.Error

            assertEquals(
                Fido2GetCredentialsResult.Error,
                fido2CredentialManager.getFido2CredentialsForRelyingParty(mockRequest),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CredentialsForRelyingParty should return Error when decryption result contains unexpected entries`() =
        runTest {
            val mockRequest = createMockFido2GetCredentialsRequest(
                number = 1,
                signingInfo = mockSigningInfo,
                requestJson = DEFAULT_FIDO2_AUTH_REQUEST_JSON,
            )
            val mockAssertionOptions = createMockPasskeyAssertionOptions(number = 1)
            val mockFido2Credential = createMockSdkFido2Credential(
                number = 1,
                rpId = mockAssertionOptions.relyingPartyId!!,
            )
            val mockCipherView = createMockCipherView(
                number = 1,
                fido2Credentials = listOf(mockFido2Credential),
            )
            val cipherViews = listOf(mockCipherView)
            every {
                json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
            } returns createMockPasskeyAssertionOptions(number = 1)
            coEvery {
                mockFido2CredentialStore.findCredentials(
                    ids = any(),
                    ripId = mockAssertionOptions.relyingPartyId,
                )
            } returns cipherViews
            coEvery {
                mockVaultRepository.getDecryptedFido2CredentialAutofillViews(cipherViews)
            } returns DecryptFido2CredentialAutofillViewResult.Success(
                fido2CredentialAutofillViews = listOf(
                    createMockFido2CredentialAutofillView(number = 1),
                    createMockFido2CredentialAutofillView(number = 2),
                ),
            )

            assertEquals(
                Fido2GetCredentialsResult.Error,
                fido2CredentialManager.getFido2CredentialsForRelyingParty(mockRequest),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getUserVerificationRequirementForAssertion should return UserVerificationRequirement when options are not null`() {
        val mockAssertionOptions = createMockPasskeyAssertionOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        every {
            json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
        } returns mockAssertionOptions
        assertEquals(
            UserVerificationRequirement.PREFERRED,
            fido2CredentialManager.getUserVerificationRequirementForAssertion(
                createMockFido2CredentialAssertionRequest(number = 1),
                UserVerificationRequirement.REQUIRED,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getUserVerificationRequirementForAssertion should return fallback requirement when options are null`() {
        every {
            json.decodeFromStringOrNull<PasskeyAssertionOptions>(any())
        } returns null
        assertEquals(
            UserVerificationRequirement.REQUIRED,
            fido2CredentialManager.getUserVerificationRequirementForAssertion(
                createMockFido2CredentialAssertionRequest(number = 1),
                UserVerificationRequirement.REQUIRED,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getUserVerificationRequirementForRegistration should return UserVerificationRequirement when options are not null`() {
        val mockAttestationOptions = createMockPasskeyAttestationOptions(
            number = 1,
            userVerificationRequirement = UserVerificationRequirement.PREFERRED,
        )
        every {
            json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
        } returns mockAttestationOptions

        assertEquals(
            UserVerificationRequirement.PREFERRED,
            fido2CredentialManager.getUserVerificationRequirementForRegistration(
                createMockFido2CreateCredentialRequest(number = 1),
                UserVerificationRequirement.REQUIRED,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getUserVerificationRequirementForRegistration should return fallback requirement when options are null`() {
        every {
            json.decodeFromStringOrNull<PasskeyAttestationOptions>(any())
        } returns null
        assertEquals(
            UserVerificationRequirement.REQUIRED,
            fido2CredentialManager.getUserVerificationRequirementForRegistration(
                createMockFido2CreateCredentialRequest(number = 1),
                UserVerificationRequirement.REQUIRED,
            ),
        )
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
private val DEFAULT_CIPHER_VIEW = createMockCipherView(number = 1)
private fun createMockFido2AssertionRequest(
    mockOrigin: String? = "bitwarden.com",
    mockClientDataHash: ByteArray? = null,
    mockSigningInfo: SigningInfo,
) = mockk<Fido2CredentialAssertionRequest> {
    every { cipherId } returns DEFAULT_CIPHER_VIEW.id
    every { userId } returns "activeUserId"
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
