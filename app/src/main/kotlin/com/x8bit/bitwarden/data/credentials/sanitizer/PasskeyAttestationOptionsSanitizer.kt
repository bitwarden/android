package com.x8bit.bitwarden.data.credentials.sanitizer

import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions

/**
 * Defines a contract for sanitizing [PasskeyAttestationOptions] received from a server.
 *
 * Sanitization ensures that the options conform to expected formats and values before being
 * used by the client to create a passkey credential. This can involve tasks like Base64 URL
 * decoding certain fields and setting appropriate default values.
 */
interface PasskeyAttestationOptionsSanitizer {

    /**
     * Sanitizes the given [PasskeyAttestationOptions] in preparation for use in the
     * passkey creation process.
     *
     * @param options The [PasskeyAttestationOptions] to sanitize.
     * @return A new, sanitized instance of [PasskeyAttestationOptions].
     */
    fun sanitize(options: PasskeyAttestationOptions): PasskeyAttestationOptions
}
