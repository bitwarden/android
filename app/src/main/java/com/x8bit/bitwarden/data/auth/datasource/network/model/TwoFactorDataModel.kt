package com.x8bit.bitwarden.data.auth.datasource.network.model

/**
 * Hold the information necessary to add two-factor authorization
 * to a login request.
 *
 * @property code The two-factor code.
 * @property method The two-factor method.
 * @property remember The two-factor remember setting.
 */
data class TwoFactorDataModel(
    val code: String,
    val method: String,
    val remember: Boolean,
)
