package com.x8bit.bitwarden.data.credentials.repository

import com.x8bit.bitwarden.data.credentials.datasource.disk.PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val ANDROID_TYPE = "android"
private const val RELEASE_BUILD = "release"

/**
 * Primary implementation of [PrivilegedAppRepository].
 */
class PrivilegedAppRepositoryImpl(
    private val privilegedAppDiskSource: PrivilegedAppDiskSource,
    private val json: Json,
) : PrivilegedAppRepository {

    override val userTrustedPrivilegedAppsFlow: Flow<PrivilegedAppAllowListJson> =
        privilegedAppDiskSource.userTrustedPrivilegedAppsFlow
            .map { it.toPrivilegedAppAllowListJson() }

    override suspend fun getAllUserTrustedPrivilegedApps(): PrivilegedAppAllowListJson =
        privilegedAppDiskSource.getAllUserTrustedPrivilegedApps()
            .toPrivilegedAppAllowListJson()

    override suspend fun isPrivilegedAppAllowed(
        packageName: String,
        signature: String,
    ): Boolean = privilegedAppDiskSource
        .isPrivilegedAppTrustedByUser(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun addTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ): Unit = privilegedAppDiskSource
        .addTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun removeTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ): Unit = privilegedAppDiskSource
        .removeTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun getUserTrustedAllowListJson(): String = json
        .encodeToString(
            privilegedAppDiskSource
                .getAllUserTrustedPrivilegedApps()
                .toPrivilegedAppAllowListJson(),
        )
}

private fun List<PrivilegedAppEntity>.toPrivilegedAppAllowListJson() =
    PrivilegedAppAllowListJson(
        apps = map { it.toPrivilegedAppJson() },
    )

private fun PrivilegedAppEntity.toPrivilegedAppJson() =
    PrivilegedAppAllowListJson.PrivilegedAppJson(
        type = ANDROID_TYPE,
        info = PrivilegedAppAllowListJson.PrivilegedAppJson.InfoJson(
            packageName = packageName,
            signatures = listOf(
                PrivilegedAppAllowListJson
                    .PrivilegedAppJson
                    .InfoJson
                    .SignatureJson(
                        build = RELEASE_BUILD,
                        certFingerprintSha256 = signature,
                    ),
            ),
        ),
    )
