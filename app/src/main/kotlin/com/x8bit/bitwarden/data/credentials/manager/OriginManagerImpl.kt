package com.x8bit.bitwarden.data.credentials.manager

import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.ui.platform.base.util.prefixHttpsIfNecessary
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.platform.util.validatePrivilegedApp
import timber.log.Timber

private const val GOOGLE_ALLOW_LIST_FILE_NAME = "fido2_privileged_google.json"
private const val COMMUNITY_ALLOW_LIST_FILE_NAME = "fido2_privileged_community.json"
private const val DELEGATE_PERMISSION_HANDLE_ALL_URLS = "delegate_permission/common.handle_all_urls"

/**
 * Primary implementation of [OriginManager].
 */
class OriginManagerImpl(
    private val assetManager: AssetManager,
    private val digitalAssetLinkService: DigitalAssetLinkService,
    private val privilegedAppRepository: PrivilegedAppRepository,
) : OriginManager {

    override suspend fun validateOrigin(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult {
        return if (callingAppInfo.isOriginPopulated()) {
            validatePrivilegedAppOrigin(relyingPartyId, callingAppInfo)
        } else {
            validateCallingApplicationAssetLinks(relyingPartyId, callingAppInfo)
        }
    }

    private suspend fun validateCallingApplicationAssetLinks(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult {
        return digitalAssetLinkService
            .checkDigitalAssetLinksRelations(
                sourceWebSite = relyingPartyId.prefixHttpsIfNecessary(),
                targetPackageName = callingAppInfo.packageName,
                targetCertificateFingerprint = callingAppInfo
                    .getSignatureFingerprintAsHexString()
                    .orEmpty(),
                relations = listOf(DELEGATE_PERMISSION_HANDLE_ALL_URLS),
            )
            .fold(
                onSuccess = {
                    Timber.d("Digital asset link validation result: linked = ${it.linked}")
                    if (it.linked) {
                        ValidateOriginResult.Success(null)
                    } else {
                        ValidateOriginResult.Error.PasskeyNotSupportedForApp
                    }
                },
                onFailure = {
                    Timber.e("Failed to validate origin for calling app")
                    ValidateOriginResult.Error.AssetLinkNotFound
                },
            )
    }

    private suspend fun validatePrivilegedAppOrigin(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult =
        validatePrivilegedAppSignatureWithGoogleList(relyingPartyId, callingAppInfo)
            .takeUnless { it is ValidateOriginResult.Error.PrivilegedAppNotAllowed }
            ?: validatePrivilegedAppSignatureWithCommunityList(relyingPartyId, callingAppInfo)
                .takeUnless { it is ValidateOriginResult.Error.PrivilegedAppNotAllowed }
            ?: validatePrivilegedAppSignatureWithUserTrustList(relyingPartyId, callingAppInfo)

    private suspend fun validatePrivilegedAppSignatureWithGoogleList(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult =
        validatePrivilegedAppSignatureWithAllowList(
            relyingPartyId = relyingPartyId,
            callingAppInfo = callingAppInfo,
            fileName = GOOGLE_ALLOW_LIST_FILE_NAME,
            isVerifiedSource = true,
        )

    private suspend fun validatePrivilegedAppSignatureWithCommunityList(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult = validatePrivilegedAppSignatureWithAllowList(
        relyingPartyId = relyingPartyId,
        callingAppInfo = callingAppInfo,
        fileName = COMMUNITY_ALLOW_LIST_FILE_NAME,
        isVerifiedSource = false,
    )

    private suspend fun validatePrivilegedAppSignatureWithUserTrustList(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult = callingAppInfo.validatePrivilegedApp(
        relyingPartyId = relyingPartyId,
        allowList = privilegedAppRepository.getUserTrustedAllowListJson(),
        isVerifiedSource = true,
    )

    private suspend fun validatePrivilegedAppSignatureWithAllowList(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
        fileName: String,
        isVerifiedSource: Boolean,
    ): ValidateOriginResult =
        assetManager
            .readAsset(fileName)
            .mapCatching { allowList ->
                callingAppInfo.validatePrivilegedApp(
                    relyingPartyId = relyingPartyId,
                    allowList = allowList,
                    isVerifiedSource = isVerifiedSource,
                )
            }
            .fold(
                onSuccess = { it },
                onFailure = {
                    Timber.e(it, "Failed to validate calling app is privileged.")
                    ValidateOriginResult.Error.Unknown
                },
            )
}
