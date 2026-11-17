package com.x8bit.bitwarden.data.auth.manager.onboarding

import com.bitwarden.core.data.manager.biometrics.BiometricsSupportManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * The default implementation of the [OnboardingManager].
 */
internal class OnboardingManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val biometricsSupportManager: BiometricsSupportManager,
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
    private val settingsRepository: SettingsRepository,
    private val featureFlagManager: FeatureFlagManager,
) : OnboardingManager {

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override fun goToNextStep() {
        val userId = activeUserId ?: return
        authDiskSource.storeOnboardingStatus(
            userId = userId,
            onboardingStatus = getNextStatus(
                userId = userId,
                current = authDiskSource.getOnboardingStatus(userId = userId),
            ),
        )
    }

    override fun initializeOnboardingFlow(userId: String) {
        if (!featureFlagManager.getFeatureFlag(key = FlagKey.UpdatedOnboardingFlow)) return
        authDiskSource.getOnboardingStatus(userId = userId)?.let {
            // We already have a status, no need to change it.
            return
        }
        authDiskSource.storeOnboardingStatus(
            userId = userId,
            onboardingStatus = getNextStatus(userId = userId, current = null),
        )
    }

    override fun setOnboardingStatus(status: OnboardingStatus) {
        activeUserId?.let {
            authDiskSource.storeOnboardingStatus(userId = it, onboardingStatus = status)
        }
    }

    /**
     * The first step after [current] that is still needed, or [OnboardingStatus.COMPLETE] if there
     * are none. The steps run in the declaration order of [OnboardingStatus], and only steps after
     * [current] are considered so skipping a step never sends the user back to it.
     */
    private fun getNextStatus(userId: String, current: OnboardingStatus?): OnboardingStatus =
        OnboardingStatus
            .entries
            // A null current status starts from the first step.
            .filter { it.ordinal > (current?.ordinal ?: -1) }
            .firstOrNull { isNeeded(status = it, userId = userId, current = current) }
            ?: OnboardingStatus.COMPLETE

    /**
     * Whether the given [status] step is still needed based on the live state of the app.
     */
    private fun isNeeded(
        status: OnboardingStatus,
        userId: String,
        current: OnboardingStatus?,
    ): Boolean = when (status) {
        OnboardingStatus.ACCOUNT_LOCK_SETUP -> {
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false) &&
                authDiskSource.getUserBiometricUnlockKey(userId = userId) == null
        }

        OnboardingStatus.AUTOFILL_SETUP -> {
            !settingsRepository.isAutofillEnabledStateFlow.value
        }

        OnboardingStatus.BROWSER_AUTOFILL_SETUP -> {
            settingsRepository.isAutofillEnabledStateFlow.value &&
                browserThirdPartyAutofillEnabledManager
                    .browserThirdPartyAutofillStatus
                    .isDefaultBrowserAvailableAndDisabled
        }

        // Only show the final step after a real step, not when nothing needed setup.
        OnboardingStatus.FINAL_STEP -> current != null

        OnboardingStatus.COMPLETE -> true
    }
}
