package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {
    @Test
    fun `Feature flags have the correct key name set`() {
        assertEquals(
            FlagKey.BitwardenAuthenticationEnabled.keyName,
            "bitwarden-authentication-enabled",
        )
        assertEquals(
            FlagKey.PasswordManagerSync.keyName,
            "enable-pm-bwa-sync",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.BitwardenAuthenticationEnabled,
                FlagKey.PasswordManagerSync,
            ).all {
                !it.defaultValue
            },
        )
    }

    @Test
    fun `All feature flags are correctly set to be remotely configured`() {
        assertTrue(FlagKey.PasswordManagerSync.isRemotelyConfigured)
        assertFalse(FlagKey.BitwardenAuthenticationEnabled.isRemotelyConfigured)
    }
}
