package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance

/**
 * Manages the screen from which the app should be resumed after unlock.
 */
interface AppResumeManager {

    /**
     * Sets the screen from which the app should be resumed after unlock.
     *
     * @param screenData The screen identifier (e.g., "HomeScreen", "SettingsScreen").
     */
    fun setResumeScreen(screenData: AppResumeScreenData)

    /**
     * Gets the screen from which the app should be resumed after unlock.
     *
     * @return The screen identifier, or an empty string if not set.
     */
    fun getResumeScreen(): AppResumeScreenData?

    /**
     * Gets the special circumstance associated with the resume screen for the current user.
     *
     * @return The special circumstance, or null if no special circumstance
     * is associated with the resume screen.
     */
    fun getResumeSpecialCircumstance(): SpecialCircumstance?

    /**
     * Clears the saved resume screen for the current user.
     */
    fun clearResumeScreen()
}
