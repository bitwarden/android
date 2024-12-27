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

    /**
     * Indicates if the flag should respect the network value or not.
     */
    abstract val isRemotelyConfigured: Boolean

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
                OnboardingCarousel,
                ImportLoginsFlow,
                SshKeyCipherItems,
                VerifiedSsoDomainEndpoint,
                CredentialExchangeProtocolImport,
                CredentialExchangeProtocolExport,
                AppReviewPrompt,
                NewDevicePermanentDismiss,
                NewDeviceTemporaryDismiss,
            )
        }
    }

    /**
     *  Data object holding the key for syncing with the Bitwarden Authenticator app.
     */
    data object AuthenticatorSync : FlagKey<Boolean>() {
        override val keyName: String = "enable-authenticator-sync-android"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the key for Email Verification feature.
     */
    data object EmailVerification : FlagKey<Boolean>() {
        override val keyName: String = "email-verification"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the Onboarding Carousel feature.
     */
    data object OnboardingCarousel : FlagKey<Boolean>() {
        override val keyName: String = "native-carousel-flow"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the new onboarding feature.
     */
    data object OnboardingFlow : FlagKey<Boolean>() {
        override val keyName: String = "native-create-account-flow"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the import logins feature.
     */
    data object ImportLoginsFlow : FlagKey<Boolean>() {
        override val keyName: String = "import-logins-flow"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the SSH key cipher items feature.
     */
    data object SshKeyCipherItems : FlagKey<Boolean>() {
        override val keyName: String = "ssh-key-vault-item"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the new verified SSO domain endpoint feature.
     */
    data object VerifiedSsoDomainEndpoint : FlagKey<Boolean>() {
        override val keyName: String = "pm-12337-refactor-sso-details-endpoint"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding hte feature flag key for the Credential Exchange Protocol (CXP) import
     * feature.
     */
    data object CredentialExchangeProtocolImport : FlagKey<Boolean>() {
        override val keyName: String = "cxp-import-mobile"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the Credential Exchange Protocol (CXP) export
     * feature.
     */
    data object CredentialExchangeProtocolExport : FlagKey<Boolean>() {
        override val keyName: String = "cxp-export-mobile"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the App Review Prompt feature.
     */
    data object AppReviewPrompt : FlagKey<Boolean>() {
        override val keyName: String = "app-review-prompt"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the Cipher Key Encryption feature.
     */
    data object CipherKeyEncryption : FlagKey<Boolean>() {
        override val keyName: String = "cipher-key-encryption"
        override val defaultValue: Boolean = true
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the New Device Temporary Dismiss feature.
     */
    data object NewDeviceTemporaryDismiss : FlagKey<Boolean>() {
        override val keyName: String = "new-device-temporary-dismiss"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the feature flag key for the New Device Permanent Dismiss feature.
     */
    data object NewDevicePermanentDismiss : FlagKey<Boolean>() {
        override val keyName: String = "new-device-permanent-dismiss"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    //region Dummy keys for testing
    /**
     * Data object holding the key for a [Boolean] flag to be used in tests.
     */
    data object DummyBoolean : FlagKey<Boolean>() {
        override val keyName: String = "dummy-boolean"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = true
    }

    /**
     * Data object holding the key for an [Int] flag to be used in tests.
     */
    data class DummyInt(
        override val isRemotelyConfigured: Boolean = true,
    ) : FlagKey<Int>() {
        override val keyName: String = "dummy-int"
        override val defaultValue: Int = Int.MIN_VALUE
    }

    /**
     * Data object holding the key for a [String] flag to be used in tests.
     */
    data object DummyString : FlagKey<String>() {
        override val keyName: String = "dummy-string"
        override val defaultValue: String = "defaultValue"
        override val isRemotelyConfigured: Boolean = true
    }
    //endregion Dummy keys for testing
}
