package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `AuthenticatorSync default value should be false`() {
        assertFalse(FlagKey.AuthenticatorSync.defaultValue)
    }
}
