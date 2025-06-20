package com.x8bit.bitwarden.data.autofill.util

import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem

/**
 * Convert the [AutofillRequest.Fillable] to an [AutofillSaveItem].
 */
fun AutofillRequest.Fillable.toAutofillSaveItem(): AutofillSaveItem =
    when (this.partition) {
        is AutofillPartition.Card -> {
            AutofillSaveItem.Card(
                number = partition.numberSaveValue,
                expirationMonth = partition.expirationMonthSaveValue,
                expirationYear = partition.expirationYearSaveValue,
                securityCode = partition.securityCodeSaveValue,
            )
        }

        is AutofillPartition.Login -> {
            // Skip the scheme for the save value.
            val uri = this
                .uri
                ?.replace("https://", "")
                ?.replace("http://", "")

            AutofillSaveItem.Login(
                username = partition.usernameSaveValue,
                password = partition.passwordSaveValue,
                uri = uri,
            )
        }
    }
