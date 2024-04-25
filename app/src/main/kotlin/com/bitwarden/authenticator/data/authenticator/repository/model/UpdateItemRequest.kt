package com.bitwarden.authenticator.data.authenticator.repository.model

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType

/**
 * Models a request to modify an existing authenticator item.
 *
 * @property key Key used to generate verification codes for the authenticator item.
 * @property accountName Required account or username associated with the item.
 * @property type Type of authenticator item.
 * @property algorithm Hashing algorithm applied to the authenticator item verification code.
 * @property period Time, in seconds, the authenticator item verification code is valid.
 * @property digits Number of digits contained in the verification code for this authenticator item.
 * @property issuer Entity that provided the authenticator item.
 */
data class UpdateItemRequest(
    val key: String,
    val accountName: String?,
    val type: AuthenticatorItemType,
    val algorithm: AuthenticatorItemAlgorithm,
    val period: Int,
    val digits: Int,
    val issuer: String,
) {
    /**
     * The composite label of the authenticator item. Derived from combining [issuer] and [accountName]
     *  ```
     *  label = accountName /issuer (“:” / “%3A”) *”%20” accountName
     *  ```
     */
    val label = if (accountName.isNullOrBlank()) {
        issuer
    } else {
        "$issuer:$accountName"
    }
}
