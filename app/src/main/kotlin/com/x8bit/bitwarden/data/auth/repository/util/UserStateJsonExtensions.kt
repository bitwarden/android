package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.data.repository.util.toEnvironmentUrlsOrDefault
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.ui.platform.base.util.toHexColorRepresentation
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor

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
            masterPasswordUnlock = null,
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
    val userDecryptionOptions = syncResponse
        .userDecryption
        ?.let { syncUserDecryption ->
            profile
                .userDecryptionOptions
                ?.copy(masterPasswordUnlock = syncUserDecryption.masterPasswordUnlock)
                ?: UserDecryptionOptionsJson(
                    hasMasterPassword = syncUserDecryption.masterPasswordUnlock != null,
                    trustedDeviceUserDecryptionOptions = null,
                    keyConnectorUserDecryptionOptions = null,
                    masterPasswordUnlock = syncUserDecryption.masterPasswordUnlock,
                )
        }
        ?: profile
            .userDecryptionOptions
            ?.copy(masterPasswordUnlock = null)

    val updatedProfile = profile
        .copy(
            avatarColorHex = syncProfile.avatarColor,
            stamp = syncProfile.securityStamp,
            hasPremium = syncProfile.isPremium || syncProfile.isPremiumFromOrganization,
            isTwoFactorEnabled = syncProfile.isTwoFactorEnabled,
            creationDate = syncProfile.creationDate,
            userDecryptionOptions = userDecryptionOptions,
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
                    masterPasswordUnlock = null,
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
 * Updates the [UserStateJson] KDF settings to minimum requirements.
 */
fun UserStateJson.toUserStateJsonKdfUpdatedMinimums(): UserStateJson {
    val account = this.activeAccount
    val profile = account.profile
    val updatedProfile = profile
        .copy(
            kdfType = KdfTypeJson.PBKDF2_SHA256,
            kdfIterations = DEFAULT_PBKDF2_ITERATIONS,
            kdfMemory = null,
            kdfParallelism = null,
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
    getUserPolicies: (userId: String, policy: PolicyTypeJson) -> List<SyncResponseJson.Policy>,
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

                val hasPersonalOwnershipRestrictedOrg = getUserPolicies(
                    userId,
                    PolicyTypeJson.PERSONAL_OWNERSHIP,
                )
                    .any { it.isEnabled }

                val hasPersonalVaultExportRestrictedOrg = getUserPolicies(
                    userId,
                    PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT,
                )
                    .any { it.isEnabled }

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
                    isExportable = !hasPersonalOwnershipRestrictedOrg &&
                        !hasPersonalVaultExportRestrictedOrg,
                )
            },
        hasPendingAccountAddition = hasPendingAccountAddition,
    )
