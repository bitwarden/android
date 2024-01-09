package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.util.applyOverlayToDataset
import com.x8bit.bitwarden.ui.autofill.buildAutofillRemoteViews

/**
 * The default implementation for [FillResponseBuilder]. This is a component for compiling fulfilled
 * internal models into a [FillResponse] whenever possible.
 */
class FillResponseBuilderImpl : FillResponseBuilder {
    override fun build(
        autofillAppInfo: AutofillAppInfo,
        filledData: FilledData,
    ): FillResponse? =
        if (filledData.filledItems.isNotEmpty()) {
            val remoteViewsPlaceholder = buildAutofillRemoteViews(
                packageName = autofillAppInfo.packageName,
                context = autofillAppInfo.context,
            )
            val datasetBuilder = Dataset.Builder()

            // Set UI for each valid autofill view.
            filledData.filledItems.forEach { filledItem ->
                filledItem.applyOverlayToDataset(
                    appInfo = autofillAppInfo,
                    datasetBuilder = datasetBuilder,
                    remoteViews = remoteViewsPlaceholder,
                )
            }
            val dataset = datasetBuilder.build()
            FillResponse.Builder()
                .addDataset(dataset)
                .setIgnoredIds(*filledData.ignoreAutofillIds.toTypedArray())
                .build()
        } else {
            // It is impossible for an `AutofillPartition` to be empty due to the way it is
            // constructed. However, the [FillRequest] requires at least one dataset or an
            // authentication intent with a presentation view. Neither of these make sense in the
            // case where we have no views to fill. What we are supposed to do when we cannot
            // fulfill a request is replace [FillResponse] with null in order to avoid this crash.
            null
        }
}
