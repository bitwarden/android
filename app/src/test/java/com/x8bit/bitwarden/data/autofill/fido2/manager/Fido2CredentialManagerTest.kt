package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.pm.Signature
import android.content.pm.SigningInfo
import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.PublicKeyCredentialCreationOptions
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
    private val mockCreateOptions = mockk<PublicKeyCredentialCreationOptions> {
        every {
            relyingParty
        } returns PublicKeyCredentialCreationOptions.PublicKeyCredentialRpEntity(
            name = "mockRpName",
            id = "www.bitwarden.com",
        )
    }
    private val json = mockk<Json> {
        every {
            decodeFromString<PublicKeyCredentialCreationOptions>(any())
        } returns mockCreateOptions
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
        every { digest(any()) } returns "0987654321ABCDEF".toByteArray()
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(MessageDigest::class)
        every { MessageDigest.getInstance(any()) } returns mockMessageDigest

        fido2CredentialManager = Fido2CredentialManagerImpl(
            assetManager = assetManager,
            digitalAssetLinkService = digitalAssetLinkService,
            json = json,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(MessageDigest::class)
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
            json.decodeFromString<PublicKeyCredentialCreationOptions>(any())
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
                json.decodeFromString<PublicKeyCredentialCreationOptions>(any())
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
    fun `createCredentialForCipher should return error while not implemented`() {
        val result = fido2CredentialManager.createCredentialForCipher(
            credentialRequest = mockk(),
            cipherView = mockk(),
        )

        assertTrue(
            result is Fido2CreateCredentialResult.Error,
        )
    }
}

@Suppress("MaxLineLength")
private const val DEFAULT_CERT_FINGERPRINT =
    "30:39:38:37:36:35:34:33:32:31:41:42:43:44:45:46"
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
