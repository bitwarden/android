package com.x8bit.bitwarden.data.platform.error

/**
 * An exception indicating that the selected mTLS certificate or its private key cannot be loaded.
 */
class MutualTlsCertificateUnavailableException :
    IllegalStateException("The selected mTLS certificate cannot be loaded!")
