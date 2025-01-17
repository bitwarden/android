package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@Suppress("LargeClass")
class UserStateJsonExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toUpdatedUserStateJson should do nothing for a non-matching account using toRemovedPasswordUserStateJson`() {
        val originalUserState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf("activeUserId" to mockk()),
        )
        assertEquals(
            originalUserState,
            originalUserState.toRemovedPasswordUserStateJson(userId = "nonActiveUserId"),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUpdatedUserStateJson should create user decryption options without a password if not present`() {
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
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = null,
            settings = AccountJson.Settings(environmentUrlData = null),
        )
        val originalUserState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf("activeUserId" to originalAccount),
        )

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                            ),
                        ),
                    ),
                ),
            ),
            originalUserState.toRemovedPasswordUserStateJson(userId = "activeUserId"),
        )
    }

    @Test
    fun `toUpdatedUserStateJson should update user decryption options to not have a password`() {
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
                trustedDeviceUserDecryptionOptions = null,
                keyConnectorUserDecryptionOptions = null,
            ),
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = null,
            settings = AccountJson.Settings(environmentUrlData = null),
        )
        val originalUserState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf("activeUserId" to originalAccount),
        )

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                            ),
                        ),
                    ),
                ),
            ),
            originalUserState.toRemovedPasswordUserStateJson(userId = "activeUserId"),
        )
    }

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
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
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
                            isTwoFactorEnabled = false,
                            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
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
                            every { isTwoFactorEnabled } returns false
                            every { creationDate } returns ZonedDateTime
                                .parse("2024-09-13T01:00:00.00Z")
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
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
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
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
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
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.PIN,
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.NOT_STARTED,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.ADMIN,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = false,
                        ),
                    ),
                    hasPendingAccountAddition = false,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.PIN },
                    isDeviceTrustedProvider = { false },
                    onboardingStatus = OnboardingStatus.NOT_STARTED,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = true,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = null,
                        hasMasterPassword = false,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.NOT_STARTED,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.ADMIN,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = null,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { true },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { false },
                    onboardingStatus = OnboardingStatus.NOT_STARTED,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.ADMIN,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = null,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should set the correct onboarding values result`() {
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
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.ADMIN,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should set the default value of onboarding to COMPLETE when passed value is null`() {
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
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.ADMIN,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = null,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should set true for needsMasterPassword for TDE user with permission through organization`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
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
                                // Key part of the result #1, this is true or the role is owner or
                                // admin
                                shouldManageResetPassword = true,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.USER,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        // Key part of the result #2, TDE options should exist
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        // Key part of the result #3, options should have false for
                        // hasMasterPassword
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                                    shouldManageResetPassword = true,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.USER,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = null,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Test
    fun `toUserState should set true for needsMasterPassword for SSO user with no key connector`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        avatarColorHex = "#ffecbc49",
                        environment = Environment.Eu,
                        isPremium = false,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = emptyList(),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = false,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                            every { hasPremium } returns false
                            every { forcePasswordResetReason } returns null
                            // The decryption options are what are determining the result
                            @Suppress("MaxLineLength")
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = KeyConnectorUserDecryptionOptionsJson(
                                    keyConnectorUrl = "keyConnectorUrl",
                                ),
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
                    userOrganizationsList = emptyList(),
                    userIsUsingKeyConnectorList = emptyList(),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = null,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Test
    fun `toUserState should set false for needsMasterPassword for SSO user with key connector`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        avatarColorHex = "#ffecbc49",
                        environment = Environment.Eu,
                        isPremium = false,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = emptyList(),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = null,
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                            every { hasPremium } returns false
                            every { forcePasswordResetReason } returns null
                            // The decryption options are what are determining the result
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
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
                    userOrganizationsList = emptyList(),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = null,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should set false for needsMasterPassword for SSO user with TDE but no permission via organization`() {
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        avatarColorHex = "#ffecbc49",
                        environment = Environment.Eu,
                        isPremium = false,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                                // Key part of the result #1, this is true or the role is owner or
                                // admin
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.USER,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = false,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                            every { hasPremium } returns false
                            every { forcePasswordResetReason } returns null
                            // The decryption options are what are determining the result
                            @Suppress("MaxLineLength")
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.USER,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = null,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should set the correct first time state values result`() {
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
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = false,
                                role = OrganizationType.ADMIN,
                                shouldUsersGetPremium = false,
                            ),
                        ),
                        isBiometricsEnabled = false,
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                        needsMasterPassword = true,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = true,
                            hasAdminApproval = false,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = true,
                        onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = false,
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
                                    shouldManageResetPassword = false,
                                    shouldUseKeyConnector = false,
                                    role = OrganizationType.ADMIN,
                                    shouldUsersGetPremium = false,
                                ),
                            ),
                        ),
                    ),
                    userIsUsingKeyConnectorList = listOf(
                        UserKeyConnectorState(
                            userId = "activeUserId",
                            isUsingKeyConnector = true,
                        ),
                    ),
                    hasPendingAccountAddition = true,
                    isBiometricsEnabledProvider = { false },
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                    isDeviceTrustedProvider = { true },
                    onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
                    firstTimeState = FirstTimeState(
                        showImportLoginsCard = false,
                    ),
                ),
        )
    }
}
