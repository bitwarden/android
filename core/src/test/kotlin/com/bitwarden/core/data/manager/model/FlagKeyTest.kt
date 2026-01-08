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
            FlagKey.BitwardenAuthenticationEnabled.keyName,
            "bitwarden-authentication-enabled",
        )
        assertEquals(
            FlagKey.ForceUpdateKdfSettings.keyName,
            "pm-18021-force-update-kdf-settings",
        )
        assertEquals(
            FlagKey.MigrateMyVaultToMyItems.keyName,
            "pm-20558-migrate-myvault-to-myitems",
        )
        assertEquals(
            FlagKey.ArchiveItems.keyName,
            "pm-19148-innovation-archive",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.CipherKeyEncryption,
                FlagKey.BitwardenAuthenticationEnabled,
                FlagKey.ForceUpdateKdfSettings,
                FlagKey.MigrateMyVaultToMyItems,
                FlagKey.ArchiveItems,
            ).all {
                !it.defaultValue
            },
        )
    }
}
