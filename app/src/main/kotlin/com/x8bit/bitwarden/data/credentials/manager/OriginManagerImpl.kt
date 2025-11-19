package com.x8bit.bitwarden.data.credentials.manager

import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.network.service.DigitalAssetLinkService
import com.bitwarden.ui.platform.base.util.prefixHttpsIfNecessary
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.getAllSignatureFingerprintsAsHexStrings
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
            validatePrivilegedAppOrigin(callingAppInfo)
        } else {
            validateCallingApplicationAssetLinks(relyingPartyId, callingAppInfo)
        }
    }

    private suspend fun validateCallingApplicationAssetLinks(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult {
        val fingerprints = callingAppInfo.getAllSignatureFingerprintsAsHexStrings()

        if (fingerprints.isEmpty()) {
            return ValidateOriginResult.Error.PasskeyNotSupportedForApp
        }

        var assetLinkFound = false

        // Check each fingerprint in the signing certificate history
        return fingerprints
            .firstNotNullOfOrNull { fingerprint ->
                digitalAssetLinkService
                    .checkDigitalAssetLinksRelations(
                        sourceWebSite = relyingPartyId.prefixHttpsIfNecessary(),
                        targetPackageName = callingAppInfo.packageName,
                        targetCertificateFingerprint = fingerprint,
                        relations = listOf(DELEGATE_PERMISSION_HANDLE_ALL_URLS),
                    )
                    .fold(
                        onSuccess = {
                            assetLinkFound = true
                            if (it.linked) {
                                ValidateOriginResult.Success(null)
                            } else {
                                null
                            }
                        },
                        onFailure = {
                            null
                        },
                    )
            }
            ?: if (assetLinkFound) {
                ValidateOriginResult.Error.PasskeyNotSupportedForApp
            } else {
                ValidateOriginResult.Error.AssetLinkNotFound
            }
    }

    private suspend fun validatePrivilegedAppOrigin(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult =
        validatePrivilegedAppSignatureWithGoogleList(callingAppInfo)
            .takeUnless { it is ValidateOriginResult.Error.PrivilegedAppNotAllowed }
            ?: validatePrivilegedAppSignatureWithCommunityList(callingAppInfo)
                .takeUnless { it is ValidateOriginResult.Error.PrivilegedAppNotAllowed }
            ?: validatePrivilegedAppSignatureWithUserTrustList(callingAppInfo)

    private suspend fun validatePrivilegedAppSignatureWithGoogleList(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult =
        validatePrivilegedAppSignatureWithAllowList(
            callingAppInfo = callingAppInfo,
            fileName = GOOGLE_ALLOW_LIST_FILE_NAME,
        )

    private suspend fun validatePrivilegedAppSignatureWithCommunityList(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult = validatePrivilegedAppSignatureWithAllowList(
        callingAppInfo = callingAppInfo,
        fileName = COMMUNITY_ALLOW_LIST_FILE_NAME,
    )

    private suspend fun validatePrivilegedAppSignatureWithUserTrustList(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult = callingAppInfo.validatePrivilegedApp(
        allowList = privilegedAppRepository.getUserTrustedAllowListJson(),
    )

    private suspend fun validatePrivilegedAppSignatureWithAllowList(
        callingAppInfo: CallingAppInfo,
        fileName: String,
    ): ValidateOriginResult =
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
                    ValidateOriginResult.Error.Unknown
                },
            )
}
