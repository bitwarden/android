package com.x8bit.bitwarden.data.credentials.manager

import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult
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
) : OriginManager {

    override suspend fun validateOrigin(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult {
        return if (callingAppInfo.isOriginPopulated()) {
            validatePrivilegedAppOrigin(callingAppInfo)
        } else {
            validateCallingApplicationAssetLinks(callingAppInfo)
        }
    }

    private suspend fun validateCallingApplicationAssetLinks(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult {
        return digitalAssetLinkService
            .checkDigitalAssetLinksRelations(
                packageName = callingAppInfo.packageName,
                certificateFingerprint = callingAppInfo
                    .getSignatureFingerprintAsHexString()
                    .orEmpty(),
                relation = DELEGATE_PERMISSION_HANDLE_ALL_URLS,
            )
            .fold(
                onSuccess = {
                    if (it.linked) {
                        ValidateOriginResult.Success(null)
                    } else {
                        ValidateOriginResult.Error.PasskeyNotSupportedForApp //TODO doesn't match anymore when passwords are supported
                    }
                },
                onFailure = {
                    ValidateOriginResult.Error.AssetLinkNotFound
                },
            )
    }

    private suspend fun validatePrivilegedAppOrigin(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult {
        val googleAllowListResult =
            validatePrivilegedAppSignatureWithGoogleList(callingAppInfo)
        return when (googleAllowListResult) {
            is ValidateOriginResult.Success -> {
                // Application was found and successfully validated against the Google allow list so
                // we can return the result as the final validation result.
                googleAllowListResult
            }

            is ValidateOriginResult.Error -> {
                // Check the community allow list if the Google allow list failed, and return the
                // result as the final validation result.
                validatePrivilegedAppSignatureWithCommunityList(callingAppInfo)
            }
        }
    }

    private suspend fun validatePrivilegedAppSignatureWithGoogleList(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult =
        validatePrivilegedAppSignatureWithAllowList(
            callingAppInfo = callingAppInfo,
            fileName = GOOGLE_ALLOW_LIST_FILE_NAME,
        )

    private suspend fun validatePrivilegedAppSignatureWithCommunityList(
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult =
        validatePrivilegedAppSignatureWithAllowList(
            callingAppInfo = callingAppInfo,
            fileName = COMMUNITY_ALLOW_LIST_FILE_NAME,
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
