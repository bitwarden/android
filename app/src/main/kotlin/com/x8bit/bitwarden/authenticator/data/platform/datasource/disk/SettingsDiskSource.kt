package com.x8bit.bitwarden.authenticator.data.platform.datasource.disk

import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for general settings-related disk information.
 */
interface SettingsDiskSource {

    /**
     * The currently persisted app theme (or `null` if not set).
     */
    var appTheme: AppTheme

    /**
     * Emits updates that track [appTheme].
     */
    val appThemeFlow: Flow<AppTheme>

    /**
     * Clears all the settings data for the given user.
     */
    fun clearData(userId: String)

    /**
     * Gets whether or not the given [userId] has enabled screen capture.
     */
    fun getScreenCaptureAllowed(userId: String): Boolean?

    /**
     * Emits updates that track [getScreenCaptureAllowed] for the given [userId].
     */
    fun getScreenCaptureAllowedFlow(userId: String): Flow<Boolean?>

    /**
     * Stores whether or not [isScreenCaptureAllowed] for the given [userId].
     */
    fun storeScreenCaptureAllowed(userId: String, isScreenCaptureAllowed: Boolean?)
}
