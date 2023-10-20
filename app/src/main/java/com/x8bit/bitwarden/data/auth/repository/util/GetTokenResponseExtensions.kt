package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson

/**
 * Converts the given [GetTokenResponseJson.Success] to a [UserStateJson], given the current
 * [previousUserState].
 */
fun GetTokenResponseJson.Success.toUserState(
    previousUserState: UserStateJson?,
): UserStateJson {
    val accessToken = this.accessToken

    @Suppress("UnsafeCallOnNullableType")
    val jwtTokenData = parseJwtTokenDataOrNull(jwtToken = accessToken)!!
    val userId = jwtTokenData.userId

    // TODO: Update null properties below via sync request (BIT-916)
    val account = AccountJson(
        profile = AccountJson.Profile(
            userId = userId,
            email = jwtTokenData.email,
            isEmailVerified = jwtTokenData.isEmailVerified,
            name = jwtTokenData.name,
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            hasPremium = jwtTokenData.hasPremium,
            forcePasswordResetReason = null,
            kdfType = this.kdfType,
            kdfIterations = this.kdfIterations,
            kdfMemory = this.kdfMemory,
            kdfParallelism = this.kdfParallelism,
            userDecryptionOptions = this.userDecryptionOptions,
        ),
        tokens = AccountJson.Tokens(
            accessToken = accessToken,
            refreshToken = this.refreshToken,
        ),
        settings = AccountJson.Settings(
            environmentUrlData = null,
        ),
    )

    // Create a new UserState with the updated info or update the existing one.
    return previousUserState
        ?.copy(
            activeUserId = userId,
            accounts = previousUserState
                .accounts
                .toMutableMap()
                .apply {
                    put(userId, account)
                },
        )
        ?: UserStateJson(
            activeUserId = userId,
            accounts = mapOf(userId to account),
        )
}
