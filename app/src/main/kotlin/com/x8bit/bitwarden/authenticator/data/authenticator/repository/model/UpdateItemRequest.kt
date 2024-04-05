package com.x8bit.bitwarden.authenticator.data.authenticator.repository.model

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType

/**
 * Models a request to modify an existing authenticator item.
 *
 * @property type Type of authenticator item.
 * @property algorithm Hashing algorithm applied to the authenticator item verification code.
 * @property period Time, in seconds, the authenticator item verification code is valid. Default is
 * 30 seconds.
 * @property digits Number of digits contained in the verification code for this authenticator item.
 * Default is 6 digits.
 * @property key Key used to generate verification codes for the authenticator item.
 * @property issuer Entity that provided the authenticator item.
 * @property username Optional username associated with .
 */
data class UpdateItemRequest(
    val type: AuthenticatorItemType,
    val algorithm: AuthenticatorItemAlgorithm = AuthenticatorItemAlgorithm.SHA1,
    val period: Int = 30,
    val digits: Int = 6,
    val key: String,
    val issuer: String?,
    val username: String?,
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
