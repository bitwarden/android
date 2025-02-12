package com.bitwarden.authenticator.data.platform.util

import android.content.Intent

/**
 * Returns true if this intent contains unexpected or suspicious data.
 */
val Intent.isSuspicious: Boolean
    get() {
        return try {
            val containsSuspiciousExtras = extras?.isEmpty() == false
            val containsSuspiciousData = data != null
            containsSuspiciousData || containsSuspiciousExtras
        } catch (_: Exception) {
            // `unparcel()` throws an exception on Android 12 and below if the bundle contains
            // suspicious data, so we catch the exception and return true.
            true
        }
    }
