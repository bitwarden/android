package com.bitwarden.authenticator.ui.platform.manager.intent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * The default implementation of the [IntentManager] for simplifying the handling of Android
 * Intents within a given context.
 */
class IntentManagerImpl(
    private val context: Context,
) : IntentManager {
    override fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // no-op
        }
    }

    @Composable
    override fun getActivityResultLauncher(
        onResult: (ActivityResult) -> Unit,
    ): ManagedActivityResultLauncher<Intent, ActivityResult> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = onResult,
        )

    override fun startApplicationDetailsSettingsActivity() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        startActivity(intent = intent)
    }

    override fun launchUri(uri: Uri) {
        val newUri = if (uri.scheme == null) {
            uri.buildUpon().scheme("https").build()
        } else {
            uri.normalizeScheme()
        }
        startActivity(Intent(Intent.ACTION_VIEW, newUri))
    }
}
