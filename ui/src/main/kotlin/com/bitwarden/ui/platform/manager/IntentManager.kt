package com.bitwarden.ui.platform.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.ui.platform.model.FileData
import kotlinx.parcelize.Parcelize
import java.time.Clock

/**
 * A manager class for simplifying the handling of Android Intents within a given context.
 */
@Suppress("TooManyFunctions")
@Immutable
interface IntentManager {
    /**
     * The package name for the current app, this can be used to generate an [Intent].
     */
    val packageName: String

    /**
     * Start an activity using the provided [Intent].
     *
     * @return `true` if the activity was started successfully, `false` otherwise.
     */
    fun startActivity(intent: Intent): Boolean

    /**
     * Start a Custom Tabs Activity using the provided [Uri].
     */
    fun startCustomTabsActivity(uri: Uri)

    /**
     * Starts the credential manager settings.
     */
    fun startCredentialManagerSettings()

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
     * Launches the share sheet with an error report based on the provided [throwable].
     */
    fun shareErrorReport(throwable: Throwable)

    /**
     * Processes the [activityResult] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromActivityResult(activityResult: ActivityResult): FileData?

    /**
     * Processes the [intent] and attempts to derive [ShareData] information from it.
     */
    fun getShareDataFromIntent(intent: Intent): ShareData?

    /**
     * Creates an intent for choosing a file saved to disk.
     *
     * @param withCameraIntents Whether to include camera intents in the chooser.
     * @param mimeType The MIME type of the files to be selected. Defaults to wildcard to allow all
     * types.
     */
    fun createFileChooserIntent(withCameraIntents: Boolean, mimeType: String = "*/*"): Intent

    /**
     * Creates an intent to use when selecting to save an item with [fileName] to disk.
     */
    fun createDocumentIntent(fileName: String): Intent

    /**
     * Represents data for a share request coming from outside the app.
     */
    sealed class ShareData : Parcelable {
        /**
         * The data required to create a new Text Send.
         */
        @Parcelize
        data class TextSend(
            val subject: String?,
            val text: String,
        ) : ShareData()

        /**
         * The data required to create a new File Send.
         */
        @Parcelize
        data class FileSend(
            val fileData: FileData,
        ) : ShareData()
    }

    @Suppress("UndocumentedPublicClass")
    @OmitFromCoverage
    companion object {
        /**
         * Creates a new [IntentManager] instance.
         */
        fun create(
            context: Context,
            clock: Clock,
            buildInfoManager: BuildInfoManager,
        ): IntentManager = IntentManagerImpl(
            context = context,
            clock = clock,
            buildInfoManager = buildInfoManager,
        )
    }
}
