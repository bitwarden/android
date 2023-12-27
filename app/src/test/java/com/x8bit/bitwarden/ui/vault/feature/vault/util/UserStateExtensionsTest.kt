package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import org.junit.jupiter.api.Assertions.assertEquals
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
                    isVaultUnlocked = true,
                ),
                AccountSummary(
                    userId = "lockedUserId",
                    name = "lockedName",
                    email = "lockedEmail",
                    avatarColorHex = "lockedAvatarColorHex",
                    environmentLabel = "bitwarden.eu",
                    isActive = false,
                    isVaultUnlocked = false,
                ),
                AccountSummary(
                    userId = "unlockedUserId",
                    name = "unlockedName",
                    email = "unlockedEmail",
                    avatarColorHex = "unlockedAvatarColorHex",
                    environmentLabel = "vault.qa.bitwarden.pw",
                    isActive = false,
                    isVaultUnlocked = true,
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
                isVaultUnlocked = true,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
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
                isVaultUnlocked = false,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = false,
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
}
