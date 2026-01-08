package com.bitwarden.core.data.manager.model

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
         * List of all active Authenticator flag keys.
         */
        val activeAuthenticatorFlags: List<FlagKey<*>> by lazy {
            listOf(
                BitwardenAuthenticationEnabled,
            )
        }

        /**
         * List of all active Password Manager flag keys.
         */
        val activePasswordManagerFlags: List<FlagKey<*>> by lazy {
            listOf(
                CredentialExchangeProtocolImport,
                CredentialExchangeProtocolExport,
                ForceUpdateKdfSettings,
                CipherKeyEncryption,
                NoLogoutOnKdfChange,
                MigrateMyVaultToMyItems,
                ArchiveItems,
            )
        }
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
     *  Indicates the state of Bitwarden authentication.
     */
    data object BitwardenAuthenticationEnabled : FlagKey<Boolean>() {
        override val keyName: String = "bitwarden-authentication-enabled"
        override val defaultValue: Boolean = false
    }

    /**
     *  Data object holding the feature flag key for the Force Update KDF Settings feature.
     */
    data object ForceUpdateKdfSettings : FlagKey<Boolean>() {
        override val keyName: String = "pm-18021-force-update-kdf-settings"
        override val defaultValue: Boolean = false
    }

    /**
     *  Data object holding the feature flag key for the No Logout On KDF Change feature.
     */
    data object NoLogoutOnKdfChange : FlagKey<Boolean>() {
        override val keyName: String = "pm-23995-no-logout-on-kdf-change"
        override val defaultValue: Boolean = false
    }

    /**
     *  Data object holding the feature flag key for the Migrate My Vault to My Items feature.
     */
    data object MigrateMyVaultToMyItems : FlagKey<Boolean>() {
        override val keyName: String = "pm-20558-migrate-myvault-to-myitems"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the feature flag key for the Archive Items feature.
     */
    data object ArchiveItems : FlagKey<Boolean>() {
        override val keyName: String = "pm-19148-innovation-archive"
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
