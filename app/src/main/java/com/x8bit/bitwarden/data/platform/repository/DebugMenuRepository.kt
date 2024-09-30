package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing data required or associated with the debug menu.
 */
interface DebugMenuRepository {

    /**
     * Value to determine if the debug menu is enabled.
     */
    val isDebugMenuEnabled: Boolean

    /**
     * Observable flow for when any of the feature flag overrides have been updated.
     */
    val featureFlagOverridesUpdatedFlow: Flow<Unit>

    /**
     * Update a feature flag which matches the given [key] to the given [value].
     */
    fun <T : Any> updateFeatureFlag(key: FlagKey<T>, value: T)

    /**
     * Get a feature flag value based on the associated [FlagKey].
     */
    fun <T : Any> getFeatureFlag(key: FlagKey<T>): T?

    /**
     * Reset all feature flag overrides to their default values or values from the network.
     */
    fun resetFeatureFlagOverrides()

    /**
     * Resets the onboarding status to NOT_STARTED for the current active user, if applicable.
     */
    fun resetOnboardingStatusForCurrentUser()

    /**
     * Manipulates the state to force showing the onboarding carousel.
     *
     * @param userStateUpdateTrigger A passable lambda to trigger a user state update.
     */
    fun modifyStateToShowOnboardingCarousel(userStateUpdateTrigger: () -> Unit)
}
