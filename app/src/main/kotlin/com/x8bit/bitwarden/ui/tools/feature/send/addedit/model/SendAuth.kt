package com.x8bit.bitwarden.ui.tools.feature.send.addedit.model

import android.os.Parcelable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize

/**
 * Sealed class representing the authentication types for a send.
 */
sealed class SendAuth : Parcelable {
    /**
     * The display text for the authentication type.
     */
    abstract val text: Text

    /**
     * The display text for the supporting component.
     */
    abstract val supportingText: Text?

    /**
     * Anyone with the link can view the send.
     */
    @Parcelize
    data object None : SendAuth() {
        override val text: Text
            get() = BitwardenString.anyone_with_the_link.asText()

        override val supportingText: Text
            get() = BitwardenString.anyone_with_link_can_view_send.asText()
    }

    /**
     * Specific people who verify their email can view the send.
     *
     * @property emails The list of email addresses that can view the send.
     */
    @Parcelize
    data class Email(
        val emails: ImmutableList<AuthEmail> = persistentListOf(AuthEmail(value = "")),
    ) : SendAuth() {
        override val text: Text
            get() = BitwardenString.specific_people.asText()

        override val supportingText: Text
            get() = BitwardenString
                .specific_people_verification_info.asText()
    }

    /**
     * Anyone with the password set by the user can view the send.
     * Note: The password value is stored separately in the state's `passwordInput` field.
     */
    @Parcelize
    data object Password : SendAuth() {
        override val text: Text
            get() = BitwardenString.anyone_with_password.asText()

        override val supportingText: Text?
            get() = null // Not used, password field shown instead
    }
}
