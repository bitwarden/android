package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import java.time.Clock

private const val UNLOCK_NAVIGATION_TIME_SECONDS: Long = 5 * 60

/**
 * Primary implementation of [AppResumeManager].
 */
class AppResumeManagerImpl(
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val authRepository: AuthRepository,
    private val vaultLockManager: VaultLockManager,
    private val clock: Clock,
) : AppResumeManager {

    override fun setResumeScreen(screenData: AppResumeScreenData) {
        authRepository.activeUserId?.let {
            settingsDiskSource.storeAppResumeScreen(
                userId = it,
                screenData = screenData,
            )
        }
    }

    override fun getResumeScreen(): AppResumeScreenData? {
        return authRepository.activeUserId?.let { userId ->
            settingsDiskSource.getAppResumeScreen(userId)
        }
    }

    override fun getResumeSpecialCircumstance(): SpecialCircumstance? {
        val userId = authRepository.activeUserId ?: return null
        val timeNowMinus5Min = clock.instant().minusSeconds(UNLOCK_NAVIGATION_TIME_SECONDS)
        val lastLockTimestamp = authDiskSource
            .getLastLockTimestamp(userId = userId)
            ?: return null

        if (timeNowMinus5Min.isAfter(lastLockTimestamp)) {
            settingsDiskSource.storeAppResumeScreen(userId = userId, screenData = null)
            return null
        }
        return when (val resumeScreenData = getResumeScreen()) {
            AppResumeScreenData.GeneratorScreen -> SpecialCircumstance.GeneratorShortcut
            AppResumeScreenData.SendScreen -> SpecialCircumstance.SendShortcut
            is AppResumeScreenData.SearchScreen -> SpecialCircumstance.SearchShortcut(
                searchTerm = resumeScreenData.searchTerm,
            )

            AppResumeScreenData.VerificationCodeScreen -> {
                SpecialCircumstance.VerificationCodeShortcut
            }

            else -> null
        }
    }

    override fun clearResumeScreen() {
        val userId = authRepository.activeUserId ?: return
        if (vaultLockManager.isVaultUnlocked(userId = userId)) {
            settingsDiskSource.storeAppResumeScreen(
                userId = userId,
                screenData = null,
            )
        }
    }
}
