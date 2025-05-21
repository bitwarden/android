package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Represents the type of request to be made when making auth requests.
 */
enum class AuthRequestType {
    OTHER_DEVICE,
    SSO_OTHER_DEVICE,
    SSO_ADMIN_APPROVAL,
}
