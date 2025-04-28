package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Models the result of importing a private key.
 */
sealed class ImportPrivateKeyResult {

    /**
     * Represents a successful result of importing a private key.
     *
     * @property alias The alias assigned to the imported private key.
     */
    data class Success(val alias: String) : ImportPrivateKeyResult()

    /**
     * Represents a generic error during the import process.
     */
    sealed class Error : ImportPrivateKeyResult() {
        /**
         * The underlying error.
         */
        abstract val throwable: Throwable?

        /**
         * Indicates that the provided key is unrecoverable or the password is incorrect.
         */
        data class UnrecoverableKey(
            override val throwable: Throwable,
        ) : Error()

        /**
         * Indicates that the certificate chain associated with the key is invalid.
         */
        data class InvalidCertificateChain(
            override val throwable: Throwable,
        ) : Error()

        /**
         * Indicates that an error occurred during the key store operation.
         */
        data class KeyStoreOperationFailed(
            override val throwable: Throwable,
        ) : Error()

        /**
         * Indicates the provided key is not supported.
         */
        data class UnsupportedKey(
            override val throwable: Throwable,
        ) : Error()
    }
}
