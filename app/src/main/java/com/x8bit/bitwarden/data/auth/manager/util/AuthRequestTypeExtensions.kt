package com.x8bit.bitwarden.data.auth.manager.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestTypeJson
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType

/**
 * Indicates if the given [AuthRequestType] uses SSO authentication.
 */
val AuthRequestType.isSso: Boolean
    get() = when (this) {
        AuthRequestType.OTHER_DEVICE -> false
        AuthRequestType.SSO_OTHER_DEVICE,
        AuthRequestType.SSO_ADMIN_APPROVAL,
            -> true
    }

/**
 * Converts the [AuthRequestType] to the appropriate [AuthRequestTypeJson].
 */
fun AuthRequestType.toAuthRequestTypeJson(): AuthRequestTypeJson =
    when (this) {
        AuthRequestType.OTHER_DEVICE,
        AuthRequestType.SSO_OTHER_DEVICE,
            -> AuthRequestTypeJson.LOGIN_WITH_DEVICE

        AuthRequestType.SSO_ADMIN_APPROVAL -> AuthRequestTypeJson.ADMIN_APPROVAL
    }
