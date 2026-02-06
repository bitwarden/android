package com.x8bit.bitwarden.ui.tools.feature.send.model

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Enum representing the authentication types for a send.
 *
 * @param text The display text for the authentication type.
 * @param supportingTextRes The string resource ID for the supporting text.
 */
enum class SendAuthType(
    val text: Text,
    val supportingTextRes: Int?,
) {
    /**
     * Anyone with the link can view the send.
     */
    NONE(
        text = BitwardenString.anyone_with_the_link.asText(),
        supportingTextRes = BitwardenString.anyone_with_link_can_view_send,
    ),

    /**
     * Specific people who verify their email can view the send.
     */
    EMAIL(
        text = BitwardenString.specific_people.asText(),
        supportingTextRes = BitwardenString.specific_people_verification_info,
    ),

    /**
     * Anyone with the password set by the user can view the send.
     */
    PASSWORD(
        text = BitwardenString.anyone_with_password.asText(),
        supportingTextRes = null, // Not used, password field shown instead
    ),
}
