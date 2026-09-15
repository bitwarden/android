package com.x8bit.bitwarden.data.platform.manager.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import androidx.core.os.persistableBundleOf
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bitwarden.core.util.isBuildVersionAtLeast

/**
 * A worker to clear the clipboard manager.
 */
class ClearClipboardWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    private val clipboardManager =
        appContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

    override fun doWork(): Result {
        clipboardManager.setPrimaryClip(
            ClipData
                .newPlainText("", "")
                .apply {
                    description.extras = persistableBundleOf(
                        if (isBuildVersionAtLeast(version = Build.VERSION_CODES.TIRAMISU)) {
                            ClipDescription.EXTRA_IS_SENSITIVE to true
                        } else {
                            "android.content.extra.IS_SENSITIVE" to true
                        },
                    )
                },
        )
        clipboardManager.clearPrimaryClip()
        return Result.success()
    }
}
