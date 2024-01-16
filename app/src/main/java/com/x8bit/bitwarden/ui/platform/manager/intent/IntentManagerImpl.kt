package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * The default implementation of the [IntentManager] for simplifying the handling of Android
 * Intents within a given context.
 */
@OmitFromCoverage
class IntentManagerImpl(
    private val context: Context,
) : IntentManager {

    override fun exitApplication() {
        // Note that we fire an explicit Intent rather than try to cast to an Activity and call
        // finish to avoid assumptions about what kind of context we have.
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(intent)
    }

    override fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }

    override fun startCustomTabsActivity(uri: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(context, uri)
    }

    override fun launchUri(uri: Uri) {
        val newUri = if (uri.scheme == null) {
            uri.buildUpon().scheme("https").build()
        } else {
            uri.normalizeScheme()
        }
        startActivity(Intent(Intent.ACTION_VIEW, newUri))
    }

    override fun shareText(text: String) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }
}
