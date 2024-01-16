package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.Intent
import android.net.Uri

/**
 * A manager class for simplifying the handling of Android Intents within a given context.
 */
interface IntentManager {

    /**
     * Starts an intent to exit the application.
     */
    fun exitApplication()

    /**
     * Start an activity using the provided [Intent].
     */
    fun startActivity(intent: Intent)

    /**
     * Start a Custom Tabs Activity using the provided [Uri].
     */
    fun startCustomTabsActivity(uri: Uri)

    /**
     * Start an activity to view the given [uri] in an external browser.
     */
    fun launchUri(uri: Uri)

    /**
     * Launches the share sheet with the given [text].
     */
    fun shareText(text: String)
}
