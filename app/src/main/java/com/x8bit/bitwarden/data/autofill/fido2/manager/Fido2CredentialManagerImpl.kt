package com.x8bit.bitwarden.data.autofill.fido2.manager

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.graphics.drawable.IconCompat
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.BiometricPromptData
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.core.annotation.OmitFromCoverage
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.takeUntilLoaded
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.fido.ClientData
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.fido.Origin
import com.bitwarden.fido.UnverifiedAssetLink
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherView
import com.bumptech.glide.Glide
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAssertionOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.PasskeyAttestationOptions
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.processor.GET_PASSKEY_INTENT
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.util.getAppOrigin
import com.x8bit.bitwarden.data.platform.util.getAppSigningSignatureFingerprint
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.AuthenticateFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.RegisterFido2CredentialRequest
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidAttestationResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.util.toAndroidFido2PublicKeyCredential
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.ui.platform.base.util.prefixHttpsIfNecessaryOrNull
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ExecutionException
import javax.crypto.Cipher
import kotlin.random.Random

/**
 * Primary implementation of [Fido2CredentialManager].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class Fido2CredentialManagerImpl(
    private val context: Context,
    private val vaultSdkSource: VaultSdkSource,
    private val fido2CredentialStore: Fido2CredentialStore,
    private val intentManager: IntentManager,
    private val featureFlagManager: FeatureFlagManager,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val json: Json,
    private val vaultRepository: VaultRepository,
    private val environmentRepository: EnvironmentRepository,
    dispatcherManager: DispatcherManager,
) : Fido2CredentialManager,
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
                        ?: return Fido2CredentialAssertionResult
                            .Error
                            .InvalidAppSignature,
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

    override suspend fun getPublicKeyCredentialEntries(
        userId: String,
        option: BeginGetPublicKeyCredentialOption,
    ): Result<List<CredentialEntry>> = withContext(ioScope.coroutineContext) {
        val options = getPasskeyAssertionOptionsOrNull(option.requestJson)
            ?: return@withContext GetCredentialUnknownException("Invalid data.").asFailure()
        val relyingPartyId = options.relyingPartyId
            ?: return@withContext GetCredentialUnknownException("Invalid data.").asFailure()

        val cipherViews = vaultRepository
            .ciphersStateFlow
            .takeUntilLoaded()
            .fold(initial = emptyList<CipherView>()) { initial, dataState ->
                when (dataState) {
                    is DataState.Loaded -> {
                        dataState.data.filter { it.isActiveWithFido2Credentials }
                    }

                    else -> emptyList()
                }
            }

        if (cipherViews.isEmpty()) {
            return@withContext emptyList<CredentialEntry>().asSuccess()
        }

        val decryptResult = vaultRepository
            .getDecryptedFido2CredentialAutofillViews(cipherViews)
        when (decryptResult) {
            is DecryptFido2CredentialAutofillViewResult.Error -> {
                GetCredentialUnknownException("Error decrypting credentials.")
                    .asFailure()
            }

            is DecryptFido2CredentialAutofillViewResult.Success -> {
                val baseIconUrl = environmentRepository
                    .environment
                    .environmentUrlData
                    .baseIconUrl
                val autofillViews = decryptResult.fido2CredentialAutofillViews
                    .filter { it.rpId == relyingPartyId }
                if (autofillViews.isEmpty()) {
                    return@withContext emptyList<CredentialEntry>().asSuccess()
                }
                val cipherIdsToMatch = autofillViews
                    .map { it.cipherId }
                    .toSet()

                cipherViews
                    .filter { cipherView -> cipherView.id in cipherIdsToMatch }
                    .associateWith { cipherView ->
                        autofillViews.first { it.cipherId == cipherView.id }
                    }
                    .toPublicKeyCredentialEntryList(
                        baseIconUrl = baseIconUrl,
                        userId = userId,
                        option = option,
                    )
                    .asSuccess()
            }
        }
    }

    private suspend fun Map<CipherView, Fido2CredentialAutofillView>.toPublicKeyCredentialEntryList(
        baseIconUrl: String,
        userId: String,
        option: BeginGetPublicKeyCredentialOption,
    ): List<PublicKeyCredentialEntry> = this.map { (cipherView, autofillView) ->
        val loginIconData = cipherView.login
            ?.uris
            .toLoginIconData(
                // TODO: [PM-20176] Enable web icons in passkey credential entries
                // Leave web icons disabled until CredentialManager TransactionTooLargeExceptions
                // are addressed. See https://issuetracker.google.com/issues/355141766 for details.
                isIconLoadingDisabled = true,
                baseIconUrl = baseIconUrl,
                usePasskeyDefaultIcon = true,
            )
        val iconCompat = when (loginIconData) {
            is IconData.Local -> {
                IconCompat.createWithResource(context, loginIconData.iconRes)
            }

            is IconData.Network -> {
                loginIconData.toIconCompat()
            }
        }

        val pkEntryBuilder = PublicKeyCredentialEntry
            .Builder(
                context = context,
                username = autofillView.userNameForUi
                    ?: context.getString(R.string.no_username),
                pendingIntent = intentManager
                    .createFido2GetCredentialPendingIntent(
                        action = GET_PASSKEY_INTENT,
                        userId = userId,
                        credentialId = autofillView.credentialId.toString(),
                        cipherId = autofillView.cipherId,
                        isUserVerified = isUserVerified,
                        requestCode = Random.nextInt(),
                    ),
                beginGetPublicKeyCredentialOption = option,
            )
            .setIcon(iconCompat.toIcon(context))

        if (featureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyAuthentication)) {
            biometricsEncryptionManager
                .getOrCreateCipher(userId)
                ?.let { cipher ->
                    pkEntryBuilder
                        .setBiometricPromptDataIfSupported(cipher = cipher)
                }
        }

        pkEntryBuilder.build()
    }

    private fun PublicKeyCredentialEntry.Builder.setBiometricPromptDataIfSupported(
        cipher: Cipher,
    ): PublicKeyCredentialEntry.Builder =
        if (isBuildVersionBelow(Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
            this
        } else {
            setBiometricPromptData(
                biometricPromptData = buildPromptDataWithCipher(cipher),
            )
        }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun buildPromptDataWithCipher(
        cipher: Cipher,
    ): BiometricPromptData = BiometricPromptData.Builder()
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setCryptoObject(BiometricPrompt.CryptoObject(cipher))
        .build()

    /**
     * Converts a network icon to an [IconCompat]. Performs a blocking network request to fetch the
     * icon, so only call this method from a background thread or coroutine.
     */
    @OmitFromCoverage
    @WorkerThread
    private suspend fun IconData.Network.toIconCompat(): IconCompat = try {
        val futureTargetBitmap = Glide
            .with(context)
            .asBitmap()
            .load(this.uri)
            .placeholder(R.drawable.ic_bw_passkey)
            .submit()

        IconCompat.createWithBitmap(futureTargetBitmap.get())
    } catch (_: ExecutionException) {
        null
    } catch (_: InterruptedException) {
        null
    }
        ?: IconCompat.createWithResource(
            context,
            this.fallbackIconRes,
        )

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
            ?: return Fido2RegisterCredentialResult.Error.InvalidAppSignature

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
