package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.crypto.TrustDeviceResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TrustedDeviceManagerTests {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val devicesService: DevicesService = mockk()

    private val manager: TrustedDeviceManager = TrustedDeviceManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        vaultSdkSource = vaultSdkSource,
        devicesService = devicesService,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `trustThisDeviceIfNecessary when shouldTrustDevice false should return success with false`() =
        runTest {
            val userId = "userId"
            fakeAuthDiskSource.shouldTrustDevice = false

            val result = manager.trustThisDeviceIfNecessary(userId = userId)

            assertEquals(false.asSuccess(), result)
            coVerify(exactly = 0) {
                vaultSdkSource.getTrustDevice(userId = userId)
                devicesService.trustDevice(
                    appId = any(),
                    encryptedUserKey = any(),
                    encryptedDevicePublicKey = any(),
                    encryptedDevicePrivateKey = any(),
                )
            }
        }

    @Test
    fun `trustThisDeviceIfNecessary when getTrustDevice fails should return failure`() = runTest {
        val userId = "userId"
        fakeAuthDiskSource.shouldTrustDevice = true
        val error = Throwable("Fail")
        coEvery {
            vaultSdkSource.getTrustDevice(userId = userId)
        } returns error.asFailure()

        val result = manager.trustThisDeviceIfNecessary(userId = userId)

        assertEquals(error.asFailure(), result)
        assertFalse(fakeAuthDiskSource.shouldTrustDevice)
        coVerify(exactly = 1) {
            vaultSdkSource.getTrustDevice(userId = userId)
        }
        coVerify(exactly = 0) {
            devicesService.trustDevice(
                appId = any(),
                encryptedUserKey = any(),
                encryptedDevicePublicKey = any(),
                encryptedDevicePrivateKey = any(),
            )
        }
    }

    @Test
    fun `trustThisDeviceIfNecessary when trustDevice fails should return failure`() = runTest {
        val userId = "userId"
        val deviceKey = "deviceKey"
        val protectedUserKey = "protectedUserKey"
        val protectedDevicePrivateKey = "protectedDevicePrivateKey"
        val protectedDevicePublicKey = "protectedDevicePublicKey"
        val trustedDeviceResponse = TrustDeviceResponse(
            deviceKey = deviceKey,
            protectedUserKey = protectedUserKey,
            protectedDevicePrivateKey = protectedDevicePrivateKey,
            protectedDevicePublicKey = protectedDevicePublicKey,
        )
        val error = Throwable("Fail")
        fakeAuthDiskSource.shouldTrustDevice = true
        coEvery {
            vaultSdkSource.getTrustDevice(userId = userId)
        } returns trustedDeviceResponse.asSuccess()
        coEvery {
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        } returns error.asFailure()

        val result = manager.trustThisDeviceIfNecessary(userId = userId)

        assertEquals(error.asFailure(), result)
        assertFalse(fakeAuthDiskSource.shouldTrustDevice)
        coVerify(exactly = 1) {
            vaultSdkSource.getTrustDevice(userId = userId)
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        }
    }

    @Test
    fun `trustThisDeviceIfNecessary when success should return success with true`() = runTest {
        val userId = "userId"
        val deviceKey = "deviceKey"
        val protectedUserKey = "protectedUserKey"
        val protectedDevicePrivateKey = "protectedDevicePrivateKey"
        val protectedDevicePublicKey = "protectedDevicePublicKey"
        val trustedDeviceResponse = TrustDeviceResponse(
            deviceKey = deviceKey,
            protectedUserKey = protectedUserKey,
            protectedDevicePrivateKey = protectedDevicePrivateKey,
            protectedDevicePublicKey = protectedDevicePublicKey,
        )
        val trustedDeviceKeysResponseJson = TrustedDeviceKeysResponseJson(
            id = "id",
            name = "name",
            identifier = "identifier",
            type = 0,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
        )
        fakeAuthDiskSource.shouldTrustDevice = true
        coEvery {
            vaultSdkSource.getTrustDevice(userId = userId)
        } returns trustedDeviceResponse.asSuccess()
        coEvery {
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        } returns trustedDeviceKeysResponseJson.asSuccess()

        val result = manager.trustThisDeviceIfNecessary(userId = userId)

        assertEquals(true.asSuccess(), result)
        fakeAuthDiskSource.assertDeviceKey(userId = userId, deviceKey = deviceKey)
        assertFalse(fakeAuthDiskSource.shouldTrustDevice)
        coVerify(exactly = 1) {
            vaultSdkSource.getTrustDevice(userId = userId)
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        }
    }
}
