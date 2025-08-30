package com.x8bit.bitwarden.data.autofill.util

import android.os.Build
import android.service.autofill.FillRequest
import android.widget.inline.InlinePresentationSpec
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo

/**
 * Extract the list of [InlinePresentationSpec]s. If it fails, return an empty list.
 */
fun FillRequest?.getInlinePresentationSpecs(
    autofillAppInfo: AutofillAppInfo,
    isInlineAutofillEnabled: Boolean,
): List<InlinePresentationSpec>? =
    if (!autofillAppInfo.isVersionAtLeast(version = Build.VERSION_CODES.R)) {
        // When SDK version is bellow 30, InlinePresentationSpec is not available and null
        // must be returned.
        null
    } else if (isInlineAutofillEnabled) {
        this?.inlineSuggestionsRequest?.inlinePresentationSpecs.orEmpty()
    } else {
        emptyList()
    }

/**
 * Extract the max inline suggestions count. If the OS is below Android R, this will always
 * return 0.
 */
fun FillRequest?.getMaxInlineSuggestionsCount(
    autofillAppInfo: AutofillAppInfo,
    isInlineAutofillEnabled: Boolean,
): Int =
    if (this != null &&
        isInlineAutofillEnabled &&
        autofillAppInfo.isVersionAtLeast(version = Build.VERSION_CODES.R)
    ) {
        inlineSuggestionsRequest?.maxSuggestionCount ?: 0
    } else {
        0
    }
