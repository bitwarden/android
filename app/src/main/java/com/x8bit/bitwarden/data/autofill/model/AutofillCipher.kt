package com.x8bit.bitwarden.data.autofill.model

import androidx.annotation.DrawableRes
import com.bitwarden.core.Uuid
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
     * Whether or not TOTP is enabled for this cipher.
     */
    abstract val isTotpEnabled: Boolean

    /**
     * The name of the cipher.
     */
    abstract val name: String

    /**
     * The subtitle for giving additional context to the cipher.
     */
    abstract val subtitle: String

    /**
     * The ID that corresponds to the CipherView used to create this [AutofillCipher].
     */
    abstract val cipherId: String?

    /**
     * The card [AutofillCipher] model. This contains all of the data for building fulfilling a card
     * partition.
     */
    data class Card(
        override val cipherId: String?,
        override val name: String,
        override val subtitle: String,
        val cardholderName: String,
        val code: String,
        val expirationMonth: String,
        val expirationYear: String,
        val number: String,
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = R.drawable.ic_payment_card

        override val isTotpEnabled: Boolean
            get() = false
    }

    /**
     * The card [AutofillCipher] model. This contains all of the data for building fulfilling a
     * login partition.
     */
    data class Login(
        override val cipherId: Uuid?,
        override val isTotpEnabled: Boolean,
        override val name: String,
        override val subtitle: String,
        val password: String,
        val username: String,
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = R.drawable.ic_globe
    }
}
