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
         * Indicates that the provided key is unrecoverable or the password is incorrect.
         */
        data object UnrecoverableKey : Error()

        /**
         * Indicates that the certificate chain associated with the key is invalid.
         */
        data object InvalidCertificateChain : Error()

        /**
         * Indicates that the specified alias is already in use.
         */
        data object DuplicateAlias : Error()

        /**
         * Indicates that an error occurred during the key store operation.
         */
        data object KeyStoreOperationFailed : Error()

        /**
         * Indicates the provided key is not supported.
         */
        data object UnsupportedKey : Error()
    }
}
