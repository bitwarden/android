package com.x8bit.bitwarden.authenticator.data.authenticator.manager.model

/**
 * Models the items returned by the TotpCodeManager.
 *
 * @property code The verification code for the item.
 * @property totpCode The totp code for the item.
 * @property periodSeconds The time span where the code is valid in seconds.
 * @property timeLeftSeconds The seconds remaining until a new code is required.
 * @property issueTime The time the verification code was issued.
 * @property id The cipher id of the item.
 * @property username The username associated with the item.
 */
data class VerificationCodeItem(
    val code: String,
    val totpCode: String,
    val periodSeconds: Int,
    val timeLeftSeconds: Int,
    val issueTime: Long,
    val id: String,
    val username: String?,
    val issuer: String?,
) {
    /**
     * The composite label of the authenticator item.
     *  ```
     *  label = issuer (“:” / “%3A”) *”%20” username
     *  ```
     */
    val label = if (issuer != null) {
        issuer + username?.let { ":$it" }.orEmpty()
    } else {
        username.orEmpty()
    }
}
