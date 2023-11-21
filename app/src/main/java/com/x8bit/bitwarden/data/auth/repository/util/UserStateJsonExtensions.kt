package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

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
