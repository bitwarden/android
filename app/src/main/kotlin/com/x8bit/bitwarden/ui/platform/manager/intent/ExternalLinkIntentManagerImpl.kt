package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.bitwarden.core.util.isBuildVersionAtLeast

/**
 * Primary implementation of [ExternalLinkIntentManager] that handles launching external links.
 */
class ExternalLinkIntentManagerImpl(
    private val context: Context,
) : ExternalLinkIntentManager {
    /**
     * Start a Custom Tabs Activity using the provided [Uri].
     */
    override fun startCustomTabsActivity(uri: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(context, uri)
    }

    /**
     * Start an activity to view the given [uri] in an external browser.
     */
    override fun launchUri(uri: Uri) {
        if (uri.scheme.equals(other = "androidapp", ignoreCase = true)) {
            val packageName = uri.toString().removePrefix(prefix = "androidapp://")
            if (!isBuildVersionAtLeast(Build.VERSION_CODES.TIRAMISU)) {
                startActivity(createPlayStoreIntent(packageName))
            } else {
                try {
                    context
                        .packageManager
                        .getLaunchIntentSenderForPackage(packageName)
                        .sendIntent(context, Activity.RESULT_OK, null, null, null)
                } catch (_: IntentSender.SendIntentException) {
                    startActivity(createPlayStoreIntent(packageName))
                }
            }
        } else {
            val newUri = if (uri.scheme == null) {
                uri.buildUpon().scheme("https").build()
            } else {
                uri.normalizeScheme()
            }
            startActivity(Intent(Intent.ACTION_VIEW, newUri))
        }
    }

    private fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // no-op
        }
    }

    private fun createPlayStoreIntent(packageName: String): Intent {
        val playStoreUri = "https://play.google.com/store/apps/details"
            .toUri()
            .buildUpon()
            .appendQueryParameter("id", packageName)
            .build()
        return Intent(Intent.ACTION_VIEW, playStoreUri)
    }
}
