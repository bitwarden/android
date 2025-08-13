package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.util.toFormattedPattern
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.manager.util.deviceData
import com.bitwarden.ui.platform.manager.util.fileProviderAuthority
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import java.io.File
import java.time.Clock

/**
 * Temporary file name for a camera image.
 */
private const val TEMP_CAMERA_IMAGE_NAME: String = "temp_camera_image.jpg"

/**
 * This directory must also be declared in file_paths.xml
 */
private const val TEMP_CAMERA_IMAGE_DIR: String = "camera_temp"

/**
 * The default implementation of the [IntentManager] for simplifying the handling of Android
 * Intents within a given context.
 */
@Suppress("TooManyFunctions")
@OmitFromCoverage
class IntentManagerImpl(
    private val context: Context,
    private val clock: Clock,
    private val buildInfoManager: BuildInfoManager,
) : IntentManager {
    override fun startActivity(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    @Composable
    override fun getActivityResultLauncher(
        onResult: (ActivityResult) -> Unit,
    ): ManagedActivityResultLauncher<Intent, ActivityResult> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = onResult,
        )

    override fun startCustomTabsActivity(uri: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(context, uri)
    }

    override fun startCredentialManagerSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            CredentialManager.create(context).createSettingsPendingIntent().send()
        }
    }

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

    override fun shareFile(title: String?, fileUri: Uri) {
        val providedFile = FileProvider.getUriForFile(
            context,
            buildInfoManager.fileProviderAuthority,
            File(fileUri.toString()),
        )
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, title)
            putExtra(Intent.EXTRA_STREAM, providedFile)
            type = "application/zip"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }

    override fun shareText(text: String) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }

    override fun shareErrorReport(throwable: Throwable) {
        shareText(
            StringBuilder()
                .append("Stacktrace:\n")
                .append("$throwable\n")
                .apply { throwable.stackTrace.forEach { append("\t$it\n") } }
                .append("\n")
                .append("Version: ${buildInfoManager.versionData}\n")
                .append("Device: ${buildInfoManager.deviceData}\n")
                .apply { buildInfoManager.ciBuildInfo?.let { append("CI: $it\n") } }
                .append("\n")
                .toString(),
        )
    }

    override fun getFileDataFromActivityResult(
        activityResult: ActivityResult,
    ): FileData? {
        if (activityResult.resultCode != Activity.RESULT_OK) return null
        val uri = activityResult.data?.data
        return if (uri != null) getLocalFileData(uri) else getCameraFileData()
    }

    private fun getFileDataFromIntent(
        intent: Intent,
    ): FileData? = intent
        .clipData
        ?.getItemAt(0)
        ?.uri
        ?.takeUnless { uri ->
            val uriString = uri.toString()
            context
                .packageManager
                .getPackageInfo(buildInfoManager.applicationId, PackageManager.GET_PROVIDERS)
                .providers
                ?.any { uriString.contains(other = it.authority) } == true
        }
        ?.let { getLocalFileData(it) }

    override fun getShareDataFromIntent(intent: Intent): IntentManager.ShareData? {
        if (intent.action != Intent.ACTION_SEND) return null
        return if (intent.type?.contains("text/") == true) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val title = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            IntentManager.ShareData.TextSend(
                subject = subject,
                text = title,
            )
        } else {
            getFileDataFromIntent(intent = intent)
                ?.let {
                    IntentManager.ShareData.FileSend(
                        fileData = it,
                    )
                }
        }
    }

    override fun createFileChooserIntent(withCameraIntents: Boolean, mimeType: String): Intent {
        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mimeType),
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
                buildInfoManager.fileProviderAuthority,
                file,
            )

            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                getCameraIntents(outputFileUri).toTypedArray(),
            )
        }

        return chooserIntent
    }

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

    private fun createPlayStoreIntent(packageName: String): Intent {
        val playStoreUri = "https://play.google.com/store/apps/details"
            .toUri()
            .buildUpon()
            .appendQueryParameter("id", packageName)
            .build()
        return Intent(Intent.ACTION_VIEW, playStoreUri)
    }

    private fun getCameraFileData(): FileData {
        val tmpDir = File(context.filesDir, TEMP_CAMERA_IMAGE_DIR)
        val file = File(tmpDir, TEMP_CAMERA_IMAGE_NAME)
        val uri = FileProvider.getUriForFile(
            context,
            buildInfoManager.fileProviderAuthority,
            file,
        )
        val fileName = "photo_${clock.instant().toFormattedPattern(pattern = "yyyyMMddHHmmss")}.jpg"
        return FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = file.length(),
        )
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
