package com.x8bit.bitwarden.ui.platform.manager.exit

import androidx.compose.runtime.Immutable

/**
 * A manager class for handling the various ways to exit the app.
 */
@Immutable
interface ExitManager {
    /**
     * Finishes the activity.
     */
    fun exitApplication()
}
