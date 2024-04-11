package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
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
 * Updates the [UserStateJson] to set the `hasMasterPassword` value to `true` after a user sets
 * their password.
 */
fun UserStateJson.toUserStateJsonWithPassword(): UserStateJson {
    val account = this.accounts[activeUserId] ?: return this
    val profile = account.profile
    val updatedProfile = profile
        .copy(
            userDecryptionOptions = profile
                .userDecryptionOptions
                ?.copy(hasMasterPassword = true)
                ?: UserDecryptionOptionsJson(
                    hasMasterPassword = true,
                    keyConnectorUserDecryptionOptions = null,
                    trustedDeviceUserDecryptionOptions = null,
                ),
        )
    val updatedAccount = account.copy(profile = updatedProfile)
    return this
        .copy(
            accounts = accounts
                .toMutableMap()
                .apply {
                    replace(activeUserId, updatedAccount)
                },
        )
}

/**
 * Converts the given [UserStateJson] to a [UserState] using the given [vaultState].
 */
@Suppress("LongParameterList")
fun UserStateJson.toUserState(
    vaultState: List<VaultUnlockData>,
    userOrganizationsList: List<UserOrganizations>,
    hasPendingAccountAddition: Boolean,
    isBiometricsEnabledProvider: (userId: String) -> Boolean,
    vaultUnlockTypeProvider: (userId: String) -> VaultUnlockType,
    isLoggedInProvider: (userId: String) -> Boolean,
    isDeviceTrustedProvider: (userId: String) -> Boolean,
): UserState =
    UserState(
        activeUserId = this.activeUserId,
        accounts = this
            .accounts
            .values
            .map { accountJson ->
                val profile = accountJson.profile
                val userId = profile.userId
                val vaultUnlocked = vaultState.statusFor(userId) == VaultUnlockData.Status.UNLOCKED
                val needsPasswordReset = profile.forcePasswordResetReason != null
                val decryptionOptions = profile.userDecryptionOptions
                val trustedDeviceOptions = decryptionOptions?.trustedDeviceUserDecryptionOptions
                val keyConnectorOptions = decryptionOptions?.keyConnectorUserDecryptionOptions
                val needsMasterPassword = decryptionOptions?.hasMasterPassword == false &&
                    trustedDeviceOptions?.hasManageResetPasswordPermission != false &&
                    keyConnectorOptions == null
                val trustedDevice = trustedDeviceOptions?.let {
                    UserState.TrustedDevice(
                        isDeviceTrusted = isDeviceTrustedProvider(userId),
                        hasMasterPassword = decryptionOptions.hasMasterPassword,
                        hasAdminApproval = it.hasAdminApproval,
                        hasLoginApprovingDevice = it.hasLoginApprovingDevice,
                        hasResetPasswordPermission = it.hasManageResetPasswordPermission,
                    )
                }

                UserState.Account(
                    userId = userId,
                    name = profile.name,
                    email = profile.email,
                    avatarColorHex = profile.avatarColorHex ?: userId.toHexColorRepresentation(),
                    environment = accountJson
                        .settings
                        .environmentUrlData
                        .toEnvironmentUrlsOrDefault(),
                    isPremium = profile.hasPremium == true,
                    isLoggedIn = isLoggedInProvider(userId),
                    isVaultUnlocked = vaultUnlocked,
                    needsPasswordReset = needsPasswordReset,
                    organizations = userOrganizationsList
                        .find { it.userId == userId }
                        ?.organizations
                        .orEmpty(),
                    isBiometricsEnabled = isBiometricsEnabledProvider(userId),
                    vaultUnlockType = vaultUnlockTypeProvider(userId),
                    needsMasterPassword = needsMasterPassword,
                    trustedDevice = trustedDevice,
                )
            },
        hasPendingAccountAddition = hasPendingAccountAddition,
    )
