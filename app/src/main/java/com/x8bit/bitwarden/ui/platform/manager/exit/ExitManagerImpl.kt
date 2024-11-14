package com.x8bit.bitwarden.ui.platform.manager.exit

import android.app.Activity
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * The default implementation of the [ExitManager] for managing the various ways to exit the app.
 */
@OmitFromCoverage
class ExitManagerImpl(
    private val activity: Activity,
) : ExitManager {
    override fun exitApplication() {
        activity.finishAndRemoveTask()
    }
}
