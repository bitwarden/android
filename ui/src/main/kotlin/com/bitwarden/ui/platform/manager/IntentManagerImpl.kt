package com.bitwarden.ui.platform.manager

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
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
import com.bitwarden.ui.platform.util.getLocalFileData
import timber.log.Timber
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
internal class IntentManagerImpl(
    private val activity: Activity,
    private val clock: Clock,
    private val buildInfoManager: BuildInfoManager,
) : IntentManager {
    override val packageName: String get() = activity.packageName

    override fun startActivity(intent: Intent): Boolean = try {
        activity.startActivity(intent)
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

    override fun startAuthTab(
        uri: Uri,
        redirectScheme: String,
        launcher: ActivityResultLauncher<Intent>,
    ) {
        val providerPackageName = CustomTabsClient.getPackageName(activity, null).toString()
        if (CustomTabsClient.isAuthTabSupported(activity, providerPackageName)) {
            Timber.d("Launching uri with AuthTab for $providerPackageName")
            AuthTabIntent.Builder().build().launch(launcher, uri, redirectScheme)
        } else {
            // Fall back to a Custom Tab.
            Timber.d("Launching uri with CustomTabs fallback for $providerPackageName")
            startCustomTabsActivity(uri = uri)
        }
    }

    override fun startCustomTabsActivity(uri: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(activity, uri)
    }

    override fun startCredentialManagerSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            CredentialManager.create(activity).createSettingsPendingIntent().send()
        }
    }

    override fun launchUri(uri: Uri) {
        if (uri.scheme.equals(other = "androidapp", ignoreCase = true)) {
            val packageName = uri.toString().removePrefix(prefix = "androidapp://")
            if (!isBuildVersionAtLeast(Build.VERSION_CODES.TIRAMISU)) {
                startActivity(createPlayStoreIntent(packageName))
            } else {
                try {
                    activity
                        .packageManager
                        .getLaunchIntentSenderForPackage(packageName)
                        .sendIntent(activity, Activity.RESULT_OK, null, null, null)
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
            activity,
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
        return if (uri != null) activity.getLocalFileData(uri) else getCameraFileData()
    }

    override fun createFileChooserIntent(withCameraIntents: Boolean, mimeType: String): Intent {
        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mimeType),
            ContextCompat.getString(activity, BitwardenString.file_source),
        )

        if (withCameraIntents) {
            val tmpDir = File(activity.filesDir, TEMP_CAMERA_IMAGE_DIR)
            val file = File(tmpDir, TEMP_CAMERA_IMAGE_NAME)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            val outputFileUri = FileProvider.getUriForFile(
                activity,
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
        val tmpDir = File(activity.filesDir, TEMP_CAMERA_IMAGE_DIR)
        val file = File(tmpDir, TEMP_CAMERA_IMAGE_NAME)
        val uri = FileProvider.getUriForFile(
            activity,
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

    private fun getCameraIntents(outputUri: Uri): List<Intent> {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return activity
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
