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
        when (authDiskSource.getOnboardingStatus(userId = userId)) {
            OnboardingStatus.ACCOUNT_LOCK_SETUP -> {
                authDiskSource.storeOnboardingStatus(
                    userId = userId,
                    onboardingStatus = getOnboardingStatusAfterUnlockSetup(),
                )
            }

            OnboardingStatus.AUTOFILL_SETUP -> {
                authDiskSource.storeOnboardingStatus(
                    userId = userId,
                    onboardingStatus = getOnboardingStatusAfterAutofillSetup(),
                )
            }

            OnboardingStatus.BROWSER_AUTOFILL_SETUP -> {
                // Always go straight to the final step.
                authDiskSource.storeOnboardingStatus(
                    userId = userId,
                    onboardingStatus = OnboardingStatus.FINAL_STEP,
                )
            }

            OnboardingStatus.FINAL_STEP -> {
                // Always go straight to completion.
                authDiskSource.storeOnboardingStatus(
                    userId = userId,
                    onboardingStatus = OnboardingStatus.COMPLETE,
                )
            }

            OnboardingStatus.COMPLETE -> {
                // Don't do anything, we're done!
            }

            null -> {
                // Let's set do the full setup.
                authDiskSource.storeOnboardingStatus(
                    userId = userId,
                    onboardingStatus = getInitialOnboardingStatus(userId = userId),
                )
            }
        }
    }

    override fun initializeOnboardingFlow(userId: String) {
        if (!featureFlagManager.getFeatureFlag(key = FlagKey.UpdatedOnboardingFlow)) return
        authDiskSource.getOnboardingStatus(userId = userId)?.let {
            // We already have a status, no need to change it.
            return
        }
        authDiskSource.storeOnboardingStatus(
            userId = userId,
            onboardingStatus = getInitialOnboardingStatus(userId = userId),
        )
    }

    override fun setOnboardingStatus(status: OnboardingStatus) {
        activeUserId?.let {
            authDiskSource.storeOnboardingStatus(userId = it, onboardingStatus = status)
        }
    }

    private fun getOnboardingStatusAfterUnlockSetup(): OnboardingStatus {
        val isAutofillSetup = settingsRepository.isAutofillEnabledStateFlow.value
        val isBrowserAutofillNotSetup = browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatus
            .isDefaultBrowserAvailableAndDisabled
        return when {
            !isAutofillSetup -> {
                // If Autofill is not configured, go to the autofill setup.
                OnboardingStatus.AUTOFILL_SETUP
            }

            isBrowserAutofillNotSetup -> {
                // If Autofill is configured but the default browser is not,
                // go to the autofill browser setup.
                OnboardingStatus.BROWSER_AUTOFILL_SETUP
            }

            else -> {
                // Otherwise we are done, go to the final step.
                OnboardingStatus.FINAL_STEP
            }
        }
    }

    private fun getOnboardingStatusAfterAutofillSetup(): OnboardingStatus {
        val isAutofillSetup = settingsRepository.isAutofillEnabledStateFlow.value
        val isBrowserAutofillNotSetup = browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatus
            .isDefaultBrowserAvailableAndDisabled
        return if (isAutofillSetup && isBrowserAutofillNotSetup) {
            // If Autofill is configured but the default browser is not,
            // go to the autofill browser setup.
            OnboardingStatus.BROWSER_AUTOFILL_SETUP
        } else {
            // Otherwise we are done, go to the final step.
            OnboardingStatus.FINAL_STEP
        }
    }

    private fun getInitialOnboardingStatus(userId: String): OnboardingStatus {
        val isBiometricsSupported = biometricsSupportManager.isBiometricsSupported(
            isDeviceCredentialAllowed = false,
        )
        val isBiometricsSetup = authDiskSource.getUserBiometricUnlockKey(userId = userId) != null
        val isAutofillSetup = settingsRepository.isAutofillEnabledStateFlow.value
        val isBrowserAutofillNotSetup = browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatus
            .isDefaultBrowserAvailableAndDisabled
        return when {
            isBiometricsSupported && !isBiometricsSetup -> OnboardingStatus.ACCOUNT_LOCK_SETUP
            !isAutofillSetup -> OnboardingStatus.AUTOFILL_SETUP
            isBrowserAutofillNotSetup -> OnboardingStatus.BROWSER_AUTOFILL_SETUP
            else -> OnboardingStatus.COMPLETE
        }
    }
}
