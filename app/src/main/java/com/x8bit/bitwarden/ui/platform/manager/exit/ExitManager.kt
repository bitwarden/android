package com.x8bit.bitwarden.ui.platform.manager.exit

import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * A manager class for handling the various ways to exit the app.
 */
@OmitFromCoverage
interface ExitManager {
    /**
     * Finishes the activity.
     */
    fun exitApplication()
}
