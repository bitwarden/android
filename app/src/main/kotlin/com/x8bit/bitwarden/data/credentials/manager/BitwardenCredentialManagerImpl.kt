package com.x8bit.bitwarden.data.credentials.manager

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.takeUntilLoaded
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Origin
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.ui.platform.base.util.prefixHttpsIfNecessaryOrNull
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.autofill.util.isActiveWithPasswordCredentials
import com.x8bit.bitwarden.data.credentials.builder.CredentialEntryBuilder
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.credentials.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.credentials.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.platform.util.getAppOrigin
import com.x8bit.bitwarden.data.platform.util.getAppSigningSignatureFingerprint
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Primary implementation of [BitwardenCredentialManager].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class BitwardenCredentialManagerImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val fido2CredentialStore: Fido2CredentialStore,
    private val autofillCipherProvider: AutofillCipherProvider,
    private val credentialEntryBuilder: CredentialEntryBuilder,
    private val json: Json,
    private val vaultRepository: VaultRepository,
    dispatcherManager: DispatcherManager,
) : BitwardenCredentialManager,
    Fido2CredentialStore by fido2CredentialStore {

    private val ioScope = CoroutineScope(dispatcherManager.io)

    override var isUserVerified: Boolean = false

    override var authenticationAttempts: Int = 0

    override suspend fun registerFido2Credential(
        userId: String,
        callingAppInfo: CallingAppInfo,
        createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult {
        return if (callingAppInfo.isOriginPopulated()) {
            registerFido2CredentialForPrivilegedApp(
                userId = userId,
                callingAppInfo = callingAppInfo,
                createPublicKeyCredentialRequest = createPublicKeyCredentialRequest,
                selectedCipherView = selectedCipherView,
            )
        } else {
            registerFido2CredentialForUnprivilegedApp(
                userId = userId,
                callingAppInfo = callingAppInfo,
                createPublicKeyCredentialRequest = createPublicKeyCredentialRequest,
                selectedCipherView = selectedCipherView,
            )
        }
    }

    override fun getPasskeyAttestationOptionsOrNull(
        requestJson: String,
    ): PasskeyAttestationOptions? = json.decodeFromStringOrNull(requestJson)

    @Suppress("LongMethod")
    override suspend fun authenticateFido2Credential(
        userId: String,
        callingAppInfo: CallingAppInfo,
        request: GetPublicKeyCredentialOption,
        selectedCipherView: CipherView,
        origin: String?,
    ): Fido2CredentialAssertionResult {
        val clientData = request.clientDataHash
            ?.let { ClientData.DefaultWithCustomHash(hash = it) }
            ?: ClientData.DefaultWithExtraData(androidPackageName = callingAppInfo.getAppOrigin())

        val sdkOrigin = if (!origin.isNullOrEmpty()) {
            Origin.Web(origin)
        } else {
            val hostUrl = getOriginUrlFromAssertionOptionsOrNull(request.requestJson)
                ?: return Fido2CredentialAssertionResult.Error.MissingHostUrl
            Origin.Android(
                UnverifiedAssetLink(
                    packageName = callingAppInfo.packageName,
                    sha256CertFingerprint = callingAppInfo
                        .getSignatureFingerprintAsHexString()
                        .orEmpty(),
                    host = hostUrl,
                    assetLinkUrl = hostUrl,
                ),
            )
        }
        return vaultSdkSource
            .authenticateFido2Credential(
                request = AuthenticateFido2CredentialRequest(
                    userId = userId,
                    origin = sdkOrigin,
                    requestJson = """{"publicKey": ${request.requestJson}}""",
                    clientData = clientData,
                    selectedCipherView = selectedCipherView,
                    isUserVerificationSupported = true,
                ),
                fido2CredentialStore = this,
            )
            .map { it.toAndroidFido2PublicKeyCredential() }
            .mapCatching { json.encodeToString(it) }
            .fold(
                onSuccess = { Fido2CredentialAssertionResult.Success(it) },
                onFailure = {
                    Timber.e(it, "Failed to authenticate FIDO2 credential.")
                    Fido2CredentialAssertionResult.Error.InternalError
                },
            )
    }

    override fun hasAuthenticationAttemptsRemaining(): Boolean =
        authenticationAttempts < MAX_AUTHENTICATION_ATTEMPTS

    override fun getUserVerificationRequirement(
        request: ProviderGetCredentialRequest,
        fallbackRequirement: UserVerificationRequirement,
    ): UserVerificationRequirement = request
        .credentialOptions
        .filterIsInstance<GetPublicKeyCredentialOption>()
        .firstOrNull()
        ?.let { option ->
            getPasskeyAssertionOptionsOrNull(option.requestJson)
                ?.userVerification
        }
        ?: fallbackRequirement

    override fun getUserVerificationRequirement(
        request: CreatePublicKeyCredentialRequest,
        fallbackRequirement: UserVerificationRequirement,
    ): UserVerificationRequirement = getPasskeyAttestationOptionsOrNull(request.requestJson)
        ?.authenticatorSelection
        ?.userVerification
        ?: fallbackRequirement

    override suspend fun getCredentialEntries(
        getCredentialsRequest: GetCredentialsRequest,
    ): Result<List<CredentialEntry>> = withContext(ioScope.coroutineContext) {
        val cipherViews = vaultRepository
            .ciphersStateFlow
            .takeUntilLoaded()
            .fold(initial = emptyList<CipherView>()) { _, dataState ->
                when (dataState) {
                    is DataState.Loaded -> {
                        dataState.data
                    }

                    else -> emptyList()
                }
            }

        val fido2CredentialResult = getCredentialsRequest
            .beginGetPublicKeyCredentialOptions
            .toPublicKeyCredentialEntries(
                userId = getCredentialsRequest.userId,
                cipherViewsWithPublicKeyCredentials = cipherViews.filter { it.isActiveWithFido2Credentials },
            )
            .onFailure { Timber.e(it, "Failed to get FIDO 2 credential entries.") }

        val passwordCredentialResult = getCredentialsRequest
            .beginGetPasswordOption
            .toPasswordCredentialEntries(
                userId = getCredentialsRequest.userId,
                cipherViewsWithPasswordCredentials = cipherViews.filter { it.isActiveWithPasswordCredentials },
            )
            .onFailure { Timber.e(it, "Failed to get Password credential entries.") }

        return@withContext if (fido2CredentialResult.isSuccess || passwordCredentialResult.isSuccess) {
            ((fido2CredentialResult.getOrNull()
                ?: emptyList()) + (passwordCredentialResult.getOrNull() ?: emptyList())).asSuccess()
        } else {
            val exception = fido2CredentialResult.exceptionOrNull()
                ?: passwordCredentialResult.exceptionOrNull()
            exception?.asFailure() ?: Throwable("Failed to get credential entries.").asFailure()
        }

    }

    private fun getPasskeyAssertionOptionsOrNull(
        requestJson: String,
    ): PasskeyAssertionOptions? = json.decodeFromStringOrNull(requestJson)

    private suspend fun List<BeginGetPublicKeyCredentialOption>.toPublicKeyCredentialEntries(
        userId: String,
        cipherViewsWithPublicKeyCredentials: List<CipherView>,
    ): Result<List<CredentialEntry>> {
        val relyingPartyIds = this
            .mapNotNull { getPasskeyAssertionOptionsOrNull(it.requestJson)?.relyingPartyId }
            .distinct()
            .ifEmpty {
                return GetCredentialUnknownException("Relying party id required.").asFailure()
            }

        val decryptResult = vaultRepository
            .getDecryptedFido2CredentialAutofillViews(cipherViewsWithPublicKeyCredentials)

        return when (decryptResult) {
            is DecryptFido2CredentialAutofillViewResult.Error -> {
                GetCredentialUnknownException("Error decrypting credentials.").asFailure()
            }

            is DecryptFido2CredentialAutofillViewResult.Success -> {
                credentialEntryBuilder
                    .buildPublicKeyCredentialEntries(
                        userId = userId,
                        fido2CredentialAutofillViews = decryptResult
                            .fido2CredentialAutofillViews
                            .filter { it.rpId in relyingPartyIds },
                        beginGetPublicKeyCredentialOptions = this,
                        isUserVerified = isUserVerified,
                    )
                    .asSuccess()
            }
        }
    }

    private suspend fun registerFido2CredentialForUnprivilegedApp(
        userId: String,
        callingAppInfo: CallingAppInfo,
        createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult {
        val clientData = ClientData.DefaultWithExtraData(callingAppInfo.packageName)

        val host = getOriginUrlFromAttestationOptionsOrNull(
            requestJson = createPublicKeyCredentialRequest.requestJson,
        )
            ?: return Fido2RegisterCredentialResult.Error.MissingHostUrl

        val signatureFingerprint = callingAppInfo
            .getSignatureFingerprintAsHexString()
            .orEmpty()

        val sdkOrigin = Origin.Android(
            UnverifiedAssetLink(
                packageName = callingAppInfo.packageName,
                sha256CertFingerprint = signatureFingerprint,
                host = host,
                assetLinkUrl = host,
            ),
        )

        return registerFido2CredentialInternal(
            userId = userId,
            sdkOrigin = sdkOrigin,
            createPublicKeyCredentialRequest = createPublicKeyCredentialRequest,
            selectedCipherView = selectedCipherView,
            clientData = clientData,
        )
    }

    private suspend fun registerFido2CredentialForPrivilegedApp(
        userId: String,
        callingAppInfo: CallingAppInfo,
        createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult {
        val clientData = callingAppInfo
            .getAppSigningSignatureFingerprint()
            ?.let { ClientData.DefaultWithCustomHash(hash = it) }
            ?: return Fido2RegisterCredentialResult.Error.InvalidAppSignature

        val sdkOrigin = createPublicKeyCredentialRequest.origin
            ?.let { Origin.Web(it) }
            ?: return Fido2RegisterCredentialResult.Error.MissingHostUrl

        return registerFido2CredentialInternal(
            userId = userId,
            sdkOrigin = sdkOrigin,
            createPublicKeyCredentialRequest = createPublicKeyCredentialRequest,
            selectedCipherView = selectedCipherView,
            clientData = clientData,
        )
    }

    private suspend fun registerFido2CredentialInternal(
        userId: String,
        sdkOrigin: Origin,
        createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest,
        selectedCipherView: CipherView,
        clientData: ClientData,
    ): Fido2RegisterCredentialResult = vaultSdkSource
        .registerFido2Credential(
            request = RegisterFido2CredentialRequest(
                userId = userId,
                origin = sdkOrigin,
                requestJson = """{"publicKey": ${createPublicKeyCredentialRequest.requestJson}}""",
                clientData = clientData,
                selectedCipherView = selectedCipherView,
                // User verification is handled prior to engaging the SDK. We always respond
                // `true` so that the SDK does not fail if the relying party requests UV.
                isUserVerificationSupported = true,
            ),
            fido2CredentialStore = this,
        )
        .map { it.toAndroidAttestationResponse() }
        .mapCatching { json.encodeToString(it) }
        .fold(
            onSuccess = { Fido2RegisterCredentialResult.Success(it) },
            onFailure = {
                Timber.e(it, "Failed to register FIDO2 credential.")
                Fido2RegisterCredentialResult.Error.InternalError
            },
        )

    private fun List<BeginGetPasswordOption>.toPasswordCredentialEntries(
        userId: String,
        cipherViewsWithPasswordCredentials: List<CipherView>,
    ): Result<List<CredentialEntry>> {
        return credentialEntryBuilder
            .buildPasswordCredentialEntries(
                userId = userId,
                passwordCredentialAutofillViews = cipherViewsWithPasswordCredentials,
                beginGetPasswordCredentialOptions = this,
                isUserVerified = isUserVerified,
            )
            .asSuccess()
    }

    private fun getOriginUrlFromAssertionOptionsOrNull(requestJson: String) =
        getPasskeyAssertionOptionsOrNull(requestJson)
            ?.relyingPartyId
            ?.prefixHttpsIfNecessaryOrNull()

    private fun getOriginUrlFromAttestationOptionsOrNull(requestJson: String) =
        getPasskeyAttestationOptionsOrNull(requestJson)
            ?.relyingParty
            ?.id
            ?.prefixHttpsIfNecessaryOrNull()
}

private const val MAX_AUTHENTICATION_ATTEMPTS = 5
