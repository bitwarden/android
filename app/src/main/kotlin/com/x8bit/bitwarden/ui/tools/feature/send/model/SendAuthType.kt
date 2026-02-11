package com.x8bit.bitwarden.ui.tools.feature.send.model

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Enum representing the authentication types for a send.
 *
 * @param text The display text for the authentication type.
 * @param supportingText The display text for the supporting component.
 */
enum class SendAuthType(
    val text: Text,
    val supportingText: Text?,
) {
    /**
     * Anyone with the link can view the send.
     */
    NONE(
        text = BitwardenString.anyone_with_the_link.asText(),
        supportingText = BitwardenString.anyone_with_link_can_view_send.asText(),
    ),

    /**
     * Specific people who verify their email can view the send.
     */
    EMAIL(
        text = BitwardenString.specific_people.asText(),
        supportingText = BitwardenString.specific_people_verification_info.asText(),
    ),

    /**
     * Anyone with the password set by the user can view the send.
     */
    PASSWORD(
        text = BitwardenString.anyone_with_password.asText(),
        supportingText = null, // Not used, password field shown instead
    ),
}
