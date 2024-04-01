package com.x8bit.bitwarden.data.auth.manager.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestTypeJson
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthRequestTypeExtensionsTest {
    @Test
    fun `isSso should return the correct value for each type`() {
        mapOf(
            AuthRequestType.OTHER_DEVICE to false,
            AuthRequestType.SSO_OTHER_DEVICE to true,
            AuthRequestType.SSO_ADMIN_APPROVAL to true,
        )
            .forEach { (type, expected) ->
                assertEquals(expected, type.isSso)
            }
    }

    @Test
    fun `toAuthRequestTypeJson should return the correct value for each type`() {
        mapOf(
            AuthRequestType.OTHER_DEVICE to AuthRequestTypeJson.LOGIN_WITH_DEVICE,
            AuthRequestType.SSO_OTHER_DEVICE to AuthRequestTypeJson.LOGIN_WITH_DEVICE,
            AuthRequestType.SSO_ADMIN_APPROVAL to AuthRequestTypeJson.ADMIN_APPROVAL,
        )
            .forEach { (type, expected) ->
                assertEquals(expected, type.toAuthRequestTypeJson())
            }
    }
}
