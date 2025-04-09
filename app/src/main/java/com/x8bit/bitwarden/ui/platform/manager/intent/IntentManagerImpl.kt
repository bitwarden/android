package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
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
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeReleaseChannel
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import java.io.File
import java.time.Clock

/**
 * The authority used for pulling in photos from the camera.
 *
 * Note: This must match the file provider authority in the manifest.
 */
private const val FILE_PROVIDER_AUTHORITY: String = "${BuildConfig.APPLICATION_ID}.fileprovider"

/**
 * Temporary file name for a camera image.
 */
private const val TEMP_CAMERA_IMAGE_NAME: String = "temp_camera_image.jpg"

/**
 * This directory must also be declared in file_paths.xml
 */
private const val TEMP_CAMERA_IMAGE_DIR: String = "camera_temp"

/**
 * Key for the user id included in FIDO 2 provider "create entries".
 *
 * @see IntentManager.createFido2CreationPendingIntent
 */
const val EXTRA_KEY_USER_ID: String = "user_id"

/**
 * Key for the credential id included in FIDO 2 provider "get entries".
 *
 * @see IntentManager.createFido2GetCredentialPendingIntent
 */
const val EXTRA_KEY_CREDENTIAL_ID: String = "credential_id"

/**
 * Key for the cipher id included in FIDO 2 provider "get entries".
 *
 * @see IntentManager.createFido2GetCredentialPendingIntent
 */
const val EXTRA_KEY_CIPHER_ID: String = "cipher_id"

/**
 * Key for the user verification performed during vault unlock while processing a FIDO 2 request.
 */
const val EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK: String = "uv_performed_during_unlock"

/**
 * The default implementation of the [IntentManager] for simplifying the handling of Android
 * Intents within a given context.
 */
@Suppress("TooManyFunctions")
@OmitFromCoverage
class IntentManagerImpl(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
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

    override fun startCustomTabsActivity(uri: Uri) {
        CustomTabsIntent
            .Builder()
            .build()
            .launchUrl(context, uri)
    }

    override fun startSystemAccessibilitySettingsActivity() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun startSystemAutofillSettingsActivity(): Boolean =
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                .apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }

    override fun startApplicationDetailsSettingsActivity() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        startActivity(intent = intent)
    }

    override fun startCredentialManagerSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            CredentialManager.create(context).createSettingsPendingIntent().send()
        }
    }

    override fun startChromeAutofillSettingsActivity(
        releaseChannel: ChromeReleaseChannel,
    ): Boolean = try {
        val intent = Intent(Intent.ACTION_APPLICATION_PREFERENCES)
            .apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_APP_BROWSER)
                addCategory(Intent.CATEGORY_PREFERENCE)
                setPackage(releaseChannel.packageName)
            }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    override fun launchUri(uri: Uri) {
        if (uri.scheme.equals(other = "androidapp", ignoreCase = true)) {
            val packageName = uri.toString().removePrefix(prefix = "androidapp://")
            if (isBuildVersionBelow(Build.VERSION_CODES.TIRAMISU)) {
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
            FILE_PROVIDER_AUTHORITY,
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

    override fun getFileDataFromActivityResult(
        activityResult: ActivityResult,
    ): IntentManager.FileData? {
        if (activityResult.resultCode != Activity.RESULT_OK) return null
        val uri = activityResult.data?.data
        return if (uri != null) getLocalFileData(uri) else getCameraFileData()
    }

    override fun getFileDataFromIntent(intent: Intent): IntentManager.FileData? =
        intent.clipData?.getItemAt(0)?.uri?.let { getLocalFileData(it) }

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

    override fun createFileChooserIntent(withCameraIntents: Boolean): Intent {
        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"),
            ContextCompat.getString(context, R.string.file_source),
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
                FILE_PROVIDER_AUTHORITY,
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

    override fun createTileIntent(data: String): Intent {
        return Intent(
            context,
            MainActivity::class.java,
        )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setData(data.toUri())
    }

    override fun createTilePendingIntent(requestCode: Int, tileIntent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            tileIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun createFido2CreationPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    override fun createFido2GetCredentialPendingIntent(
        action: String,
        userId: String,
        credentialId: String,
        cipherId: String,
        isUserVerified: Boolean,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)
            .putExtra(EXTRA_KEY_CREDENTIAL_ID, credentialId)
            .putExtra(EXTRA_KEY_CIPHER_ID, cipherId)
            .putExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, isUserVerified)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    override fun createFido2UnlockPendingIntent(
        action: String,
        userId: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_KEY_USER_ID, userId)

        return PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
    }

    override fun startDefaultEmailApplication() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun createPlayStoreIntent(packageName: String): Intent {
        val playStoreUri = "https://play.google.com/store/apps/details"
            .toUri()
            .buildUpon()
            .appendQueryParameter("id", packageName)
            .build()
        return Intent(Intent.ACTION_VIEW, playStoreUri)
    }

    private fun getCameraFileData(): IntentManager.FileData {
        val tmpDir = File(context.filesDir, TEMP_CAMERA_IMAGE_DIR)
        val file = File(tmpDir, TEMP_CAMERA_IMAGE_NAME)
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        val fileName = "photo_${clock.instant().toFormattedPattern(pattern = "yyyyMMddHHmmss")}.jpg"
        return IntentManager.FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = file.length(),
        )
    }

    private fun getLocalFileData(uri: Uri): IntentManager.FileData? =
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
                IntentManager.FileData(
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
