package com.bitwarden.authenticator.data.platform.manager.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.getSystemService
import androidx.core.os.persistableBundleOf
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.toAnnotatedString

/**
 * Default implementation of the [BitwardenClipboardManager] interface.
 */
class BitwardenClipboardManagerImpl(
    private val context: Context,
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
            val descriptor = toastDescriptorOverride ?: text
            Toast
                .makeText(
                    context,
                    context.resources.getString(R.string.value_has_been_copied, descriptor),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    override fun setText(text: String, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toAnnotatedString(), isSensitive, toastDescriptorOverride)
    }

    override fun setText(text: Text, isSensitive: Boolean, toastDescriptorOverride: String?) {
        setText(text.toString(context.resources), isSensitive, toastDescriptorOverride)
    }
}
