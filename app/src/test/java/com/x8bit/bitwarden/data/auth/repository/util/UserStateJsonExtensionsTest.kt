package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
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
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                        vaultUnlockType = VaultUnlockType.PIN,
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
                        },
                        tokens = AccountJson.Tokens(
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
                    vaultState = VaultState(
                        unlockedVaultUserIds = setOf("activeUserId"),
                        unlockingVaultUserIds = emptySet(),
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
                    specialCircumstance = null,
                    vaultUnlockTypeProvider = { VaultUnlockType.PIN },
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
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                        vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                    ),
                ),
                specialCircumstance = UserState.SpecialCircumstance.PendingAccountAddition,
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
                        },
                        tokens = AccountJson.Tokens(
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
                    vaultState = VaultState(
                        unlockedVaultUserIds = emptySet(),
                        unlockingVaultUserIds = emptySet(),
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
                    specialCircumstance = UserState.SpecialCircumstance.PendingAccountAddition,
                    vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                ),
        )
    }
}
