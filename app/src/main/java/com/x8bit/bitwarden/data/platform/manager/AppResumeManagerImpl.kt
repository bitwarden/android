package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Primary implementation of [AppResumeManager].
 */
class AppResumeManagerImpl(
    val settingsDiskSource: SettingsDiskSource,
    private val authRepository: AuthRepository,
    private val appStateManager: AppStateManager,
    dispatcherManager: DispatcherManager,
) : AppResumeManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override fun setResumeScreen(screenData: AppResumeScreenData) {
        authRepository.activeUserId?.let {
            settingsDiskSource.storeAppResumeScreen(
                it,
                Json.encodeToString(screenData),
            )
        }
    }

    override fun getResumeScreen(): AppResumeScreenData? {
        val data =
            authRepository.activeUserId?.let { settingsDiskSource.getAppResumeScreen(it) } ?: ""

        return Json.decodeFromStringOrNull<AppResumeScreenData>(data)
    }

    override fun getResumeSpecialCircumstance(): SpecialCircumstance? {
        return when (val resumeScreenData = getResumeScreen()) {
            AppResumeScreenData.GeneratorScreen -> SpecialCircumstance.GeneratorShortcut
            AppResumeScreenData.SendScreen -> SpecialCircumstance.SendShortcut
            is AppResumeScreenData.SearchScreen -> SpecialCircumstance.SearchShortcut(
                resumeScreenData.searchTerm,
            )

            AppResumeScreenData.VerificationCodeScreen ->
                SpecialCircumstance.VerificationCodeShortcut

            else -> null
        }
    }

    @Suppress("MagicNumber")
    override fun clearResumeScreen() {
        unconfinedScope.launch {
            // We must ensure that we wait enough for the AppForegroundState to be updated
            // before we check it state
            delay(500)
            if (appStateManager.appForegroundStateFlow.value == AppForegroundState.FOREGROUNDED) {
                authRepository.activeUserId?.let {
                    settingsDiskSource.storeAppResumeScreen(
                        it,
                        null,
                    )
                }
            }
        }
    }
}
