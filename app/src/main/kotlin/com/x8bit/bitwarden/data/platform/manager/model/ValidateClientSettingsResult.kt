package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Models the result of validating the client settings for an environment before saving it.
 */
sealed class ValidateClientSettingsResult {

    /**
     * The settings and the mTLS certificate are valid.
     */
    data object Success : ValidateClientSettingsResult()

    /**
     * The settings or the mTLS certificate are invalid.
     */
    sealed class Error : ValidateClientSettingsResult() {
        /**
         * The underlying error.
         */
        abstract val throwable: Throwable?

        /**
         * The identity or API URL is invalid.
         */
        data class InvalidUrls(
            override val throwable: Throwable,
        ) : Error()

        /**
         * The mTLS certificate cannot complete a TLS handshake.
         */
        data class UnsupportedCertificate(
            override val throwable: Throwable,
        ) : Error()

        /**
         * The selected mTLS certificate or its private key cannot be loaded.
         */
        data object CertificateUnavailable : Error() {
            override val throwable: Throwable? = null
        }

        /**
         * Another setting is invalid.
         */
        data class Other(
            override val throwable: Throwable,
        ) : Error()
    }
}
