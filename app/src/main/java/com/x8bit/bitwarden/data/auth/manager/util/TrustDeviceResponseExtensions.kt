package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.crypto.TrustDeviceResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson

/**
 * Converts the given [TrustDeviceResponse] to an updated [UserStateJson], given the following
 * additional information:
 *
 * - the [userId]
 * - the [previousUserState]
 */
fun TrustDeviceResponse.toUserStateJson(
    userId: String,
    previousUserState: UserStateJson,
): UserStateJson {
    val trustedAccount = requireNotNull(previousUserState.accounts[userId])
    val profile = trustedAccount.profile
    // The UserDecryptionOptionsJson and TrustedDeviceUserDecryptionOptionsJson
    // should be present at this time, but we have fallbacks just in case.
    val decryptionOptions = profile
        .userDecryptionOptions
        ?: UserDecryptionOptionsJson(
            hasMasterPassword = false,
            trustedDeviceUserDecryptionOptions = null,
            keyConnectorUserDecryptionOptions = null,
        )
    val deviceOptions = decryptionOptions
        .trustedDeviceUserDecryptionOptions
        ?.copy(
            encryptedPrivateKey = this.protectedDevicePrivateKey,
            encryptedUserKey = this.protectedUserKey,
        )
        ?: TrustedDeviceUserDecryptionOptionsJson(
            encryptedPrivateKey = this.protectedDevicePrivateKey,
            encryptedUserKey = this.protectedUserKey,
            hasAdminApproval = false,
            hasLoginApprovingDevice = false,
            hasManageResetPasswordPermission = false,
        )
    val account = trustedAccount.copy(
        profile = profile.copy(
            userDecryptionOptions = decryptionOptions.copy(
                trustedDeviceUserDecryptionOptions = deviceOptions,
            ),
        ),
    )

    // Update the existing UserState.
    return previousUserState.copy(
        accounts = previousUserState
            .accounts
            .toMutableMap()
            .apply { put(userId, account) },
    )
}
