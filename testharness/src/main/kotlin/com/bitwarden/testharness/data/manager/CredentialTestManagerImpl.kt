package com.bitwarden.testharness.data.manager

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
import com.bitwarden.testharness.data.model.CredentialTestResult
import com.bitwarden.testharness.data.util.WebAuthnJsonBuilder
import javax.inject.Inject
import javax.inject.Singleton

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
        val request = CreatePasswordRequest(
            id = username,
            password = password,
            origin = origin,
        )

        val result = try {
            credentialManager.createCredential(
                context = application,
                request = request,
            )
        } catch (_: CreateCredentialCancellationException) {
            return CredentialTestResult.Cancelled
        } catch (e: CreateCredentialException) {
            return CredentialTestResult.Error(exception = e)
        }

        return when (result) {
            is CreatePasswordResponse -> {
                CredentialTestResult.Success(
                    data = "Username: $username\nOrigin: ${origin ?: "null"}",
                )
            }

            else -> {
                CredentialTestResult.Error(
                    exception = IllegalStateException(
                        "Unexpected response type: ${result::class.simpleName}",
                    ),
                )
            }
        }
    }

    override suspend fun getPassword(): CredentialTestResult {
        val request = GetCredentialRequest(
            credentialOptions = listOf(GetPasswordOption()),
        )

        val result = try {
            credentialManager.getCredential(
                context = application,
                request = request,
            )
        } catch (_: GetCredentialCancellationException) {
            return CredentialTestResult.Cancelled
        } catch (e: GetCredentialException) {
            return CredentialTestResult.Error(exception = e)
        }

        return when (val credential = result.credential) {
            is PasswordCredential -> {
                CredentialTestResult.Success(
                    data = "Username: ${credential.id}\nPassword: ${credential.password}",
                )
            }

            else -> {
                CredentialTestResult.Error(
                    exception = IllegalStateException(
                        "Unexpected credential type: ${credential::class.simpleName}",
                    ),
                )
            }
        }
    }

    override suspend fun createPasskey(
        username: String,
        rpId: String,
        origin: String?,
    ): CredentialTestResult {
        // Build minimal passkey creation request JSON
        val requestJson = WebAuthnJsonBuilder.buildPasskeyCreationJson(username, rpId)

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

        val result = try {
            credentialManager.createCredential(
                context = application,
                request = request,
            )
        } catch (_: CreateCredentialCancellationException) {
            return CredentialTestResult.Cancelled
        } catch (e: CreateCredentialException) {
            return CredentialTestResult.Error(exception = e)
        }

        return when (result) {
            is CreatePublicKeyCredentialResponse -> {
                CredentialTestResult.Success(
                    data = "RP ID: $rpId\nOrigin: ${origin ?: "null"}\n\n" +
                        result.registrationResponseJson,
                )
            }

            else -> {
                CredentialTestResult.Error(
                    exception = IllegalStateException(
                        "Unexpected response type: ${result::class.simpleName}",
                    ),
                )
            }
        }
    }

    override suspend fun getPasskey(
        rpId: String,
        origin: String?,
    ): CredentialTestResult {
        // Build minimal passkey authentication request JSON
        val requestJson = WebAuthnJsonBuilder.buildPasskeyAuthenticationJson(rpId)

        val option = GetPublicKeyCredentialOption(
            requestJson = requestJson,
        )

        val request = GetCredentialRequest(
            credentialOptions = listOf(option),
            origin = origin,
        )

        val result = try {
            credentialManager.getCredential(
                context = application,
                request = request,
            )
        } catch (_: GetCredentialCancellationException) {
            return CredentialTestResult.Cancelled
        } catch (e: GetCredentialException) {
            return CredentialTestResult.Error(exception = e)
        }

        return when (val credential = result.credential) {
            is PublicKeyCredential -> {
                CredentialTestResult.Success(
                    data = "RP ID: $rpId\nOrigin: ${origin ?: "null"}\n\n" +
                        credential.authenticationResponseJson,
                )
            }

            else -> {
                CredentialTestResult.Error(
                    exception = IllegalStateException(
                        "Unexpected credential type: ${credential::class.simpleName}",
                    ),
                )
            }
        }
    }

    override suspend fun getPasswordOrPasskey(
        rpId: String,
        origin: String?,
    ): CredentialTestResult {
        // Build passkey authentication request JSON
        val requestJson = WebAuthnJsonBuilder.buildPasskeyAuthenticationJson(rpId)

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

        val result = try {
            credentialManager.getCredential(
                context = application,
                request = request,
            )
        } catch (_: GetCredentialCancellationException) {
            return CredentialTestResult.Cancelled
        } catch (e: GetCredentialException) {
            return CredentialTestResult.Error(exception = e)
        }

        return when (val credential = result.credential) {
            is PasswordCredential -> {
                CredentialTestResult.Success(
                    data = "Type: PASSWORD\n" +
                        "Username: ${credential.id}\n" +
                        "Password: ${credential.password}\n" +
                        "Origin: ${origin ?: "null"}",
                )
            }

            is PublicKeyCredential -> {
                CredentialTestResult.Success(
                    data = "Type: PASSKEY\n" +
                        "Origin: ${origin ?: "null"}\n" +
                        "Response JSON:\n${credential.authenticationResponseJson}",
                )
            }

            else -> {
                CredentialTestResult.Error(
                    exception = IllegalStateException(
                        "Unexpected credential type: ${credential::class.simpleName}",
                    ),
                )
            }
        }
    }
}
