package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserStateExtensionsTest {
    @Test
    fun `toAccountSummaries should return the correct list`() {
        assertEquals(
            listOf(
                AccountSummary(
                    userId = "activeUserId",
                    name = "activeName",
                    email = "activeEmail",
                    avatarColorHex = "activeAvatarColorHex",
                    environmentLabel = "bitwarden.com",
                    isActive = true,
                    isLoggedIn = true,
                    isVaultUnlocked = true,
                ),
                AccountSummary(
                    userId = "lockedUserId",
                    name = "lockedName",
                    email = "lockedEmail",
                    avatarColorHex = "lockedAvatarColorHex",
                    environmentLabel = "bitwarden.eu",
                    isActive = false,
                    isLoggedIn = true,
                    isVaultUnlocked = false,
                ),
                AccountSummary(
                    userId = "unlockedUserId",
                    name = "unlockedName",
                    email = "unlockedEmail",
                    avatarColorHex = "unlockedAvatarColorHex",
                    environmentLabel = "vault.qa.bitwarden.pw",
                    isActive = false,
                    isLoggedIn = true,
                    isVaultUnlocked = true,
                ),
                AccountSummary(
                    userId = "loggedOutUserId",
                    name = "loggedOutName",
                    email = "loggedOutEmail",
                    avatarColorHex = "loggedOutAvatarColorHex",
                    environmentLabel = "vault.qa.bitwarden.pw",
                    isActive = false,
                    isLoggedIn = false,
                    isVaultUnlocked = false,
                ),
            ),
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "activeName",
                        email = "activeEmail",
                        avatarColorHex = "activeAvatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                    ),
                    UserState.Account(
                        userId = "lockedUserId",
                        name = "lockedName",
                        email = "lockedEmail",
                        avatarColorHex = "lockedAvatarColorHex",
                        environment = Environment.Eu,
                        isPremium = false,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                    ),
                    UserState.Account(
                        userId = "unlockedUserId",
                        name = "unlockedName",
                        email = "unlockedEmail",
                        avatarColorHex = "unlockedAvatarColorHex",
                        environment = Environment.SelfHosted(
                            environmentUrlData = EnvironmentUrlDataJson(
                                base = "https://vault.qa.bitwarden.pw",
                            ),
                        ),
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                    ),
                    UserState.Account(
                        userId = "loggedOutUserId",
                        name = "loggedOutName",
                        email = "loggedOutEmail",
                        avatarColorHex = "loggedOutAvatarColorHex",
                        environment = Environment.SelfHosted(
                            environmentUrlData = EnvironmentUrlDataJson(
                                base = "https://vault.qa.bitwarden.pw",
                            ),
                        ),
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                    ),
                ),
            )
                .toAccountSummaries(),
        )
    }

    @Test
    fun `toAccountSummary for an active account should return an active AccountSummary`() {
        assertEquals(
            AccountSummary(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environmentLabel = "bitwarden.com",
                isActive = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
                organizations = listOf(
                    Organization(
                        id = "organizationId",
                        name = "organizationName",
                    ),
                ),
            )
                .toAccountSummary(isActive = true),
        )
    }

    @Test
    fun `toAccountSummary for an inactive account should return an inactive AccountSummary`() {
        assertEquals(
            AccountSummary(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environmentLabel = "bitwarden.com",
                isActive = false,
                isLoggedIn = true,
                isVaultUnlocked = false,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = false,
                isLoggedIn = true,
                isVaultUnlocked = false,
                organizations = listOf(
                    Organization(
                        id = "organizationId",
                        name = "organizationName",
                    ),
                ),
            )
                .toAccountSummary(isActive = false),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toActiveAccountSummary should return an active AccountSummary`() {
        assertEquals(
            AccountSummary(
                userId = "activeUserId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environmentLabel = "bitwarden.com",
                isActive = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
            ),
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        organizations = listOf(
                            Organization(
                                id = "organizationId",
                                name = "organizationName",
                            ),
                        ),
                    ),
                ),
            )
                .toActiveAccountSummary(),
        )
    }

    @Test
    fun `toVaultFilterData for an account with no organizations should return a null value`() {
        assertNull(
            UserState.Account(
                userId = "activeUserId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
                organizations = emptyList(),
            )
                .toVaultFilterData(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toVaultFilterData for an account with organizations should return data with the available types in the correct order`() {
        assertEquals(
            VaultFilterData(
                selectedVaultFilterType = VaultFilterType.AllVaults,
                vaultFilterTypes = listOf(
                    VaultFilterType.AllVaults,
                    VaultFilterType.MyVault,
                    VaultFilterType.OrganizationVault(
                        organizationId = "organizationId-A",
                        organizationName = "Organization A",
                    ),
                    VaultFilterType.OrganizationVault(
                        organizationId = "organizationId-B",
                        organizationName = "Organization B",
                    ),
                ),
            ),
            UserState.Account(
                userId = "activeUserId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
                organizations = listOf(
                    Organization(
                        id = "organizationId-B",
                        name = "Organization B",
                    ),
                    Organization(
                        id = "organizationId-A",
                        name = "Organization A",
                    ),
                ),
            )
                .toVaultFilterData(),
        )
    }
}
