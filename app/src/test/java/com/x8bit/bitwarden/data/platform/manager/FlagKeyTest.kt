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
            FlagKey.OnboardingCarousel.keyName,
            "native-carousel-flow",
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
            FlagKey.SshKeyCipherItems.keyName,
            "ssh-key-vault-item",
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
            FlagKey.AppReviewPrompt.keyName,
            "app-review-prompt",
        )
        assertEquals(
            FlagKey.CipherKeyEncryption.keyName,
            "cipher-key-encryption",
        )
        assertEquals(
            FlagKey.NewDeviceTemporaryDismiss.keyName,
            "new-device-temporary-dismiss",
        )
        assertEquals(
            FlagKey.NewDevicePermanentDismiss.keyName,
            "new-device-permanent-dismiss",
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
            FlagKey.IgnoreEnvironmentCheck.keyName,
            "ignore-environment-check",
        )
        assertEquals(
            FlagKey.MutualTls.keyName,
            "mutual-tls",
        )
        assertEquals(
            FlagKey.AnonAddySelfHostAlias.keyName,
            "anon-addy-self-host-alias",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.AuthenticatorSync,
                FlagKey.EmailVerification,
                FlagKey.OnboardingCarousel,
                FlagKey.OnboardingFlow,
                FlagKey.ImportLoginsFlow,
                FlagKey.SshKeyCipherItems,
                FlagKey.VerifiedSsoDomainEndpoint,
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.AppReviewPrompt,
                FlagKey.NewDeviceTemporaryDismiss,
                FlagKey.NewDevicePermanentDismiss,
                FlagKey.SingleTapPasskeyCreation,
                FlagKey.SingleTapPasskeyAuthentication,
                FlagKey.AnonAddySelfHostAlias,
            ).all {
                !it.defaultValue
            },
        )

        assertTrue(FlagKey.CipherKeyEncryption.defaultValue)
    }

    @Test
    fun `All feature flags are correctly set to be remotely configured`() {
        assertTrue(
            listOf(
                FlagKey.AuthenticatorSync,
                FlagKey.EmailVerification,
                FlagKey.OnboardingCarousel,
                FlagKey.OnboardingFlow,
                FlagKey.ImportLoginsFlow,
                FlagKey.SshKeyCipherItems,
                FlagKey.VerifiedSsoDomainEndpoint,
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.AppReviewPrompt,
                FlagKey.CipherKeyEncryption,
                FlagKey.NewDeviceTemporaryDismiss,
                FlagKey.NewDevicePermanentDismiss,
                FlagKey.SingleTapPasskeyCreation,
                FlagKey.SingleTapPasskeyAuthentication,
                FlagKey.MutualTls,
                FlagKey.AnonAddySelfHostAlias,
            ).all {
                it.isRemotelyConfigured
            },
        )

        assertTrue(
            listOf(
                FlagKey.IgnoreEnvironmentCheck,
            ).all {
                !it.isRemotelyConfigured
            },
        )
    }
}
