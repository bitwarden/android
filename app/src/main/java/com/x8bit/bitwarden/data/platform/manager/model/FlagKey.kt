package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Class to hold feature flag keys.
 * @property [keyName] corresponds to the string value of a given key
 * @property [defaultValue] corresponds to default value of the flag of type [T]
 */
sealed class FlagKey<out T : Any> {
    abstract val keyName: String
    abstract val defaultValue: T

    /**
     * Data object holding the key for Email Verification feature
     */
    data object EmailVerification : FlagKey<Boolean>() {
        override val keyName: String = "email-verification"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for an Int flag to be used in tests
     */
    data object DummyInt : FlagKey<Int>() {
        override val keyName: String = "dummy-int"
        override val defaultValue: Int = Int.MIN_VALUE
    }

    /**
     * Data object holding the key for an String flag to be used in tests
     */
    data object DummyString : FlagKey<String>() {
        override val keyName: String = "dummy-string"
        override val defaultValue: String = "defaultValue"
    }
}
