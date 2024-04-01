package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.util

import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType

/**
 * Converts the [LoginWithDeviceType] to an appropriate [AuthRequestType].
 */
fun LoginWithDeviceType.toAuthRequestType(): AuthRequestType =
    when (this) {
        LoginWithDeviceType.OTHER_DEVICE -> AuthRequestType.OTHER_DEVICE
        LoginWithDeviceType.SSO_ADMIN_APPROVAL -> AuthRequestType.SSO_ADMIN_APPROVAL
        LoginWithDeviceType.SSO_OTHER_DEVICE -> AuthRequestType.SSO_OTHER_DEVICE
    }
