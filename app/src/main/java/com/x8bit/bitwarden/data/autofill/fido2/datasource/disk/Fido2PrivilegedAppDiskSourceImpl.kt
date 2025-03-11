package com.x8bit.bitwarden.data.autofill.fido2.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.dao.Fido2PrivilegedAppInfoDao
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity.Fido2PrivilegedAppInfoEntity
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of the [Fido2PrivilegedAppDiskSource] interface.
 */
class Fido2PrivilegedAppDiskSourceImpl(
    private val privilegedAppDao: Fido2PrivilegedAppInfoDao,
    sharedPreferences: SharedPreferences,
) : Fido2PrivilegedAppDiskSource, BaseDiskSource(sharedPreferences = sharedPreferences) {

    override val userTrustedPrivilegedAppsFlow: Flow<List<Fido2PrivilegedAppInfoEntity>> =
        privilegedAppDao.getUserTrustedPrivilegedAppsFlow()

    override suspend fun getAllUserTrustedPrivilegedApps(): List<Fido2PrivilegedAppInfoEntity> {
        return privilegedAppDao.getAllUserTrustedPrivilegedApps()
    }

    override suspend fun isPrivilegedAppTrustedByUser(
        packageName: String,
        signature: String,
    ): Boolean = privilegedAppDao
        .getAllUserTrustedPrivilegedApps()
        .any { it.packageName == packageName && it.signature == signature }

    override suspend fun addTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) {
        privilegedAppDao.addTrustedPrivilegedApp(
            appInfo = Fido2PrivilegedAppInfoEntity(
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
