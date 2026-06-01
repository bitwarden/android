package com.bitwarden.authenticator.data.platform.manager.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.getSystemService
import androidx.core.os.persistableBundleOf
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text

/**
 * Default implementation of the [BitwardenClipboardManager] interface.
 */
class BitwardenClipboardManagerImpl(
    private val context: Context,
    private val toastManager: ToastManager,
) : BitwardenClipboardManager {
    private val clipboardManager: ClipboardManager = requireNotNull(context.getSystemService())

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
            toastManager.show(
                message = context.resources.getString(
                    BitwardenString.value_has_been_copied,
                    toastDescriptorOverride ?: text,
                ),
            )
        }
    }

    override fun setText(text: String, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toAnnotatedString(), isSensitive, toastDescriptorOverride)
    }

    override fun setText(text: Text, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toString(context.resources), isSensitive, toastDescriptorOverride)
    }
}
