package com.x8bit.bitwarden.data.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents raw data from a user completing a form and deciding to save that data to their vault
 * via the autofill framework.
 */
sealed class AutofillSaveItem : Parcelable {

    /**
     * Data for a card item.
     *
     * @property number The actual card number (if applicable).
     * @property expirationMonth The expiration month in string form (if applicable).
     * @property expirationYear The expiration year in string form (if applicable).
     * @property securityCode The security code for the card (if applicable).
     */
    @Parcelize
    data class Card(
        val number: String?,
        val expirationMonth: String?,
        val expirationYear: String?,
        val securityCode: String?,
    ) : AutofillSaveItem()

    /**
     * Data for a login item.
     *
     * @property username The username/email for the login (if applicable).
     * @property password The password for the login (if applicable).
     * @property uri The URI associated with the login (if applicable).
     */
    @Parcelize
    data class Login(
        val username: String?,
        val password: String?,
        val uri: String?,
    ) : AutofillSaveItem()
}
