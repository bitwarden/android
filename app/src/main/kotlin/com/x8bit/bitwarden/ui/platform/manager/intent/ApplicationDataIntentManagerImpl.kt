package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * A manager interface for handling intents related to application data.
 */
class ApplicationDataIntentManagerImpl(
    private val context: Context,
) : ApplicationDataIntentManager {
    /**
     * Starts the application's settings activity.
     */
    override fun startApplicationDetailsSettingsActivity() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = "package:${context.packageName}".toUri()
        startActivity(intent = intent)
    }

    private fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // no-op
        }
    }
}
