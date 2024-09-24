package com.bitwarden.authenticatorbridge.manager.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticatorBridgeConnectionTypeTest {

    @Test
    fun `toPackageName RELEASE should map to correct release package`() {
        assertEquals(
            "com.x8bit.bitwarden",
            AuthenticatorBridgeConnectionType.RELEASE.toPackageName()
        )
    }

    @Test
    fun `toPackageName DEV should map to correct dev package`() {
        assertEquals(
            "com.x8bit.bitwarden.dev",
            AuthenticatorBridgeConnectionType.DEV.toPackageName()
        )
    }
}
