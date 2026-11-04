package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.core.UpdateKdfResponse
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.crypto.Kdf
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.MasterPasswordAuthenticationDataJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.UpdateKdfJsonRequest
import com.bitwarden.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfRequestModel
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.util.toUserStateJsonKdfUpdatedMinimums
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import timber.log.Timber

/**
 * Default implementation of [KdfManager].
 */
class KdfManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val accountsService: AccountsService,
    private val featureFlagManager: FeatureFlagManager,
) : KdfManager {

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override fun needsKdfUpdateToMinimums(userId: String): Boolean {
        if (!featureFlagManager.getFeatureFlag(FlagKey.ForceUpdateKdfSettings)) {
            return false
        }

        val account = authDiskSource
            .userState
            ?.accounts
            ?.get(userId)
            ?: return false

        if (account.profile.userDecryptionOptions != null &&
            !account.profile.userDecryptionOptions.hasMasterPassword
        ) {
            return false
        }

        return account.profile.kdfType == KdfTypeJson.PBKDF2_SHA256 &&
            account.profile.kdfIterations != null &&
            account.profile.kdfIterations < DEFAULT_PBKDF2_ITERATIONS
    }

    override suspend fun updateKdfToMinimumsIfNeeded(
        userId: String,
        password: String,
    ): UpdateKdfMinimumsResult {
        // The update request is authenticated as the active user, so only update the KDF when
        // the given user is the active user. This prevents updating the KDF of a user that is not
        // active yet, such as when logging in to an additional account.
        if (userId != activeUserId) return UpdateKdfMinimumsResult.ActiveAccountNotFound

        if (!needsKdfUpdateToMinimums(userId = userId)) {
            return UpdateKdfMinimumsResult.Success
        }
        return vaultSdkSource
            .makeUpdateKdf(
                userId = userId,
                password = password,
                kdf = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
            )
            .flatMap {
                accountsService.updateKdf(createUpdateKdfRequest(it))
            }
            .fold(
                onSuccess = {
                    authDiskSource.userState = authDiskSource.userState
                        ?.toUserStateJsonKdfUpdatedMinimums(userId = userId)
                    Timber.d("[Auth] Upgraded user's KDF to minimums")
                    UpdateKdfMinimumsResult.Success
                },
                onFailure = { UpdateKdfMinimumsResult.Error(error = it) },
            )
    }

    private fun createUpdateKdfRequest(response: UpdateKdfResponse): UpdateKdfJsonRequest {
        val authData = response.masterPasswordAuthenticationData
        val oldAuthData = response.oldMasterPasswordAuthenticationData
        val unlockData = response.masterPasswordUnlockData

        return UpdateKdfJsonRequest(
            authenticationData = MasterPasswordAuthenticationDataJson(
                kdf = authData.kdf.toKdfRequestModel(),
                masterPasswordAuthenticationHash = authData.masterPasswordAuthenticationHash,
                salt = authData.salt,
            ),
            key = unlockData.masterKeyWrappedUserKey,
            masterPasswordHash = oldAuthData.masterPasswordAuthenticationHash,
            newMasterPasswordHash = authData.masterPasswordAuthenticationHash,
            unlockData = MasterPasswordUnlockDataJson(
                kdf = unlockData.kdf.toKdfRequestModel(),
                masterKeyWrappedUserKey = unlockData.masterKeyWrappedUserKey,
                salt = unlockData.salt,
                containedKeyId = unlockData.containedKeyId,
            ),
        )
    }
}
