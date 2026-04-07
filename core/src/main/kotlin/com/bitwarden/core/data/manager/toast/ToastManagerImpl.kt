package com.bitwarden.core.data.manager.toast

import android.content.Context
import android.widget.Toast

/**
 * The default implementation of the [ToastManager].
 */
class ToastManagerImpl(
    private val context: Context,
) : ToastManager {
    override fun show(message: CharSequence, duration: Int) {
        Toast.makeText(context, message, duration).show()
    }

    override fun show(messageId: Int, duration: Int) {
        Toast.makeText(context, messageId, duration).show()
    }
}
