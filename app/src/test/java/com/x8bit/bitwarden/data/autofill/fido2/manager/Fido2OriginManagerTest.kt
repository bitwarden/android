package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.pm.Signature
import android.util.Base64
import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson
import com.bitwarden.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class Fido2OriginManagerTest {

    private val mockAssetManager = mockk<AssetManager>()
    private val mockDigitalAssetLinkService = mockk<DigitalAssetLinkService>()
    private val mockPrivilegedAppInfo = mockk<CallingAppInfo> {
        every { isOriginPopulated() } returns true
        every { packageName } returns DEFAULT_PACKAGE_NAME
        every { getOrigin(any()) } returns DEFAULT_ORIGIN
    }
    private val mockNonPrivilegedAppInfo = mockk<CallingAppInfo> {
        every { isOriginPopulated() } returns false
        every { packageName } returns DEFAULT_PACKAGE_NAME
        every { getOrigin(any()) } returns null
        every { signingInfo } returns mockk {
            every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_APP_SIGNATURE))
            every { hasMultipleSigners() } returns false
        }
    }
    private val mockMessageDigest = mockk<MessageDigest> {
        every { digest(any()) } returns DEFAULT_APP_SIGNATURE.toByteArray()
    }

    private val fido2OriginManager = Fido2OriginManagerImpl(
        assetManager = mockAssetManager,
        digitalAssetLinkService = mockDigitalAssetLinkService,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(
            MessageDigest::class,
            Base64::class,
        )
        every { MessageDigest.getInstance(any()) } returns mockMessageDigest
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            MessageDigest::class,
            Base64::class,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return Success when calling app is Privileged and is in Google allow list`() =
        runTest {
            coEvery {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
            } returns DEFAULT_ALLOW_LIST.asSuccess()

            val result = fido2OriginManager.validateOrigin(
                callingAppInfo = mockPrivilegedAppInfo,
            )
            coVerify(exactly = 1) {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
            }
            assertEquals(
                Fido2ValidateOriginResult.Success(DEFAULT_ORIGIN),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return Success when calling app is Privileged and is in the Community allow list but not the Google allow list`() =
        runTest {
            coEvery {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
            } returns FAIL_ALLOW_LIST.asSuccess()
            coEvery {
                mockAssetManager.readAsset(COMMUNITY_ALLOW_LIST_FILENAME)
            } returns DEFAULT_ALLOW_LIST.asSuccess()

            val result = fido2OriginManager.validateOrigin(
                callingAppInfo = mockPrivilegedAppInfo,
            )
            coVerify(exactly = 1) {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
                mockAssetManager.readAsset(COMMUNITY_ALLOW_LIST_FILENAME)
            }
            assertEquals(
                Fido2ValidateOriginResult.Success(DEFAULT_ORIGIN),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return ApplicationNotFound when calling app is Privileged but not in either allow list`() =
        runTest {
            coEvery {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
            } returns FAIL_ALLOW_LIST.asSuccess()
            coEvery {
                mockAssetManager.readAsset(COMMUNITY_ALLOW_LIST_FILENAME)
            } returns FAIL_ALLOW_LIST.asSuccess()

            val result = fido2OriginManager.validateOrigin(
                callingAppInfo = mockPrivilegedAppInfo,
            )

            coVerify(exactly = 1) {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
                mockAssetManager.readAsset(COMMUNITY_ALLOW_LIST_FILENAME)
            }
            assertEquals(
                Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return Success when calling app is NonPrivileged and has a valid asset link entry`() =
        runTest {
            coEvery {
                mockDigitalAssetLinkService.checkDigitalAssetLinksRelations(
                    packageName = DEFAULT_PACKAGE_NAME,
                    certificateFingerprint = DEFAULT_CERT_FINGERPRINT,
                    relation = "delegate_permission/common.handle_all_urls",
                )
            } returns DEFAULT_ASSET_LINKS_CHECK_RESPONSE.asSuccess()

            val result = fido2OriginManager.validateOrigin(
                callingAppInfo = mockNonPrivilegedAppInfo,
            )

            assertEquals(
                Fido2ValidateOriginResult.Success(null),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return PasskeysNotSupportedForApp when calling app is NonPrivileged but signature does not match asset link entry`() =
        runTest {
            coEvery {
                mockDigitalAssetLinkService.checkDigitalAssetLinksRelations(
                    packageName = DEFAULT_PACKAGE_NAME,
                    certificateFingerprint = DEFAULT_CERT_FINGERPRINT,
                    relation = "delegate_permission/common.handle_all_urls",
                )
            } returns DEFAULT_ASSET_LINKS_CHECK_RESPONSE
                .copy(linked = false)
                .asSuccess()

            assertEquals(
                Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp,
                fido2OriginManager.validateOrigin(callingAppInfo = mockNonPrivilegedAppInfo),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return AssetLinkNotFound when calling app is NonPrivileged and asset link does not exist`() =
        runTest {
            coEvery {
                mockDigitalAssetLinkService.checkDigitalAssetLinksRelations(
                    packageName = DEFAULT_PACKAGE_NAME,
                    certificateFingerprint = DEFAULT_CERT_FINGERPRINT,
                    relation = "delegate_permission/common.handle_all_urls",
                )
            } returns RuntimeException().asFailure()

            assertEquals(
                Fido2ValidateOriginResult.Error.AssetLinkNotFound,
                fido2OriginManager.validateOrigin(callingAppInfo = mockNonPrivilegedAppInfo),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateOrigin should return Unknown error when calling app is Privileged and allow list file read fails`() =
        runTest {
            coEvery {
                mockAssetManager.readAsset(GOOGLE_ALLOW_LIST_FILENAME)
            } returns IllegalStateException().asFailure()
            coEvery {
                mockAssetManager.readAsset(COMMUNITY_ALLOW_LIST_FILENAME)
            } returns IllegalStateException().asFailure()

            assertEquals(
                Fido2ValidateOriginResult.Error.Unknown,
                fido2OriginManager.validateOrigin(
                    callingAppInfo = mockPrivilegedAppInfo,
                ),
            )
        }
}

private const val DEFAULT_PACKAGE_NAME = "com.x8bit.bitwarden"
private const val DEFAULT_APP_SIGNATURE = "0987654321ABCDEF"
private const val DEFAULT_CERT_FINGERPRINT = "30:39:38:37:36:35:34:33:32:31:41:42:43:44:45:46"
private const val DEFAULT_ORIGIN = "bitwarden.com"
private const val GOOGLE_ALLOW_LIST_FILENAME = "fido2_privileged_google.json"
private const val COMMUNITY_ALLOW_LIST_FILENAME = "fido2_privileged_community.json"
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
private const val FAIL_ALLOW_LIST = """
{
  "apps": [
    {
      "type": "android",
      "info": {
        "package_name": "com.not.bitwarden",
        "signatures": [
          {
            "build": "release",
            "cert_fingerprint_sha256": "FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF"
          },
          {
            "build": "userdebug",
            "cert_fingerprint_sha256": "FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF"
          }
        ]
      }
    }
  ]
}
"""
private val DEFAULT_ASSET_LINKS_CHECK_RESPONSE =
    DigitalAssetLinkCheckResponseJson(
        linked = true,
        maxAge = "30s",
        debugString = null,
)
