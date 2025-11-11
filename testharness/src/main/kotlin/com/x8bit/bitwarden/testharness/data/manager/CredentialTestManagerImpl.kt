package com.x8bit.bitwarden.testharness.data.manager

import android.app.Application
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.x8bit.bitwarden.testharness.data.model.CredentialTestResult
import javax.inject.Inject
import javax.inject.Singleton

private const val CHALLENGE_SEED_SIZE = 32

/**
 * Default implementation of [CredentialTestManager].
 */
@Singleton
class CredentialTestManagerImpl @Inject constructor(
    private val application: Application,
    private val credentialManager: CredentialManager,
) : CredentialTestManager {

    override suspend fun createPassword(
        username: String,
        password: String,
        origin: String?,
    ): CredentialTestResult {
        return try {
            val request = CreatePasswordRequest(
                id = username,
                password = password,
                origin = origin,
            )

            val result = credentialManager.createCredential(
                context = application,
                request = request,
            )

            when (result) {
                is CreatePasswordResponse -> {
                    CredentialTestResult.Success(
                        message = "Password created successfully",
                        data = "Username: $username\nOrigin: ${origin ?: "null"}",
                    )
                }
                else -> {
                    CredentialTestResult.Error(
                        message = "Unexpected response type: ${result::class.simpleName}",
                    )
                }
            }
        } catch (e: CreateCredentialCancellationException) {
            CredentialTestResult.Cancelled
        } catch (e: CreateCredentialException) {
            CredentialTestResult.Error(
                message = "Failed to create password: ${e.message}",
                exception = e,
            )
        }
    }

    override suspend fun getPassword(): CredentialTestResult {
        return try {
            val request = GetCredentialRequest(
                credentialOptions = listOf(GetPasswordOption()),
            )

            val result = credentialManager.getCredential(
                context = application,
                request = request,
            )

            val credential = result.credential
            when (credential) {
                is PasswordCredential -> {
                    CredentialTestResult.Success(
                        message = "Password retrieved successfully",
                        data = "Username: ${credential.id}\nPassword: ${credential.password}",
                    )
                }
                else -> {
                    CredentialTestResult.Error(
                        message = "Unexpected credential type: ${credential::class.simpleName}",
                    )
                }
            }
        } catch (e: GetCredentialCancellationException) {
            CredentialTestResult.Cancelled
        } catch (e: GetCredentialException) {
            CredentialTestResult.Error(
                message = "Failed to get password: ${e.message}",
                exception = e,
            )
        }
    }

    override suspend fun createPasskey(
        username: String,
        rpId: String,
        origin: String?,
    ): CredentialTestResult {
        return try {
            // Build minimal passkey creation request JSON
            val requestJson = buildPasskeyCreationJson(username, rpId)

            // Conditionally include origin parameter for privileged app simulation
            val request = if (origin.isNullOrBlank()) {
                CreatePublicKeyCredentialRequest(
                    requestJson = requestJson,
                )
            } else {
                CreatePublicKeyCredentialRequest(
                    requestJson = requestJson,
                    origin = origin,
                )
            }

            val result = credentialManager.createCredential(
                context = application,
                request = request,
            )

            when (result) {
                is CreatePublicKeyCredentialResponse -> {
                    CredentialTestResult.Success(
                        message = "Passkey created successfully",
                        data = "RP ID: $rpId\nOrigin: ${origin ?: "null"}\n\n" +
                            result.registrationResponseJson,
                    )
                }
                else -> {
                    CredentialTestResult.Error(
                        message = "Unexpected response type: ${result::class.simpleName}",
                    )
                }
            }
        } catch (e: CreateCredentialCancellationException) {
            CredentialTestResult.Cancelled
        } catch (e: CreateCredentialException) {
            CredentialTestResult.Error(
                message = "Failed to create passkey: ${e.message}",
                exception = e,
            )
        }
    }

    /**
     * Build a minimal valid WebAuthn registration request JSON.
     *
     * This follows the WebAuthn specification for PublicKeyCredentialCreationOptions.
     */
    private fun buildPasskeyCreationJson(username: String, rpId: String): String {
        // Generate random challenge (base64url encoded)
        val challenge = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(java.security.SecureRandom().generateSeed(CHALLENGE_SEED_SIZE))

        // Generate random user ID (base64url encoded)
        val userId = java.util.Base64.getUrlEncoder()
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

    override suspend fun getPasskey(
        rpId: String,
        origin: String?,
    ): CredentialTestResult {
        return try {
            // Build minimal passkey authentication request JSON
            val requestJson = buildPasskeyAuthenticationJson(rpId)

            val option = GetPublicKeyCredentialOption(
                requestJson = requestJson,
            )

            val request = GetCredentialRequest(
                credentialOptions = listOf(option),
                origin = origin,
            )

            val result = credentialManager.getCredential(
                context = application,
                request = request,
            )

            val credential = result.credential
            when (credential) {
                is PublicKeyCredential -> {
                    CredentialTestResult.Success(
                        message = "Passkey authenticated successfully",
                        data = "RP ID: $rpId\nOrigin: ${origin ?: "null"}\n\n" +
                            credential.authenticationResponseJson,
                    )
                }
                else -> {
                    CredentialTestResult.Error(
                        message = "Unexpected credential type: ${credential::class.simpleName}",
                    )
                }
            }
        } catch (e: GetCredentialCancellationException) {
            CredentialTestResult.Cancelled
        } catch (e: GetCredentialException) {
            CredentialTestResult.Error(
                message = "Failed to authenticate passkey: ${e.message}",
                exception = e,
            )
        }
    }

    override suspend fun getPasswordOrPasskey(
        rpId: String,
        origin: String?,
    ): CredentialTestResult {
        return try {
            // Build passkey authentication request JSON
            val requestJson = buildPasskeyAuthenticationJson(rpId)

            // Create request with both password and passkey options
            // Conditionally include origin parameter for privileged app simulation
            val request = if (origin.isNullOrBlank()) {
                GetCredentialRequest(
                    credentialOptions = listOf(
                        GetPasswordOption(),
                        GetPublicKeyCredentialOption(requestJson = requestJson),
                    ),
                )
            } else {
                GetCredentialRequest(
                    credentialOptions = listOf(
                        GetPasswordOption(),
                        GetPublicKeyCredentialOption(requestJson = requestJson),
                    ),
                    origin = origin,
                )
            }

            val result = credentialManager.getCredential(
                context = application,
                request = request,
            )

            val credential = result.credential
            when (credential) {
                is PasswordCredential -> {
                    CredentialTestResult.Success(
                        message = "Password retrieved successfully",
                        data = "Type: PASSWORD\n" +
                            "Username: ${credential.id}\n" +
                            "Password: ${credential.password}\n" +
                            "Origin: ${origin ?: "null"}",
                    )
                }
                is PublicKeyCredential -> {
                    CredentialTestResult.Success(
                        message = "Passkey authenticated successfully",
                        data = "Type: PASSKEY\n" +
                            "Origin: ${origin ?: "null"}\n" +
                            "Response JSON:\n${credential.authenticationResponseJson}",
                    )
                }
                else -> {
                    CredentialTestResult.Error(
                        message = "Unexpected credential type: ${credential::class.simpleName}",
                    )
                }
            }
        } catch (e: GetCredentialCancellationException) {
            CredentialTestResult.Cancelled
        } catch (e: GetCredentialException) {
            CredentialTestResult.Error(
                message = "Failed to get credential: ${e.message}",
                exception = e,
            )
        }
    }

    /**
     * Build a minimal valid WebAuthn authentication request JSON.
     *
     * This follows the WebAuthn specification for PublicKeyCredentialRequestOptions.
     */
    private fun buildPasskeyAuthenticationJson(rpId: String): String {
        // Generate random challenge (base64url encoded)
        val challenge = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(java.security.SecureRandom().generateSeed(CHALLENGE_SEED_SIZE))

        return """
        {
          "challenge": "$challenge",
          "rpId": "$rpId",
          "userVerification": "preferred"
        }
        """.trimIndent()
    }
}
