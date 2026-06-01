package com.x8bit.bitwarden.data.credentials.datasource.disk

import com.x8bit.bitwarden.data.credentials.datasource.disk.dao.PrivilegedAppDao
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of the [PrivilegedAppDiskSource] interface.
 */
class PrivilegedAppDiskSourceImpl(
    private val privilegedAppDao: PrivilegedAppDao,
) : PrivilegedAppDiskSource {

    override val userTrustedPrivilegedAppsFlow: Flow<List<PrivilegedAppEntity>> =
        privilegedAppDao.getUserTrustedPrivilegedAppsFlow()

    override suspend fun getAllUserTrustedPrivilegedApps(): List<PrivilegedAppEntity> {
        return privilegedAppDao.getAllUserTrustedPrivilegedApps()
    }

    override suspend fun isPrivilegedAppTrustedByUser(
        packageName: String,
        signature: String,
    ): Boolean = privilegedAppDao
        .isPrivilegedAppTrustedByUser(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun addTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) {
        privilegedAppDao.addTrustedPrivilegedApp(
            appInfo = PrivilegedAppEntity(
                packageName = packageName,
                signature = signature,
            ),
        )
    }

    override suspend fun removeTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) {
        privilegedAppDao.removeTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )
    }
}
