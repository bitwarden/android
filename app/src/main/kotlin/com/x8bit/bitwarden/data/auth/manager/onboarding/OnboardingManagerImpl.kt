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

    /**
     * The setup steps in the order they are shown, each with the live-state check that decides
     * whether the step is still needed.
     */
    private val setupSteps: List<SetupStep> = listOf(
        SetupStep(OnboardingStatus.ACCOUNT_LOCK_SETUP) { userId ->
            biometricsSupportManager.isBiometricsSupported(isDeviceCredentialAllowed = false) &&
                authDiskSource.getUserBiometricUnlockKey(userId = userId) == null
        },
        SetupStep(OnboardingStatus.AUTOFILL_SETUP) {
            !settingsRepository.isAutofillEnabledStateFlow.value
        },
        SetupStep(OnboardingStatus.BROWSER_AUTOFILL_SETUP) {
            settingsRepository.isAutofillEnabledStateFlow.value &&
                browserThirdPartyAutofillEnabledManager
                    .browserThirdPartyAutofillStatus
                    .isDefaultBrowserAvailableAndDisabled
        },
    )

    override fun goToNextStep() {
        val userId = activeUserId ?: return
        val nextStatus = when (val current = authDiskSource.getOnboardingStatus(userId = userId)) {
            OnboardingStatus.COMPLETE -> return
            OnboardingStatus.FINAL_STEP -> OnboardingStatus.COMPLETE
            else -> getNextStatus(userId = userId, current = current)
        }
        authDiskSource.storeOnboardingStatus(userId = userId, onboardingStatus = nextStatus)
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
     * The first step after [current] that is still needed. Only later steps are considered, so
     * skipping a step never sends the user back to it.
     */
    private fun getNextStatus(userId: String, current: OnboardingStatus?): OnboardingStatus {
        // A null (or unlisted) current status gives -1 + 1, which starts at the first step.
        val startIndex = setupSteps.indexOfFirst { it.status == current } + 1
        return setupSteps
            .drop(n = startIndex)
            .firstOrNull { it.isNeeded(userId) }
            ?.status
            ?: if (current == null) OnboardingStatus.COMPLETE else OnboardingStatus.FINAL_STEP
    }

    private class SetupStep(
        val status: OnboardingStatus,
        val isNeeded: (userId: String) -> Boolean,
    )
}
