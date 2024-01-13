package com.x8bit.bitwarden.data.autofill.builder

import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.util.buildFilledItemOrNull

/**
 * The default [FilledDataBuilder]. This converts parsed autofill data into filled data that is
 * ready to be loaded into an autofill response.
 */
class FilledDataBuilderImpl(
    private val autofillCipherProvider: AutofillCipherProvider,
) : FilledDataBuilder {
    override suspend fun build(autofillRequest: AutofillRequest.Fillable): FilledData {
        // TODO: determine whether or not the vault is locked (BIT-1296)

        val filledPartitions = when (autofillRequest.partition) {
            is AutofillPartition.Card -> {
                autofillCipherProvider
                    .getCardAutofillCiphers()
                    .map { autofillCipher ->
                        fillCardPartition(
                            autofillCipher = autofillCipher,
                            autofillViews = autofillRequest.partition.views,
                        )
                    }
            }

            is AutofillPartition.Login -> {
                autofillRequest
                    .uri
                    ?.let { nonNullUri ->
                        autofillCipherProvider
                            .getLoginAutofillCiphers(
                                uri = nonNullUri,
                            )
                            .map { autofillCipher ->
                                fillLoginPartition(
                                    autofillCipher = autofillCipher,
                                    autofillViews = autofillRequest.partition.views,
                                )
                            }
                    }
                    ?: emptyList()
            }
        }

        return FilledData(
            filledPartitions = filledPartitions,
            ignoreAutofillIds = autofillRequest.ignoreAutofillIds,
        )
    }

    /**
     * Construct a [FilledPartition] by fulfilling the card [autofillViews] with data from the
     * card [autofillCipher].
     */
    private fun fillCardPartition(
        autofillCipher: AutofillCipher.Card,
        autofillViews: List<AutofillView.Card>,
    ): FilledPartition {
        val filledItems = autofillViews
            .map { autofillView ->
                val value = when (autofillView) {
                    is AutofillView.Card.ExpirationMonth -> autofillCipher.expirationMonth
                    is AutofillView.Card.ExpirationYear -> autofillCipher.expirationYear
                    is AutofillView.Card.Number -> autofillCipher.number
                    is AutofillView.Card.SecurityCode -> autofillCipher.code
                }
                autofillView.buildFilledItemOrNull(
                    value = value,
                )
            }

        return FilledPartition(
            autofillCipher = autofillCipher,
            filledItems = filledItems,
        )
    }

    /**
     * Construct a [FilledPartition] by fulfilling the login [autofillViews] with data from the
     * login [autofillCipher].
     */
    private fun fillLoginPartition(
        autofillCipher: AutofillCipher.Login,
        autofillViews: List<AutofillView.Login>,
    ): FilledPartition {
        val filledItems = autofillViews
            .map { autofillView ->
                val value = when (autofillView) {
                    is AutofillView.Login.EmailAddress,
                    is AutofillView.Login.Username,
                    -> autofillCipher.username

                    is AutofillView.Login.Password -> autofillCipher.password
                }
                autofillView.buildFilledItemOrNull(
                    value = value,
                )
            }

        return FilledPartition(
            autofillCipher = autofillCipher,
            filledItems = filledItems,
        )
    }
}
