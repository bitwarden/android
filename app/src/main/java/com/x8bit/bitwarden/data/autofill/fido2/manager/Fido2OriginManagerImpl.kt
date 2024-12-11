package com.x8bit.bitwarden.data.autofill.fido2.manager

import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service.DigitalAssetLinkService
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
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

    override suspend fun getPrivilegedAppOriginOrNull(callingAppInfo: CallingAppInfo): String? {
        if (!callingAppInfo.isOriginPopulated()) return null
        return callingAppInfo.getOrigin(getGoogleAllowListOrNull().orEmpty())
            ?: callingAppInfo.getOrigin(getCommunityAllowListOrNull().orEmpty())
                ?.takeUnless { !callingAppInfo.isOriginPopulated() }
    }

    private suspend fun validateCallingApplicationAssetLinks(
        callingAppInfo: CallingAppInfo,
        relyingPartyId: String,
    ): Fido2ValidateOriginResult = digitalAssetLinkService
        .getDigitalAssetLinkForRp(relyingParty = relyingPartyId)
        .onFailure {
            return Fido2ValidateOriginResult.Error.AssetLinkNotFound
        }
        .mapCatching { statements ->
            statements
                .filterMatchingAppStatementsOrNull(
                    rpPackageName = callingAppInfo.packageName,
                )
                ?: return Fido2ValidateOriginResult.Error.ApplicationNotFound
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
                ?: return Fido2ValidateOriginResult.Error.ApplicationFingerprintNotVerified
        }
        .fold(
            onSuccess = {
                Fido2ValidateOriginResult.Success(null)
            },
            onFailure = {
                Fido2ValidateOriginResult.Error.Unknown
            },
        )

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

    private suspend fun getGoogleAllowListOrNull(): String? =
        assetManager
            .readAsset(GOOGLE_ALLOW_LIST_FILE_NAME)
            .onFailure { Timber.e(it, "Failed to read Google allow list.") }
            .getOrNull()

    private suspend fun getCommunityAllowListOrNull(): String? =
        assetManager
            .readAsset(COMMUNITY_ALLOW_LIST_FILE_NAME)
            .onFailure { Timber.e(it, "Failed to read Community allow list.") }
            .getOrNull()
}
