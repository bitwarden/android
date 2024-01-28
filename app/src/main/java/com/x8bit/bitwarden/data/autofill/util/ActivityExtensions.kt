@file:OmitFromCoverage

package com.x8bit.bitwarden.data.autofill.util

import android.app.Activity
import android.os.Build
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Build an [AutofillAppInfo] from the given [Activity].
 */
fun Activity.toAutofillAppInfo(): AutofillAppInfo =
    AutofillAppInfo(
        context = this.applicationContext,
        packageName = this.packageName,
        sdkInt = Build.VERSION.SDK_INT,
    )
