package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.FillResponse
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.util.buildDataset

/**
 * The default implementation for [FillResponseBuilder]. This is a component for compiling fulfilled
 * internal models into a [FillResponse] whenever possible.
 */
class FillResponseBuilderImpl : FillResponseBuilder {
    override fun build(
        autofillAppInfo: AutofillAppInfo,
        filledData: FilledData,
    ): FillResponse? =
        if (filledData.filledPartitions.any { it.filledItems.isNotEmpty() }) {
            val fillResponseBuilder = FillResponse.Builder()

            filledData
                .filledPartitions
                .forEach { filledPartition ->
                    // It won't be empty but we really don't want to make an empty dataset,
                    // it causes a crash.
                    if (filledPartition.filledItems.isNotEmpty()) {
                        // We build a dataset for each filled partition. A filled partition is a
                        // copy of all the views that we are going to fill, loaded with the data
                        // from one of the ciphers that can fulfill this partition type.
                        val dataset = filledPartition.buildDataset(
                            autofillAppInfo = autofillAppInfo,
                        )

                        // Load the dataset into the fill request.
                        fillResponseBuilder.addDataset(dataset)
                    }
                }

            // TODO: add vault item dataset (BIT-1296)

            fillResponseBuilder
                .setIgnoredIds(*filledData.ignoreAutofillIds.toTypedArray())
                .build()
        } else {
            // It is impossible for [filledData] to be empty due to the way it is constructed.
            // However, the [FillRequest] requires at least one dataset or an authentication intent
            // with a presentation view. Neither of these make sense in the case where we have no
            // views to fill. What we are supposed to do when we cannot fulfill a request is
            // replace [FillResponse] with null in order to avoid this crash.
            null
        }
}
