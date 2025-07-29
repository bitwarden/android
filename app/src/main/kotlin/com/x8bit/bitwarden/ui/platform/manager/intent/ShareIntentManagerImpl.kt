package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.core.content.FileProvider
import com.bitwarden.core.data.manager.BitwardenBuildConfigManager
import com.bitwarden.core.data.util.toFormattedPattern
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.ui.platform.manager.intent.model.FileData
import com.x8bit.bitwarden.ui.platform.manager.intent.model.ShareData
import java.io.File
import java.time.Clock

/**
 * This directory must also be declared in file_paths.xml
 */
private const val TEMP_CAMERA_IMAGE_DIR: String = "camera_temp"

/**
 * Temporary file name for a camera image.
 */
private const val TEMP_CAMERA_IMAGE_NAME: String = "temp_camera_image.jpg"

/**
 * Primary implementation of [ShareIntentManager] for managing share-related intents.
 */
class ShareIntentManagerImpl(
    private val context: Context,
    private val bitwardenBuildConfigManager: BitwardenBuildConfigManager,
    private val clock: Clock,
) : ShareIntentManager {
    /**
     * Shares a file.
     *
     * @param title The title for the share action, or `null` if no title is needed.
     * @param fileUri The URI of the file to be shared.
     */
    override fun shareFile(title: String?, fileUri: Uri) {
        val providedFile = FileProvider.getUriForFile(
            context,
            bitwardenBuildConfigManager.fileProviderAuthority,
            File(fileUri.toString()),
        )
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, title)
            putExtra(Intent.EXTRA_STREAM, providedFile)
            type = "application/zip"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }

    /**
     * Shares text content.
     *
     * @param text The text content to be shared.
     */
    override fun shareText(text: String) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }

    /**
     * Shares an error report.
     *
     * @param throwable The throwable to be included in the error report.
     */
    override fun shareErrorReport(throwable: Throwable) {
        shareText(
            StringBuilder()
                .append("Stacktrace:\n")
                .append("$throwable\n")
                .apply { throwable.stackTrace.forEach { append("\t$it\n") } }
                .append("\n")
                .append("Version: ${bitwardenBuildConfigManager.versionData}\n")
                .append("Device: ${bitwardenBuildConfigManager.deviceData}\n")
                .apply { bitwardenBuildConfigManager.ciBuildInfo?.let { append("CI: $it\n") } }
                .append("\n")
                .toString(),
        )
    }

    /**
     * Retrieves share data from an [Intent].
     * @param intent The intent to extract share data from.
     * @return The extracted [ShareData], or `null` if no share data is found.
     */
    override fun getShareDataFromIntent(intent: Intent): ShareData? {
        if (intent.action != Intent.ACTION_SEND) return null
        return if (intent.type?.contains("text/") == true) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val title = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            ShareData.TextSend(
                subject = subject,
                text = title,
            )
        } else {
            getFileDataFromIntent(intent = intent)
                ?.let {
                    ShareData.FileSend(
                        fileData = it,
                    )
                }
        }
    }

    /**
     * Processes the [intent] and attempts to get the relevant file data from it.
     */
    override fun getFileDataFromIntent(
        intent: Intent,
    ): FileData? = intent
        .clipData
        ?.getItemAt(0)
        ?.uri
        ?.takeUnless { uri ->
            val uriString = uri.toString()
            context
                .packageManager
                .getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_PROVIDERS)
                .providers
                ?.any { uriString.contains(other = it.authority) } == true
        }
        ?.let { getLocalFileData(it) }

    override fun getFileDataFromActivityResult(activityResult: ActivityResult): FileData? {
        if (activityResult.resultCode != Activity.RESULT_OK) return null
        val uri = activityResult.data?.data
        return if (uri != null) getLocalFileData(uri) else getCameraFileData()
    }

    /**
     * Open the default email app on device.
     */
    override fun startDefaultEmailApplication() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // no-op
        }
    }

    private fun getLocalFileData(uri: Uri): FileData? =
        context
            .contentResolver
            .query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                ),
                null,
                null,
                null,
            )
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val fileName = cursor
                    .getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 }
                    ?.let { cursor.getString(it) }
                val fileSize = cursor
                    .getColumnIndex(MediaStore.MediaColumns.SIZE)
                    .takeIf { it >= 0 }
                    ?.let { cursor.getLong(it) }
                if (fileName == null || fileSize == null) return@use null
                FileData(
                    fileName = fileName,
                    uri = uri,
                    sizeBytes = fileSize,
                )
            }

    private fun getCameraFileData(): FileData {
        val tmpDir = File(context.filesDir, TEMP_CAMERA_IMAGE_DIR)
        val file = File(tmpDir, TEMP_CAMERA_IMAGE_NAME)
        val uri = FileProvider.getUriForFile(
            context,
            bitwardenBuildConfigManager.fileProviderAuthority,
            file,
        )
        val fileName = "photo_${clock.instant().toFormattedPattern(pattern = "yyyyMMddHHmmss")}.jpg"
        return FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = file.length(),
        )
    }
}
