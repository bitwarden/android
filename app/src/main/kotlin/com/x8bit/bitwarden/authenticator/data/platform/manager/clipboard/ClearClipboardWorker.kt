package com.x8bit.bitwarden.data.platform.manager.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * A worker to clear the clipboard manager.
 */
class ClearClipboardWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val clipboardManager =
        appContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

    override fun doWork(): Result {
        clipboardManager.clearPrimaryClip()
        return Result.success()
    }
}
