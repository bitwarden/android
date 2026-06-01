package com.x8bit.bitwarden.data.autofill.builder

import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.FilledData

/**
 * A class for converting parsed autofill data into filled data that is ready to be loaded into a
 * fill response.
 */
interface FilledDataBuilder {
    /**
     * Construct filled data from [autofillRequest].
     */
    suspend fun build(
        autofillRequest: AutofillRequest.Fillable,
    ): FilledData
}
