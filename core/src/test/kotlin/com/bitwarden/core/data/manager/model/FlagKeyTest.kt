package com.bitwarden.core.data.manager.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `Feature flags have the correct key name set`() {
        assertEquals(
            FlagKey.CredentialExchangeProtocolImport.keyName,
            "cxp-import-mobile",
        )
        assertEquals(
            FlagKey.CredentialExchangeProtocolExport.keyName,
            "cxp-export-mobile",
        )
        assertEquals(
            FlagKey.CipherKeyEncryption.keyName,
            "cipher-key-encryption",
        )
        assertEquals(
            FlagKey.UserManagedPrivilegedApps.keyName,
            "pm-18970-user-managed-privileged-apps",
        )
        assertEquals(
            FlagKey.BitwardenAuthenticationEnabled.keyName,
            "bitwarden-authentication-enabled",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.CipherKeyEncryption,
                FlagKey.UserManagedPrivilegedApps,
                FlagKey.BitwardenAuthenticationEnabled,
            ).all {
                !it.defaultValue
            },
        )
    }
}
