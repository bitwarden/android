package com.x8bit.bitwarden.ui.platform.base.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * A utility class for simplifying the handling of Android Intents within a given context.
 */
@OmitFromCoverage
class IntentHandler(private val context: Context) {

    /**
     * Starts an intent to exit the application.
     */
    fun exitApplication() {
        // Note that we fire an explicit Intent rather than try to cast to an Activity and call
        // finish to avoid assumptions about what kind of context we have.
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(intent)
    }

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

    /**
     * Start an activity to view the given [uri] in an external browser.
     */
    fun launchUri(uri: Uri) {
        val newUri = if (uri.scheme == null) {
            uri.buildUpon().scheme("https").build()
        } else {
            uri.normalizeScheme()
        }
        startActivity(Intent(Intent.ACTION_VIEW, newUri))
    }
}
