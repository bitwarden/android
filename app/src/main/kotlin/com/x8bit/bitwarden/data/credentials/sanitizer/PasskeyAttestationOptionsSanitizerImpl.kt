package com.x8bit.bitwarden.data.credentials.sanitizer

import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions

/**
 * Default implementation of [PasskeyAttestationOptionsSanitizer].
 */
object PasskeyAttestationOptionsSanitizerImpl : PasskeyAttestationOptionsSanitizer {
    override fun sanitize(options: PasskeyAttestationOptions): PasskeyAttestationOptions {
        // The AliExpress Android app (com.alibaba.aliexpresshd) incorrectly appends a newline
        // to the user.id field when creating a passkey. This causes the operation to fail
        // downstream. As a workaround, we detect this specific scenario, trim the newline, and
        // re-serialize the JSON request.
        return if (options.relyingParty.id == ALIEXPRESS_RP_ID &&
            options.user.id.endsWith("\n")
        ) {
            options.copy(
                user = options.user.copy(id = options.user.id.trimEnd('\n')),
            )
        } else {
            options
        }
    }
}

private const val ALIEXPRESS_RP_ID = "m.aliexpress.com"
