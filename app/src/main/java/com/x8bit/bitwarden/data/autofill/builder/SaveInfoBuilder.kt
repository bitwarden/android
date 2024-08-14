package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition

/**
 * A builder for converting processed autofill request data into save info.
 */
interface SaveInfoBuilder {
    /**
     * Build a save info out the provided data. If that isn't possible, return null.
     *
     * @param autofillPartition The portion of the processed [FillRequest] that will be filled.
     * @param fillRequest The [FillRequest] that initiated the autofill flow.
     * @param packageName The package name that was extracted from the [FillRequest].
     */
    fun build(
        autofillPartition: AutofillPartition,
        fillRequest: FillRequest,
        packageName: String?,
    ): SaveInfo?
}
