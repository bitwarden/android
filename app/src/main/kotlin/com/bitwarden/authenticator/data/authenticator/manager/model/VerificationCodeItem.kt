package com.bitwarden.authenticator.data.authenticator.manager.model

import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem

/**
 * Models the items returned by the TotpCodeManager which are then used to display rows
 * of verification items.
 *
 * @property code The verification code for the item.
 * @property periodSeconds The time span where the code is valid in seconds.
 * @property timeLeftSeconds The seconds remaining until a new code is required.
 * @property issueTime The time the verification code was issued.
 * @property id The cipher id of the item.
 * @property username The username associated with the item.
 */
data class VerificationCodeItem(
    val code: String,
    val periodSeconds: Int,
    val timeLeftSeconds: Int,
    val issueTime: Long,
    val id: String,
    val issuer: String?,
    val label: String?,
    val source: AuthenticatorItem.Source,
) {
    /**
     * The composite label of the authenticator item. Used for constructing an OTPAuth URI.
     *  ```
     *  label = issuer (“:” / “%3A”) *”%20” username
     *  ```
     */
    val otpAuthUriLabel = if (issuer != null) {
        issuer + label?.let { ":$it" }.orEmpty()
    } else {
        label.orEmpty()
    }
}
