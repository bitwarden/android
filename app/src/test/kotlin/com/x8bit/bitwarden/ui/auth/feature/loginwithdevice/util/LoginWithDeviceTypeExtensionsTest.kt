package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.util

import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginWithDeviceTypeExtensionsTest {
    @Test
    fun `toAuthRequestTypeJson should return the correct value for each type`() {
        mapOf(
            LoginWithDeviceType.OTHER_DEVICE to AuthRequestType.OTHER_DEVICE,
            LoginWithDeviceType.SSO_OTHER_DEVICE to AuthRequestType.SSO_OTHER_DEVICE,
            LoginWithDeviceType.SSO_ADMIN_APPROVAL to AuthRequestType.SSO_ADMIN_APPROVAL,
        )
            .forEach { (type, expected) ->
                assertEquals(expected, type.toAuthRequestType())
            }
    }
}
