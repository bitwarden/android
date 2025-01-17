package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrlsOrDefault
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import com.x8bit.bitwarden.ui.platform.base.util.toHexColorRepresentation

/**
 * Updates the given [UserStateJson] with the data to indicate that the password has been removed.
 * The original will be returned if the [userId] does not match any accounts in the [UserStateJson].
 */
fun UserStateJson.toRemovedPasswordUserStateJson(
    userId: String,
): UserStateJson {
    val account = this.accounts[userId] ?: return this
    val profile = account.profile
    val updatedUserDecryptionOptions = profile
        .userDecryptionOptions
        ?.copy(hasMasterPassword = false)
        ?: UserDecryptionOptionsJson(
            hasMasterPassword = false,
            trustedDeviceUserDecryptionOptions = null,
            keyConnectorUserDecryptionOptions = null,
        )
    val updatedProfile = profile.copy(userDecryptionOptions = updatedUserDecryptionOptions)
    val updatedAccount = account.copy(profile = updatedProfile)
    return this.copy(
        accounts = accounts
            .toMutableMap()
            .apply { replace(userId, updatedAccount) },
    )
}

/**
 * Updates the given [UserStateJson] with the data from the [syncResponse] to return a new
 * [UserStateJson]. The original will be returned if the sync response does not match any accounts
 * in the [UserStateJson].
 */
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
            isTwoFactorEnabled = syncProfile.isTwoFactorEnabled,
            creationDate = syncProfile.creationDate,
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
    val account = this.activeAccount
    val profile = account.profile
    val updatedProfile = profile
        .copy(
            forcePasswordResetReason = null,
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
@Suppress("LongParameterList", "LongMethod")
fun UserStateJson.toUserState(
    vaultState: List<VaultUnlockData>,
    userAccountTokens: List<UserAccountTokens>,
    userOrganizationsList: List<UserOrganizations>,
    userIsUsingKeyConnectorList: List<UserKeyConnectorState>,
    hasPendingAccountAddition: Boolean,
    onboardingStatus: OnboardingStatus?,
    firstTimeState: FirstTimeState,
    isBiometricsEnabledProvider: (userId: String) -> Boolean,
    vaultUnlockTypeProvider: (userId: String) -> VaultUnlockType,
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
                val organizations = userOrganizationsList
                    .find { it.userId == userId }
                    ?.organizations
                    .orEmpty()
                val hasManageResetPasswordPermission = organizations.any {
                    it.role == OrganizationType.OWNER ||
                        it.role == OrganizationType.ADMIN ||
                        it.shouldManageResetPassword
                }
                val trustedDevice = trustedDeviceOptions?.let {
                    UserState.TrustedDevice(
                        isDeviceTrusted = isDeviceTrustedProvider(userId),
                        hasAdminApproval = it.hasAdminApproval,
                        hasLoginApprovingDevice = it.hasLoginApprovingDevice,
                        hasResetPasswordPermission = it.hasManageResetPasswordPermission,
                    )
                }
                // If a user does not have a Master Password we want to check if they have another
                // method for unlocking the vault. In the case of a TDE user we check if they
                // have the reset password permission via their organization(S). If the user does
                // not belong to a TDE or we check to see if they user key connector.
                val tdeUserNeedsMasterPassword =
                    hasManageResetPasswordPermission.takeIf { trustedDevice != null }
                val needsMasterPassword = decryptionOptions?.hasMasterPassword == false &&
                    (tdeUserNeedsMasterPassword ?: (keyConnectorOptions == null))
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
                    isLoggedIn = userAccountTokens
                        .find { it.userId == userId }
                        ?.isLoggedIn == true,
                    isVaultUnlocked = vaultUnlocked,
                    needsPasswordReset = needsPasswordReset,
                    organizations = organizations,
                    isBiometricsEnabled = isBiometricsEnabledProvider(userId),
                    vaultUnlockType = vaultUnlockTypeProvider(userId),
                    needsMasterPassword = needsMasterPassword,
                    hasMasterPassword = decryptionOptions?.hasMasterPassword != false,
                    trustedDevice = trustedDevice,
                    isUsingKeyConnector = userIsUsingKeyConnectorList
                        .find { it.userId == userId }
                        ?.isUsingKeyConnector == true,
                    // If the user exists with no onboarding status we can assume they have been
                    // using the app prior to the release of the onboarding flow.
                    onboardingStatus = onboardingStatus ?: OnboardingStatus.COMPLETE,
                    firstTimeState = firstTimeState,
                )
            },
        hasPendingAccountAddition = hasPendingAccountAddition,
    )
