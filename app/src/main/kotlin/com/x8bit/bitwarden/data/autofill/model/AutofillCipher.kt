package com.x8bit.bitwarden.data.autofill.model

import androidx.annotation.DrawableRes
import com.bitwarden.core.Uuid
import com.bitwarden.ui.platform.resource.BitwardenDrawable

/**
 * A paired down model of the CipherView for use within the autofill feature.
 */
sealed class AutofillCipher {
    /**
     * The icon res to represent this [AutofillCipher].
     */
    abstract val iconRes: Int

    /**
     * Whether TOTP is enabled for this cipher.
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
        val brand: String,
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = BitwardenDrawable.ic_payment_card

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
        val website: String,
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = BitwardenDrawable.ic_globe
    }

    /**
     * The identity [AutofillCipher] model. This contains all the data for building
     * an identity partition.
     *
     * @param fullName The identity's name parts joined for filling a combined full-name field.
     * @param fullAddress The identity's address parts joined for filling a combined full-address
     * field.
     */
    data class Identity(
        override val cipherId: String?,
        override val name: String,
        override val subtitle: String,
        val fullName: String,
        val fullAddress: String,
        val title: String,
        val firstName: String,
        val middleName: String,
        val lastName: String,
        val address1: String,
        val address2: String,
        val address3: String,
        val city: String,
        val state: String,
        val postalCode: String,
        val country: String,
        val company: String,
        val email: String,
        val phone: String,
        val ssn: String,
        val passportNumber: String,
        val licenseNumber: String,
    ) : AutofillCipher() {
        override val iconRes: Int
            @DrawableRes get() = BitwardenDrawable.ic_id_card

        override val isTotpEnabled: Boolean
            get() = false
    }
}
