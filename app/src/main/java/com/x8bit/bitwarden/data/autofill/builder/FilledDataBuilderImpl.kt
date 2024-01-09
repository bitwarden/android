package com.x8bit.bitwarden.data.autofill.builder

import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem

/**
 * The default [FilledDataBuilder]. This converts parsed autofill data into filled data that is
 * ready to be loaded into an autofill response.
 */
class FilledDataBuilderImpl : FilledDataBuilder {
    override suspend fun build(autofillRequest: AutofillRequest.Fillable): FilledData {
        // TODO: determine whether or not the vault is locked (BIT-1296)

        val filledItems = autofillRequest
            .partition
            .views
            .map(AutofillView::toFilledItem)

        // TODO: perform fulfillment with dummy data (BIT-1315)

        return FilledData(
            filledItems = filledItems,
            ignoreAutofillIds = autofillRequest.ignoreAutofillIds,
        )
    }
}

/**
 * Map this [AutofillView] to a [FilledItem].
 */
private fun AutofillView.toFilledItem(): FilledItem =
    FilledItem(
        autofillId = autofillId,
    )
