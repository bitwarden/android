package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.KeyConnectorUserDecryptionOptionsJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.UserDecryptionJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.createMockPolicy
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
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
                                masterPasswordUnlock = null,
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
                masterPasswordUnlock = null,
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
                                masterPasswordUnlock = null,
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
                            every { userDecryption } returns null
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
                                masterPasswordUnlock = null,
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
                masterPasswordUnlock = null,
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
                                masterPasswordUnlock = null,
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ -> emptyList() },
                ),
        )
    }

    @Test
    fun `toUserState isExportable should be false if organization match policies`() {
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = false,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ ->
                        listOf(
                            createMockPolicy(
                                id = "policyId",
                                organizationId = "organizationId",
                                type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT,
                                data = null,
                                isEnabled = true,
                            ),
                        )
                    },
                ),
        )
    }

    @Test
    fun `toUserState isExportable should be true if policies is not enabled`() {
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
                                keyConnectorUrl = null,
                                userIsClaimedByOrganization = false,
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
                        isExportable = true,
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
                                masterPasswordUnlock = null,
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
                                    keyConnectorUrl = null,
                                    userIsClaimedByOrganization = false,
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
                    getUserPolicies = { _, _ ->
                        listOf(
                            createMockPolicy(
                                id = "policyId",
                                organizationId = "organizationId",
                                type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT,
                                data = null,
                                isEnabled = false,
                            ),
                        )
                    },
                ),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should create UserDecryptionOptionsJson when null and syncResponse has masterPasswordUnlock`() {
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

        val syncResponse = mockk<SyncResponseJson>(relaxed = true) {
            every { profile } returns mockk {
                every { id } returns "activeUserId"
                every { avatarColor } returns "avatarColor"
                every { securityStamp } returns "securityStamp"
                every { isPremium } returns false
                every { isPremiumFromOrganization } returns false
                every { isTwoFactorEnabled } returns true
                every { creationDate } returns ZonedDateTime.parse("2024-09-13T01:00:00.00Z")
            }
            every { userDecryption } returns UserDecryptionJson(
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
            )
        }

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "avatarColor",
                            stamp = "securityStamp",
                            hasPremium = false,
                            isTwoFactorEnabled = true,
                            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                            ),
                        ),
                    ),
                ),
            ),
            originalUserState.toUpdatedUserStateJson(syncResponse),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should update existing UserDecryptionOptionsJson with masterPasswordUnlock`() {
        val trustedDeviceOptions = TrustedDeviceUserDecryptionOptionsJson(
            encryptedPrivateKey = "encryptedPrivateKey",
            encryptedUserKey = "encryptedUserKey",
            hasAdminApproval = true,
            hasLoginApprovingDevice = false,
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
                trustedDeviceUserDecryptionOptions = trustedDeviceOptions,
                keyConnectorUserDecryptionOptions = null,
                masterPasswordUnlock = null,
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

        val syncResponse = mockk<SyncResponseJson> {
            every { profile } returns mockk {
                every { id } returns "activeUserId"
                every { avatarColor } returns "newAvatarColor"
                every { securityStamp } returns "newSecurityStamp"
                every { isPremium } returns true
                every { isPremiumFromOrganization } returns false
                every { isTwoFactorEnabled } returns true
                every { creationDate } returns ZonedDateTime.parse("2024-09-13T01:00:00.00Z")
            }
            every { userDecryption } returns UserDecryptionJson(
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
            )
        }

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "newAvatarColor",
                            stamp = "newSecurityStamp",
                            hasPremium = true,
                            isTwoFactorEnabled = true,
                            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = trustedDeviceOptions,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                            ),
                        ),
                    ),
                ),
            ),
            originalUserState.toUpdatedUserStateJson(syncResponse),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should update existing UserDecryptionOptionsJson when syncResponse has no userDecryption`() {
        val keyConnectorOptions = KeyConnectorUserDecryptionOptionsJson("keyConnectorUrl")
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
                keyConnectorUserDecryptionOptions = keyConnectorOptions,
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
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

        val syncResponse = mockk<SyncResponseJson> {
            every { profile } returns mockk {
                every { id } returns "activeUserId"
                every { avatarColor } returns "updatedAvatarColor"
                every { securityStamp } returns "updatedSecurityStamp"
                every { isPremium } returns false
                every { isPremiumFromOrganization } returns true
                every { isTwoFactorEnabled } returns false
                every { creationDate } returns ZonedDateTime.parse("2024-09-13T01:00:00.00Z")
            }
            every { userDecryption } returns null
        }

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "updatedAvatarColor",
                            stamp = "updatedSecurityStamp",
                            hasPremium = true,
                            isTwoFactorEnabled = false,
                            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = keyConnectorOptions,
                                masterPasswordUnlock = null,
                            ),
                        ),
                    ),
                ),
            ),
            originalUserState.toUpdatedUserStateJson(syncResponse),
        )
    }

    @Test
    fun `toUserStateJsonKdfUpdatedMinimums should update KDF settings to minimum values`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = "stamp",
            organizationId = null,
            avatarColorHex = "avatarColorHex",
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

        val result = originalUserState.toUserStateJsonKdfUpdatedMinimums()

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = DEFAULT_PBKDF2_ITERATIONS,
                            kdfMemory = null,
                            kdfParallelism = null,
                        ),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUserStateJsonKdfUpdatedMinimums should preserve other profile data while updating KDF`() {
        val userDecryptionOptions = UserDecryptionOptionsJson(
            hasMasterPassword = true,
            trustedDeviceUserDecryptionOptions = null,
            keyConnectorUserDecryptionOptions = null,
            masterPasswordUnlock = null,
        )
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "test@example.com",
            isEmailVerified = true,
            name = "Test User",
            stamp = "securityStamp",
            organizationId = "orgId",
            avatarColorHex = "#FF0000",
            hasPremium = false,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 100000,
            kdfMemory = 32,
            kdfParallelism = 8,
            userDecryptionOptions = userDecryptionOptions,
            isTwoFactorEnabled = true,
            creationDate = ZonedDateTime.parse("2024-01-01T00:00:00.00Z"),
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = null,
            settings = AccountJson.Settings(
                environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.bitwarden.com"),
            ),
        )
        val originalUserState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf("activeUserId" to originalAccount),
        )

        val result = originalUserState.toUserStateJsonKdfUpdatedMinimums()

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = DEFAULT_PBKDF2_ITERATIONS,
                            kdfMemory = null,
                            kdfParallelism = null,
                        ),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toUserStateJsonKdfUpdatedMinimums should only update active user account`() {
        val activeProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "active@example.com",
            isEmailVerified = true,
            name = "Active User",
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
        val inactiveProfile = AccountJson.Profile(
            userId = "inactiveUserId",
            email = "inactive@example.com",
            isEmailVerified = true,
            name = "Inactive User",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremium = false,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 500000,
            kdfMemory = 8,
            kdfParallelism = 2,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-08-13T01:00:00.00Z"),
        )
        val activeAccount = AccountJson(
            profile = activeProfile,
            tokens = null,
            settings = AccountJson.Settings(environmentUrlData = null),
        )
        val inactiveAccount = AccountJson(
            profile = inactiveProfile,
            tokens = null,
            settings = AccountJson.Settings(environmentUrlData = null),
        )
        val originalUserState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf(
                "activeUserId" to activeAccount,
                "inactiveUserId" to inactiveAccount,
            ),
        )

        val result = originalUserState.toUserStateJsonKdfUpdatedMinimums()

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to activeAccount.copy(
                        profile = activeProfile.copy(
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = DEFAULT_PBKDF2_ITERATIONS,
                            kdfMemory = null,
                            kdfParallelism = null,
                        ),
                    ),
                    "inactiveUserId" to inactiveAccount, // Should remain unchanged
                ),
            ),
            result,
        )
    }
}

private val MOCK_MASTER_PASSWORD_UNLOCK_DATA = MasterPasswordUnlockDataJson(
    salt = "mockSalt",
    kdf = mockk(),
    masterKeyWrappedUserKey = "masterKeyWrappedUserKeyMock",
)
