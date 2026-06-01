package com.bitwarden.ui.platform.manager.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.ui.platform.manager.share.model.ShareData
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.util.getLocalFileData

/**
 * The default implementation of the [ShareManager].
 */
class ShareManagerImpl(
    private val context: Context,
    private val buildInfoManager: BuildInfoManager,
) : ShareManager {
    override fun getShareDataOrNull(
        intent: Intent,
    ): ShareData? = if (intent.action != Intent.ACTION_SEND) {
        null
    } else if (intent.type?.contains("text/") == true) {
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        intent
            .getStringExtra(Intent.EXTRA_TEXT)
            ?.let { ShareData.TextSend(subject = subject, text = it) }
    } else {
        getFileDataFromIntent(intent = intent)?.let { ShareData.FileSend(fileData = it) }
    }

    private fun getFileDataFromIntent(intent: Intent): FileData? = intent
        .clipData
        ?.getItemAt(0)
        ?.uri
        ?.takeUnless { uri ->
            context
                .packageManager
                .getPackageInfo(buildInfoManager.applicationId, PackageManager.GET_PROVIDERS)
                .providers
                ?.any { uri.toString().contains(other = it.authority) } == true
        }
        ?.let { context.getLocalFileData(uri = it) }
}
