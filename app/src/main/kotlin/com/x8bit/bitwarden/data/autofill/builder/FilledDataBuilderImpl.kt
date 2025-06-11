package com.x8bit.bitwarden.data.autofill.builder

import android.widget.inline.InlinePresentationSpec
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.util.buildFilledItemOrNull

/**
 * The maximum amount of filled partitions the user will see. Viewing the rest will require opening
 * the vault.
 *
 * Note: The vault item is not included in this count.
 */
private const val MAX_FILLED_PARTITIONS_COUNT: Int = 20

/**
 * The maximum amount of inline suggestions the user will see. Viewing the rest will require
 * opening the vault.
 *
 * Note: The vault item is not included in this count.
 */
private const val MAX_INLINE_SUGGESTION_COUNT: Int = 5

/**
 * The default [FilledDataBuilder]. This converts parsed autofill data into filled data that is
 * ready to be loaded into an autofill response.
 */
class FilledDataBuilderImpl(
    private val autofillCipherProvider: AutofillCipherProvider,
) : FilledDataBuilder {
    override suspend fun build(autofillRequest: AutofillRequest.Fillable): FilledData {
        val isVaultLocked = autofillCipherProvider.isVaultLocked()

        // Subtract one to make sure there is space for the vault item.
        val maxCipherInlineSuggestionsCount = (autofillRequest.maxInlineSuggestionsCount - 1)
            .coerceAtMost(maximumValue = MAX_INLINE_SUGGESTION_COUNT)

        // Track the number of inline suggestions that have been added.
        var inlineSuggestionsAdded = 0

        // A function for managing the cipher InlinePresentationSpecs.
        fun getCipherInlinePresentationOrNull(): InlinePresentationSpec? =
            if (inlineSuggestionsAdded < maxCipherInlineSuggestionsCount) {
                // Use getOrLastOrNull so if the list has run dry take the last spec.
                autofillRequest
                    .inlinePresentationSpecs
                    ?.getOrLastOrNull(inlineSuggestionsAdded)
            } else {
                null
            }
                ?.also { inlineSuggestionsAdded += 1 }

        val filledPartitions = when (autofillRequest.partition) {
            is AutofillPartition.Card -> {
                autofillCipherProvider
                    .getCardAutofillCiphers()
                    .map { autofillCipher ->
                        fillCardPartition(
                            autofillCipher = autofillCipher,
                            autofillViews = autofillRequest.partition.views,
                            inlinePresentationSpec = getCipherInlinePresentationOrNull(),
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
                                    inlinePresentationSpec = getCipherInlinePresentationOrNull(),
                                )
                            }
                    }
                    ?: emptyList()
            }
        }

        // Use getOrLastOrNull so if the list has run dry take the last spec.
        val vaultItemInlinePresentationSpec = autofillRequest
            .inlinePresentationSpecs
            ?.getOrLastOrNull(inlineSuggestionsAdded)

        return FilledData(
            filledPartitions = filledPartitions.take(n = MAX_FILLED_PARTITIONS_COUNT),
            ignoreAutofillIds = autofillRequest.ignoreAutofillIds,
            originalPartition = autofillRequest.partition,
            uri = autofillRequest.uri,
            vaultItemInlinePresentationSpec = vaultItemInlinePresentationSpec,
            isVaultLocked = isVaultLocked,
        )
    }

    /**
     * Construct a [FilledPartition] by fulfilling the card [autofillViews] with data from the
     * card [autofillCipher].
     */
    private fun fillCardPartition(
        autofillCipher: AutofillCipher.Card,
        autofillViews: List<AutofillView.Card>,
        inlinePresentationSpec: InlinePresentationSpec?,
    ): FilledPartition {
        val filledItems = autofillViews
            .mapNotNull { autofillView ->
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
            inlinePresentationSpec = inlinePresentationSpec,
        )
    }

    /**
     * Construct a [FilledPartition] by fulfilling the login [autofillViews] with data from the
     * login [autofillCipher].
     */
    private fun fillLoginPartition(
        autofillCipher: AutofillCipher.Login,
        autofillViews: List<AutofillView.Login>,
        inlinePresentationSpec: InlinePresentationSpec?,
    ): FilledPartition {
        val filledItems = autofillViews
            .mapNotNull { autofillView ->
                val value = when (autofillView) {
                    is AutofillView.Login.Username -> autofillCipher.username
                    is AutofillView.Login.Password -> autofillCipher.password
                }
                autofillView.buildFilledItemOrNull(
                    value = value,
                )
            }

        return FilledPartition(
            autofillCipher = autofillCipher,
            filledItems = filledItems,
            inlinePresentationSpec = inlinePresentationSpec,
        )
    }
}

/**
 * Get the item at the [index]. If that fails, return the last item in the list. If that also fails,
 * return null.
 */
private fun <T> List<T>.getOrLastOrNull(index: Int): T? =
    getOrNull(index)
        ?: lastOrNull()
