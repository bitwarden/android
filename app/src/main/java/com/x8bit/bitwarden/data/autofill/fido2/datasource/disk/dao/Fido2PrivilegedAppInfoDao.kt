package com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity.Fido2PrivilegedAppInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for trusted privileged apps.
 */
@Dao
interface Fido2PrivilegedAppInfoDao {

    /**
     * A flow of all the trusted privileged apps.
     */
    @Query("SELECT * FROM fido2_privileged_apps")
    fun getUserTrustedPrivilegedAppsFlow(): Flow<List<Fido2PrivilegedAppInfoEntity>>

    /**
     * Retrieves all the trusted privileged apps.
     */
    @Query("SELECT * FROM fido2_privileged_apps")
    suspend fun getAllUserTrustedPrivilegedApps(): List<Fido2PrivilegedAppInfoEntity>

    /**
     * Adds a trusted privileged app.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrustedPrivilegedApp(appInfo: Fido2PrivilegedAppInfoEntity)

    /**
     * Removes a trusted privileged app.
     */
    @Suppress("MaxLineLength")
    @Query("DELETE FROM fido2_privileged_apps WHERE package_name = :packageName AND signature = :signature")
    suspend fun removeTrustedPrivilegedApp(packageName: String, signature: String)
}
