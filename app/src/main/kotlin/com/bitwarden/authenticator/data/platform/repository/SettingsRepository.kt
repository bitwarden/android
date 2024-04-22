package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing and modifying settings state.
 */
interface SettingsRepository {

    /**
     * The [AppLanguage] for the current user.
     */
    var appLanguage: AppLanguage

    /**
     * The currently stored [AppTheme].
     */
    var appTheme: AppTheme

    /**
     * Tracks changes to the [AppTheme].
     */
    val appThemeStateFlow: StateFlow<AppTheme>

    /**
     * The currently stored expiration alert threshold.
     */
    var authenticatorAlertThresholdSeconds: Int

    /**
     * Tracks changes to the expiration alert threshold.
     */
    val authenticatorAlertThresholdSecondsFlow: StateFlow<Int>

    /**
     * Whether the user has seen the Welcome tutorial.
     */
    var hasSeenWelcomeTutorial: Boolean

    /**
     * Tracks whether the user has seen the Welcome tutorial.
     */
    val hasSeenWelcomeTutorialFlow: StateFlow<Boolean>
}
