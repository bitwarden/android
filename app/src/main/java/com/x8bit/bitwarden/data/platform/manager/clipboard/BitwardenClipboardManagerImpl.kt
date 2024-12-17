package com.x8bit.bitwarden.data.platform.manager.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.getSystemService
import androidx.core.os.persistableBundleOf
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import java.util.concurrent.TimeUnit

/**
 * Default implementation of the [BitwardenClipboardManager] interface.
 */
@OmitFromCoverage
class BitwardenClipboardManagerImpl(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : BitwardenClipboardManager {
    private val clipboardManager: ClipboardManager = requireNotNull(context.getSystemService())

    private val clearClipboardFrequencySeconds: Int?
        get() = settingsRepository.clearClipboardFrequency.frequencySeconds

    override fun setText(
        text: AnnotatedString,
        isSensitive: Boolean,
        toastDescriptorOverride: String?,
    ) {
        clipboardManager.setPrimaryClip(
            ClipData
                .newPlainText("", text)
                .apply {
                    description.extras = persistableBundleOf(
                        "android.content.extra.IS_SENSITIVE" to isSensitive,
                    )
                },
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val descriptor = toastDescriptorOverride
                ?.let { context.resources.getString(R.string.value_has_been_copied, it) }
                ?: context.resources.getString(R.string.copied_to_clipboard)
            Toast.makeText(context, descriptor, Toast.LENGTH_SHORT).show()
        }

        val frequency = clearClipboardFrequencySeconds ?: return
        val clearClipboardRequest: OneTimeWorkRequest =
            OneTimeWorkRequest
                .Builder(ClearClipboardWorker::class.java)
                .setInitialDelay(frequency.toLong(), TimeUnit.SECONDS)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ClearClipboard",
            ExistingWorkPolicy.REPLACE,
            clearClipboardRequest,
        )
    }

    override fun setText(text: String, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toAnnotatedString(), isSensitive, toastDescriptorOverride)
    }

    override fun setText(text: Text, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toString(context.resources), isSensitive, toastDescriptorOverride)
    }
}
