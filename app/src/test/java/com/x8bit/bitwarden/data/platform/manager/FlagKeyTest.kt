package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `Feature flags have the correct key name set`() {
        assertEquals(
            FlagKey.AuthenticatorSync.keyName,
            "enable-pm-bwa-sync",
        )
        assertEquals(
            FlagKey.EmailVerification.keyName,
            "email-verification",
        )
        assertEquals(
            FlagKey.OnboardingFlow.keyName,
            "native-create-account-flow",
        )
        assertEquals(
            FlagKey.ImportLoginsFlow.keyName,
            "import-logins-flow",
        )
        assertEquals(
            FlagKey.VerifiedSsoDomainEndpoint.keyName,
            "pm-12337-refactor-sso-details-endpoint",
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
            FlagKey.FlightRecorder.keyName,
            "enable-pm-flight-recorder",
        )
        assertEquals(
            FlagKey.RestrictCipherItemDeletion.keyName,
            "pm-15493-restrict-item-deletion-to-can-manage-permission",
        )
        assertEquals(
            FlagKey.PreAuthSettings.keyName,
            "enable-pm-prelogin-settings",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.AuthenticatorSync,
                FlagKey.EmailVerification,
                FlagKey.OnboardingFlow,
                FlagKey.ImportLoginsFlow,
                FlagKey.VerifiedSsoDomainEndpoint,
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.SingleTapPasskeyCreation,
                FlagKey.SingleTapPasskeyAuthentication,
                FlagKey.AnonAddySelfHostAlias,
                FlagKey.SimpleLoginSelfHostAlias,
                FlagKey.CipherKeyEncryption,
                FlagKey.ChromeAutofill,
                FlagKey.MobileErrorReporting,
                FlagKey.FlightRecorder,
                FlagKey.RestrictCipherItemDeletion,
                FlagKey.PreAuthSettings,
            ).all {
                !it.defaultValue
            },
        )
    }

    @Test
    fun `All feature flags are correctly set to be remotely configured`() {
        assertTrue(
            listOf(
                FlagKey.AuthenticatorSync,
                FlagKey.EmailVerification,
                FlagKey.OnboardingFlow,
                FlagKey.ImportLoginsFlow,
                FlagKey.VerifiedSsoDomainEndpoint,
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.CipherKeyEncryption,
                FlagKey.SingleTapPasskeyCreation,
                FlagKey.SingleTapPasskeyAuthentication,
                FlagKey.MutualTls,
                FlagKey.AnonAddySelfHostAlias,
                FlagKey.SimpleLoginSelfHostAlias,
                FlagKey.ChromeAutofill,
                FlagKey.MobileErrorReporting,
            ).all {
                it.isRemotelyConfigured
            },
        )

        assertTrue(
            listOf(
                FlagKey.FlightRecorder,
                FlagKey.PreAuthSettings,
                FlagKey.RestrictCipherItemDeletion,
            ).all {
                !it.isRemotelyConfigured
            },
        )
    }
}
