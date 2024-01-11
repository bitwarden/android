package com.x8bit.bitwarden.data.autofill.builder

import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.FilledPartition

/**
 * The default [FilledDataBuilder]. This converts parsed autofill data into filled data that is
 * ready to be loaded into an autofill response.
 */
class FilledDataBuilderImpl : FilledDataBuilder {
    override suspend fun build(autofillRequest: AutofillRequest.Fillable): FilledData {
        // TODO: determine whether or not the vault is locked (BIT-1296)

        val filledPartitions = when (autofillRequest.partition) {
            is AutofillPartition.Card -> {
                // TODO: perform fulfillment with dummy data (BIT-1315)
                listOf(
                    fillCardPartition(
                        autofillViews = autofillRequest.partition.views,
                    ),
                )
            }

            is AutofillPartition.Login -> {
                // TODO: perform fulfillment with dummy data (BIT-1315)
                listOf(
                    fillLoginPartition(
                        autofillViews = autofillRequest.partition.views,
                    ),
                )
            }
        }

        return FilledData(
            filledPartitions = filledPartitions,
            ignoreAutofillIds = autofillRequest.ignoreAutofillIds,
        )
    }

    /**
     * Construct a [FilledPartition] by fulfilling the card [autofillViews] with data.
     */
    private fun fillCardPartition(
        autofillViews: List<AutofillView.Card>,
    ): FilledPartition {
        val filledItems = autofillViews
            .map { autofillView ->
                FilledItem(
                    autofillId = autofillView.autofillId,
                )
            }

        return FilledPartition(
            filledItems = filledItems,
        )
    }

    /**
     * Construct a [FilledPartition] by fulfilling the login [autofillViews] with data.
     */
    private fun fillLoginPartition(
        autofillViews: List<AutofillView.Login>,
    ): FilledPartition {
        val filledItems = autofillViews
            .map { autofillView ->
                FilledItem(
                    autofillId = autofillView.autofillId,
                )
            }

        return FilledPartition(
            filledItems = filledItems,
        )
    }
}
