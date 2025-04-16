package com.x8bit.bitwarden.data.platform.datasource.disk.util

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant

/**
 * Fake, memory-based implementation of [SettingsDiskSource].
 */
class FakeSettingsDiskSource : SettingsDiskSource {

    private val mutableAppLanguageFlow = bufferedMutableSharedFlow<AppLanguage?>(replay = 1)

    private val mutableAppThemeFlow = bufferedMutableSharedFlow<AppTheme>(replay = 1)

    private val mutableScreenCaptureAllowedFlow = bufferedMutableSharedFlow<Boolean?>(replay = 1)

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

    private val mutableShouldShowAddLoginCoachMarkFlow = bufferedMutableSharedFlow<Boolean?>()

    private val mutableShouldShowGeneratorCoachMarkFlow =
        bufferedMutableSharedFlow<Boolean?>()

    private val mutableFlightRecorderDataFlow =
        bufferedMutableSharedFlow<FlightRecorderDataSet?>(replay = 1)

    private var storedAppLanguage: AppLanguage? = null
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
    private var storedScreenCaptureAllowed: Boolean? = null
    private var storedSystemBiometricIntegritySource: String? = null
    private val storedAccountBiometricIntegrityValidity = mutableMapOf<String, Boolean?>()
    private val storedAppResumeScreenData = mutableMapOf<String, String?>()
    private val userSignIns = mutableMapOf<String, Boolean>()
    private val userShowAutoFillBadge = mutableMapOf<String, Boolean?>()
    private val userShowUnlockBadge = mutableMapOf<String, Boolean?>()
    private val userShowImportLoginsBadge = mutableMapOf<String, Boolean?>()
    private val vaultRegisteredForExport = mutableMapOf<String, Boolean?>()
    private var addCipherActionCount: Int? = null
    private var generatedActionCount: Int? = null
    private var createSendActionCount: Int? = null
    private var hasSeenAddLoginCoachMark: Boolean? = null
    private var hasSeenGeneratorCoachMark: Boolean? = null
    private var storedFlightRecorderData: FlightRecorderDataSet? = null
    private var storedIsDynamicColorsEnabled: Boolean? = null

    private val mutableShowAutoFillSettingBadgeFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableShowUnlockSettingBadgeFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableShowImportLoginsSettingBadgeFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableVaultRegisteredForExportFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableIsDynamicColorsEnabled =
        bufferedMutableSharedFlow<Boolean?>()

    override var appLanguage: AppLanguage?
        get() = storedAppLanguage
        set(value) {
            storedAppLanguage = value
            mutableAppLanguageFlow.tryEmit(value)
        }

    override val appLanguageFlow: Flow<AppLanguage?>
        get() = mutableAppLanguageFlow.onSubscription { emit(appLanguage) }

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

    override var isDynamicColorsEnabled: Boolean?
        get() = storedIsDynamicColorsEnabled
        set(value) {
            storedIsDynamicColorsEnabled = value
            mutableIsDynamicColorsEnabled.tryEmit(value)
        }

    override val isDynamicColorsEnabledFlow: Flow<Boolean?>
        get() = mutableIsDynamicColorsEnabled.onSubscription {
            emit(isDynamicColorsEnabled)
        }

    override var screenCaptureAllowed: Boolean?
        get() = storedScreenCaptureAllowed
        set(value) {
            storedScreenCaptureAllowed = value
            mutableScreenCaptureAllowedFlow.tryEmit(value)
        }

    override val screenCaptureAllowedFlow: Flow<Boolean?>
        get() = mutableScreenCaptureAllowedFlow.onSubscription { emit(screenCaptureAllowed) }

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

    override var flightRecorderData: FlightRecorderDataSet?
        get() = storedFlightRecorderData
        set(value) {
            storedFlightRecorderData = value
            mutableFlightRecorderDataFlow.tryEmit(value)
        }

    override val flightRecorderDataFlow: Flow<FlightRecorderDataSet?>
        get() = mutableFlightRecorderDataFlow
            .onSubscription { emit(storedFlightRecorderData) }

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
        mutableVaultRegisteredForExportFlowMap.remove(userId)
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

    override fun getShowImportLoginsSettingBadge(userId: String): Boolean? =
        userShowImportLoginsBadge[userId]

