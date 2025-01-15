package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import java.time.Instant

/**
 * Primary implementation of [AppResumeManager].
 */
class AppResumeManagerImpl(
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val authRepository: AuthRepository,
    private val vaultLockManager: VaultLockManager,
) : AppResumeManager {
    private companion object {
        // 5 minutes
        private const val UNLOCK_NAVIGATION_TIME: Long = 5 * 60
    }

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
        val timeNowMinus5Min = Instant.now().minusSeconds(UNLOCK_NAVIGATION_TIME)
        val lastLockTimestamp = Instant.ofEpochMilli(
            authDiskSource.getLastLockTimestamp(
                userId = authRepository.activeUserId ?: "",
            ),
        )
        if (timeNowMinus5Min.isAfter(lastLockTimestamp)) {
            return null
        }
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
        if (vaultLockManager.isVaultUnlocked(userId = authRepository.activeUserId ?: "")) {
            authRepository.activeUserId?.let {
                settingsDiskSource.storeAppResumeScreen(
                    userId = it,
                    screenData = null,
                )
            }
        }
    }
}
