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
            )
        }
    }

    /**
     *  Data object holding the key for syncing with the Bitwarden Authenticator app.
     */
    data object AuthenticatorSync : FlagKey<Boolean>() {
        override val keyName: String = "enable-authenticator-sync-android"
        override val defaultValue: Boolean = false
        override val isRemotelyConfigured: Boolean = false
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
}