    override fun storeShowImportLoginsSettingBadge(userId: String, showBadge: Boolean?) {
        userShowImportLoginsBadge[userId] = showBadge
        getMutableShowImportLoginsSettingBadgeFlow(userId).tryEmit(showBadge)
    }

    override fun getShowImportLoginsSettingBadgeFlow(userId: String): Flow<Boolean?> =
        getMutableShowImportLoginsSettingBadgeFlow(userId = userId).onSubscription {
            emit(getShowImportLoginsSettingBadge(userId = userId))
        }

    override fun getVaultRegisteredForExport(userId: String): Boolean =
        vaultRegisteredForExport[userId] ?: false

    override fun storeVaultRegisteredForExport(userId: String, registered: Boolean?) {
        vaultRegisteredForExport[userId] = registered
        getMutableVaultRegisteredForExportFlow(userId = userId).tryEmit(registered)
    }

    override fun getVaultRegisteredForExportFlow(userId: String): Flow<Boolean?> =
        getMutableVaultRegisteredForExportFlow(userId = userId).onSubscription {
            emit(getVaultRegisteredForExport(userId = userId))
        }

    override fun getAddCipherActionCount(): Int? {
        return addCipherActionCount
    }

    override fun storeAddCipherActionCount(count: Int?) {
        addCipherActionCount = count
    }

    override fun getGeneratedResultActionCount(): Int? {
        return generatedActionCount
    }

    override fun storeGeneratedResultActionCount(count: Int?) {
        generatedActionCount = count
    }

    override fun getCreateSendActionCount(): Int? {
        return createSendActionCount
    }

    override fun storeCreateSendActionCount(count: Int?) {
        createSendActionCount = count
    }

    override fun getShouldShowAddLoginCoachMark(): Boolean? {
        return hasSeenAddLoginCoachMark
    }

    override fun storeShouldShowAddLoginCoachMark(shouldShow: Boolean?) {
        hasSeenAddLoginCoachMark = shouldShow
        mutableShouldShowAddLoginCoachMarkFlow.tryEmit(shouldShow)
    }

    override fun getShouldShowAddLoginCoachMarkFlow(): Flow<Boolean?> =
        mutableShouldShowAddLoginCoachMarkFlow.onSubscription {
            emit(getShouldShowAddLoginCoachMark())
        }

    override fun getShouldShowGeneratorCoachMark(): Boolean? =
        hasSeenGeneratorCoachMark

    override fun storeShouldShowGeneratorCoachMark(shouldShow: Boolean?) {
        hasSeenGeneratorCoachMark = shouldShow
        mutableShouldShowGeneratorCoachMarkFlow.tryEmit(shouldShow)
    }

    override fun getShouldShowGeneratorCoachMarkFlow(): Flow<Boolean?> =
        mutableShouldShowGeneratorCoachMarkFlow.onSubscription {
            emit(hasSeenGeneratorCoachMark)
        }

    override fun storeAppResumeScreen(userId: String, screenData: AppResumeScreenData?) {
        storedAppResumeScreenData[userId] = screenData.let { Json.encodeToString(it) }
    }

    override fun getAppResumeScreen(userId: String): AppResumeScreenData? {
        return storedAppResumeScreenData[userId]?.let { Json.decodeFromStringOrNull(it) }
    }

    /**
     * Asserts that the stored [FlightRecorderDataSet] matches the [expected] one.
     */
    fun assertFlightRecorderData(expected: FlightRecorderDataSet) {
        assertEquals(expected, storedFlightRecorderData)
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

    private fun getMutableShowAutoFillSettingBadgeFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> = mutableShowAutoFillSettingBadgeFlowMap.getOrPut(userId) {
        bufferedMutableSharedFlow(replay = 1)
    }

    private fun getMutableShowUnlockSettingBadgeFlow(userId: String): MutableSharedFlow<Boolean?> =
        mutableShowUnlockSettingBadgeFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableShowImportLoginsSettingBadgeFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> = mutableShowImportLoginsSettingBadgeFlowMap.getOrPut(userId) {
        bufferedMutableSharedFlow(replay = 1)
    }

    private fun getMutableVaultRegisteredForExportFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> =
        mutableVaultRegisteredForExportFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    //endregion Private helper functions
}
