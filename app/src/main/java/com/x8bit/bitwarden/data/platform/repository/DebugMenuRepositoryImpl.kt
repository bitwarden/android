package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.getFlagValueOrDefault
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

/**
 * Default implementation of the [DebugMenuRepository]
 */
class DebugMenuRepositoryImpl(
    private val featureFlagOverrideDiskSource: FeatureFlagOverrideDiskSource,
    private val serverConfigRepository: ServerConfigRepository,
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
) : DebugMenuRepository {

    private val mutableOverridesUpdatedFlow = bufferedMutableSharedFlow<Unit>(replay = 1)
    override val featureFlagOverridesUpdatedFlow: Flow<Unit> = mutableOverridesUpdatedFlow
        .onSubscription { emit(Unit) }

    override val isDebugMenuEnabled: Boolean
        get() = BuildConfig.HAS_DEBUG_MENU

    override fun <T : Any> updateFeatureFlag(key: FlagKey<T>, value: T) {
        featureFlagOverrideDiskSource.saveFeatureFlag(key = key, value = value)
        mutableOverridesUpdatedFlow.tryEmit(Unit)
    }

    override fun <T : Any> getFeatureFlag(key: FlagKey<T>): T? =
        featureFlagOverrideDiskSource.getFeatureFlag(
            key = key,
        )

    override fun resetFeatureFlagOverrides() {
        val currentServerConfig = serverConfigRepository.serverConfigStateFlow.value
        FlagKey.activeFlags.forEach { flagKey ->
            updateFeatureFlag(
                flagKey,
                currentServerConfig.getFlagValueOrDefault(flagKey),
            )
        }
    }

    override fun resetOnboardingStatusForCurrentUser() {
        val currentUserId = authDiskSource.userState?.activeUserId ?: return
        authDiskSource.storeOnboardingStatus(
            userId = currentUserId,
            onboardingStatus = OnboardingStatus.NOT_STARTED,
        )
    }

    override fun modifyStateToShowOnboardingCarousel(
        userStateUpdateTrigger: () -> Unit,
    ) {
        settingsDiskSource.hasUserLoggedInOrCreatedAccount = false
        userStateUpdateTrigger.invoke()
    }
}
