package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.crypto.Kdf
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.KdfJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.KeyConnectorUserDecryptionOptionsJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.UserDecryptionJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.createMockOrganizationNetwork
import com.bitwarden.network.model.createMockPermissions
import com.bitwarden.network.model.createMockProfile
import com.bitwarden.network.model.createMockSyncResponse
import com.bitwarden.policies.PolicyType
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfRequestModel
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@Suppress("LargeClass")
class UserStateJsonExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Kdf::toKdfRequestModel)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Kdf::toKdfRequestModel)
    }

    @Test
    fun `toRemovedPasswordUserStateJson should do nothing for a non-matching account`() {
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
    fun `toRemovedPasswordUserStateJson should create user decryption options without a password if not present`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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

    @Suppress("MaxLineLength")
    @Test
    fun `toRemovedPasswordUserStateJson should update user decryption options to not have a password`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = UserDecryptionOptionsJson(
                hasMasterPassword = true,
                trustedDeviceUserDecryptionOptions = null,
                keyConnectorUserDecryptionOptions = null,
                masterPasswordUnlock = MasterPasswordUnlockDataJson(
                    salt = "salt",
                    kdf = mockk(),
                    masterKeyWrappedUserKey = "masterKeyWrappedUserKey",
                ),
            ),
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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

    @Suppress("MaxLineLength")
    @Test
    fun `toUpdatedUserStateJson should store personal and organization-granted premium separately`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = "stamp",
            organizationId = null,
            avatarColorHex = "color",
            hasPremiumPersonally = false,
            hasPremiumFromOrganization = false,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = null,
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = mockk(),
            settings = mockk(),
        )
        val originalState = UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf("activeUserId" to originalAccount),
        )

        val orgOnlyResult = originalState.toUpdatedUserStateJson(
            syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(
                    number = 1,
                    id = "activeUserId",
                    avatarColor = "color",
                    securityStamp = "stamp",
                    isPremium = false,
                    isPremiumFromOrganization = true,
                ),
                userDecryption = null,
            ),
        )
        val orgOnlyProfile = orgOnlyResult.accounts.getValue("activeUserId").profile
        assertEquals(false, orgOnlyProfile.hasPremiumPersonally)
        assertEquals(true, orgOnlyProfile.hasPremiumFromOrganization)

        val personalOnlyResult = originalState.toUpdatedUserStateJson(
            syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(
                    number = 1,
                    id = "activeUserId",
                    avatarColor = "color",
                    securityStamp = "stamp",
                    isPremium = true,
                    isPremiumFromOrganization = false,
                ),
                userDecryption = null,
            ),
        )
        val personalOnlyProfile = personalOnlyResult.accounts.getValue("activeUserId").profile
        assertEquals(true, personalOnlyProfile.hasPremiumPersonally)
        assertEquals(false, personalOnlyProfile.hasPremiumFromOrganization)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserState should derive aggregate isPremium and isPremiumFromSelf from profile premium fields`() {
        val baseProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = "stamp",
            organizationId = null,
            avatarColorHex = "color",
            hasPremiumPersonally = false,
            hasPremiumFromOrganization = false,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = null,
        )

        fun stateWith(hasPremiumPersonally: Boolean?, hasPremiumFromOrg: Boolean?): UserStateJson =
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to AccountJson(
                        profile = baseProfile.copy(
                            hasPremiumPersonally = hasPremiumPersonally,
                            hasPremiumFromOrganization = hasPremiumFromOrg,
                        ),
                        tokens = null,
                        settings = AccountJson.Settings(environmentUrlData = null),
                    ),
                ),
            )

        fun toAccount(state: UserStateJson) = state
            .toUserState(
                vaultState = emptyList(),
                userAccountTokens = emptyList(),
                userOrganizationsList = emptyList(),
                userIsUsingKeyConnectorList = emptyList(),
                hasPendingAccountAddition = false,
                onboardingStatus = OnboardingStatus.COMPLETE,
                firstTimeState = FirstTimeState(),
                isBiometricsEnabledProvider = { false },
                vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                isDeviceTrustedProvider = { false },
                getUserPolicies = { _, _ -> emptyList() },
            )
            .accounts
            .first()

        val freeAccount = toAccount(
            stateWith(hasPremiumPersonally = false, hasPremiumFromOrg = false),
        )
        assertFalse(freeAccount.isPremium)
        assertFalse(freeAccount.isPremiumFromSelf)

        val personalAccount = toAccount(
            stateWith(hasPremiumPersonally = true, hasPremiumFromOrg = false),
        )
        assertTrue(personalAccount.isPremium)
        assertTrue(personalAccount.isPremiumFromSelf)

        val orgOnlyAccount = toAccount(
            stateWith(hasPremiumPersonally = false, hasPremiumFromOrg = true),
        )
        assertTrue(orgOnlyAccount.isPremium)
        assertFalse(orgOnlyAccount.isPremiumFromSelf)

        val bothAccount = toAccount(
            stateWith(hasPremiumPersonally = true, hasPremiumFromOrg = true),
        )
        assertTrue(bothAccount.isPremium)
        assertTrue(bothAccount.isPremiumFromSelf)
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
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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
                            hasPremiumFromOrganization = true,
                            isTwoFactorEnabled = false,
                            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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
                    syncResponse = createMockSyncResponse(
                        number = 1,
                        profile = createMockProfile(
                            number = 1,
                            id = "activeUserId",
                            avatarColor = "avatarColor",
                            securityStamp = "securityStamp",
                            isPremium = true,
                            isPremiumFromOrganization = true,
                            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
                        ),
                        userDecryption = null,
                    ),
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toUserStateJsonWithPassword with masterPasswordUnlock should update active account to set hasMasterPassword and masterPasswordUnlock`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = ForcePasswordResetReason
                .TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
        )
        val originalAccount = AccountJson(
            profile = originalProfile,
            tokens = mockk(),
            settings = mockk(),
        )
        val kdf = mockk<KdfJson>()
        val masterKeyWrappedUserKey = "masterKeyWrappedUserKey"
        val salt = "salt"
        val masterPasswordUnlock = MasterPasswordUnlockData(
            kdf = mockk<Kdf> { every { toKdfRequestModel() } returns kdf },
            masterKeyWrappedUserKey = masterKeyWrappedUserKey,
            salt = salt,
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
                                masterPasswordUnlock = MasterPasswordUnlockDataJson(
                                    kdf = kdf,
                                    masterKeyWrappedUserKey = masterKeyWrappedUserKey,
                                    salt = salt,
                                ),
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
                .toUserStateJsonWithPassword(masterPasswordUnlock = masterPasswordUnlock),
        )
    }

    @Test
    fun `toUserState should return the correct UserState for an unlocked vault`() {
        val expectedCreationDate = Instant.parse("2024-06-15T10:30:00Z")
        assertEquals(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        avatarColorHex = "activeAvatarColorHex",
                        environment = Environment.Prod.Eu,
                        isPremium = false,
                        isPremiumFromSelf = false,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = expectedCreationDate,
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
                            every { hasPremiumPersonally } returns null
                            every { hasPremiumFromOrganization } returns null
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = null,
                            )
                            every { creationDate } returns expectedCreationDate
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = true,
                        isPremiumFromSelf = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns true
                            every { hasPremiumFromOrganization } returns null
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = null,
                            )
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = true,
                        isPremiumFromSelf = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns true
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = true,
                        isPremiumFromSelf = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns true
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = true,
                        isPremiumFromSelf = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns true
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    role = OrganizationType.ADMIN,
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = true,
                        isPremiumFromSelf = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                // Key part of the result #1, this is true or the role is owner or
                                // admin
                                shouldManageResetPassword = true,
                                role = OrganizationType.USER,
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns true
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    shouldManageResetPassword = true,
                                    role = OrganizationType.USER,
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = false,
                        isPremiumFromSelf = false,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns false
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                        environment = Environment.Prod.Eu,
                        isPremium = false,
                        isPremiumFromSelf = false,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns false
                            every { hasPremiumFromOrganization } returns null
                            every { forcePasswordResetReason } returns null
                            // The decryption options are what are determining the result
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = false,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = null,
                            )
                            every { creationDate } returns null
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
                        environment = Environment.Prod.Eu,
                        isPremium = false,
                        isPremiumFromSelf = false,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                role = OrganizationType.USER,
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns false
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    role = OrganizationType.USER,
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = true,
                        isPremiumFromSelf = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns true
                            every { hasPremiumFromOrganization } returns null
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
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                        environment = Environment.Prod.Eu,
                        isPremium = false,
                        isPremiumFromSelf = false,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns null
                            every { hasPremiumFromOrganization } returns null
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = null,
                            )
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                            createMockPolicyView(
                                id = "policyId",
                                organizationId = "organizationId",
                                type = PolicyType.DISABLE_PERSONAL_VAULT_EXPORT,
                                enabled = true,
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
                        environment = Environment.Prod.Eu,
                        isPremium = false,
                        isPremiumFromSelf = false,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        organizations = listOf(
                            createMockOrganization(
                                number = 1,
                                id = "organizationId",
                                name = "organizationName",
                                keyConnectorUrl = null,
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
                        creationDate = null,
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
                            every { hasPremiumPersonally } returns null
                            every { hasPremiumFromOrganization } returns null
                            every { forcePasswordResetReason } returns null
                            every { userDecryptionOptions } returns UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
                                masterPasswordUnlock = null,
                            )
                            every { creationDate } returns null
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
                                createMockOrganization(
                                    number = 1,
                                    id = "organizationId",
                                    name = "organizationName",
                                    keyConnectorUrl = null,
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
                            createMockPolicyView(
                                id = "policyId",
                                organizationId = "organizationId",
                                type = PolicyType.DISABLE_PERSONAL_VAULT_EXPORT,
                                enabled = false,
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
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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

        val syncResponse = createMockSyncResponse(
            number = 1,
            profile = createMockProfile(
                number = 1,
                id = "activeUserId",
                avatarColor = "avatarColor",
                securityStamp = "securityStamp",
                isPremium = false,
                isPremiumFromOrganization = false,
                isTwoFactorEnabled = true,
                creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
            ),
            userDecryption = UserDecryptionJson(
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
            ),
        )

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "avatarColor",
                            stamp = "securityStamp",
                            hasPremiumPersonally = false,
                            hasPremiumFromOrganization = false,
                            isTwoFactorEnabled = true,
                            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = 600000,
                            kdfMemory = 16,
                            kdfParallelism = 4,
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
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
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
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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

        val syncResponse = createMockSyncResponse(
            number = 1,
            profile = createMockProfile(
                number = 1,
                id = "activeUserId",
                avatarColor = "newAvatarColor",
                securityStamp = "newSecurityStamp",
                isPremium = true,
                isPremiumFromOrganization = false,
                isTwoFactorEnabled = true,
                creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
            ),
            userDecryption = UserDecryptionJson(
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
            ),
        )

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "newAvatarColor",
                            stamp = "newSecurityStamp",
                            hasPremiumPersonally = true,
                            hasPremiumFromOrganization = false,
                            isTwoFactorEnabled = true,
                            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = 600000,
                            kdfMemory = 16,
                            kdfParallelism = 4,
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
    fun `toUpdatedUserStateJson should clear hasMasterPassword and masterPasswordUnlock when syncResponse has no userDecryption`() {
        val keyConnectorOptions = KeyConnectorUserDecryptionOptionsJson("keyConnectorUrl")
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
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
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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

        val syncResponse = createMockSyncResponse(
            number = 1,
            profile = createMockProfile(
                number = 1,
                id = "activeUserId",
                avatarColor = "updatedAvatarColor",
                securityStamp = "updatedSecurityStamp",
                isPremium = false,
                isPremiumFromOrganization = true,
                creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
                organizations = emptyList(),
            ),
            userDecryption = null,
        )

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            avatarColorHex = "updatedAvatarColor",
                            stamp = "updatedSecurityStamp",
                            hasPremiumPersonally = false,
                            hasPremiumFromOrganization = true,
                            isTwoFactorEnabled = false,
                            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = false,
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
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should update KDF settings when sync response provides updated values`() {
        val originalProfile = AccountJson.Profile(
            userId = "activeUserId",
            email = "email",
            isEmailVerified = true,
            name = "name",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremiumPersonally = false,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.PBKDF2_SHA256,
            kdfIterations = 100_000,
            kdfMemory = null,
            kdfParallelism = null,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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

        val syncResponse = createMockSyncResponse(
            number = 1,
            profile = createMockProfile(
                number = 1,
                id = "activeUserId",
                avatarColor = null,
                securityStamp = null,
                creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
            ),
            userDecryption = UserDecryptionJson(
                masterPasswordUnlock = MasterPasswordUnlockDataJson(
                    salt = "mockSalt",
                    kdf = KdfJson(
                        kdfType = KdfTypeJson.PBKDF2_SHA256,
                        iterations = DEFAULT_PBKDF2_ITERATIONS,
                        memory = null,
                        parallelism = null,
                    ),
                    masterKeyWrappedUserKey = "mockMasterKeyWrappedUserKey",
                ),
            ),
        )

        assertEquals(
            UserStateJson(
                activeUserId = "activeUserId",
                accounts = mapOf(
                    "activeUserId" to originalAccount.copy(
                        profile = originalProfile.copy(
                            kdfIterations = DEFAULT_PBKDF2_ITERATIONS,
                            hasPremiumFromOrganization = false,
                            userDecryptionOptions = UserDecryptionOptionsJson(
                                hasMasterPassword = true,
                                masterPasswordUnlock = MasterPasswordUnlockDataJson(
                                    salt = "mockSalt",
                                    kdf = KdfJson(
                                        kdfType = KdfTypeJson.PBKDF2_SHA256,
                                        iterations = DEFAULT_PBKDF2_ITERATIONS,
                                        memory = null,
                                        parallelism = null,
                                    ),
                                    masterKeyWrappedUserKey = "mockMasterKeyWrappedUserKey",
                                ),
                                trustedDeviceUserDecryptionOptions = null,
                                keyConnectorUserDecryptionOptions = null,
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
    fun `toUpdatedUserStateJson should set forcePasswordResetReason when user without master password is an organization admin`() {
        val originalUserState = createUserStateWithDecryptionOptions(
            userDecryptionOptions = TDE_USER_DECRYPTION_OPTIONS,
        )

        val result = originalUserState.toUpdatedUserStateJson(
            syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(
                    number = 1,
                    id = "activeUserId",
                    organizations = listOf(
                        createMockOrganizationNetwork(
                            number = 1,
                            type = OrganizationType.ADMIN,
                        ),
                    ),
                ),
                userDecryption = null,
            ),
        )

        assertEquals(
            ForcePasswordResetReason.TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
            result.accounts.getValue("activeUserId").profile.forcePasswordResetReason,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should set forcePasswordResetReason when user without master password has reset password permission`() {
        val originalUserState = createUserStateWithDecryptionOptions(
            userDecryptionOptions = TDE_USER_DECRYPTION_OPTIONS,
        )

        val result = originalUserState.toUpdatedUserStateJson(
            syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(
                    number = 1,
                    id = "activeUserId",
                    organizations = listOf(
                        createMockOrganizationNetwork(
                            number = 1,
                            type = OrganizationType.USER,
                            permissions = createMockPermissions(
                                shouldManageResetPassword = true,
                            ),
                        ),
                    ),
                ),
                userDecryption = UserDecryptionJson(masterPasswordUnlock = null),
            ),
        )

        assertEquals(
            ForcePasswordResetReason.TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
            result.accounts.getValue("activeUserId").profile.forcePasswordResetReason,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should not set forcePasswordResetReason when user without master password lacks reset password permission`() {
        val originalUserState = createUserStateWithDecryptionOptions(
            userDecryptionOptions = TDE_USER_DECRYPTION_OPTIONS,
        )

        val result = originalUserState.toUpdatedUserStateJson(
            syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(
                    number = 1,
                    id = "activeUserId",
                    organizations = listOf(
                        createMockOrganizationNetwork(
                            number = 1,
                            type = OrganizationType.USER,
                        ),
                    ),
                ),
                userDecryption = null,
            ),
        )

        assertEquals(
            null,
            result.accounts.getValue("activeUserId").profile.forcePasswordResetReason,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toUpdatedUserStateJson should preserve previous forcePasswordResetReason when user has a master password`() {
        val originalUserState = createUserStateWithDecryptionOptions(
            userDecryptionOptions = UserDecryptionOptionsJson(
                hasMasterPassword = true,
                trustedDeviceUserDecryptionOptions = null,
                keyConnectorUserDecryptionOptions = null,
                masterPasswordUnlock = null,
            ),
            forcePasswordResetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
        )

        val result = originalUserState.toUpdatedUserStateJson(
            syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(
                    number = 1,
                    id = "activeUserId",
                    organizations = listOf(
                        createMockOrganizationNetwork(
                            number = 1,
                            type = OrganizationType.OWNER,
                        ),
                    ),
                ),
                userDecryption = UserDecryptionJson(
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
            ),
        )

        assertEquals(
            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
            result.accounts.getValue("activeUserId").profile.forcePasswordResetReason,
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
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
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
            hasPremiumPersonally = false,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 100000,
            kdfMemory = 32,
            kdfParallelism = 8,
            userDecryptionOptions = userDecryptionOptions,
            isTwoFactorEnabled = true,
            creationDate = Instant.parse("2024-01-01T00:00:00.00Z"),
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
            hasPremiumPersonally = true,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
        )
        val inactiveProfile = AccountJson.Profile(
            userId = "inactiveUserId",
            email = "inactive@example.com",
            isEmailVerified = true,
            name = "Inactive User",
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremiumPersonally = false,
            hasPremiumFromOrganization = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 500000,
            kdfMemory = 8,
            kdfParallelism = 2,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = Instant.parse("2024-08-13T01:00:00.00Z"),
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
    kdf = KdfJson(
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        iterations = 600_000,
        memory = null,
        parallelism = null,
    ),
    masterKeyWrappedUserKey = "masterKeyWrappedUserKeyMock",
)

private val TDE_USER_DECRYPTION_OPTIONS = UserDecryptionOptionsJson(
    hasMasterPassword = false,
    trustedDeviceUserDecryptionOptions = TrustedDeviceUserDecryptionOptionsJson(
        encryptedPrivateKey = "encryptedPrivateKey",
        encryptedUserKey = "encryptedUserKey",
        hasAdminApproval = true,
        hasLoginApprovingDevice = false,
        hasManageResetPasswordPermission = false,
    ),
    keyConnectorUserDecryptionOptions = null,
    masterPasswordUnlock = null,
)

/**
 * Creates a [UserStateJson] with a single "activeUserId" account using the given
 * [userDecryptionOptions] and [forcePasswordResetReason].
 */
private fun createUserStateWithDecryptionOptions(
    userDecryptionOptions: UserDecryptionOptionsJson?,
    forcePasswordResetReason: ForcePasswordResetReason? = null,
): UserStateJson =
    UserStateJson(
        activeUserId = "activeUserId",
        accounts = mapOf(
            "activeUserId" to AccountJson(
                profile = AccountJson.Profile(
                    userId = "activeUserId",
                    email = "email",
                    isEmailVerified = true,
                    name = "name",
                    stamp = null,
                    organizationId = null,
                    avatarColorHex = null,
                    hasPremiumPersonally = true,
                    hasPremiumFromOrganization = null,
                    forcePasswordResetReason = forcePasswordResetReason,
                    kdfType = KdfTypeJson.ARGON2_ID,
                    kdfIterations = 600000,
                    kdfMemory = 16,
                    kdfParallelism = 4,
                    userDecryptionOptions = userDecryptionOptions,
                    isTwoFactorEnabled = false,
                    creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
                ),
                tokens = null,
                settings = AccountJson.Settings(environmentUrlData = null),
            ),
        ),
    )
