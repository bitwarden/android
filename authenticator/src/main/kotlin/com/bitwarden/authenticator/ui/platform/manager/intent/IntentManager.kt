package com.bitwarden.authenticator.ui.platform.manager.intent

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.bitwarden.ui.platform.model.FileData

/**
 * A manager class for simplifying the handling of Android Intents within a given context.
 */
@Immutable
interface IntentManager {
    /**
     * The package name for the current app, this can be used to generate an [Intent].
     */
    val packageName: String

    /**
     * Start an activity using the provided [Intent].
     */
    fun startActivity(intent: Intent)

    /**
     * Start an activity to view the given [uri] in an external browser.
     */
    fun launchUri(uri: Uri)

    /**
     * Start an activity using the provided [Intent] and provides a callback, via [onResult], for
     * retrieving the [ActivityResult].
     */
    @Composable
    fun getActivityResultLauncher(
        onResult: (ActivityResult) -> Unit,
    ): ManagedActivityResultLauncher<Intent, ActivityResult>

    /**
     * Processes the [activityResult] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromActivityResult(activityResult: ActivityResult): FileData?

    /**
     * Creates an intent for choosing a file saved to disk.
     */
    fun createFileChooserIntent(mimeType: String): Intent

    /**
     * Creates an intent to use when selecting to save an item with [fileName] to disk.
     */
    fun createDocumentIntent(fileName: String): Intent
}
