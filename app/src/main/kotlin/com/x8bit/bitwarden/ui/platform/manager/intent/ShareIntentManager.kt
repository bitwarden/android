package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import com.x8bit.bitwarden.ui.platform.manager.intent.model.FileData
import com.x8bit.bitwarden.ui.platform.manager.intent.model.ShareData

/**
 * A manager interface for handling intents related to sharing data.
 */
interface ShareIntentManager {
    /**
     * Shares a file.
     *
     * @param title The title for the share action, or `null` if no title is needed.
     * @param fileUri The URI of the file to be shared.
     */
    fun shareFile(title: String?, fileUri: Uri)

    /**
     * Shares text content.
     *
     * @param text The text content to be shared.
     */
    fun shareText(text: String)

    /**
     * Shares an error report.
     */
    fun shareErrorReport(throwable: Throwable)

    /**
     * Retrieves share data from an [Intent].
     * @param intent The intent to extract share data from.
     * @return The extracted [ShareData], or `null` if no share data is found.
     */
    fun getShareDataFromIntent(intent: Intent): ShareData?

    /**
     * Processes the [intent] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromIntent(intent: Intent): FileData?

    /**
     * Processes the [activityResult] and attempts to get the relevant file data from it.
     */
    fun getFileDataFromActivityResult(activityResult: ActivityResult): FileData?

    /**
     * Open the default email app on device.
     */
    fun startDefaultEmailApplication()
}
