package com.x8bit.bitwarden.data.platform.manager.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.getSystemService
import androidx.core.os.persistableBundleOf
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Default implementation of the [BitwardenClipboardManager] interface.
 */
@OmitFromCoverage
class BitwardenClipboardManagerImpl(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val toastManager: ToastManager,
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
                        if (isBuildVersionAtLeast(version = Build.VERSION_CODES.TIRAMISU)) {
                            ClipDescription.EXTRA_IS_SENSITIVE to isSensitive
                        } else {
                            "android.content.extra.IS_SENSITIVE" to isSensitive
                        },
                    )
                },
        )
        if (!isBuildVersionAtLeast(version = Build.VERSION_CODES.TIRAMISU)) {
            val descriptor = toastDescriptorOverride
                ?.let { context.resources.getString(BitwardenString.value_has_been_copied, it) }
                ?: context.resources.getString(
                    BitwardenString.value_has_been_copied,
                    context.resources.getString(BitwardenString.value),
                )
            toastManager.show(message = descriptor)
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

    override fun setText(text: String, isSensitive: Boolean, toastDescriptorOverride: Text) {
        setText(
            text.toAnnotatedString(),
            isSensitive,
            toastDescriptorOverride.toString(context.resources),
        )
    }

    override fun setText(text: Text, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toString(context.resources), isSensitive, toastDescriptorOverride)
    }
}
