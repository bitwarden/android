package com.x8bit.bitwarden.data.credentials.sanitizer

import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions

/**
 * Defines a contract for sanitizing [PasskeyAttestationOptions] received from applications.
 *
 * Sanitization applies workarounds for known issues with specific applications'
 * passkey implementations, ensuring the options are in the correct format before
 * being used to create a passkey credential.
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
