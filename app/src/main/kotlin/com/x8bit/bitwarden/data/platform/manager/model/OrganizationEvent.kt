package com.x8bit.bitwarden.data.platform.manager.model

import com.bitwarden.network.model.OrganizationEventType

/**
 * A representation of events used for organization tracking.
 */
sealed class OrganizationEvent {
    /**
     * The type of event this model represents.
     */
    abstract val type: OrganizationEventType

    /**
     * The optional cipher ID.
     */
    abstract val cipherId: String?

    /**
     * Tracks when a value is successfully auto-filled
     */
    data class CipherClientAutoFilled(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_AUTO_FILLED
    }

    /**
     * Tracks when a card code is copied.
     */
    data class CipherClientCopiedCardCode(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_COPIED_CARD_CODE
    }

    /**
     * Tracks when a hidden field is copied.
     */
    data class CipherClientCopiedHiddenField(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_COPIED_HIDDEN_FIELD
    }

    /**
     * Tracks when a password is copied.
     */
    data class CipherClientCopiedPassword(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_COPIED_PASSWORD
    }

    /**
     * Tracks when a card code is made visible.
     */
    data class CipherClientToggledCardCodeVisible(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_TOGGLED_CARD_CODE_VISIBLE
    }

    /**
     * Tracks when a card number is made visible.
     */
    data class CipherClientToggledCardNumberVisible(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_TOGGLED_CARD_NUMBER_VISIBLE
    }

    /**
     * Tracks when a hidden field is made visible.
     */
    data class CipherClientToggledHiddenFieldVisible(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_TOGGLED_HIDDEN_FIELD_VISIBLE
    }

    /**
     * Tracks when a password is made visible.
     */
    data class CipherClientToggledPasswordVisible(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_TOGGLED_PASSWORD_VISIBLE
    }

    /**
     * Tracks when a cipher is viewed.
     */
    data class CipherClientViewed(
        override val cipherId: String,
    ) : OrganizationEvent() {
        override val type: OrganizationEventType
            get() = OrganizationEventType.CIPHER_CLIENT_VIEWED
    }
}
