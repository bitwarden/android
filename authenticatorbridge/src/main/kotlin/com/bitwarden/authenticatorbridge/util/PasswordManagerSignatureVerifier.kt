package com.bitwarden.authenticatorbridge.util

/**
 * Verifies that a Password Manager application is authentic by validating its signing certificate.
 */
internal interface PasswordManagerSignatureVerifier {
    /**
     * Validates the signature of the specified Password Manager package.
     *
     * @param packageName The package name to verify
     * @return true if the package has a valid signature, false otherwise
     */
    fun isValidPasswordManagerApp(packageName: String): Boolean
}
