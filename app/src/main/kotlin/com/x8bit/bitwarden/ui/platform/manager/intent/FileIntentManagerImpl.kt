package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bitwarden.core.data.manager.BitwardenBuildConfigManager
import com.bitwarden.ui.platform.resource.BitwardenString
import java.io.File

/**
 * This directory must also be declared in file_paths.xml
 */
private const val TEMP_CAMERA_IMAGE_DIR: String = "camera_temp"

/**
 * Temporary file name for a camera image.
 */
private const val TEMP_CAMERA_IMAGE_NAME: String = "temp_camera_image.jpg"

/**
 * Primary implementation of [FileIntentManager] for managing file-related intents.
 */
class FileIntentManagerImpl(
    private val context: Context,
    private val bitwardenBuildConfigManager: BitwardenBuildConfigManager,
) : FileIntentManager {
    /**
     * Creates an intent for choosing a file with the specified MIME type.
     *
     * @param withCameraIntents If `true`, includes camera intents for capturing images.
     * @return An [Intent] for choosing a file.
     */
    override fun createFileChooserIntent(withCameraIntents: Boolean): Intent {
        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"),
            ContextCompat.getString(context, BitwardenString.file_source),
        )

        if (withCameraIntents) {
            val tmpDir = File(context.filesDir, TEMP_CAMERA_IMAGE_DIR)
            val file = File(tmpDir, TEMP_CAMERA_IMAGE_NAME)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            val outputFileUri = FileProvider.getUriForFile(
                context,
                bitwardenBuildConfigManager.fileProviderAuthority,
                file,
            )

            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                getCameraIntents(outputFileUri).toTypedArray(),
            )
        }

        return chooserIntent
    }

    /**
     * Creates an intent to use when selecting to save an item with [fileName] to disk.
     */
    override fun createDocumentIntent(fileName: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Attempt to get the MIME type from the file extension
            val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
            type = extension?.let {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
            }
                ?: "*/*"

            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

    private fun getCameraIntents(outputUri: Uri): List<Intent> {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return context
            .packageManager
            .queryIntentActivities(captureIntent, PackageManager.MATCH_ALL)
            .map {
                val packageName = it.activityInfo.packageName
                Intent(captureIntent).apply {
                    component = ComponentName(packageName, it.activityInfo.name)
                    setPackage(packageName)
                    putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                }
            }
    }
}
