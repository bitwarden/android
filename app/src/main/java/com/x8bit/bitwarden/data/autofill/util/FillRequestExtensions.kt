package com.x8bit.bitwarden.data.autofill.util

import android.annotation.SuppressLint
import android.os.Build
import android.service.autofill.FillRequest
import android.widget.inline.InlinePresentationSpec
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo

/**
 * Extract the list of [InlinePresentationSpec]s. If it fails, return an empty list.
 */
@SuppressLint("NewApi")
fun FillRequest?.getInlinePresentationSpecs(
    autofillAppInfo: AutofillAppInfo,
    isInlineAutofillEnabled: Boolean,
): List<InlinePresentationSpec> =
    if (this != null &&
        isInlineAutofillEnabled &&
        autofillAppInfo.sdkInt >= Build.VERSION_CODES.R
    ) {
        inlineSuggestionsRequest?.inlinePresentationSpecs ?: emptyList()
    } else {
        emptyList()
    }

/**
 * Extract the max inline suggestions count. If the OS is below Android R, this will always
 * return 0.
 */
@SuppressLint("NewApi")
fun FillRequest?.getMaxInlineSuggestionsCount(
    autofillAppInfo: AutofillAppInfo,
    isInlineAutofillEnabled: Boolean,
): Int =
    if (this != null &&
        isInlineAutofillEnabled &&
        autofillAppInfo.sdkInt >= Build.VERSION_CODES.R
    ) {
        inlineSuggestionsRequest?.maxSuggestionCount ?: 0
    } else {
        0
    }
