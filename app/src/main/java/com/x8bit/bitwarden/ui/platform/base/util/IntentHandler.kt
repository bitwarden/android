package com.x8bit.bitwarden.ui.platform.base.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * A utility class for simplifying the handling of Android Intents within a given context.
 */
class IntentHandler(private val context: Context) {

    /**
     * Start an activity using the provided [Intent].
     */
    fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }

    /**
     * Start a Custom Tabs Activity using the provided [Uri].
     */
    fun startCustomTabsActivity(uri: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(context, uri)
    }
}
