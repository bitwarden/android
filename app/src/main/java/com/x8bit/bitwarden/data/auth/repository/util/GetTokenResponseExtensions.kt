package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson

/**
 * Converts the given [GetTokenResponseJson.Success] to a [UserStateJson], given the following
 * additional information:
 *
 * - the [previousUserState]
 * - the current [environmentUrlData]
 */
fun GetTokenResponseJson.Success.toUserState(
    previousUserState: UserStateJson?,
    environmentUrlData: EnvironmentUrlDataJson,
): UserStateJson {
    val accessToken = this.accessToken

    @Suppress("UnsafeCallOnNullableType")
    val jwtTokenData = parseJwtTokenDataOrNull(jwtToken = accessToken)!!
    val userId = jwtTokenData.userId

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
            forcePasswordResetReason = if (this.shouldForcePasswordReset) {
                ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET
            } else {
                null
            },
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
            environmentUrlData = environmentUrlData,
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
