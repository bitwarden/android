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
    private fun getNextStatus(
        userId: String,
        current: OnboardingStatus?,
    ): OnboardingStatus = OnboardingStatus
        .entries
        .filter {
            // If the current state is null, we still need to do all the steps. Otherwise,
            // keep any step that is after the current one.
            current == null || it > current
        }
        .firstOrNull { it.isNeeded(userId = userId, current = current) }
        ?: OnboardingStatus.COMPLETE

    /**
     * Whether the given step is still needed based on the live state of the app.
     */
    private fun OnboardingStatus.isNeeded(
        userId: String,
        current: OnboardingStatus?,
    ): Boolean = when (this) {
        OnboardingStatus.ACCOUNT_LOCK_SETUP -> isAccountLockSetupNeeded(userId = userId)
        OnboardingStatus.AUTOFILL_SETUP -> isAutofillSetupNeeded()
        OnboardingStatus.BROWSER_AUTOFILL_SETUP -> isBrowserAutofillSetupNeeded()

        // Only show the final step after a real step was shown and every setup step is now done.
        // If the user skipped a step that is still needed, go straight to complete.
        OnboardingStatus.FINAL_STEP -> {
            current != null &&
                !isAccountLockSetupNeeded(userId = userId) &&
                !isAutofillSetupNeeded() &&
                !isBrowserAutofillSetupNeeded()
        }

        OnboardingStatus.COMPLETE -> true
    }

    private fun isAccountLockSetupNeeded(userId: String): Boolean =
        biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false) &&
            authDiskSource.getUserBiometricUnlockKey(userId = userId) == null

    private fun isAutofillSetupNeeded(): Boolean =
        !settingsRepository.isAutofillEnabledStateFlow.value

    private fun isBrowserAutofillSetupNeeded(): Boolean =
        settingsRepository.isAutofillEnabledStateFlow.value &&
            browserThirdPartyAutofillEnabledManager
                .browserThirdPartyAutofillStatus
                .isDefaultBrowserAvailableAndDisabled
}
