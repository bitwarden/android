package com.x8bit.bitwarden.data.platform.util

import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.util.Base64
import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class CallingAppInfoExtensionsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(MessageDigest::class)
        mockkStatic(Base64::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(MessageDigest::class)
        unmockkStatic(Base64::class)
    }

    @Test
    fun `getFido2RpOrNull should return null when origin is populated with invalid URI`() {
        val mockCallingAppInfo = mockk<CallingAppInfo> {
            every { isOriginPopulated() } returns true
            every { origin } returns "invalidUri9685%^$^&(*"
        }

        assertNull(mockCallingAppInfo.getFido2RpIdOrNull())
    }

    @Test
    fun `getFido2RpOrNull should return origin when origin is populated`() {
        val mockCallingAppInfo = mockk<CallingAppInfo> {
            every { isOriginPopulated() } returns true
            every { origin } returns "mockUri"
        }

        assertEquals("mockUri", mockCallingAppInfo.getFido2RpIdOrNull())
    }

    @Test
    fun `getFido2RpOrNull should return null when origin is null`() {
        val mockCallingAppInfo = mockk<CallingAppInfo> {
            every { isOriginPopulated() } returns true
            every { origin } returns null
        }

        assertNull(mockCallingAppInfo.getFido2RpIdOrNull())
    }

    @Test
    fun `getFido2RpOrNull should return package name when origin is not populated`() {
        val mockCallingAppInfo = mockk<CallingAppInfo> {
            every { isOriginPopulated() } returns false
            every { packageName } returns "mockPackageName"
        }

        assertEquals("mockPackageName", mockCallingAppInfo.getFido2RpIdOrNull())
    }

    @Test
    fun `getCallingAppApkFingerprint should return key hash`() {
        val mockMessageDigest = mockk<MessageDigest> {
            every { digest(any()) } returns DEFAULT_SIGNATURE.toByteArray()
        }
        every { MessageDigest.getInstance(any()) } returns mockMessageDigest
        every { Base64.encodeToString(any(), any()) } returns DEFAULT_SIGNATURE

        val mockSigningInfo = mockk<SigningInfo> {
            every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_SIGNATURE))
            every { hasMultipleSigners() } returns false
        }
        val appInfo = mockk<CallingAppInfo> {
            every { packageName } returns "packageName"
            every { signingInfo } returns mockSigningInfo
            every { origin } returns null
        }
        assertEquals(
            DEFAULT_SIGNATURE_HASH,
            appInfo.getSignatureFingerprintAsHexString(),
        )
    }

    @Test
    fun `getCallingAppApkFingerprint should return null when app has multiple signers`() {
        val mockMessageDigest = mockk<MessageDigest> {
            every { digest(any()) } returns DEFAULT_SIGNATURE.toByteArray()
        }
        every { MessageDigest.getInstance(any()) } returns mockMessageDigest
        every { Base64.encodeToString(any(), any()) } returns DEFAULT_SIGNATURE

        val mockSigningInfo = mockk<SigningInfo> {
            every { hasMultipleSigners() } returns true
        }
        val appInfo = mockk<CallingAppInfo> {
            every { packageName } returns "packageName"
            every { signingInfo } returns mockSigningInfo
            every { origin } returns null
        }
        assertNull(appInfo.getSignatureFingerprintAsHexString())
    }

    @Test
    fun `validatePrivilegedApp should return Success when privileged app is allowed`() {
        val mockAppInfo = mockk<CallingAppInfo> {
            every { getOrigin(any()) } returns "origin"
            every { packageName } returns "com.x8bit.bitwarden"
        }

        assertEquals(
            Fido2ValidateOriginResult.Success("origin"),
            mockAppInfo.validatePrivilegedApp(
                allowList = DEFAULT_ALLOW_LIST,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePrivilegedApp should return PasskeyNotSupportedForApp when allow list is invalid`() {
        val appInfo = mockk<CallingAppInfo> {
            every { packageName } returns "com.x8bit.bitwarden"
            every { getOrigin(any()) } throws IllegalArgumentException()
        }

        assertEquals(
            Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp,
            appInfo.validatePrivilegedApp(
                allowList = INVALID_ALLOW_LIST,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePrivilegedApp should return PrivilegedAppSignatureNotFound when IllegalStateException is thrown`() {
        val appInfo = mockk<CallingAppInfo> {
            every { packageName } returns "com.x8bit.bitwarden"
            every { getOrigin(any()) } throws IllegalStateException()
        }

        assertEquals(
            Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound,
            appInfo.validatePrivilegedApp(
                allowList = INVALID_ALLOW_LIST,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePrivilegedApp should return PrivilegedAppNotAllowed when calling app is not present in allow list`() {
        val appInfo = mockk<CallingAppInfo> {
            every { packageName } returns "packageName"
            every { origin } returns "origin"
        }

        assertEquals(
            Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed,
            appInfo.validatePrivilegedApp(
                allowList = DEFAULT_ALLOW_LIST,
            ),
        )
    }

    @Test
    fun `validatePrivilegedApp should return PasskeyNotSupportedForApp when getOrigin is null`() {
        val appInfo = mockk<CallingAppInfo> {
            every { getOrigin(any()) } returns null
            every { packageName } returns "com.x8bit.bitwarden"
        }

        assertEquals(
            Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp,
            appInfo.validatePrivilegedApp(DEFAULT_ALLOW_LIST),
        )
    }

    @Test
    fun `getAppOrigin should return apk key hash as origin`() {
        val mockMessageDigest = mockk<MessageDigest> {
            every { digest(any()) } returns DEFAULT_SIGNATURE.toByteArray()
        }
        every { MessageDigest.getInstance(any()) } returns mockMessageDigest
        every { Base64.encodeToString(any(), any()) } returns DEFAULT_SIGNATURE
        val mockSigningInfo = mockk<SigningInfo> {
            every { apkContentsSigners } returns arrayOf(Signature(DEFAULT_SIGNATURE))
            every { hasMultipleSigners() } returns false
        }
        val appInfo = mockk<CallingAppInfo> {
            every { signingInfo } returns mockSigningInfo
        }

        assertEquals(
            "android:apk-key-hash:$DEFAULT_SIGNATURE",
            appInfo.getAppOrigin(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getAppSigningSignatureFingerprint should return null when calling app has multiple signers`() {
        val mockAppInfo = mockk<CallingAppInfo> {
            every { signingInfo } returns mockk {
                every { hasMultipleSigners() } returns true
            }
        }

        assertNull(mockAppInfo.getAppSigningSignatureFingerprint())
    }
}

private const val DEFAULT_SIGNATURE = "0987654321ABCDEF"
private const val DEFAULT_SIGNATURE_HASH = "30:39:38:37:36:35:34:33:32:31:41:42:43:44:45:46"
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
private const val INVALID_ALLOW_LIST = """
  "apps": [
    {
      "type": "android",
      "info": {
        "package_name": "com.x8bit.bitwarden",
        "signatures": [
          {
            "cert_fingerprint_sha256": "F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:DF:91:04:80:AD:6B:2D:60:DB:83"
          },
          {
            "cert_fingerprint_sha256": "19:75:B2:F1:71:77:BC:89:A5:DF:F3:1F:9E:64:A6:CA:E2:81:A5:3D:C1:D1:D5:9B:1D:14:7F:E1:C8:2A:FA:00"
          }
        ]
      }
    }
  ]
}
"""
