package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Class to hold feature flag keys.
 */
sealed class FlagKey<out T : Any> {
    /**
     * The string value of the given key. This must match the network value.
     */
    abstract val keyName: String

    /**
     * The value to be used if the flags value cannot be determined or is not remotely configured.
     */
    abstract val defaultValue: T

    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * List of all flag keys to consider
         */
        val activeFlags: List<FlagKey<*>> by lazy {
            listOf(
                AuthenticatorSync,
                EmailVerification,
                OnboardingFlow,
                ImportLoginsFlow,
                VerifiedSsoDomainEndpoint,
                CredentialExchangeProtocolImport,
                CredentialExchangeProtocolExport,
                MutualTls,
                SingleTapPasskeyCreation,
                SingleTapPasskeyAuthentication,
                AnonAddySelfHostAlias,
                SimpleLoginSelfHostAlias,
                ChromeAutofill,
                MobileErrorReporting,
                FlightRecorder,
                RestrictCipherItemDeletion,
                PreAuthSettings,
            )
        }
    }

    /**
     * Data object holding the key for syncing with the Bitwarden Authenticator app.
     */
    data object AuthenticatorSync : FlagKey<Boolean>() {
        override val keyName: String = "enable-pm-bwa-sync"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for Email Verification feature.
     */
    data object EmailVerification : FlagKey<Boolean>() {
        override val keyName: String = "email-verification"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for syncing with the Bitwarden Authenticator app.
     */
    data object MobileErrorReporting : FlagKey<Boolean>() {
        override val keyName: String = "mobile-error-reporting"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for enabling the flught recorder feature.
     */
    data object FlightRecorder : FlagKey<Boolean>() {
        override val keyName: String = "enable-pm-flight-recorder"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the new onboarding feature.
     */
    data object OnboardingFlow : FlagKey<Boolean>() {
        override val keyName: String = "native-create-account-flow"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the import logins feature.
     */
    data object ImportLoginsFlow : FlagKey<Boolean>() {
        override val keyName: String = "import-logins-flow"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the new verified SSO domain endpoint feature.
     */
    data object VerifiedSsoDomainEndpoint : FlagKey<Boolean>() {
        override val keyName: String = "pm-12337-refactor-sso-details-endpoint"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding hte feature flag key for the Credential Exchange Protocol (CXP) import
     * feature.
     */
    data object CredentialExchangeProtocolImport : FlagKey<Boolean>() {
        override val keyName: String = "cxp-import-mobile"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the Credential Exchange Protocol (CXP) export
     * feature.
     */
    data object CredentialExchangeProtocolExport : FlagKey<Boolean>() {
        override val keyName: String = "cxp-export-mobile"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the Cipher Key Encryption feature.
     */
    data object CipherKeyEncryption : FlagKey<Boolean>() {
        override val keyName: String = "cipher-key-encryption"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the Mutual TLS feature.
     */
    data object MutualTls : FlagKey<Boolean>() {
        override val keyName: String = "mutual-tls"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable single tap passkey creation.
     */
    data object SingleTapPasskeyCreation : FlagKey<Boolean>() {
        override val keyName: String = "single-tap-passkey-creation"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable single tap passkey authentication.
     */
    data object SingleTapPasskeyAuthentication : FlagKey<Boolean>() {
        override val keyName: String = "single-tap-passkey-authentication"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable single tap passkey authentication.
     */
    data object SingleTapPasswordAuthentication : FlagKey<Boolean>() {
        override val keyName: String = "single-tap-password-authentication"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable AnonAddy (addy.io) self host alias
     * generation.
     */
    data object AnonAddySelfHostAlias : FlagKey<Boolean>() {
        override val keyName: String = "anon-addy-self-host-alias"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable SimpleLogin self-host alias generation.
     */
    data object SimpleLoginSelfHostAlias : FlagKey<Boolean>() {
        override val keyName: String = "simple-login-self-host-alias"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable the checking for Chrome's third party
     * autofill.
     */
    data object ChromeAutofill : FlagKey<Boolean>() {
        override val keyName: String = "android-chrome-autofill"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable the restriction of cipher item deletion
     */
    data object RestrictCipherItemDeletion : FlagKey<Boolean>() {
        override val keyName: String = "pm-15493-restrict-item-deletion-to-can-manage-permission"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key to enable the settings menu before login.
     */
    data object PreAuthSettings : FlagKey<Boolean>() {
        override val keyName: String = "enable-pm-prelogin-settings"
        override val defaultValue: Boolean = false
    }

    //region Dummy keys for testing
    /**
     * Data object holding the key for a [Boolean] flag to be used in tests.
     */
    data object DummyBoolean : FlagKey<Boolean>() {
        override val keyName: String = "dummy-boolean"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for an [Int] flag to be used in tests.
     */
    data object DummyInt : FlagKey<Int>() {
        override val keyName: String = "dummy-int"
        override val defaultValue: Int = Int.MIN_VALUE
    }

    /**
     * Data object holding the key for a [String] flag to be used in tests.
     */
    data object DummyString : FlagKey<String>() {
        override val keyName: String = "dummy-string"
        override val defaultValue: String = "defaultValue"
    }
    //endregion Dummy keys for testing
}
