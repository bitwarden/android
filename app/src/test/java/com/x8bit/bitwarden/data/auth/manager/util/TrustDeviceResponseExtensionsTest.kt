package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.crypto.TrustDeviceResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TrustDeviceResponseExtensionsTest {
    @Test
    fun `toUserState updates the previous state`() {
        assertEquals(
            UPDATED_USER_STATE,
            DEFAULT_TRUST_DEVICE_RESPONSE.toUserStateJson(
                userId = USER_ID,
                previousUserState = DEFAULT_USER_STATE,
            ),
        )
    }
}

private const val USER_ID: String = "userId"
private const val USER_KEY: String = "protectedUserKey"
private const val PRIVATE_KEY: String = "protectedDevicePrivateKey"

private val DEFAULT_TRUST_DEVICE_RESPONSE: TrustDeviceResponse = TrustDeviceResponse(
    deviceKey = "deviceKey",
    protectedUserKey = USER_KEY,
    protectedDevicePrivateKey = PRIVATE_KEY,
    protectedDevicePublicKey = "protectedDevicePublicKey",
)

private val DEFAULT_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS = TrustedDeviceUserDecryptionOptionsJson(
    encryptedPrivateKey = null,
    encryptedUserKey = null,
    hasAdminApproval = false,
    hasLoginApprovingDevice = false,
    hasManageResetPasswordPermission = false,
)

private val UPDATED_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS = TrustedDeviceUserDecryptionOptionsJson(
    encryptedPrivateKey = PRIVATE_KEY,
    encryptedUserKey = USER_KEY,
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
