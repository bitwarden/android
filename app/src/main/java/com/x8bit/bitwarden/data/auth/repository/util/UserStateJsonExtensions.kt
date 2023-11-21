package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.repository.model.VaultState

/**
 * Updates the given [UserStateJson] with the data from the [syncResponse] to return a new
 * [UserStateJson]. The original will be returned if the sync response does not match any accounts
 * in the [UserStateJson].
 */
@Suppress("ReturnCount")
fun UserStateJson.toUpdatedUserStateJson(
    syncResponse: SyncResponseJson,
): UserStateJson {
    val userId = syncResponse.profile?.id ?: return this
    val account = this.accounts[userId] ?: return this
    val profile = account.profile
    // TODO: Update additional missing UserStateJson properties (BIT-916)
    val updatedProfile = profile
        .copy(
            avatarColorHex = syncResponse.profile.avatarColor,
            stamp = syncResponse.profile.securityStamp,
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
    vaultState: VaultState,
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
                    // TODO Calculate default color (BIT-1191)
                    avatarColorHex = accountJson.profile.avatarColorHex ?: "#00aaaa",
                    isVaultUnlocked = userId in vaultState.unlockedVaultUserIds,
                )
            },
    )
