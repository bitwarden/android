package com.x8bit.bitwarden.data.platform.datasource.disk.util

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import java.time.Instant

/**
 * Fake, memory-based implementation of [SettingsDiskSource].
 */
class FakeSettingsDiskSource : SettingsDiskSource {

    private val mutableAppThemeFlow =
        bufferedMutableSharedFlow<AppTheme>(replay = 1)

    private val mutableLastSyncCallFlowMap = mutableMapOf<String, MutableSharedFlow<Instant?>>()

    private val mutableVaultTimeoutActionsFlowMap =
        mutableMapOf<String, MutableSharedFlow<VaultTimeoutAction?>>()

    private val mutableVaultTimeoutInMinutesFlowMap =
        mutableMapOf<String, MutableSharedFlow<Int?>>()

    private val mutablePullToRefreshEnabledFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableIsIconLoadingDisabled =
        bufferedMutableSharedFlow<Boolean?>()

    private var storedAppTheme: AppTheme = AppTheme.DEFAULT
    private val storedLastSyncTime = mutableMapOf<String, Instant?>()
    private val storedVaultTimeoutActions = mutableMapOf<String, VaultTimeoutAction?>()
    private val storedVaultTimeoutInMinutes = mutableMapOf<String, Int?>()

    private val storedPullToRefreshEnabled = mutableMapOf<String, Boolean?>()
    private val storedInlineAutofillEnabled = mutableMapOf<String, Boolean?>()
    private val storedBlockedAutofillUris = mutableMapOf<String, List<String>?>()

    private var storedIsIconLoadingDisabled: Boolean? = null

    private val storedApprovePasswordLoginsEnabled = mutableMapOf<String, Boolean?>()

    override var appLanguage: AppLanguage? = null

    override var appTheme: AppTheme
        get() = storedAppTheme
        set(value) {
            storedAppTheme = value
            mutableAppThemeFlow.tryEmit(value)
        }

    override val appThemeFlow: Flow<AppTheme>
        get() = mutableAppThemeFlow.onSubscription {
            emit(appTheme)
        }

    override var isIconLoadingDisabled: Boolean?
        get() = storedIsIconLoadingDisabled
        set(value) {
            storedIsIconLoadingDisabled = value
            mutableIsIconLoadingDisabled.tryEmit(value)
        }

    override val isIconLoadingDisabledFlow: Flow<Boolean?>
        get() = mutableIsIconLoadingDisabled.onSubscription {
            emit(isIconLoadingDisabled)
        }

    override fun clearData(userId: String) {
        storedVaultTimeoutActions.remove(userId)
        storedVaultTimeoutInMinutes.remove(userId)
        storedPullToRefreshEnabled.remove(userId)
        storedInlineAutofillEnabled.remove(userId)
        storedBlockedAutofillUris.remove(userId)

        mutableVaultTimeoutActionsFlowMap.remove(userId)
        mutableVaultTimeoutInMinutesFlowMap.remove(userId)
        mutableLastSyncCallFlowMap.remove(userId)
    }

    override fun getLastSyncTime(userId: String): Instant? = storedLastSyncTime[userId]

    override fun getLastSyncTimeFlow(userId: String): Flow<Instant?> =
        getMutableLastSyncTimeFlow(userId = userId)
            .onSubscription { emit(getLastSyncTime(userId = userId)) }

    override fun storeLastSyncTime(userId: String, lastSyncTime: Instant?) {
        storedLastSyncTime[userId] = lastSyncTime
        getMutableLastSyncTimeFlow(userId = userId).tryEmit(lastSyncTime)
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

    override fun getInlineAutofillEnabled(userId: String): Boolean? =
        storedInlineAutofillEnabled[userId]

    override fun storeInlineAutofillEnabled(
        userId: String,
        isInlineAutofillEnabled: Boolean?,
    ) {
        storedInlineAutofillEnabled[userId] = isInlineAutofillEnabled
    }

    override fun getBlockedAutofillUris(userId: String): List<String>? =
        storedBlockedAutofillUris[userId]

    override fun storeBlockedAutofillUris(
        userId: String,
        blockedAutofillUris: List<String>?,
    ) {
        storedBlockedAutofillUris[userId] = blockedAutofillUris
    }

    override fun getApprovePasswordlessLoginsEnabled(userId: String): Boolean? =
        storedApprovePasswordLoginsEnabled[userId]

    override fun storeApprovePasswordlessLoginsEnabled(
        userId: String,
        isApprovePasswordlessLoginsEnabled: Boolean?,
    ) {
        storedApprovePasswordLoginsEnabled[userId] = isApprovePasswordlessLoginsEnabled
    }

    //region Private helper functions

    private fun getMutableLastSyncTimeFlow(
        userId: String,
    ): MutableSharedFlow<Instant?> =
        mutableLastSyncCallFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

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
