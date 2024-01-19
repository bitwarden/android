package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable

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
     * Attempts to start the system autofill settings activity. The return value indicates whether
     * or not this was successful.
     */
    fun startSystemAutofillSettingsActivity(): Boolean

    /**
     * Starts the application's settings activity.
     */
    fun startApplicationDetailsSettingsActivity()

    /**
     * Start an activity to view the given [uri] in an external browser.
     */
    fun launchUri(uri: Uri)

    /**
     * Start an activity using the provided [Intent] and provides a callback, via [onResult], for
     * retrieving the [ActivityResult].
     */
    @Composable
    fun launchActivityForResult(
        onResult: (ActivityResult) -> Unit,
    ): ManagedActivityResultLauncher<Intent, ActivityResult>

    /**
     * Launches the share sheet with the given [text].
     */
    fun shareText(text: String)

    /**
     * Processes the [activityResult] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromIntent(activityResult: ActivityResult): FileData?

    /**
     * Creates an intent for choosing a file saved to disk.
     */
    fun createFileChooserIntent(withCameraIntents: Boolean): Intent

    /**
     * Represents file information.
     */
    data class FileData(
        val fileName: String,
        val uri: Uri,
        val sizeBytes: Long,
    )
}
