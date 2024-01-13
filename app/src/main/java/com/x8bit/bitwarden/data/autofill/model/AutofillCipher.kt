package com.x8bit.bitwarden.data.autofill.model

/**
 * A paired down model of the CipherView for use within the autofill feature.
 */
sealed class AutofillCipher {
    /**
     * The name of the cipher.
     */
    abstract val name: String

    /**
     * The subtitle for giving additional context to the cipher.
     */
    abstract val subtitle: String

    /**
     * The card [AutofillCipher] model. This contains all of the data for building fulfilling a card
     * partition.
     */
    data class Card(
        override val name: String,
        override val subtitle: String,
        val cardholderName: String,
        val code: String,
        val expirationMonth: String,
        val expirationYear: String,
        val number: String,
    ) : AutofillCipher()

    /**
     * The card [AutofillCipher] model. This contains all of the data for building fulfilling a
     * login partition.
     */
    data class Login(
        override val name: String,
        override val subtitle: String,
        val password: String,
        val username: String,
    ) : AutofillCipher()
}
