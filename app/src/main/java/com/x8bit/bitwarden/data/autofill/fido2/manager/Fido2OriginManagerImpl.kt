package com.x8bit.bitwarden.data.autofill.fido2.manager

import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.fido2.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.platform.util.validatePrivilegedApp
import timber.log.Timber

private const val GOOGLE_ALLOW_LIST_FILE_NAME = "fido2_privileged_google.json"
private const val COMMUNITY_ALLOW_LIST_FILE_NAME = "fido2_privileged_community.json"

/**
 * Primary implementation of [Fido2OriginManager].
 */
@Suppress("TooManyFunctions")
class Fido2OriginManagerImpl(
    private val assetManager: AssetManager,
    private val digitalAssetLinkService: DigitalAssetLinkService,
    private val privilegedAppRepository: PrivilegedAppRepository,
) : Fido2OriginManager {

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

    private suspend fun validateCallingApplicationAssetLinks(
        callingAppInfo: CallingAppInfo,
        relyingPartyId: String,
    ): Fido2ValidateOriginResult {
        return digitalAssetLinkService
            .getDigitalAssetLinkForRp(relyingParty = relyingPartyId)
            .onFailure {
                return Fido2ValidateOriginResult.Error.DigitalAssetLinkError.AssetLinkNotFound
            }
            .mapCatching { statements ->
                statements
                    .filterMatchingAppStatementsOrNull(
                        rpPackageName = callingAppInfo.packageName,
                    )
                    ?: return Fido2ValidateOriginResult
                        .Error
                        .DigitalAssetLinkError
                        .ApplicationNotFound
            }
            .mapCatching { matchingStatements ->
                callingAppInfo
                    .getSignatureFingerprintAsHexString()
                    ?.let { certificateFingerprint ->
                        matchingStatements
                            .filterMatchingAppSignaturesOrNull(
                                signature = certificateFingerprint,
                            )
                    }
                    ?: return Fido2ValidateOriginResult
                        .Error
                        .DigitalAssetLinkError
                        .ApplicationFingerprintNotVerified
            }
            .fold(
                onSuccess = {
                    Fido2ValidateOriginResult.Success(null)
                },
                onFailure = {
                    Fido2ValidateOriginResult.Error.Unknown
                },
            )
    }

    private suspend fun validatePrivilegedAppOrigin(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult {
        val googleAllowListResult =
            validatePrivilegedAppSignatureWithGoogleList(callingAppInfo)
        return when (googleAllowListResult) {
            is Fido2ValidateOriginResult.Success -> {
                // Application was found and successfully validated against the Google allow list so
                // we can return the result as the final validation result.
                googleAllowListResult
            }

            is Fido2ValidateOriginResult.Error -> {
                // Check the community allow list if the Google allow list failed, and return the
                // result as the final validation result.
                // TODO: REVERT BEFORE RELEASE - Switch back to using the community allow list first
                validatePrivilegedAppSignatureWithUserTrustList(callingAppInfo)
                // validatePrivilegedAppSignatureWithCommunityList(callingAppInfo)
            }
        }
    }

    private suspend fun validatePrivilegedAppSignatureWithGoogleList(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult =
        validatePrivilegedAppSignatureWithAllowList(
            callingAppInfo = callingAppInfo,
            fileName = GOOGLE_ALLOW_LIST_FILE_NAME,
        )

    private suspend fun validatePrivilegedAppSignatureWithCommunityList(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult {
        val communityListResult = validatePrivilegedAppSignatureWithAllowList(
            callingAppInfo = callingAppInfo,
            fileName = COMMUNITY_ALLOW_LIST_FILE_NAME,
        )
        return when (communityListResult) {
            is Fido2ValidateOriginResult
            .Error
            .PrivilegedAppError,
                -> {
                validatePrivilegedAppSignatureWithUserTrustList(callingAppInfo)
            }

            else -> communityListResult
        }
    }

    private suspend fun validatePrivilegedAppSignatureWithUserTrustList(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult = callingAppInfo
        .validatePrivilegedApp(
            allowList = privilegedAppRepository.getUserTrustedAllowListJson(),
        )

    private suspend fun validatePrivilegedAppSignatureWithAllowList(
        callingAppInfo: CallingAppInfo,
        fileName: String,
    ): Fido2ValidateOriginResult =
        assetManager
            .readAsset(fileName)
            .mapCatching { allowList ->
                callingAppInfo.validatePrivilegedApp(
                    allowList = allowList,
                )
            }
            .fold(
                onSuccess = { it },
                onFailure = {
                    Timber.e(it, "Failed to validate privileged app: ${callingAppInfo.packageName}")
                    Fido2ValidateOriginResult.Error.Unknown
                },
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
}
