package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledData

/**
 * A component for building fill responses out of fulfilled internal models.
 */
interface FillResponseBuilder {
    /**
     * Build the [filledData] into a [FillResponse]. Return null if not possible.
     */
    fun build(
        autofillAppInfo: AutofillAppInfo,
        filledData: FilledData,
        saveInfo: SaveInfo?,
    ): FillResponse?
}
