package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update

/**
 * Primary implementation [VaultLockManager].
 */
class VaultLockManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
) : VaultLockManager {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableVaultStateStateFlow =
        MutableStateFlow(
            VaultState(
                unlockedVaultUserIds = emptySet(),
                unlockingVaultUserIds = emptySet(),
            ),
        )

    override val vaultStateFlow: StateFlow<VaultState>
        get() = mutableVaultStateStateFlow.asStateFlow()

    override fun isVaultUnlocked(userId: String): Boolean =
        userId in mutableVaultStateStateFlow.value.unlockedVaultUserIds

    override fun isVaultUnlocking(userId: String): Boolean =
        userId in mutableVaultStateStateFlow.value.unlockingVaultUserIds

    override fun lockVault(userId: String) {
        setVaultToLocked(userId = userId)
    }

    override fun lockVaultForCurrentUser() {
        activeUserId?.let {
            lockVaultIfNecessary(it)
        }
    }

    override fun lockVaultIfNecessary(userId: String) {
        // TODO: Check for VaultTimeout.Never (BIT-1019)
        lockVault(userId = userId)
    }

    override suspend fun unlockVault(
        userId: String,
        email: String,
        kdf: Kdf,
        privateKey: String,
        initUserCryptoMethod: InitUserCryptoMethod,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult =
        flow {
            setVaultToUnlocking(userId = userId)
            emit(
                vaultSdkSource
                    .initializeCrypto(
                        userId = userId,
                        request = InitUserCryptoRequest(
                            kdfParams = kdf,
                            email = email,
                            privateKey = privateKey,
                            method = initUserCryptoMethod,
                        ),
                    )
                    .flatMap { result ->
                        // Initialize the SDK for organizations if necessary
                        if (organizationKeys != null &&
                            result is InitializeCryptoResult.Success
                        ) {
                            vaultSdkSource.initializeOrganizationCrypto(
                                userId = userId,
                                request = InitOrgCryptoRequest(
                                    organizationKeys = organizationKeys,
                                ),
                            )
                        } else {
                            result.asSuccess()
                        }
                    }
                    .fold(
                        onFailure = { VaultUnlockResult.GenericError },
                        onSuccess = { initializeCryptoResult ->
                            initializeCryptoResult
                                .toVaultUnlockResult()
                                .also {
                                    if (it is VaultUnlockResult.Success) {
                                        setVaultToUnlocked(userId = userId)
                                    }
                                }
                        },
                    ),
            )
        }
            .onCompletion { setVaultToNotUnlocking(userId = userId) }
            .first()

    private fun setVaultToUnlocked(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockedVaultUserIds = it.unlockedVaultUserIds + userId,
            )
        }
    }

    private fun setVaultToLocked(userId: String) {
        vaultSdkSource.clearCrypto(userId = userId)
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockedVaultUserIds = it.unlockedVaultUserIds - userId,
            )
        }
    }

    private fun setVaultToUnlocking(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockingVaultUserIds = it.unlockingVaultUserIds + userId,
            )
        }
    }

    private fun setVaultToNotUnlocking(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockingVaultUserIds = it.unlockingVaultUserIds - userId,
            )
        }
    }
}
