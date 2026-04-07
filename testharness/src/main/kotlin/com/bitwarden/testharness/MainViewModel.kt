package com.bitwarden.testharness

import android.os.Parcelable
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * ViewModel that manages Activity-level state for the test harness.
 *
 * Handles theme and other cross-cutting concerns at the Activity level.
 * This follows the pattern from the main app's MainActivity/MainViewModel.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    // Minimal dependencies for test harness
    // Could add SettingsRepository if theme persistence needed
) : BaseViewModel<MainState, MainEvent, MainAction>(
    initialState = MainState(
        theme = AppTheme.DEFAULT,
    ),
) {

    override fun handleAction(action: MainAction) {
        // Minimal actions for test harness
        // Could handle theme changes if needed
    }
}

/**
 * Models state for the [MainActivity].
 *
 * @property theme The current app theme.
 */
@Parcelize
data class MainState(
    val theme: AppTheme,
) : Parcelable

/**
 * Models actions for the [MainActivity].
 *
 * Currently empty but reserved for future Activity-level actions.
 */
sealed class MainAction

/**
 * Models events emitted by the [MainActivity].
 *
 * Currently empty but reserved for future Activity-level events such as
 * theme updates or Activity recreation requirements.
 */
sealed class MainEvent
