package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `Feature flags have the correct key name set`() {
        assertEquals(
            FlagKey.EmailVerification.keyName,
            "email-verification",
        )
        assertEquals(
            FlagKey.ImportLoginsFlow.keyName,
            "import-logins-flow",
        )
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
            FlagKey.SingleTapPasskeyCreation.keyName,
            "single-tap-passkey-creation",
        )
        assertEquals(
            FlagKey.SingleTapPasskeyAuthentication.keyName,
            "single-tap-passkey-authentication",
        )
        assertEquals(
            FlagKey.MutualTls.keyName,
            "mutual-tls",
        )
        assertEquals(
            FlagKey.AnonAddySelfHostAlias.keyName,
            "anon-addy-self-host-alias",
        )
        assertEquals(
            FlagKey.ChromeAutofill.keyName,
            "android-chrome-autofill",
        )
        assertEquals(
            FlagKey.MobileErrorReporting.keyName,
            "mobile-error-reporting",
        )
        assertEquals(
            FlagKey.RestrictCipherItemDeletion.keyName,
            "pm-15493-restrict-item-deletion-to-can-manage-permission",
        )
        assertEquals(
            FlagKey.UserManagedPrivilegedApps.keyName,
            "pm-18970-user-managed-privileged-apps",
        )
        assertEquals(
            FlagKey.RemoveCardPolicy.keyName,
            "pm-16442-remove-card-item-type-policy",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.EmailVerification,
                FlagKey.ImportLoginsFlow,
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.SingleTapPasskeyCreation,
                FlagKey.SingleTapPasskeyAuthentication,
                FlagKey.AnonAddySelfHostAlias,
                FlagKey.SimpleLoginSelfHostAlias,
                FlagKey.CipherKeyEncryption,
                FlagKey.ChromeAutofill,
                FlagKey.MobileErrorReporting,
                FlagKey.RestrictCipherItemDeletion,
                FlagKey.UserManagedPrivilegedApps,
                FlagKey.RemoveCardPolicy,
            ).all {
                !it.defaultValue
            },
        )
    }
}
