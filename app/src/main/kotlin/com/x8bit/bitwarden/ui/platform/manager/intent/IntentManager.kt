package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.model.ShareData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * A manager class for simplifying the handling of Android Intents within a given context.
 */
@Suppress("TooManyFunctions")
@Immutable
interface IntentManager {
    /**
     * Start an activity using the provided [Intent].
     */
    fun startActivity(intent: Intent)

    /**
     * Start a Custom Tabs Activity using the provided [Uri].
     */
    fun startCustomTabsActivity(uri: Uri)

    /**
     * Attempts to start the system accessibility settings activity.
     */
    fun startSystemAccessibilitySettingsActivity()

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
     * Starts the credential manager settings.
     */
    fun startCredentialManagerSettings(context: Context)

    /**
     * Starts the browser autofill settings activity for the provided [BrowserPackage].
     */
    fun startBrowserAutofillSettingsActivity(browserPackage: BrowserPackage): Boolean

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
     * Launches the share sheet with the given [title] and file.
     */
    fun shareFile(title: String? = null, fileUri: Uri)

    /**
     * Launches the share sheet with the given [text].
     */
    fun shareText(text: String)

    /**
     * Processes the [activityResult] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromActivityResult(activityResult: ActivityResult): FileData?

    /**
     * Processes the [intent] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromIntent(intent: Intent): FileData?

    /**
     * Processes the [intent] and attempts to derive [ShareData] information from it.
     */
    fun getShareDataFromIntent(intent: Intent): ShareData?

    /**
     * Creates an intent for choosing a file saved to disk.
     */
    fun createFileChooserIntent(withCameraIntents: Boolean): Intent

    /**
     * Creates an intent to use when selecting to save an item with [fileName] to disk.
     */
    fun createDocumentIntent(fileName: String): Intent

    /**
     * Creates a pending intent to use when providing [CreateEntry]
     * instances for FIDO 2 credential creation.
     */
    fun createFido2CreationPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing
     * [CredentialEntry] instances for FIDO 2 credential filling.
     */
    @Suppress("LongParameterList")
    fun createFido2GetCredentialPendingIntent(
        action: String,
        userId: String,
        credentialId: String,
        cipherId: String,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing
     * [AuthenticationAction] instances for FIDO 2 credential filling.
     */
    fun createFido2UnlockPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent

    /**
     * Creates a pending intent to use when providing
     * [CredentialEntry] instances for Password credential filling.
     */
    fun createPasswordGetCredentialPendingIntent(
        action: String,
        userId: String,
        cipherId: String?,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent

    /**
     * Open the default email app on device.
     */
    fun startDefaultEmailApplication()
}
