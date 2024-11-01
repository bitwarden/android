package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView

/**
 * If someone has multiple AMEX cards, they tend to have the same last 4 digits. So we provide a
 * fifth card number digit to allow users to differentiate between cards based on the subtitle.
 */
private const val AMEX_DIGITS_DISPLAYED: Int = 5

/**
 * A normal number of card number digits to show in a subtitle for all non-AMEX cards.
 */
private const val CARD_DIGITS_DISPLAYED: Int = 4

/**
 * The subtitle for a [CipherView] used to give extra information about a particular instance.
 */
val CipherView.subtitle: String?
    get() = when (type) {
        CipherType.LOGIN -> this.login?.username.orEmpty()
        CipherType.CARD -> {
            this
                .card
                ?.let { cardView ->
                    when {
                        cardView.brand.isNullOrBlank() -> cardView.subtitleCardNumber
                        cardView.subtitleCardNumber.isNullOrBlank() -> cardView.brand
                        else -> "${cardView.brand}, *${cardView.subtitleCardNumber}"
                    }
                }
        }

        CipherType.IDENTITY -> {
            this
                .identity
                ?.let { identityView ->
                    when {
                        identityView.firstName.isNullOrBlank() -> identityView.lastName
                        identityView.lastName.isNullOrBlank() -> identityView.firstName
                        else -> "${identityView.firstName} ${identityView.lastName}"
                    }
                }
        }

        CipherType.SECURE_NOTE,
        CipherType.SSH_KEY,
            -> null
    }

/**
 * Get the card number as it should be shown in the subtitle.
 */
private val CardView.subtitleCardNumber: String?
    get() {
        val digitsDisplayedCount = if (this.number.isAmEx) {
            AMEX_DIGITS_DISPLAYED
        } else {
            CARD_DIGITS_DISPLAYED
        }
        return this
            .number
            ?.takeLast(digitsDisplayedCount)
    }

/**
 * American express cards start with "34" or "37". This function determine whether a string
 * matches that amex standard.
 */
private val String?.isAmEx: Boolean
    get() = this?.startsWith("34") == true || this?.startsWith("37") == true
