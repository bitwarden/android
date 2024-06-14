package com.bitwarden.authenticator.data.platform.util

import android.content.Intent

/**
 * Returns true if this intent contains unexpected or suspicious data.
 */
val Intent.isSuspicious: Boolean
    get() {
        val containsSuspiciousExtras = extras?.isEmpty?.not() ?: false
        val containsSuspiciousData = data != null
        return containsSuspiciousData || containsSuspiciousExtras
    }
