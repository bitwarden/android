package com.x8bit.bitwarden.data.platform.util

import androidx.annotation.StringRes
import com.bitwarden.network.util.isCookieRedirectError
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * Maps a [Throwable] to a localized error string resource ID based on its type,
 * or `null` if no specific mapping exists.
 */
@StringRes
fun Throwable?.toErrorResId(): Int? = when {
    this.isCookieRedirectError() -> BitwardenString.cookie_redirect_error
    else -> null
}
