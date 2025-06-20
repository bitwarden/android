package com.x8bit.bitwarden.data.credentials.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for privileged apps.
 */
@Dao
interface PrivilegedAppDao {

    /**
     * A flow of all the trusted privileged apps.
     */
    @Query("SELECT * FROM privileged_apps")
    fun getUserTrustedPrivilegedAppsFlow(): Flow<List<PrivilegedAppEntity>>

    /**
     * Retrieves all the trusted privileged apps.
     */
    @Query("SELECT * FROM privileged_apps")
    suspend fun getAllUserTrustedPrivilegedApps(): List<PrivilegedAppEntity>

    /**
     * Adds a trusted privileged app.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun addTrustedPrivilegedApp(appInfo: PrivilegedAppEntity)

    /**
     * Removes a trusted privileged app.
     */
    @Query(
        "DELETE FROM privileged_apps WHERE package_name = :packageName AND signature = :signature",
    )
    suspend fun removeTrustedPrivilegedApp(packageName: String, signature: String)

    /**
     * Checks if a privileged app is trusted by the user.
     */
    @Suppress("MaxLineLength")
    @Query("SELECT EXISTS(SELECT * FROM privileged_apps WHERE package_name = :packageName AND signature = :signature)")
    suspend fun isPrivilegedAppTrustedByUser(packageName: String, signature: String): Boolean
}
