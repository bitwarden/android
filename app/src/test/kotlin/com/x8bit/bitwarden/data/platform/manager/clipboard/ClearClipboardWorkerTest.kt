package com.x8bit.bitwarden.data.platform.manager.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.work.ListenableWorker
import com.bitwarden.ui.platform.base.BaseRobolectricTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class ClearClipboardWorkerTest : BaseRobolectricTest() {

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var worker: ClearClipboardWorker

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        worker = ClearClipboardWorker(
            appContext = context,
            workerParams = mockk(relaxed = true),
        )
    }

    @Test
    fun `doWork should clear a populated clipboard and return success`() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", "password"))

        val result = worker.doWork()

        assertTrue(clipboardManager.primaryClip?.getItemAt(0)?.text.isNullOrEmpty())
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork should return success when the clipboard is already empty`() {
        clipboardManager.clearPrimaryClip()

        val result = worker.doWork()

        assertFalse(clipboardManager.hasPrimaryClip())
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @Test
    fun `doWork should overwrite the clipboard with sensitive empty content before clearing`() {
        val overwrittenClip = captureEmptyClipboardOverwrite()

        assertTrue(
            overwrittenClip
                ?.description
                ?.extras
                ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true,
        )
    }

    @Config(sdk = [Build.VERSION_CODES.S_V2])
    @Test
    fun `doWork should use the legacy sensitive extra before Android 13`() {
        val overwrittenClip = captureEmptyClipboardOverwrite()

        assertTrue(
            overwrittenClip
                ?.description
                ?.extras
                ?.getBoolean("android.content.extra.IS_SENSITIVE") == true,
        )
    }

    private fun captureEmptyClipboardOverwrite(): ClipData? {
        var overwrittenClip: ClipData? = null
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val currentClip = clipboardManager.primaryClip
            if (
                overwrittenClip == null &&
                currentClip?.getItemAt(0)?.text.isNullOrEmpty()
            ) {
                overwrittenClip = currentClip
            }
        }
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", "password"))
        clipboardManager.addPrimaryClipChangedListener(listener)

        worker.doWork()

        clipboardManager.removePrimaryClipChangedListener(listener)
        return overwrittenClip
    }
}
