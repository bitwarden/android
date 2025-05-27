package com.x8bit.bitwarden.data.autofill.model

import android.content.Context

/**
 * The app information required for the autofill service.
 */
data class AutofillAppInfo(
    val context: Context,
    val packageName: String,
    val sdkInt: Int,
)
