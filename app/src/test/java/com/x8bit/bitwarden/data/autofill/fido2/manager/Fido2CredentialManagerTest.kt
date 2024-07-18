package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.util.Base64
import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.fido.ClientData
import com.bitwarden.sdk.Fido2CredentialStore
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2AttestationResponse
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialRequest
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPublicKeyAttestationResponse
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
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class Fido2CredentialManagerTest {

    private lateinit var fido2CredentialManager: Fido2CredentialManager

    private val assetManager: AssetManager = mockk {
        coEvery { readAsset(any()) } returns DEFAULT_ALLOW_LIST.asSuccess()
    }
    private val digitalAssetLinkService = mockk<DigitalAssetLinkService> {
        coEvery {
            getDigitalAssetLinkForRp(relyingParty = any())
        } returns DEFAULT_STATEMENT_LIST.asSuccess()
    }
    private val json = mockk<Json> {
        every {
            decodeFromString<PasskeyAttestationOptions>(any())
        } returns createMockPasskeyAttestationOptions(number = 1)
        every {
            decodeFromString<PasskeyAssertionOptions>(any())
        } returns createMockPasskeyAssertionOptions(number = 1)
    }
    private val mockPrivilegedCallingAppInfo = mockk<CallingAppInfo> {
        every { packageName } returns "com.x8bit.bitwarden"
        every { isOriginPopulated() } returns true
        every { getOrigin(any()) } returns "com.x8bit.bitwarden"
    }
    private val mockPrivilegedAppRequest = mockk<Fido2CredentialRequest> {
        every { callingAppInfo } returns mockPrivilegedCallingAppInfo
    }
    private val mockSigningInfo = mockk<SigningInfo> {
        every { apkContentsSigners } returns arrayOf(Signature("0987654321ABCDEF"))
        every { hasMultipleSigners() } returns false
    }
    private val mockUnprivilegedCallingAppInfo = CallingAppInfo(
        packageName = "com.x8bit.bitwarden",
        signingInfo = mockSigningInfo,
        origin = null,
    )
    private val mockUnprivilegedAppRequest = mockk<Fido2CredentialRequest> {
        every { callingAppInfo } returns mockUnprivilegedCallingAppInfo
        every { requestJson } returns "{}"
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
            assetManager = assetManager,
            digitalAssetLinkService = digitalAssetLinkService,
            vaultSdkSource = mockVaultSdkSource,
            fido2CredentialStore = mockFido2CredentialStore,
            json = json,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(MessageDigest::class, Base64::class)
    }

    @Test
    fun `validateOrigin should load allow list when origin is populated`() =
        runTest {
            fido2CredentialManager.validateOrigin(mockPrivilegedAppRequest)

            coVerify(exactly = 1) {
                assetManager.readAsset(
                    fileName = "fido2_privileged_allow_list.json",
                )
            }
        }

    @Test
    fun `validateOrigin should return Success when privileged app is allowed`() =
        runTest {
            assertEquals(
                Fido2ValidateOriginResult.Success,
                fido2CredentialManager.validateOrigin(mockPrivilegedAppRequest),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return PrivilegedAppSignatureNotFound when privileged app signature is not found in allow list`() =
        runTest {
            every { mockPrivilegedCallingAppInfo.getOrigin(any()) } throws IllegalStateException()

            assertEquals(
                Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound,
                fido2CredentialManager.validateOrigin(mockPrivilegedAppRequest),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return PrivilegedAppNotAllowed when privileged app package name is not found in allow list`() =
        runTest {
            coEvery { assetManager.readAsset(any()) } returns MISSING_PACKAGE_ALLOW_LIST.asSuccess()

            assertEquals(
                Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed,
                fido2CredentialManager.validateOrigin(mockPrivilegedAppRequest),
            )
        }

    @Test
    fun `validateOrigin should return error when allow list is unreadable`() = runTest {
        coEvery { assetManager.readAsset(any()) } returns IllegalStateException().asFailure()

        assertEquals(
            Fido2ValidateOriginResult.Error.Unknown,
            fido2CredentialManager.validateOrigin(mockPrivilegedAppRequest),
        )
    }

    @Test
    fun `validateOrigin should return PasskeyNotSupportedForApp when allow list is invalid`() =
        runTest {
            every {
                mockPrivilegedCallingAppInfo.getOrigin(any())
            } throws IllegalArgumentException()

            assertEquals(
                Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp,
                fido2CredentialManager.validateOrigin(mockPrivilegedAppRequest),
            )
        }

    @Test
    fun `validateOrigin should return success when asset links contains matching statement`() =
        runTest {
            assertEquals(
                Fido2ValidateOriginResult.Success,
                fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
            )
        }

    @Test
    fun `validateOrigin should return error when request cannot be decoded`() = runTest {
        every {
            json.decodeFromString<PasskeyAttestationOptions>(any())
        } throws SerializationException()

        assertEquals(
            Fido2ValidateOriginResult.Error.AssetLinkNotFound,
            fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
        )
    }

    @Test
    fun `validateOrigin should return error when request cannot be cast to object type`() =
        runTest {
            every {
                json.decodeFromString<PasskeyAttestationOptions>(any())
            } throws IllegalArgumentException()

            assertEquals(
                Fido2ValidateOriginResult.Error.AssetLinkNotFound,
                fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
            )
        }

    @Test
    fun `validateOrigin should return error when asset links are unavailable`() = runTest {
        coEvery {
            digitalAssetLinkService.getDigitalAssetLinkForRp(relyingParty = any())
        } returns Throwable().asFailure()

        assertEquals(
            fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
            Fido2ValidateOriginResult.Error.AssetLinkNotFound,
        )
    }

    @Test
    fun `validateOrigin should return error when asset links does not contain package name`() =
        runTest {
            every { mockUnprivilegedAppRequest.callingAppInfo } returns CallingAppInfo(
                packageName = "its.a.trap",
                signingInfo = mockSigningInfo,
                origin = null,
            )
            assertEquals(
                Fido2ValidateOriginResult.Error.ApplicationNotFound,
                fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return error when asset links does not contain android app namespace`() =
        runTest {
            coEvery {
                digitalAssetLinkService.getDigitalAssetLinkForRp(relyingParty = any())
            } returns listOf(
                DEFAULT_STATEMENT.copy(
                    target = DEFAULT_STATEMENT.target.copy(
                        namespace = "its_a_trap",
                    ),
                ),
            )
                .asSuccess()

            assertEquals(
                Fido2ValidateOriginResult.Error.ApplicationNotFound,
                fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
            )
        }

    @Test
    fun `validateOrigin should return error when asset links certificate hash no match`() =
        runTest {
            every {
                mockMessageDigest.digest(any())
            } returns "ITSATRAP".toByteArray()
            assertEquals(
                Fido2ValidateOriginResult.Error.ApplicationNotVerified,
                fido2CredentialManager.validateOrigin(mockUnprivilegedAppRequest),
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
                fido2CredentialRequest = mockFido2CreateCredentialRequest,
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
                fido2CredentialRequest = mockFido2Request,
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
                fido2CredentialRequest = mockFido2CreateCredentialRequest,
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
                fido2CredentialRequest = mockFido2CreateCredentialRequest,
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
                fido2CredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            assertTrue(
                result is Fido2RegisterCredentialResult.Error,
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
                fido2CredentialRequest = mockFido2CredentialRequest,
                selectedCipherView = createMockCipherView(number = 1),
            )

            assertTrue(
                result is Fido2RegisterCredentialResult.Error,
            )
        }
}

private const val DEFAULT_APP_SIGNATURE = "0987654321ABCDEF"
private const val DEFAULT_CERT_FINGERPRINT = "30:39:38:37:36:35:34:33:32:31:41:42:43:44:45:46"
private val DEFAULT_STATEMENT = DigitalAssetLinkResponseJson(
    relation = listOf(
        "delegate_permission/common.get_login_creds",
        "delegate_permission/common.handle_all_urls",
    ),
    target = DigitalAssetLinkResponseJson.Target(
        namespace = "android_app",
        packageName = "com.x8bit.bitwarden",
        sha256CertFingerprints = listOf(
            DEFAULT_CERT_FINGERPRINT,
        ),
    ),
)
private val DEFAULT_STATEMENT_LIST = listOf(DEFAULT_STATEMENT)
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
private const val MISSING_PACKAGE_ALLOW_LIST = """
{
  "apps": [
    {
      "type": "android",
      "info": {
        "package_name": "com.android.chrome",
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
