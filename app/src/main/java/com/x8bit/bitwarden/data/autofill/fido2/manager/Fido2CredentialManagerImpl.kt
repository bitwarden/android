package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.Context
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
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.platform.util.getAppOrigin
import com.x8bit.bitwarden.data.platform.util.getAppSigningSignatureFingerprintOrNull
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.ui.autofill.fido2.util.createFido2IconCompatFromIconDataOrDefault
import com.x8bit.bitwarden.ui.autofill.fido2.util.createFido2IconCompatFromResource
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Primary implementation of [Fido2CredentialManager].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class Fido2CredentialManagerImpl(
    @ApplicationContext private val context: Context,
    private val vaultSdkSource: VaultSdkSource,
    private val fido2CredentialStore: Fido2CredentialStore,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val environmentRepository: EnvironmentRepository,
    private val intentManager: IntentManager,
    private val fido2OriginManager: Fido2OriginManager,
    private val json: Json,
    dispatcherManager: DispatcherManager,
) : Fido2CredentialManager,
    Fido2CredentialStore by fido2CredentialStore {

    private val ioScope = CoroutineScope(dispatcherManager.io)

    override var isUserVerified: Boolean = false

    override var authenticationAttempts: Int = 0

    override suspend fun registerFido2Credential(
        userId: String,
        fido2CreateCredentialRequest: Fido2CreateCredentialRequest,
        selectedCipherView: CipherView,
    ): Fido2RegisterCredentialResult {
        val requestOptions =
            getPasskeyAttestationOptionsOrNull(fido2CreateCredentialRequest.requestJson)
                ?: return Fido2RegisterCredentialResult.Error

        fido2OriginManager.validateOrigin(
            callingAppInfo = fido2CreateCredentialRequest.callingAppInfo,
            relyingPartyId = requestOptions.relyingParty.id,
        )
            as? Fido2ValidateOriginResult.Success
            ?: return Fido2RegisterCredentialResult.Error

        val clientData = if (fido2CreateCredentialRequest.callingAppInfo.isOriginPopulated()) {
            fido2CreateCredentialRequest
                .callingAppInfo
                .getAppSigningSignatureFingerprintOrNull()
                ?.let { ClientData.DefaultWithCustomHash(hash = it) }
                ?: return Fido2RegisterCredentialResult.Error
        } else {
            ClientData.DefaultWithExtraData(
                androidPackageName = fido2CreateCredentialRequest
                    .callingAppInfo
                    .packageName,
            )
        }
        val assetLinkUrl = fido2CreateCredentialRequest.origin
            ?: "$HTTPS${requestOptions.relyingParty.id}"

        val origin = Origin.Android(
            UnverifiedAssetLink(
                packageName = fido2CreateCredentialRequest.packageName,
                sha256CertFingerprint = fido2CreateCredentialRequest
                    .callingAppInfo
                    .getSignatureFingerprintAsHexString()
                    ?: return Fido2RegisterCredentialResult.Error,
                host = assetLinkUrl.toHostOrPathOrNull()
                    ?: return Fido2RegisterCredentialResult.Error,
                assetLinkUrl = assetLinkUrl,
            ),
        )
        return vaultSdkSource
            .registerFido2Credential(
                request = RegisterFido2CredentialRequest(
                    userId = userId,
                    origin = origin,
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
                onFailure = { Fido2RegisterCredentialResult.Error },
            )
    }

    override fun getUserVerificationRequirementForAssertion(
        request: Fido2CredentialAssertionRequest,
        fallbackRequirement: UserVerificationRequirement,
    ) = getPasskeyAssertionOptionsOrNull(request.requestJson)
        ?.userVerification
        ?: fallbackRequirement

    override fun getUserVerificationRequirementForRegistration(
        request: Fido2CreateCredentialRequest,
        fallbackRequirement: UserVerificationRequirement,
    ) = getPasskeyAttestationOptionsOrNull(request.requestJson)
        ?.authenticatorSelection
        ?.userVerification
        ?: fallbackRequirement

    override fun getPasskeyAttestationOptionsOrNull(
        requestJson: String,
    ): PasskeyAttestationOptions? =
        json.decodeFromStringOrNull<PasskeyAttestationOptions>(requestJson)

    private fun getPasskeyAssertionOptionsOrNull(
        requestJson: String,
    ): PasskeyAssertionOptions? =
        json.decodeFromStringOrNull<PasskeyAssertionOptions>(requestJson)

    override suspend fun getFido2CredentialsForRelyingParty(
        fido2GetCredentialsRequest: Fido2GetCredentialsRequest,
    ): Fido2GetCredentialsResult = withContext(ioScope.coroutineContext) {

        val requestOptions =
            getPasskeyAssertionOptionsOrNull(requestJson = fido2GetCredentialsRequest.requestJson)
                ?: return@withContext Fido2GetCredentialsResult.Error

        val relyingPartyId = requestOptions.relyingPartyId
            ?: return@withContext Fido2GetCredentialsResult.Error

        fido2OriginManager.validateOrigin(
            callingAppInfo = fido2GetCredentialsRequest.callingAppInfo,
            relyingPartyId = relyingPartyId,
        )
            as? Fido2ValidateOriginResult.Success
            ?: return@withContext Fido2GetCredentialsResult.Error

        getFido2Credentials(
            relyingPartyId = relyingPartyId,
            fido2GetCredentialsRequest = fido2GetCredentialsRequest,
            options = requestOptions,
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
        )
    }

    private suspend fun getFido2Credentials(
        relyingPartyId: String,
        fido2GetCredentialsRequest: Fido2GetCredentialsRequest,
        options: PasskeyAssertionOptions,
        baseIconUrl: String,
        isIconLoadingDisabled: Boolean,
    ): Fido2GetCredentialsResult = withContext(ioScope.coroutineContext) {

        val ciphersWithFido2Credentials = fido2CredentialStore
            .findCredentials(
                ids = options.allowCredentials?.map { it.id.toByteArray() },
                ripId = relyingPartyId,
            )
        val decryptResult = vaultRepository
            .getDecryptedFido2CredentialAutofillViews(ciphersWithFido2Credentials)

        when (decryptResult) {
            is DecryptFido2CredentialAutofillViewResult.Error -> {
                Fido2GetCredentialsResult.Error
            }

            is DecryptFido2CredentialAutofillViewResult.Success -> {
                val credentialEntries = decryptResult
                    .fido2CredentialAutofillViews
                    .filter { it.rpId == relyingPartyId }
                    .associate {
                        // Locate the cipher associated with the autofill view
                        ciphersWithFido2Credentials
                            .find { cipher -> cipher.id == it.cipherId }
                            ?.let { cipher -> it to cipher }
                            ?: return@withContext Fido2GetCredentialsResult.Error
                    }
                    .toCredentialEntries(
                        isIconLoadingDisabled = isIconLoadingDisabled,
                        baseIconUrl = baseIconUrl,
                        fido2GetCredentialsRequest = fido2GetCredentialsRequest,
                    )

                Fido2GetCredentialsResult.Success(
                    userId = fido2GetCredentialsRequest.userId,
                    options = fido2GetCredentialsRequest.option,
                    credentialEntries = credentialEntries,
                )
            }
        }
    }

    override suspend fun authenticateFido2Credential(
        request: Fido2CredentialAssertionRequest,
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
        fido2OriginManager.validateOrigin(
            callingAppInfo = callingAppInfo,
            relyingPartyId = relyingPartyId,
        )
            as? Fido2ValidateOriginResult.Success
            ?: return Fido2CredentialAssertionResult.Error

        val selectedCipherView = vaultRepository.ciphersStateFlow.value.data
            ?.let { ciphers -> ciphers.find { it.id == request.cipherId } }
            ?: return Fido2CredentialAssertionResult.Error

        return vaultSdkSource
            .authenticateFido2Credential(
                request = AuthenticateFido2CredentialRequest(
                    userId = request.userId,
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

    override fun hasAuthenticationAttemptsRemaining(): Boolean =
        authenticationAttempts < MAX_AUTHENTICATION_ATTEMPTS

    private fun getOriginUrlFromAssertionOptionsOrNull(requestJson: String) =
        getPasskeyAssertionOptionsOrNull(requestJson)
            ?.relyingPartyId
            ?.let { "$HTTPS$it" }

    private suspend fun Map<Fido2CredentialAutofillView, CipherView>.toCredentialEntries(
        isIconLoadingDisabled: Boolean,
        baseIconUrl: String,
        fido2GetCredentialsRequest: Fido2GetCredentialsRequest,
    ) = map { it.toCredentialEntry(isIconLoadingDisabled, baseIconUrl, fido2GetCredentialsRequest) }

    private suspend fun Map.Entry<Fido2CredentialAutofillView, CipherView>.toCredentialEntry(
        isIconLoadingDisabled: Boolean,
        baseIconUrl: String,
        fido2GetCredentialsRequest: Fido2GetCredentialsRequest,
    ): PublicKeyCredentialEntry {
        val autofillView = key
        val cipherView = value

        val iconCompat = if (isIconLoadingDisabled) {
            context.createFido2IconCompatFromResource(R.drawable.ic_bw_passkey)
        } else {
            cipherView.login
                ?.uris
                ?.toLoginIconData(
                    isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
                    baseIconUrl = baseIconUrl,
                    usePasskeyDefaultIcon = true,
                )
                .let { iconData ->
                    context.createFido2IconCompatFromIconDataOrDefault(
                        iconData = iconData,
                        defaultResourceId = R.drawable.ic_bw_passkey,
                    )
                }
        }

        return PublicKeyCredentialEntry
            .Builder(
                context = context,
                username = autofillView.userNameForUi
                    ?: context.getString(R.string.no_username),
                pendingIntent = intentManager.createFido2GetCredentialPendingIntent(
                    action = GET_PASSKEY_INTENT,
                    userId = fido2GetCredentialsRequest.userId,
                    credentialId = autofillView.credentialId.toString(),
                    requestCode = Random.nextInt(),
                    cipherId = autofillView.cipherId,
                ),
                beginGetPublicKeyCredentialOption = fido2GetCredentialsRequest.option,
            )
            .setAutoSelectAllowed(true)
            .setDisplayName(cipherView.name)
            .setIcon(iconCompat.toIcon(context))
            .build()
    }
}

private const val MAX_AUTHENTICATION_ATTEMPTS = 5
private const val HTTPS = "https://"
