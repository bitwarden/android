package com.x8bit.bitwarden.data.autofill.fido2.repository

import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.Fido2PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity.Fido2PrivilegedAppInfoEntity
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppAllowListJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val ANDROID_TYPE = "android"
private const val RELEASE_BUILD = "release"

/**
 * Primary implementation of [PrivilegedAppRepository].
 */
class PrivilegedAppRepositoryImpl(
    private val fido2PrivilegedAppDiskSource: Fido2PrivilegedAppDiskSource,
    private val json: Json,
) : PrivilegedAppRepository {

    override val userTrustedPrivilegedAppsFlow: Flow<PrivilegedAppAllowListJson> =
        fido2PrivilegedAppDiskSource.userTrustedPrivilegedAppsFlow
            .map { it.toFido2PrivilegedAppAllowListJson() }

    override suspend fun getAllUserTrustedPrivilegedApps(): PrivilegedAppAllowListJson =
        fido2PrivilegedAppDiskSource.getAllUserTrustedPrivilegedApps()
            .toFido2PrivilegedAppAllowListJson()

    override suspend fun isPrivilegedAppAllowed(
        packageName: String,
        signature: String,
    ): Boolean = fido2PrivilegedAppDiskSource
        .isPrivilegedAppTrustedByUser(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun addTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) = fido2PrivilegedAppDiskSource
        .addTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun removeTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) = fido2PrivilegedAppDiskSource
        .removeTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun getUserTrustedAllowListJson(): String = json
        .encodeToString(
            fido2PrivilegedAppDiskSource
                .getAllUserTrustedPrivilegedApps()
                .toFido2PrivilegedAppAllowListJson(),
        )
}

@Suppress("MaxLineLength")
private fun List<Fido2PrivilegedAppInfoEntity>.toFido2PrivilegedAppAllowListJson() =
    PrivilegedAppAllowListJson(
        apps = map { it.toFido2PrivilegedAppJson() },
    )

private fun Fido2PrivilegedAppInfoEntity.toFido2PrivilegedAppJson() =
    PrivilegedAppAllowListJson.PrivilegedAppJson(
        type = ANDROID_TYPE,
        info = PrivilegedAppAllowListJson.PrivilegedAppJson.PrivilegedAppInfoJson(
            packageName = packageName,
            signatures = listOf(
                PrivilegedAppAllowListJson
                    .PrivilegedAppJson
                    .PrivilegedAppInfoJson
                    .PrivilegedAppSignatureJson(
                        build = RELEASE_BUILD,
                        certFingerprintSha256 = signature,
                    ),
            ),
        ),
    )
