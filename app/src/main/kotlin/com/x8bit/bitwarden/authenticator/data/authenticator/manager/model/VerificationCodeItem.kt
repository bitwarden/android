package com.x8bit.bitwarden.authenticator.data.authenticator.manager.model

import com.bitwarden.core.LoginUriView

/**
 * Models the items returned by the TotpCodeManager.
 *
 * @property code The verification code for the item.
 * @property totpCode The totp code for the item.
 * @property periodSeconds The time span where the code is valid in seconds.
 * @property timeLeftSeconds The seconds remaining until a new code is required.
 * @property issueTime The time the verification code was issued.
 * @property uriLoginViewList The [LoginUriView] for the login item.
 * @property id The cipher id of the item.
 * @property name The name of the cipher item.
 * @property username The username associated with the item.
 */
data class VerificationCodeItem(
    val code: String,
    val totpCode: String,
    val periodSeconds: Int,
    val timeLeftSeconds: Int,
    val issueTime: Long,
    val uriLoginViewList: List<LoginUriView>?,
    val id: String,
    val name: String,
    val username: String?,
)
