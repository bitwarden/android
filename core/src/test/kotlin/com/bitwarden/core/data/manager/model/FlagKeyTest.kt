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
            FlagKey.SendEmailVerification.keyName,
            "pm-19051-send-email-verification",
        )
        assertEquals(
            FlagKey.MobilePremiumUpgrade.keyName,
            "PM-31697-premium-upgrade-path",
        )
        assertEquals(
            FlagKey.AttachmentUpdates.keyName,
            "pm-34224-mobile-attachment-updates",
        )
        assertEquals(
            FlagKey.V2EncryptionJitPassword.keyName,
            "enable-account-encryption-v2-jit-password-registration",
        )
        assertEquals(
            FlagKey.V2EncryptionKeyConnector.keyName,
            "enable-account-encryption-v2-key-connector-registration",
        )
        assertEquals(
            FlagKey.V2EncryptionPassword.keyName,
            "pm-27278-v2-password-registration",
        )
        assertEquals(
            FlagKey.V2EncryptionTde.keyName,
            "pm-27279-v2-registration-tde-jit",
        )
        assertEquals(
            FlagKey.NewItemTypes.keyName,
            "pm-32009-new-item-types",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.BitwardenAuthenticationEnabled,
                FlagKey.ForceUpdateKdfSettings,
                FlagKey.MigrateMyVaultToMyItems,
                FlagKey.SendEmailVerification,
                FlagKey.MobilePremiumUpgrade,
                FlagKey.AttachmentUpdates,
                FlagKey.V2EncryptionJitPassword,
                FlagKey.V2EncryptionKeyConnector,
                FlagKey.V2EncryptionPassword,
                FlagKey.V2EncryptionTde,
                FlagKey.NewItemTypes,
            ).all {
                !it.defaultValue
            },
        )
    }
}
