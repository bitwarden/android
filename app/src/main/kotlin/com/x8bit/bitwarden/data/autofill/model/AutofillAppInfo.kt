package com.x8bit.bitwarden.data.autofill.model

import android.content.Context
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * The app information required for the autofill service.
 */
data class AutofillAppInfo(
    val context: Context,
    val packageName: String,
    val sdkInt: Int,
) {
    /**
     * Returns true if the current [sdkInt] version is at least the provided [version].
     */
    @ChecksSdkIntAtLeast(parameter = 0)
    fun isVersionAtLeast(version: Int): Boolean = sdkInt >= version
}
