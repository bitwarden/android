package com.x8bit.bitwarden.ui.platform.manager.intent

import android.net.Uri

/**
 * A manager interface for handling intents related to external links.
 */
interface ExternalLinkIntentManager {
    /**
     * Start a Custom Tabs Activity using the provided [Uri].
     */
    fun startCustomTabsActivity(uri: Uri)

    /**
     * Start an activity to view the given [uri] in an external browser.
     */
    fun launchUri(uri: Uri)
}
