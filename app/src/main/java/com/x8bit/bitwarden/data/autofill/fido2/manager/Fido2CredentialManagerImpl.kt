package com.x8bit.bitwarden.data.autofill.fido2.manager

import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Origin
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.platform.util.getAppOrigin
import com.x8bit.bitwarden.data.platform.util.getAppSigningSignatureFingerprint
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.platform.util.validatePrivilegedApp
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ALLOW_LIST_FILE_NAME = "fido2_privileged_allow_list.json"

/**
 * Primary implementation of [Fido2CredentialManager].
 */
@Suppress("TooManyFunctions")
class Fido2CredentialManagerImpl(
    private val assetManager: AssetManager,
    private val digitalAssetLinkService: DigitalAssetLinkService,
    private val vaultSdkSource: VaultSdkSource,
    private val fido2CredentialStore: Fido2CredentialStore,
    private val json: Json,
) : Fido2CredentialManager,
    Fido2CredentialStore by fido2CredentialStore {

    override var isUserVerified: Boolean = false

    override var authenticationAttempts: Int = 0

    override suspend fun registerFido2Credential(
        userId: String,
        fido2CredentialRequest: Fido2CredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult {
        val clientData = if (fido2CredentialRequest.callingAppInfo.isOriginPopulated()) {
            fido2CredentialRequest
                .callingAppInfo
                .getAppSigningSignatureFingerprint()
                ?.let { ClientData.DefaultWithCustomHash(hash = it) }
                ?: return Fido2RegisterCredentialResult.Error
        } else {
            ClientData.DefaultWithExtraData(
                androidPackageName = fido2CredentialRequest
                    .callingAppInfo
                    .packageName,
            )
        }
        val origin = fido2CredentialRequest
            .origin
            ?: getOriginUrlFromAttestationOptionsOrNull(fido2CredentialRequest.requestJson)
            ?: return Fido2RegisterCredentialResult.Error

        val originAndroid = Origin.Android(
            UnverifiedAssetLink(
                fido2CredentialRequest.packageName,
                fido2CredentialRequest.callingAppInfo
                    .getSignatureFingerprintAsHexString()
                    ?: return Fido2RegisterCredentialResult.Error,
                origin.toHostOrPathOrNull()
                    ?: return Fido2RegisterCredentialResult.Error,
                origin,
            ),
        )
        return vaultSdkSource
            .registerFido2Credential(
                request = RegisterFido2CredentialRequest(
                    userId = userId,
                    origin = originAndroid,
                    requestJson = """{"publicKey": ${fido2CredentialRequest.requestJson}}""",
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
                onFailure = { Fido2RegisterCredentialResult.Error },
            )
    }

    override suspend fun validateOrigin(
        callingAppInfo: CallingAppInfo,
        relyingPartyId: String,
    ): Fido2ValidateOriginResult {
        return if (callingAppInfo.isOriginPopulated()) {
            validatePrivilegedAppOrigin(callingAppInfo)
        } else {
            validateCallingApplicationAssetLinks(callingAppInfo, relyingPartyId)
        }
    }

    override fun getPasskeyAttestationOptionsOrNull(
        requestJson: String,
    ): PasskeyAttestationOptions? =
        try {
            json.decodeFromString<PasskeyAttestationOptions>(requestJson)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

    override fun getPasskeyAssertionOptionsOrNull(
        requestJson: String,
    ): PasskeyAssertionOptions? =
        try {
            json.decodeFromString<PasskeyAssertionOptions>(requestJson)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

    override suspend fun authenticateFido2Credential(
        userId: String,
        request: Fido2CredentialAssertionRequest,
        selectedCipherView: CipherView,
    ): Fido2CredentialAssertionResult {
        val callingAppInfo = request.callingAppInfo
        val clientData = request.clientDataHash
            ?.let { ClientData.DefaultWithCustomHash(hash = it) }
            ?: ClientData.DefaultWithExtraData(androidPackageName = callingAppInfo.getAppOrigin())
        val origin = callingAppInfo.origin
            ?: getOriginUrlFromAssertionOptionsOrNull(request.requestJson)
            ?: return Fido2CredentialAssertionResult.Error
        val relyingPartyId = json
            .decodeFromStringOrNull<PasskeyAssertionOptions>(request.requestJson)
            ?.relyingPartyId
            ?: return Fido2CredentialAssertionResult.Error

        val validateOriginResult = validateOrigin(
            callingAppInfo = callingAppInfo,
            relyingPartyId = relyingPartyId,
        )

        return when (validateOriginResult) {
            is Fido2ValidateOriginResult.Error -> {
                Fido2CredentialAssertionResult.Error
            }

            Fido2ValidateOriginResult.Success -> {
                vaultSdkSource
                    .authenticateFido2Credential(
                        request = AuthenticateFido2CredentialRequest(
                            userId = userId,
                            origin = Origin.Android(
                                UnverifiedAssetLink(
                                    callingAppInfo.packageName,
                                    callingAppInfo.getSignatureFingerprintAsHexString()
                                        ?: return Fido2CredentialAssertionResult.Error,
                                    origin.toHostOrPathOrNull()
                                        ?: return Fido2CredentialAssertionResult.Error,
                                    origin,
                                ),
                            ),
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
                        onFailure = { Fido2CredentialAssertionResult.Error },
                    )
            }
        }
    }

    private suspend fun validateCallingApplicationAssetLinks(
        callingAppInfo: CallingAppInfo,
        relyingPartyId: String,
    ): Fido2ValidateOriginResult {
        return digitalAssetLinkService.getDigitalAssetLinkForRp(relyingParty = relyingPartyId)
            .onFailure {
                return Fido2ValidateOriginResult.Error.AssetLinkNotFound
            }
            .map { statements ->
                statements
                    .filterMatchingAppStatementsOrNull(
                        rpPackageName = callingAppInfo.packageName,
                    )
                    ?: return Fido2ValidateOriginResult.Error.ApplicationNotFound
            }
            .map { matchingStatements ->
                callingAppInfo.getSignatureFingerprintAsHexString()
                    ?.let { certificateFingerprint ->
                        matchingStatements
                            .filterMatchingAppSignaturesOrNull(
                                signature = certificateFingerprint,
                            )
                    }
                    ?: return Fido2ValidateOriginResult.Error.ApplicationNotVerified
            }
            .fold(
                onSuccess = {
                    Fido2ValidateOriginResult.Success
                },
                onFailure = {
                    Fido2ValidateOriginResult.Error.Unknown
                },
            )
    }

    private suspend fun validatePrivilegedAppOrigin(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult =
        assetManager
            .readAsset(ALLOW_LIST_FILE_NAME)
            .map { allowList ->
                callingAppInfo.validatePrivilegedApp(
                    allowList = allowList,
                )
            }
            .fold(
                onSuccess = { it },
                onFailure = { Fido2ValidateOriginResult.Error.Unknown },
            )

    /**
     * Returns statements targeting the calling Android application, or null.
     */
    private fun List<DigitalAssetLinkResponseJson>.filterMatchingAppStatementsOrNull(
        rpPackageName: String,
    ): List<DigitalAssetLinkResponseJson>? =
        filter { statement ->
            val target = statement.target
            target.namespace == "android_app" &&
                target.packageName == rpPackageName &&
                statement.relation.containsAll(
                    listOf(
                        "delegate_permission/common.get_login_creds",
                        "delegate_permission/common.handle_all_urls",
                    ),
                )
        }
            .takeUnless { it.isEmpty() }

    /**
     * Returns statements that match the given [signature], or null.
     */
    private fun List<DigitalAssetLinkResponseJson>.filterMatchingAppSignaturesOrNull(
        signature: String,
    ): List<DigitalAssetLinkResponseJson>? =
        filter { statement ->
            statement.target.sha256CertFingerprints
                ?.contains(signature)
                ?: false
        }
            .takeUnless { it.isEmpty() }

    override fun hasAuthenticationAttemptsRemaining(): Boolean =
        authenticationAttempts < MAX_AUTHENTICATION_ATTEMPTS

    private fun getOriginUrlFromAssertionOptionsOrNull(requestJson: String) =
        getPasskeyAssertionOptionsOrNull(requestJson)
            ?.relyingPartyId
            ?.let { "$HTTPS$it" }

    private fun getOriginUrlFromAttestationOptionsOrNull(requestJson: String) =
        getPasskeyAttestationOptionsOrNull(requestJson)
            ?.relyingParty
            ?.id
            ?.let { "$HTTPS$it" }
}

private const val MAX_AUTHENTICATION_ATTEMPTS = 5
private const val HTTPS = "https://"
