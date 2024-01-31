package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrlsOrDefault
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import com.x8bit.bitwarden.ui.platform.base.util.toHexColorRepresentation

/**
 * Updates the given [UserStateJson] with the data from the [syncResponse] to return a new
 * [UserStateJson]. The original will be returned if the sync response does not match any accounts
 * in the [UserStateJson].
 */
@Suppress("ReturnCount")
fun UserStateJson.toUpdatedUserStateJson(
    syncResponse: SyncResponseJson,
): UserStateJson {
    val syncProfile = syncResponse.profile
    val userId = syncProfile.id
    val account = this.accounts[userId] ?: return this
    val profile = account.profile
    // TODO: Update additional missing UserStateJson properties (BIT-916)
    val updatedProfile = profile
        .copy(
            avatarColorHex = syncProfile.avatarColor,
            stamp = syncProfile.securityStamp,
            hasPremium = syncProfile.isPremium || syncProfile.isPremiumFromOrganization,
        )
    val updatedAccount = account.copy(profile = updatedProfile)
    return this
        .copy(
            accounts = accounts
                .toMutableMap()
                .apply {
                    replace(userId, updatedAccount)
                },
        )
}

/**
 * Converts the given [UserStateJson] to a [UserState] using the given [vaultState].
 */
fun UserStateJson.toUserState(
    vaultState: List<VaultUnlockData>,
    userOrganizationsList: List<UserOrganizations>,
    hasPendingAccountAddition: Boolean,
    isBiometricsEnabledProvider: (userId: String) -> Boolean,
    vaultUnlockTypeProvider: (userId: String) -> VaultUnlockType,
): UserState =
    UserState(
        activeUserId = this.activeUserId,
        accounts = this
            .accounts
            .values
            .map { accountJson ->
                val userId = accountJson.profile.userId
                UserState.Account(
                    userId = accountJson.profile.userId,
                    name = accountJson.profile.name,
                    email = accountJson.profile.email,
                    avatarColorHex = accountJson.profile.avatarColorHex
                        ?: accountJson.profile.userId.toHexColorRepresentation(),
                    environment = accountJson
                        .settings
                        .environmentUrlData
                        .toEnvironmentUrlsOrDefault(),
                    isPremium = accountJson.profile.hasPremium == true,
                    isLoggedIn = accountJson.isLoggedIn,
                    isVaultUnlocked = vaultState.statusFor(userId) ==
                        VaultUnlockData.Status.UNLOCKED,
                    needsPasswordReset = accountJson.profile.forcePasswordResetReason != null,
                    organizations = userOrganizationsList
                        .find { it.userId == userId }
                        ?.organizations
                        .orEmpty(),
                    isBiometricsEnabled = isBiometricsEnabledProvider(userId),
                    vaultUnlockType = vaultUnlockTypeProvider(userId),
                )
            },
        hasPendingAccountAddition = hasPendingAccountAddition,
    )
