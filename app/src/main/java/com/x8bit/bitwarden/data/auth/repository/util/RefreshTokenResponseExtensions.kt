package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson

/**
 * Converts the given [RefreshTokenResponseJson] to a [UserStateJson], given the following
 * additional information:
 *
 * - the [userId]
 * - the [previousUserState]
 */
fun RefreshTokenResponseJson.toUserStateJson(
    userId: String,
    previousUserState: UserStateJson,
): UserStateJson {
    val refreshedAccount = requireNotNull(previousUserState.accounts[userId])
    val accessToken = this.accessToken
    val jwtTokenData = requireNotNull(parseJwtTokenDataOrNull(jwtToken = accessToken))

    val account = refreshedAccount.copy(
        profile = refreshedAccount.profile.copy(
            userId = jwtTokenData.userId,
            email = jwtTokenData.email,
            isEmailVerified = jwtTokenData.isEmailVerified,
            name = jwtTokenData.name,
            hasPremium = jwtTokenData.hasPremium,
        ),
        tokens = AccountJson.Tokens(
            accessToken = accessToken,
            refreshToken = this.refreshToken,
        ),
    )

    // Update the existing UserState.
    return previousUserState.copy(
        accounts = previousUserState
            .accounts
            .toMutableMap()
            .apply {
                put(userId, account)
            },
    )
}
