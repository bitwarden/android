package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
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
                    status = AccountSummary.Status.ACTIVE,
                ),
                AccountSummary(
                    userId = "lockedUserId",
                    name = "lockedName",
                    email = "lockedEmail",
                    avatarColorHex = "lockedAvatarColorHex",
                    environmentLabel = "bitwarden.eu",
                    status = AccountSummary.Status.LOCKED,
                ),
                AccountSummary(
                    userId = "unlockedUserId",
                    name = "unlockedName",
                    email = "unlockedEmail",
                    avatarColorHex = "unlockedAvatarColorHex",
                    environmentLabel = "vault.qa.bitwarden.pw",
                    status = AccountSummary.Status.UNLOCKED,
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
                    ),
                    UserState.Account(
                        userId = "lockedUserId",
                        name = "lockedName",
                        email = "lockedEmail",
                        avatarColorHex = "lockedAvatarColorHex",
                        environment = Environment.Eu,
                        isPremium = false,
                        isVaultUnlocked = false,
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
                status = AccountSummary.Status.ACTIVE,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
                isVaultUnlocked = true,
            )
                .toAccountSummary(isActive = true),
        )
    }

    @Test
    fun `toAccountSummary for an locked account should return a locked AccountSummary`() {
        assertEquals(
            AccountSummary(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environmentLabel = "bitwarden.com",
                status = AccountSummary.Status.LOCKED,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = false,
                isVaultUnlocked = false,
            )
                .toAccountSummary(isActive = false),
        )
    }

    @Test
    fun `toAccountSummary for a unlocked account should return a locked AccountSummary`() {
        assertEquals(
            AccountSummary(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environmentLabel = "bitwarden.eu",
                status = AccountSummary.Status.UNLOCKED,
            ),
            UserState.Account(
                userId = "userId",
                name = "name",
                email = "email",
                avatarColorHex = "avatarColorHex",
                environment = Environment.Eu,
                isPremium = true,
                isVaultUnlocked = true,
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
                status = AccountSummary.Status.ACTIVE,
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
                    ),
                ),
            )
                .toActiveAccountSummary(),
        )
    }
}
