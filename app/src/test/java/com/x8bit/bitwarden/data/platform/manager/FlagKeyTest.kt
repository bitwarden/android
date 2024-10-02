package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `AuthenticatorSync default value should be false`() {
        assertTrue(FlagKey.AuthenticatorSync.defaultValue)
    }
}
