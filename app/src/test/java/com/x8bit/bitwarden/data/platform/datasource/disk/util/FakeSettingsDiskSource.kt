package com.x8bit.bitwarden.data.platform.datasource.disk.util

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

/**
 * Fake, memory-based implementation of [SettingsDiskSource].
 */
class FakeSettingsDiskSource : SettingsDiskSource {

    private val mutableVaultTimeoutActionsFlowMap =
        mutableMapOf<String, MutableSharedFlow<VaultTimeoutAction?>>()

    private val mutableVaultTimeoutInMinutesFlowMap =
        mutableMapOf<String, MutableSharedFlow<Int?>>()

    private val mutablePullToRefreshEnabledFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val storedVaultTimeoutActions = mutableMapOf<String, VaultTimeoutAction?>()
    private val storedVaultTimeoutInMinutes = mutableMapOf<String, Int?>()

    private val storedPullToRefreshEnabled = mutableMapOf<String, Boolean?>()

    override var appLanguage: AppLanguage? = null

    override var isIconLoadingDisabled: Boolean? = null

    override fun clearData(userId: String) {
        storedVaultTimeoutActions.remove(userId)
        storedVaultTimeoutInMinutes.remove(userId)

        mutableVaultTimeoutActionsFlowMap.remove(userId)
        mutableVaultTimeoutInMinutesFlowMap.remove(userId)
    }

    override fun getVaultTimeoutInMinutes(userId: String): Int? =
        storedVaultTimeoutInMinutes[userId]

    override fun getVaultTimeoutInMinutesFlow(userId: String): Flow<Int?> =
        getMutableVaultTimeoutInMinutesFlow(userId = userId)
            .onSubscription { emit(getVaultTimeoutInMinutes(userId = userId)) }

    override fun storeVaultTimeoutInMinutes(
        userId: String,
        vaultTimeoutInMinutes: Int?,
    ) {
        storedVaultTimeoutInMinutes[userId] = vaultTimeoutInMinutes
        getMutableVaultTimeoutInMinutesFlow(userId = userId).tryEmit(vaultTimeoutInMinutes)
    }

    override fun getVaultTimeoutAction(userId: String): VaultTimeoutAction? =
        storedVaultTimeoutActions[userId]

    override fun getVaultTimeoutActionFlow(userId: String): Flow<VaultTimeoutAction?> =
        getMutableVaultTimeoutActionsFlow(userId = userId)
            .onSubscription { emit(getVaultTimeoutAction(userId = userId)) }

    override fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    ) {
        storedVaultTimeoutActions[userId] = vaultTimeoutAction
        getMutableVaultTimeoutActionsFlow(userId = userId).tryEmit(vaultTimeoutAction)
    }

    override fun getPullToRefreshEnabled(userId: String): Boolean? =
        storedPullToRefreshEnabled[userId]

    override fun getPullToRefreshEnabledFlow(userId: String): Flow<Boolean?> =
        getMutablePullToRefreshEnabledFlow(userId = userId)
            .onSubscription { emit(getPullToRefreshEnabled(userId = userId)) }

    override fun storePullToRefreshEnabled(userId: String, isPullToRefreshEnabled: Boolean?) {
        storedPullToRefreshEnabled[userId] = isPullToRefreshEnabled
        getMutablePullToRefreshEnabledFlow(userId = userId).tryEmit(isPullToRefreshEnabled)
    }

    //region Private helper functions

    private fun getMutableVaultTimeoutActionsFlow(
        userId: String,
    ): MutableSharedFlow<VaultTimeoutAction?> =
        mutableVaultTimeoutActionsFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableVaultTimeoutInMinutesFlow(
        userId: String,
    ): MutableSharedFlow<Int?> =
        mutableVaultTimeoutInMinutesFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutablePullToRefreshEnabledFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> =
        mutablePullToRefreshEnabledFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    //endregion Private helper functions
}
