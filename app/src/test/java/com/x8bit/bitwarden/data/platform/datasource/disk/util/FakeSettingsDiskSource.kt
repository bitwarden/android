package com.x8bit.bitwarden.data.platform.datasource.disk.util

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
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

    private val mutableIsCrashLoggingEnabled =
        bufferedMutableSharedFlow<Boolean?>()

    private val mutableHasUserLoggedInOrCreatedAccount =
        bufferedMutableSharedFlow<Boolean?>()

    private val mutableScreenCaptureAllowedFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private var storedAppTheme: AppTheme = AppTheme.DEFAULT
    private val storedLastSyncTime = mutableMapOf<String, Instant?>()
    private val storedVaultTimeoutActions = mutableMapOf<String, VaultTimeoutAction?>()
    private val storedVaultTimeoutInMinutes = mutableMapOf<String, Int?>()
    private val storedUriMatchTypes = mutableMapOf<String, UriMatchType?>()
    private val storedClearClipboardFrequency = mutableMapOf<String, Int?>()
    private val storedDisableAutoTotpCopy = mutableMapOf<String, Boolean?>()
    private val storedDisableAutofillSavePrompt = mutableMapOf<String, Boolean?>()
    private val storedPullToRefreshEnabled = mutableMapOf<String, Boolean?>()
    private val storedInlineAutofillEnabled = mutableMapOf<String, Boolean?>()
    private val storedBlockedAutofillUris = mutableMapOf<String, List<String>?>()
    private var storedIsIconLoadingDisabled: Boolean? = null
    private var storedIsCrashLoggingEnabled: Boolean? = null
    private var storedHasUserLoggedInOrCreatedAccount: Boolean? = null
    private var storedInitialAutofillDialogShown: Boolean? = null
    private val storedScreenCaptureAllowed = mutableMapOf<String, Boolean?>()
    private var storedSystemBiometricIntegritySource: String? = null
    private val storedAccountBiometricIntegrityValidity = mutableMapOf<String, Boolean?>()
    private val userSignIns = mutableMapOf<String, Boolean>()
    private val userShowAutoFillBadge = mutableMapOf<String, Boolean?>()
    private val userShowUnlockBadge = mutableMapOf<String, Boolean?>()

    private val mutableShowAutoFillSettingBadgeFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableShowUnlockSettingBadgeFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

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

    override var systemBiometricIntegritySource: String?
        get() = storedSystemBiometricIntegritySource
        set(value) {
            storedSystemBiometricIntegritySource = value
        }

    override var isIconLoadingDisabled: Boolean?
        get() = storedIsIconLoadingDisabled
        set(value) {
            storedIsIconLoadingDisabled = value
            mutableIsIconLoadingDisabled.tryEmit(value)
        }

    override var initialAutofillDialogShown: Boolean?
        get() = storedInitialAutofillDialogShown
        set(value) {
            storedInitialAutofillDialogShown = value
        }

    override val isIconLoadingDisabledFlow: Flow<Boolean?>
        get() = mutableIsIconLoadingDisabled.onSubscription {
            emit(isIconLoadingDisabled)
        }

    override var isCrashLoggingEnabled: Boolean?
        get() = storedIsCrashLoggingEnabled
        set(value) {
            storedIsCrashLoggingEnabled = value
            mutableIsCrashLoggingEnabled.tryEmit(value)
        }

    override val isCrashLoggingEnabledFlow: Flow<Boolean?>
        get() = mutableIsCrashLoggingEnabled.onSubscription {
            emit(isCrashLoggingEnabled)
        }

    override var hasUserLoggedInOrCreatedAccount: Boolean?
        get() = storedHasUserLoggedInOrCreatedAccount
        set(value) {
            storedHasUserLoggedInOrCreatedAccount = value
            mutableHasUserLoggedInOrCreatedAccount.tryEmit(value)
        }

    override val hasUserLoggedInOrCreatedAccountFlow: Flow<Boolean?>
        get() = mutableHasUserLoggedInOrCreatedAccount.onSubscription {
            emit(hasUserLoggedInOrCreatedAccount)
        }

    override fun getAccountBiometricIntegrityValidity(
        userId: String,
        systemBioIntegrityState: String,
    ): Boolean? = storedAccountBiometricIntegrityValidity["${userId}_$systemBioIntegrityState"]

    override fun storeAccountBiometricIntegrityValidity(
        userId: String,
        systemBioIntegrityState: String,
        value: Boolean?,
    ) {
        storedAccountBiometricIntegrityValidity["${userId}_$systemBioIntegrityState"] = value
    }

    override fun clearData(userId: String) {
        storedVaultTimeoutActions.remove(userId)
        storedVaultTimeoutInMinutes.remove(userId)
        storedUriMatchTypes.remove(userId)
        storedDisableAutoTotpCopy.remove(userId)
        storedDisableAutofillSavePrompt.remove(userId)
        storedPullToRefreshEnabled.remove(userId)
        storedInlineAutofillEnabled.remove(userId)
        storedBlockedAutofillUris.remove(userId)
        storedClearClipboardFrequency.remove(userId)

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

    override fun getClearClipboardFrequencySeconds(userId: String): Int? =
        storedClearClipboardFrequency[userId]

    override fun storeClearClipboardFrequencySeconds(userId: String, frequency: Int?) {
        storedClearClipboardFrequency[userId] = frequency
    }

    override fun getDefaultUriMatchType(userId: String): UriMatchType? =
        storedUriMatchTypes[userId]

    override fun storeDefaultUriMatchType(
        userId: String,
        uriMatchType: UriMatchType?,
    ) {
        storedUriMatchTypes[userId] = uriMatchType
    }

    override fun getAutoCopyTotpDisabled(userId: String): Boolean? =
        storedDisableAutoTotpCopy[userId]

    override fun storeAutoCopyTotpDisabled(
        userId: String,
        isAutomaticallyCopyTotpDisabled: Boolean?,
    ) {
        storedDisableAutoTotpCopy[userId] = isAutomaticallyCopyTotpDisabled
    }

    override fun getAutofillSavePromptDisabled(userId: String): Boolean? =
        storedDisableAutofillSavePrompt[userId]

    override fun storeAutofillSavePromptDisabled(
        userId: String,
        isAutofillSavePromptDisabled: Boolean?,
    ) {
        storedDisableAutofillSavePrompt[userId] = isAutofillSavePromptDisabled
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

    override fun getScreenCaptureAllowed(userId: String): Boolean? =
        storedScreenCaptureAllowed[userId]

    override fun getScreenCaptureAllowedFlow(userId: String): Flow<Boolean?> {
        return getMutableScreenCaptureAllowedFlow(userId)
    }

    override fun storeScreenCaptureAllowed(
        userId: String,
        isScreenCaptureAllowed: Boolean?,
    ) {
        storedScreenCaptureAllowed[userId] = isScreenCaptureAllowed
        getMutableScreenCaptureAllowedFlow(userId).tryEmit(isScreenCaptureAllowed)
    }

    override fun storeUseHasLoggedInPreviously(userId: String) {
        userSignIns[userId] = true
    }

    override fun getUserHasSignedInPreviously(userId: String): Boolean = userSignIns[userId] == true

    override fun getShowAutoFillSettingBadge(userId: String): Boolean? =
        userShowAutoFillBadge[userId]

    override fun storeShowAutoFillSettingBadge(userId: String, showBadge: Boolean?) {
        userShowAutoFillBadge[userId] = showBadge
        getMutableShowAutoFillSettingBadgeFlow(userId).tryEmit(showBadge)
    }

    override fun getShowAutoFillSettingBadgeFlow(userId: String): Flow<Boolean?> =
        getMutableShowAutoFillSettingBadgeFlow(userId = userId).onSubscription {
            emit(getShowAutoFillSettingBadge(userId = userId))
        }

    override fun getShowUnlockSettingBadge(userId: String): Boolean? =
        userShowUnlockBadge[userId]

    override fun storeShowUnlockSettingBadge(userId: String, showBadge: Boolean?) {
        userShowUnlockBadge[userId] = showBadge
        getMutableShowUnlockSettingBadgeFlow(userId).tryEmit(showBadge)
    }

    override fun getShowUnlockSettingBadgeFlow(userId: String): Flow<Boolean?> =
        getMutableShowUnlockSettingBadgeFlow(userId = userId).onSubscription {
            emit(getShowUnlockSettingBadge(userId = userId))
        }

    //region Private helper functions
    private fun getMutableScreenCaptureAllowedFlow(userId: String): MutableSharedFlow<Boolean?> {
        return mutableScreenCaptureAllowedFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }
    }

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

    private fun getMutableShowAutoFillSettingBadgeFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> = mutableShowAutoFillSettingBadgeFlowMap.getOrPut(userId) {
        bufferedMutableSharedFlow(replay = 1)
    }

    private fun getMutableShowUnlockSettingBadgeFlow(userId: String): MutableSharedFlow<Boolean?> =
        mutableShowUnlockSettingBadgeFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    //endregion Private helper functions
}
