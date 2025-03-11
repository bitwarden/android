package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.Context
import android.graphics.drawable.Icon
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.fido.Origin
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.platform.util.getAppOrigin
import com.x8bit.bitwarden.data.platform.util.getAppSigningSignatureFingerprint
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.ui.platform.base.util.prefixHttpsIfNecessaryOrNull
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.random.Random

/**
 * Primary implementation of [Fido2CredentialManager].
 */
@Suppress("TooManyFunctions")
class Fido2CredentialManagerImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val fido2CredentialStore: Fido2CredentialStore,
    private val fido2OriginManager: Fido2OriginManager,
    private val intentManager: IntentManager,
    private val json: Json,
    private val context: Context,
) : Fido2CredentialManager,
    Fido2CredentialStore by fido2CredentialStore {

    override var isUserVerified: Boolean = false

    override var authenticationAttempts: Int = 0

    @Suppress("LongMethod")
    override suspend fun registerFido2Credential(
        userId: String,
        fido2CreateCredentialRequest: Fido2CreateCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult {
        val callingAppInfo = fido2CreateCredentialRequest.callingAppInfo
        val relyingPartyId = getPasskeyAttestationOptionsOrNull(
            fido2CreateCredentialRequest.requestJson,
        )
            ?.relyingParty
            ?.id
            ?: return Fido2RegisterCredentialResult.Error.MissingRpId
        val validateOriginResult = fido2OriginManager.validateOrigin(
            callingAppInfo = callingAppInfo,
            relyingPartyId = relyingPartyId,
        )
        if (validateOriginResult is Fido2ValidateOriginResult.Error) {
            return handleOriginValidationErrorForRegistration(
                error = validateOriginResult,
                selectedCipherId = selectedCipherView.id,
            )
        }
        val clientData = if (fido2CreateCredentialRequest.origin.isNullOrEmpty()) {
            ClientData.DefaultWithExtraData(androidPackageName = callingAppInfo.packageName)
        } else {
            callingAppInfo
                .getAppSigningSignatureFingerprint()
                ?.let { ClientData.DefaultWithCustomHash(hash = it) }
                ?: return Fido2RegisterCredentialResult.Error.InvalidSignature
        }
        val sdkOrigin = if (fido2CreateCredentialRequest.origin.isNullOrEmpty()) {
            val host = getOriginUrlFromAttestationOptionsOrNull(
                requestJson = fido2CreateCredentialRequest.requestJson,
            )
                ?: return Fido2RegisterCredentialResult.Error.MissingHostUrl
            Origin.Android(
                UnverifiedAssetLink(
                    packageName = callingAppInfo.packageName,
                    sha256CertFingerprint = callingAppInfo.getSignatureFingerprintAsHexString()
                        ?: return Fido2RegisterCredentialResult.Error.InvalidSignature,
                    host = host,
                    assetLinkUrl = host,
                ),
            )
        } else {
            Origin.Web(fido2CreateCredentialRequest.origin)
        }
        return vaultSdkSource
            .registerFido2Credential(
                request = RegisterFido2CredentialRequest(
                    userId = userId,
                    origin = sdkOrigin,
                    requestJson = """{"publicKey": ${fido2CreateCredentialRequest.requestJson}}""",
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
                onFailure = { Fido2RegisterCredentialResult.Error.Internal },
            )
    }

    override fun getPasskeyAttestationOptionsOrNull(
        requestJson: String,
    ): PasskeyAttestationOptions? =
        try {
            json.decodeFromString<PasskeyAttestationOptions>(requestJson)
        } catch (e: SerializationException) {
            Timber.e(e, "Failed to decode passkey attestation options.")
            null
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to decode passkey attestation options.")
            null
        }

    override fun getPasskeyAssertionOptionsOrNull(
        requestJson: String,
    ): PasskeyAssertionOptions? =
        try {
            json.decodeFromString<PasskeyAssertionOptions>(requestJson)
        } catch (e: SerializationException) {
            Timber.e(e, "Failed to decode passkey assertion options: $e")
            null
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to decode passkey assertion options: $e")
            null
        }

    override fun getCredentialsForRelyingParty(
        request: Fido2GetCredentialsRequest,
        autofillViews: List<Fido2CredentialAutofillView>,
    ): Fido2GetCredentialsResult {
        val relyingPartyId = getPasskeyAssertionOptionsOrNull(request.requestJson)
            ?.relyingPartyId
            ?: return Fido2GetCredentialsResult.Error
        val entries = autofillViews
            .filter { it.rpId == relyingPartyId }
            .map {
                val pendingIntent = intentManager
                    .createFido2GetCredentialPendingIntent(
                        action = GET_PASSKEY_INTENT,
                        userId = request.userId,
                        credentialId = it.credentialId.toString(),
                        cipherId = it.cipherId,
                        requestCode = Random.nextInt(),
                    )
                PublicKeyCredentialEntry
                    .Builder(
                        context = context,
                        username = it.userNameForUi
                            ?: context.getString(R.string.no_username),
                        pendingIntent = pendingIntent,
                        beginGetPublicKeyCredentialOption = request.option,
                    )
                    .setIcon(
                        Icon
                            .createWithResource(
                                context,
                                R.drawable.ic_bw_passkey,
                            ),
                    )
                    .build()
            }
        return Fido2GetCredentialsResult.Success(request.userId, request.option, entries)
    }

    @Suppress("LongMethod")
    override suspend fun authenticateFido2Credential(
        userId: String,
        request: Fido2CredentialAssertionRequest,
        selectedCipherView: CipherView,
    ): Fido2CredentialAssertionResult {
        val callingAppInfo = request.callingAppInfo
        val clientData = request.clientDataHash
            ?.let { ClientData.DefaultWithCustomHash(hash = it) }
            ?: ClientData.DefaultWithExtraData(androidPackageName = callingAppInfo.getAppOrigin())
        val relyingPartyId = json
            .decodeFromStringOrNull<PasskeyAssertionOptions>(request.requestJson)
            ?.relyingPartyId
            ?: return Fido2CredentialAssertionResult.Error.InvalidRpId

        val validateOriginResult = fido2OriginManager.validateOrigin(
            callingAppInfo = callingAppInfo,
            relyingPartyId = relyingPartyId,
        )
        if (validateOriginResult is Fido2ValidateOriginResult.Error) {
            return handleOriginValidationErrorForAssertion(validateOriginResult)
        }

        val sdkOrigin = if (!request.origin.isNullOrEmpty()) {
            Origin.Web(request.origin)
        } else {
            val hostUrl = getOriginUrlFromAssertionOptionsOrNull(request.requestJson)
                ?: return Fido2CredentialAssertionResult.Error.MissingHostUrl
            Origin.Android(
                UnverifiedAssetLink(
                    packageName = callingAppInfo.packageName,
                    sha256CertFingerprint = callingAppInfo.getSignatureFingerprintAsHexString()
                        ?: return Fido2CredentialAssertionResult.Error.InvalidAppSignature,
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
                    Timber.e(it, "Internal error while authenticating FIDO 2 credential.")
                    Fido2CredentialAssertionResult.Error.Internal
                },
            )
    }

    override fun hasAuthenticationAttemptsRemaining(): Boolean =
        authenticationAttempts < MAX_AUTHENTICATION_ATTEMPTS

    private fun handleOriginValidationErrorForRegistration(
        error: Fido2ValidateOriginResult.Error,
        selectedCipherId: String?,
    ): Fido2RegisterCredentialResult {
        return when (error) {
            is Fido2ValidateOriginResult.Error.PrivilegedAppError -> {
                Fido2RegisterCredentialResult.PrivilegedAppNotTrusted(selectedCipherId)
            }

            Fido2ValidateOriginResult
                .Error
                .DigitalAssetLinkError
                .ApplicationFingerprintNotVerified,
                -> {
                Fido2RegisterCredentialResult.Error.DigitalAssetLinkFingerprintMismatch
            }

            Fido2ValidateOriginResult.Error.DigitalAssetLinkError.ApplicationNotFound -> {
                Fido2RegisterCredentialResult.Error.DigitalAssetLinkApplicationNotFound
            }

            Fido2ValidateOriginResult.Error.DigitalAssetLinkError.AssetLinkNotFound -> {
                Fido2RegisterCredentialResult.Error.DigitalAssetLinkNotFound
            }

            Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp -> {
                Fido2RegisterCredentialResult.Error.PasskeyNotSupportedForApp
            }

            Fido2ValidateOriginResult.Error.Unknown -> {
                Fido2RegisterCredentialResult.Error.Internal
            }
        }
    }

    private fun handleOriginValidationErrorForAssertion(
        error: Fido2ValidateOriginResult.Error,
    ): Fido2CredentialAssertionResult {
        return when (error) {
            is Fido2ValidateOriginResult.Error.PrivilegedAppError -> {
                Fido2CredentialAssertionResult.PrivilegedAppNotTrusted
            }

            is Fido2ValidateOriginResult.Error.DigitalAssetLinkError -> {
                Fido2CredentialAssertionResult.Error.InvalidAssetLink
            }

            is Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp -> {
                Fido2CredentialAssertionResult.Error.NotSupported
            }

            is Fido2ValidateOriginResult.Error.Unknown -> {
                Fido2CredentialAssertionResult.Error.Internal
            }
        }
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
