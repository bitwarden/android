package com.x8bit.bitwarden.data.autofill.fido2.manager

import androidx.credentials.provider.CallingAppInfo
import com.bitwarden.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.platform.util.validatePrivilegedApp
import timber.log.Timber

private const val GOOGLE_ALLOW_LIST_FILE_NAME = "fido2_privileged_google.json"
private const val COMMUNITY_ALLOW_LIST_FILE_NAME = "fido2_privileged_community.json"
private const val DELEGATE_PERMISSION_HANDLE_ALL_URLS = "delegate_permission/common.handle_all_urls"

/**
 * Primary implementation of [Fido2OriginManager].
 */
class Fido2OriginManagerImpl(
    private val assetManager: AssetManager,
    private val digitalAssetLinkService: DigitalAssetLinkService,
) : Fido2OriginManager {

    override suspend fun validateOrigin(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult {
        return if (callingAppInfo.isOriginPopulated()) {
            validatePrivilegedAppOrigin(callingAppInfo)
        } else {
            validateCallingApplicationAssetLinks(callingAppInfo)
        }
    }

    private suspend fun validateCallingApplicationAssetLinks(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult {
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
                        Fido2ValidateOriginResult.Success(null)
                    } else {
                        Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp
                    }
                },
                onFailure = {
                    Fido2ValidateOriginResult.Error.AssetLinkNotFound
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
                validatePrivilegedAppSignatureWithCommunityList(callingAppInfo)
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
    ): Fido2ValidateOriginResult =
        validatePrivilegedAppSignatureWithAllowList(
            callingAppInfo = callingAppInfo,
            fileName = COMMUNITY_ALLOW_LIST_FILE_NAME,
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
}
