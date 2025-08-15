package com.bitwarden.authenticator.ui.platform.manager.intent

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * The default implementation of the [IntentManager] for simplifying the handling of Android
 * Intents within a given context.
 */
@OmitFromCoverage
class IntentManagerImpl(
    private val context: Context,
) : IntentManager {
    override fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
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

    override fun getFileDataFromActivityResult(
        activityResult: ActivityResult,
    ): FileData? {
        if (activityResult.resultCode != Activity.RESULT_OK) return null
        val uri = activityResult.data?.data ?: return null
        return getLocalFileData(uri)
    }

    override fun createFileChooserIntent(mimeType: String): Intent {
        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mimeType),
            ContextCompat.getString(context, BitwardenString.file_source),
        )

        return chooserIntent
    }

    override fun createDocumentIntent(fileName: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Attempt to get the MIME type from the file extension
            val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
            type = extension
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                ?: "*/*"

            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

    override fun launchUri(uri: Uri) {
        val newUri = if (uri.scheme == null) {
            uri.buildUpon().scheme("https").build()
        } else {
            uri.normalizeScheme()
        }
        startActivity(Intent(Intent.ACTION_VIEW, newUri))
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
                    ?: return@use null
                val fileSize = cursor
                    .getColumnIndex(MediaStore.MediaColumns.SIZE)
                    .takeIf { it >= 0 }
                    ?.let { cursor.getLong(it) }
                    ?: return@use null
                FileData(
                    fileName = fileName,
                    uri = uri,
                    sizeBytes = fileSize,
                )
            }
}
