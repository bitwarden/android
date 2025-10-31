package com.x8bit.bitwarden.data.credentials.sanitizer

import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions

/**
 * Interface for sanitizing [PasskeyAttestationOptions].
 */
interface PasskeyAttestationOptionsSanitizer {

    /**
     * Sanitizes the given [options] in preparation for processing.
     *
     * @param options The [PasskeyAttestationOptions] to sanitize.
     */
    fun sanitize(options: PasskeyAttestationOptions): PasskeyAttestationOptions
}
