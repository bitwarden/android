package com.bitwarden.testharness.data.util

import com.bitwarden.annotation.OmitFromCoverage
import java.security.SecureRandom
import java.util.Base64

private const val CHALLENGE_SEED_SIZE = 32

/**
 * Builder for WebAuthn JSON structures required by the Credential Manager API.
 *
 * Generates minimal valid JSON for passkey registration and authentication flows
 * following the WebAuthn specification.
 */
@OmitFromCoverage
object WebAuthnJsonBuilder {

    /**
     * Build a minimal valid WebAuthn registration request JSON.
     *
     * This follows the WebAuthn specification for PublicKeyCredentialCreationOptions.
     *
     * **WARNING: TEST HARNESS ONLY** - This implementation uses simplified challenge
     * generation suitable for testing. Production implementations should use a secure
     * backend service to generate and validate challenges.
     *
     * @param username The username for the passkey.
     * @param rpId The Relying Party ID.
     * @return JSON string for passkey creation request.
     */
    fun buildPasskeyCreationJson(username: String, rpId: String): String {
        // Generate random challenge (base64url encoded)
        val challenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(SecureRandom().generateSeed(CHALLENGE_SEED_SIZE))

        // Generate random user ID (base64url encoded)
        val userId = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(username.toByteArray())

        return """
        {
          "challenge": "$challenge",
          "rp": {
            "name": "Test Harness",
            "id": "$rpId"
          },
          "user": {
            "id": "$userId",
            "name": "$username",
            "displayName": "$username"
          },
          "pubKeyCredParams": [
            {
              "type": "public-key",
              "alg": -7
            },
            {
              "type": "public-key",
              "alg": -257
            }
          ],
          "timeout": 60000,
          "attestation": "none",
          "authenticatorSelection": {
            "authenticatorAttachment": "platform",
            "residentKey": "required",
            "requireResidentKey": true,
            "userVerification": "required"
          }
        }
        """.trimIndent()
    }

    /**
     * Build a minimal valid WebAuthn authentication request JSON.
     *
     * This follows the WebAuthn specification for PublicKeyCredentialRequestOptions.
     *
     * **WARNING: TEST HARNESS ONLY** - This implementation uses simplified challenge
     * generation suitable for testing. Production implementations should use a secure
     * backend service to generate and validate challenges.
     *
     * @param rpId The Relying Party ID.
     * @return JSON string for passkey authentication request.
     */
    fun buildPasskeyAuthenticationJson(rpId: String): String {
        // Generate random challenge (base64url encoded)
        val challenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(SecureRandom().generateSeed(CHALLENGE_SEED_SIZE))

        return """
        {
          "challenge": "$challenge",
          "rpId": "$rpId",
          "userVerification": "preferred"
        }
        """.trimIndent()
    }
}
