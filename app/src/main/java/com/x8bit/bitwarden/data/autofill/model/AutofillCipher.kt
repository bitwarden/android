package com.x8bit.bitwarden.data.autofill.model

import androidx.annotation.DrawableRes
import com.x8bit.bitwarden.R

/**
 * A paired down model of the CipherView for use within the autofill feature.
 */
sealed class AutofillCipher {
    /**
     * The icon res to represent this [AutofillCipher].
     */
    abstract val iconRes: Int

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
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = R.drawable.ic_card_item
    }

    /**
     * The card [AutofillCipher] model. This contains all of the data for building fulfilling a
     * login partition.
     */
    data class Login(
        override val name: String,
        override val subtitle: String,
        val password: String,
        val username: String,
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = R.drawable.ic_login_item
    }
}
