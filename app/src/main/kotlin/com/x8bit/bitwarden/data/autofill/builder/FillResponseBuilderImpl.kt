package com.x8bit.bitwarden.data.autofill.builder

import android.content.IntentSender
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.autofill.util.buildVaultItemDataset
import com.x8bit.bitwarden.data.autofill.util.createTotpCopyIntentSender
import com.x8bit.bitwarden.data.autofill.util.fillableAutofillIds

/**
 * The default implementation for [FillResponseBuilder]. This is a component for compiling fulfilled
 * internal models into a [FillResponse] whenever possible.
 */
class FillResponseBuilderImpl : FillResponseBuilder {
    override fun build(
        autofillAppInfo: AutofillAppInfo,
        filledData: FilledData,
        saveInfo: SaveInfo?,
    ): FillResponse? =
        if (filledData.fillableAutofillIds.isNotEmpty()) {
            val fillResponseBuilder = FillResponse.Builder()

            saveInfo
                ?.let { nonNullSaveInfo ->
                    fillResponseBuilder.setSaveInfo(nonNullSaveInfo)
                }

            filledData
                .filledPartitions
                .forEach { filledPartition ->
                    // We really don't want to make an empty dataset as it causes a crash.
                    if (filledPartition.filledItems.isNotEmpty()) {
                        // We build a dataset for each filled partition. A filled partition is a
                        // copy of all the views that we are going to fill, loaded with the data
                        // from one of the ciphers that can fulfill this partition type.
                        val authIntentSender = filledPartition.toAuthIntentSenderOrNull(
                            autofillAppInfo = autofillAppInfo,
                        )
                        val dataset = filledPartition.buildDataset(
                            authIntentSender = authIntentSender,
                            autofillAppInfo = autofillAppInfo,
                        )

                        // Load the dataset into the fill request.
                        fillResponseBuilder.addDataset(dataset)
                    }
                }

            fillResponseBuilder
                // Add the Vault Item
                .addDataset(
                    filledData
                        .buildVaultItemDataset(
                            autofillAppInfo = autofillAppInfo,
                        ),
                )
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

/**
 * Convert this [FilledPartition] and [autofillAppInfo] into an [IntentSender] if totp is enabled
 * and there the [FilledPartition.autofillCipher] has a valid cipher id.
 */
private fun FilledPartition.toAuthIntentSenderOrNull(
    autofillAppInfo: AutofillAppInfo,
): IntentSender? =
    autofillCipher
        .cipherId
        ?.let { cipherId ->
            // We always do this even if there is no TOTP code because we want to log the events
            createTotpCopyIntentSender(
                cipherId = cipherId,
                context = autofillAppInfo.context,
            )
        }
