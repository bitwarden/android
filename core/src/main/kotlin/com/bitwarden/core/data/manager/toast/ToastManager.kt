package com.bitwarden.core.data.manager.toast

import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Wrapper class for displaying a [Toast].
 */
interface ToastManager {
    /**
     * Displays a [Toast] with the given [message].
     */
    fun show(message: CharSequence, duration: Int = Toast.LENGTH_SHORT)

    /**
     * Displays a [Toast] with the given [messageId].
     */
    fun show(@StringRes messageId: Int, duration: Int = Toast.LENGTH_SHORT)
}
