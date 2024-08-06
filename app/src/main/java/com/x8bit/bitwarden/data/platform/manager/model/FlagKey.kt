package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Class to hold feature flag keys.
 * @property [stringValue] corresponds to the string value of a give key
 * @property [defaultValue] corresponds to default value of the flag of type [T]
 */
sealed class FlagKey<out T : Any> {
    abstract val stringValue: String
    abstract val defaultValue: T

    /**
     * Data object holding the key for Email Verification feature
     */
    data object EmailVerification : FlagKey<Boolean>() {
        override val stringValue: String = "email-verification"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for an Int flag to be used in tests
     */
    data object DummyInt : FlagKey<Int>() {
        override val stringValue: String = "email-verification"
        override val defaultValue: Int = Int.MIN_VALUE
    }

    /**
     * Data object holding the key for an String flag to be used in tests
     */
    data object DummyString : FlagKey<String>() {
        override val stringValue: String = "email-verification"
        override val defaultValue: String = "defaultValue"
    }
}
