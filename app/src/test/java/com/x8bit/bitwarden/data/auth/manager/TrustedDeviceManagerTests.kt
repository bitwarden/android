package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.crypto.TrustDeviceResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.manager.util.toUserStateJson
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
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

    @BeforeEach
    fun setup() {
        mockkStatic(TrustDeviceResponse::toUserStateJson)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(TrustDeviceResponse::toUserStateJson)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `trustThisDeviceIfNecessary when shouldTrustDevice false should return success with false`() =
        runTest {
            fakeAuthDiskSource.storeShouldTrustDevice(userId = USER_ID, shouldTrustDevice = false)

            val result = manager.trustThisDeviceIfNecessary(userId = USER_ID)

            assertEquals(false.asSuccess(), result)
            fakeAuthDiskSource.assertIsTdeLoginComplete(userId = USER_ID, isTdeLoginComplete = true)
            coVerify(exactly = 0) {
                vaultSdkSource.getTrustDevice(userId = USER_ID)
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
        fakeAuthDiskSource.storeShouldTrustDevice(userId = USER_ID, shouldTrustDevice = true)
        val error = Throwable("Fail")
        coEvery {
            vaultSdkSource.getTrustDevice(userId = USER_ID)
        } returns error.asFailure()

        val result = manager.trustThisDeviceIfNecessary(userId = USER_ID)

        assertEquals(error.asFailure(), result)
        fakeAuthDiskSource.assertShouldTrustDevice(userId = USER_ID, shouldTrustDevice = null)
        fakeAuthDiskSource.assertIsTdeLoginComplete(userId = USER_ID, isTdeLoginComplete = null)
        coVerify(exactly = 1) {
            vaultSdkSource.getTrustDevice(userId = USER_ID)
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
        fakeAuthDiskSource.storeShouldTrustDevice(userId = USER_ID, shouldTrustDevice = true)
        coEvery {
            vaultSdkSource.getTrustDevice(userId = USER_ID)
        } returns trustedDeviceResponse.asSuccess()
        coEvery {
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        } returns error.asFailure()

        val result = manager.trustThisDeviceIfNecessary(userId = USER_ID)

        assertEquals(error.asFailure(), result)
        fakeAuthDiskSource.assertShouldTrustDevice(userId = USER_ID, shouldTrustDevice = null)
        fakeAuthDiskSource.assertIsTdeLoginComplete(userId = USER_ID, isTdeLoginComplete = null)
        coVerify(exactly = 1) {
            vaultSdkSource.getTrustDevice(userId = USER_ID)
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
        fakeAuthDiskSource.userState = DEFAULT_USER_STATE
        fakeAuthDiskSource.storeShouldTrustDevice(userId = USER_ID, shouldTrustDevice = true)
        coEvery {
            vaultSdkSource.getTrustDevice(userId = USER_ID)
        } returns trustedDeviceResponse.asSuccess()
        coEvery {
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        } returns trustedDeviceKeysResponseJson.asSuccess()
        every {
            trustedDeviceResponse.toUserStateJson(
                userId = USER_ID,
                previousUserState = DEFAULT_USER_STATE,
            )
        } returns UPDATED_USER_STATE

        val result = manager.trustThisDeviceIfNecessary(userId = USER_ID)

        assertEquals(true.asSuccess(), result)
        fakeAuthDiskSource.assertDeviceKey(userId = USER_ID, deviceKey = deviceKey)
        fakeAuthDiskSource.assertShouldTrustDevice(userId = USER_ID, shouldTrustDevice = null)
        fakeAuthDiskSource.assertUserState(UPDATED_USER_STATE)
        fakeAuthDiskSource.assertIsTdeLoginComplete(userId = USER_ID, isTdeLoginComplete = true)
        coVerify(exactly = 1) {
            vaultSdkSource.getTrustDevice(userId = USER_ID)
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        }
    }

    @Test
    fun `trustThisDevice when success should return success with true`() = runTest {
        val deviceKey = "deviceKey"
        val protectedUserKey = "protectedUserKey"
        val protectedDevicePrivateKey = "protectedDevicePrivateKey"
        val protectedDevicePublicKey = "protectedDevicePublicKey"
        val trustDeviceResponse = TrustDeviceResponse(
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
        fakeAuthDiskSource.userState = DEFAULT_USER_STATE
        fakeAuthDiskSource.storeShouldTrustDevice(userId = USER_ID, shouldTrustDevice = true)
        coEvery {
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        } returns trustedDeviceKeysResponseJson.asSuccess()
        every {
            trustDeviceResponse.toUserStateJson(
                userId = USER_ID,
                previousUserState = DEFAULT_USER_STATE,
            )
        } returns UPDATED_USER_STATE

        val result = manager.trustThisDevice(
            userId = USER_ID,
            trustDeviceResponse = trustDeviceResponse,
        )

        assertEquals(Unit.asSuccess(), result)
        fakeAuthDiskSource.assertDeviceKey(userId = USER_ID, deviceKey = deviceKey)
        fakeAuthDiskSource.assertShouldTrustDevice(userId = USER_ID, shouldTrustDevice = null)
        fakeAuthDiskSource.assertUserState(UPDATED_USER_STATE)
        fakeAuthDiskSource.assertIsTdeLoginComplete(userId = USER_ID, isTdeLoginComplete = true)
        coVerify(exactly = 1) {
            devicesService.trustDevice(
                appId = "testUniqueAppId",
                encryptedUserKey = protectedUserKey,
                encryptedDevicePublicKey = protectedDevicePublicKey,
                encryptedDevicePrivateKey = protectedDevicePrivateKey,
            )
        }
    }
}

private const val USER_ID: String = "userId"

private val DEFAULT_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS = TrustedDeviceUserDecryptionOptionsJson(
    encryptedPrivateKey = null,
    encryptedUserKey = null,
    hasAdminApproval = false,
    hasLoginApprovingDevice = false,
    hasManageResetPasswordPermission = false,
)

private val UPDATED_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS = TrustedDeviceUserDecryptionOptionsJson(
    encryptedPrivateKey = "encryptedPrivateKey",
    encryptedUserKey = "encryptedUserKey",
    hasAdminApproval = false,
    hasLoginApprovingDevice = false,
    hasManageResetPasswordPermission = false,
)

private val DEFAULT_USER_DECRYPTION_OPTIONS: UserDecryptionOptionsJson = UserDecryptionOptionsJson(
    hasMasterPassword = false,
    trustedDeviceUserDecryptionOptions = DEFAULT_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS,
    keyConnectorUserDecryptionOptions = null,
)

private val UPDATED_USER_DECRYPTION_OPTIONS: UserDecryptionOptionsJson = UserDecryptionOptionsJson(
    hasMasterPassword = false,
    trustedDeviceUserDecryptionOptions = UPDATED_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS,
    keyConnectorUserDecryptionOptions = null,
)

private val DEFAULT_ACCOUNT = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID,
        email = "test@bitwarden.com",
        isEmailVerified = true,
        name = "Bitwarden Tester",
        hasPremium = false,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.ARGON2_ID,
        kdfIterations = 600000,
        kdfMemory = 16,
        kdfParallelism = 4,
        userDecryptionOptions = DEFAULT_USER_DECRYPTION_OPTIONS,
        isTwoFactorEnabled = false,
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val UPDATED_ACCOUNT = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID,
        email = "test@bitwarden.com",
        isEmailVerified = true,
        name = "Bitwarden Tester",
        hasPremium = false,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.ARGON2_ID,
        kdfIterations = 600000,
        kdfMemory = 16,
        kdfParallelism = 4,
        userDecryptionOptions = UPDATED_USER_DECRYPTION_OPTIONS,
        isTwoFactorEnabled = false,
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val DEFAULT_USER_STATE = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(USER_ID to DEFAULT_ACCOUNT),
)

private val UPDATED_USER_STATE = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(USER_ID to UPDATED_ACCOUNT),
)
