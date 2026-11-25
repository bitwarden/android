package com.x8bit.bitwarden.data.auth.manager.onboarding

import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus

/**
 * A manager class for handling the onboarding process.
 */
interface OnboardingManager {
    /**
     * Updates the onboarding status for the active user based on what is already configured and
     * what the current onboarding state is.
     */
    fun goToNextStep()

    /**
     * Initializes the onboarding status for the given [userId] based on what is already configured.
     */
    fun initializeOnboardingFlow(userId: String)

    /**
     * Update the value of the onboarding status for the active user.
     */
    fun setOnboardingStatus(status: OnboardingStatus)
}
