package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserStateJsonExtensionsTest {
    @Test
    fun `toUpdatedUserStateJson should do nothing for a non-matching account`() {
        val originalUserState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf(
                "activeUserId" to mockk(),
            ),
        )
        assertEquals(
            originalUserState,
            originalUserState
                .toUpdatedUserStateJson(
                    syncResponse = mockk {
                        every { profile } returns mockk {
                            every { id } returns "otherUserId"
                        }
                    },
                ),
        )
    }

    @Test
    fun `toUpdatedUserStateJson should update the correct account with new information`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremium = true,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = mockk(),
            settings = mockk(),
        )
        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "avatarColor",
                            stamp = "securityStamp",
                        ),
                    ),
                ),
            ),
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount,
                ),
            )
                .toUpdatedUserStateJson(
                    syncResponse = mockk {
                        every { profile } returns mockk {
                            every { id } returns "activeUserId"
                            every { avatarColor } returns "avatarColor"
                            every { securityStamp } returns "securityStamp"
                            every { isPremium } returns true
                            every { isPremiumFromOrganization } returns true
                        }
                    },
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserStateJsonWithPassword should update active account to set hasMasterPassword and clear forcePasswordResetReason`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremium = true,
            forcePasswordResetReason = ForcePasswordResetReason
                .TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = mockk(),
            settings = mockk(),
        )
        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            forcePasswordResetReason = null,
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                keyConnectorUserDecryptionOptions = null,
                                trustedDeviceUserDecryptionOptions = null,
                            ),
                        ),
                    ),
                ),
            ),
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount,
                ),
            )
                .toUserStateJsonWithPassword(),
        )
    }

    @Test
    fun `toUserStateJsonWithPassword should preserve values of userDecryptionOptions`() {
        val keyConnectorOptionsJson = KeyConnectorUserDecryptionOptionsJson("key")
        val trustedDeviceOptionsJson = TrustedDeviceUserDecryptionOptionsJson(
            encryptedPrivateKey = "encryptedPrivateKey",
            encryptedUserKey = "encryptedUserKey",
            hasAdminApproval = true,
            hasLoginApprovingDevice = true,
            hasManageResetPasswordPermission = true,
        )
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremium = true,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = UserDecryptionOptionsJson(
                hasMasterPassword = true,
                keyConnectorUserDecryptionOptions = keyConnectorOptionsJson,
                trustedDeviceUserDecryptionOptions = trustedDeviceOptionsJson,
            ),
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = mockk(),
            settings = mockk(),
        )
        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                keyConnectorUserDecryptionOptions = keyConnectorOptionsJson,
                                trustedDeviceUserDecryptionOptions = trustedDeviceOptionsJson,
                            ),
                        ),
                    ),
                ),
            ),
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount,
                ),
            )
                .toUserStateJsonWithPassword(),
        )
    }

    @Test
    fun `toUserState should return the correct UserState for an unlocked vault`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        avatarColorHex = "activeAvatarColorHex",
                        environment = Environment.Eu,
                        isPremium = false,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.PIN,
                        needsMasterPassword = false,
                        trustedDevice = null,
                    ),
                ),
            ),
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to AccountJson(
                        profile = mockk {
                            every { userId } returns "activeUserId"
                            every { name } returns "activeName"
                            every { email } returns "activeEmail"
                            every { avatarColorHex } returns "activeAvatarColorHex"
                            every { hasPremium } returns null
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                            )
                        },
                        tokens = AccountTokensJson(
                            accessToken = "accessToken",
                            refreshToken = "refreshToken",
                        ),
                        settings = AccountJson.Settings(
                            environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
                        ),
                    ),
                ),
            )
                .toUserState(
                    vaultState = listOf(
                        VaultUnlockData(
                            userId = "activeUserId",
                            status = VaultUnlockData.Status.UNLOCKED,
                        ),
                    ),
                    userAccountTokens = listOf(
                        UserAccountTokens(
                            userId = "activeUserId",
                            accessToken = "accessToken",
                            refreshToken = "refreshToken",
                        ),
                    ),
                    userOrganizationsList = listOf(
                        UserOrganizations(
                            userId = "activeUserId",
                            organizations = listOf(
                                Organization(
                                    id = "organizationId",
                                    name = "organizationName",
                                ),
                            ),
                        ),
                    ),
                    hasPendingAccountAddition = false,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.PIN },
                    isDeviceTrustedProvider = { false },
                ),
        )
    }

    @Test
    fun `toUserState return the correct UserState for a locked vault`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        // This value is calculated from the userId
                        avatarColorHex = "#ffecbc49",
                        environment = Environment.Eu,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                        isBiometricsEnabled = true,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = null,
                    ),
                ),
                hasPendingAccountAddition = true,
            ),
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to AccountJson(
                        profile = mockk {
                            every { userId } returns "activeUserId"
                            every { name } returns "activeName"
                            every { email } returns "activeEmail"
                            every { avatarColorHex } returns null
                            every { hasPremium } returns true
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                            )
                        },
                        tokens = AccountTokensJson(
                            accessToken = null,
                            refreshToken = null,
                        ),
                        settings = AccountJson.Settings(
                            environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
                        ),
                    ),
                ),
            )
                .toUserState(
                    vaultState = emptyList(),
                    userAccountTokens = listOf(
                        UserAccountTokens(
                            userId = "activeUserId",
                            accessToken = null,
                            refreshToken = null,
                        ),
                    ),
                    userOrganizationsList = listOf(
                        UserOrganizations(
                            userId = "activeUserId",
                            organizations = listOf(
                                Organization(
                                    id = "organizationId",
                                    name = "organizationName",
                                ),
                            ),
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { true },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { false },
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should preserve values of trustedDeviceUserDecryptionOptions`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        // This value is calculated from the userId
                        avatarColorHex = "#ffecbc49",
                        environment = Environment.Eu,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = false,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasMasterPassword = false,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                    ),
                ),
                hasPendingAccountAddition = true,
            ),
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to AccountJson(
                        profile = mockk {
                            every { userId } returns "activeUserId"
                            every { name } returns "activeName"
                            every { email } returns "activeEmail"
                            every { avatarColorHex } returns null
                            every { hasPremium } returns true
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = TrustedDeviceUserDecryptionOptionsJson(
                                    encryptedPrivateKey = null,
                                    encryptedUserKey = null,
                                    hasAdminApproval = false,
                                    hasLoginApprovingDevice = true,
                                    hasManageResetPasswordPermission = false,
                                ),
                                keyConnectorUserDecryptionOptions = null,
                            )
                        },
                        tokens = null,
                        settings = AccountJson.Settings(
                            environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
                        ),
                    ),
                ),
            )
                .toUserState(
                    vaultState = emptyList(),
                    userAccountTokens = listOf(
                        UserAccountTokens(
                            userId = "activeUserId",
                            accessToken = null,
                            refreshToken = null,
                        ),
                    ),
                    userOrganizationsList = listOf(
                        UserOrganizations(
                            userId = "activeUserId",
                            organizations = listOf(
                                Organization(
                                    id = "organizationId",
                                    name = "organizationName",
                                ),
                            ),
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                ),
        )
    }
}
