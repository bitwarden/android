package com.x8bit.bitwarden.authenticator.data.platform.repository

import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
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
     * The current setting for getting login item icons.
     */
    var isIconLoadingDisabled: Boolean

    /**
     * Emits updates that track the [isIconLoadingDisabled] value.
     */
    val isIconLoadingDisabledFlow: Flow<Boolean>
}
